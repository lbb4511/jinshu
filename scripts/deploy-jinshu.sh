#!/bin/bash
# CD Script — 在 DinD 内构建 Docker 镜像并部署全部服务到 K3s
# 用法: deploy-jinshu.sh <sha> [tag]

set -e

REGISTRY="${REGISTRY:-gitea-http.gitea.svc.cluster.local:3000}"
BACKEND_IMAGE="$REGISTRY/admin/jinshu-backend"
WORKER_IMAGE="$REGISTRY/admin/jinshu-worker"
BATCH_IMAGE="$REGISTRY/admin/jinshu-batch"
FRONTEND_IMAGE="$REGISTRY/admin/jinshu-frontend"
RENDERER_IMAGE="$REGISTRY/admin/jinshu-renderer"
NAMESPACE="${NAMESPACE:-jinshu}"
GITEA_SHA="${1:-latest}"
GITEA_TAG="${2:-}"
WORK_DIR="/tmp/jinshu"

if [ -z "$GITEA_TOKEN" ]; then
  echo "ERROR: GITEA_TOKEN not set"
  exit 1
fi

echo "=== Deploying jinshu @ $GITEA_SHA ==="

# No source at WORK_DIR? Clone from Gitea
if [ ! -d "$WORK_DIR/backend" ]; then
  echo "Source not found at $WORK_DIR, cloning from Gitea..."
  rm -rf "$WORK_DIR"
  git clone "http://git:${GITEA_TOKEN}@${GITEA_URL#http://}/admin/jinshu.git" "$WORK_DIR"
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
kubectl exec -n gitea "$POD" -c dind -- sh -c "
  rm -rf /tmp/backend /tmp/frontend /tmp/renderer
  mkdir -p /tmp/backend /tmp/frontend /tmp/renderer
"

tar czf - -C "$WORK_DIR/backend" . | kubectl exec -i -n gitea "$POD" -c dind -- tar xzf - -C /tmp/backend
tar czf - -C "$WORK_DIR/frontend" . | kubectl exec -i -n gitea "$POD" -c dind -- tar xzf - -C /tmp/frontend
tar czf - -C "$WORK_DIR/renderer" . | kubectl exec -i -n gitea "$POD" -c dind -- tar xzf - -C /tmp/renderer

# Login to registry
echo "=== Login to $REGISTRY ==="
kubectl exec -n gitea "$POD" -c dind -- sh <<DOCKERCMD
  echo "$GITEA_TOKEN" | docker login -u admin --password-stdin $REGISTRY
DOCKERCMD

# Build backend (includes api, worker, batch multi-stage Dockerfiles)
echo "=== Building backend images ==="
kubectl exec -n gitea "$POD" -c dind -- sh -c "
  echo 'Building backend...'
  DOCKER_BUILDKIT=1 docker build -t $BACKEND_IMAGE:$GITEA_SHA /tmp/backend
  docker tag $BACKEND_IMAGE:$GITEA_SHA $BACKEND_IMAGE:latest
  echo 'Pushing backend...'
  docker push $BACKEND_IMAGE:$GITEA_SHA
  docker push $BACKEND_IMAGE:latest

  echo 'Building worker...'
  DOCKER_BUILDKIT=1 docker build -f /tmp/backend/Dockerfile.worker -t $WORKER_IMAGE:$GITEA_SHA /tmp/backend
  docker tag $WORKER_IMAGE:$GITEA_SHA $WORKER_IMAGE:latest
  echo 'Pushing worker...'
  docker push $WORKER_IMAGE:$GITEA_SHA
  docker push $WORKER_IMAGE:latest

  echo 'Building batch...'
  DOCKER_BUILDKIT=1 docker build -f /tmp/backend/Dockerfile.batch -t $BATCH_IMAGE:$GITEA_SHA /tmp/backend
  docker tag $BATCH_IMAGE:$GITEA_SHA $BATCH_IMAGE:latest
  echo 'Pushing batch...'
  docker push $BATCH_IMAGE:$GITEA_SHA
  docker push $BATCH_IMAGE:latest
"

# Build frontend
echo "=== Building frontend ==="
kubectl exec -n gitea "$POD" -c dind -- sh -c "
  echo 'Building frontend...'
  DOCKER_BUILDKIT=1 docker build -t $FRONTEND_IMAGE:$GITEA_SHA /tmp/frontend
  docker tag $FRONTEND_IMAGE:$GITEA_SHA $FRONTEND_IMAGE:latest
  echo 'Pushing frontend...'
  docker push $FRONTEND_IMAGE:$GITEA_SHA
  docker push $FRONTEND_IMAGE:latest
"

# Build renderer
echo "=== Building renderer ==="
kubectl exec -n gitea "$POD" -c dind -- sh -c "
  echo 'Building renderer...'
  DOCKER_BUILDKIT=1 docker build -t $RENDERER_IMAGE:$GITEA_SHA /tmp/renderer
  docker tag $RENDERER_IMAGE:$GITEA_SHA $RENDERER_IMAGE:latest
  echo 'Pushing renderer...'
  docker push $RENDERER_IMAGE:$GITEA_SHA
  docker push $RENDERER_IMAGE:latest
"

# Deploy
echo "=== Deploying ==="
DEPLOY_REGISTRY="10.43.170.83:3000"
cd "$WORK_DIR/k8s"

# Apply base manifests
kubectl apply -k .

# Patch all deployments with specific SHA tags
kubectl set image deployment/jinshu-backend -n "$NAMESPACE" "backend=$DEPLOY_REGISTRY/admin/jinshu-backend:$GITEA_SHA"
kubectl set image deployment/jinshu-worker -n "$NAMESPACE" "worker=$DEPLOY_REGISTRY/admin/jinshu-worker:$GITEA_SHA"
kubectl set image deployment/jinshu-batch -n "$NAMESPACE" "batch=$DEPLOY_REGISTRY/admin/jinshu-batch:$GITEA_SHA"
kubectl set image deployment/jinshu-frontend -n "$NAMESPACE" "frontend=$DEPLOY_REGISTRY/admin/jinshu-frontend:$GITEA_SHA"
kubectl set image deployment/jinshu-renderer -n "$NAMESPACE" "renderer=$DEPLOY_REGISTRY/admin/jinshu-renderer:$GITEA_SHA"

echo "=== Waiting for rollouts ==="
kubectl rollout status deployment/jinshu-backend -n "$NAMESPACE" --timeout=120s
kubectl rollout status deployment/jinshu-worker -n "$NAMESPACE" --timeout=120s
kubectl rollout status deployment/jinshu-batch -n "$NAMESPACE" --timeout=120s
kubectl rollout status deployment/jinshu-frontend -n "$NAMESPACE" --timeout=60s
kubectl rollout status deployment/jinshu-renderer -n "$NAMESPACE" --timeout=120s

echo "=== Current pods ==="
kubectl get pods -n "$NAMESPACE"

echo "=== Deploy complete ==="
