#!/bin/bash
set -e

REGISTRY="10.43.170.83:3000"
BACKEND_IMAGE="$REGISTRY/admin/jinshu-backend"
FRONTEND_IMAGE="$REGISTRY/admin/jinshu-frontend"
NAMESPACE="jinshu"
GITEA_SHA="${1:-latest}"
WORK_DIR="/tmp/jinshu"

echo "=== Deploying jinshu @ $GITEA_SHA ==="

# Use WORK_DIR if it exists (CI sends code here via tar pipe)
# Otherwise clone from Gitea as fallback
if [ ! -d "$WORK_DIR/backend" ]; then
  echo "Source not found at $WORK_DIR, cloning from Gitea..."
  rm -rf "$WORK_DIR"
  git clone http://gitea-http.gitea.svc.cluster.local:3000/admin/jinshu.git "$WORK_DIR"
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

# Build backend
echo "=== Building backend ==="
kubectl exec -n gitea "$POD" -c dind -- sh -c "
  docker login -u admin -p admin123 $REGISTRY 2>/dev/null
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
sed "s|image: $BACKEND_IMAGE:latest|image: $BACKEND_IMAGE:$GITEA_SHA|g" "$WORK_DIR/k8s/deployment.yaml" | \
  sed "s|image: $FRONTEND_IMAGE:latest|image: $FRONTEND_IMAGE:$GITEA_SHA|g" | \
  kubectl apply -f -

echo "=== Waiting for backend rollout ==="
kubectl rollout status deployment/jinshu-backend -n "$NAMESPACE" --timeout=120s

echo "=== Waiting for frontend rollout ==="
kubectl rollout status deployment/jinshu-frontend -n "$NAMESPACE" --timeout=60s

echo "=== Current pods ==="
kubectl get pods -n "$NAMESPACE"

echo "=== Deploy complete ==="
