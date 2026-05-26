#!/bin/bash
# Deploy Script — 在 DinD 内构建 Docker 镜像并部署到 K3s
# 用法: deploy-jinshu.sh <sha> [tag]

set -e

REGISTRY="${REGISTRY:-gitea-http.gitea.svc.cluster.local:3000}"
BACKEND_IMAGE="$REGISTRY/admin/jinshu-backend"
FRONTEND_IMAGE="$REGISTRY/admin/jinshu-frontend"
NAMESPACE="${NAMESPACE:-jinshu}"
GITEA_SHA="${1:-latest}"
GITEA_TAG="${2:-}"
WORK_DIR="/tmp/jinshu"

echo "=== Deploying jinshu @ $GITEA_SHA ==="

# Use GITEA_TOKEN from environment for registry login
if [ -z "$GITEA_TOKEN" ]; then
  echo "ERROR: GITEA_TOKEN not set"
  exit 1
fi

# No source at WORK_DIR? Clone from Gitea
if [ ! -d "$WORK_DIR/backend" ]; then
  echo "Source not found at $WORK_DIR, cloning from Gitea..."
  rm -rf "$WORK_DIR"
  git clone http://git:${GITEA_TOKEN}@${GITEA_URL#http://}/admin/jinshu.git "$WORK_DIR"
  cd "$WORK_DIR"
  git checkout "$GITEA_SHA"
fi

# Pick first runner pod with a running dind container
POD=$(kubectl get pod -n gitea -l app=gitea-runner -o jsonpath='{.items[0].metadata.name}')
if [ -z "$POD" ]; then
  echo "ERROR: No runner pod found in gitea namespace"
  exit 1
fi
echo "Using runner pod: $POD"

# Copy source into dind container
echo "Copying source to dind container..."
kubectl exec -n gitea "$POD" -c dind -- sh -c "rm -rf /tmp/backend /tmp/frontend && mkdir -p /tmp/backend /tmp/frontend"

tar czf - -C "$WORK_DIR/backend" . | kubectl exec -i -n gitea "$POD" -c dind -- tar xzf - -C /tmp/backend
tar czf - -C "$WORK_DIR/frontend" . | kubectl exec -i -n gitea "$POD" -c dind -- tar xzf - -C /tmp/frontend

# Login to registry
echo "=== Login to $REGISTRY ==="
kubectl exec -n gitea "$POD" -c dind -- sh <<DOCKERCMD
  echo "$GITEA_TOKEN" | docker login -u admin --password-stdin $REGISTRY
DOCKERCMD

# Build backend
echo "=== Building backend ==="
kubectl exec -n gitea "$POD" -c dind -- sh -c "
  echo 'Building backend...'
  docker build -t $BACKEND_IMAGE:$GITEA_SHA /tmp/backend
  docker tag $BACKEND_IMAGE:$GITEA_SHA $BACKEND_IMAGE:latest
  echo 'Pushing backend...'
  docker push $BACKEND_IMAGE:$GITEA_SHA
  docker push $BACKEND_IMAGE:latest
"

# Build frontend
echo "=== Building frontend ==="
kubectl exec -n gitea "$POD" -c dind -- sh -c "
  echo 'Building frontend...'
  docker build -t $FRONTEND_IMAGE:$GITEA_SHA /tmp/frontend
  docker tag $FRONTEND_IMAGE:$GITEA_SHA $FRONTEND_IMAGE:latest
  echo 'Pushing frontend...'
  docker push $FRONTEND_IMAGE:$GITEA_SHA
  docker push $FRONTEND_IMAGE:latest
"

# Deploy
echo "=== Deploying ==="
# K3s containerd uses host DNS, can't resolve .svc.cluster.local
# Use NodePort ClusterIP for deployment image references
DEPLOY_REGISTRY="10.43.170.83:3000"
sed "s|image: .*/jinshu-backend:.*|image: $DEPLOY_REGISTRY/admin/jinshu-backend:$GITEA_SHA|g" "$WORK_DIR/k8s/deployment.yaml" | \
  sed "s|image: .*/jinshu-frontend:.*|image: $DEPLOY_REGISTRY/admin/jinshu-frontend:$GITEA_SHA|g" | \
  kubectl apply -f -

echo "=== Waiting for backend rollout ==="
kubectl rollout status deployment/jinshu-backend -n "$NAMESPACE" --timeout=120s

echo "=== Waiting for frontend rollout ==="
kubectl rollout status deployment/jinshu-frontend -n "$NAMESPACE" --timeout=60s

echo "=== Current pods ==="
kubectl get pods -n "$NAMESPACE"

echo "=== Deploy complete ==="
