#!/bin/bash
set -e

REGISTRY="10.43.170.83:3000"
BACKEND_IMAGE="$REGISTRY/admin/jinshu-backend"
WORKER_IMAGE="$REGISTRY/admin/jinshu-worker"
BATCH_IMAGE="$REGISTRY/admin/jinshu-batch"
FRONTEND_IMAGE="$REGISTRY/admin/jinshu-frontend"
RENDERER_IMAGE="$REGISTRY/admin/jinshu-renderer"
NAMESPACE="jinshu"
GITEA_SHA="${1:-latest}"
WORK_DIR="/tmp/jinshu"

echo "=== Deploying jinshu @ $GITEA_SHA ==="

if [ ! -d "$WORK_DIR/backend" ]; then
  echo "Source not found at $WORK_DIR, cloning from Gitea..."
  rm -rf "$WORK_DIR"
  git clone http://gitea-http.gitea.svc.cluster.local:3000/admin/jinshu.git "$WORK_DIR"
  cd "$WORK_DIR"
  git checkout "$GITEA_SHA"
fi

POD=$(kubectl get pod -n gitea -l app=gitea-runner -o jsonpath='{.items[0].metadata.name}')
if [ -z "$POD" ]; then
  echo "ERROR: No runner pod found in gitea namespace"
  exit 1
fi
echo "Using runner pod: $POD"

echo "Copying source to dind container..."
kubectl exec -n gitea "$POD" -c dind -- sh -c "
  rm -rf /tmp/backend /tmp/frontend /tmp/renderer
  mkdir -p /tmp/backend /tmp/frontend /tmp/renderer
"

tar czf - -C "$WORK_DIR/backend" . | kubectl exec -i -n gitea "$POD" -c dind -- tar xzf - -C /tmp/backend
tar czf - -C "$WORK_DIR/frontend" . | kubectl exec -i -n gitea "$POD" -c dind -- tar xzf - -C /tmp/frontend
tar czf - -C "$WORK_DIR/renderer" . | kubectl exec -i -n gitea "$POD" -c dind -- tar xzf - -C /tmp/renderer

# Build all images inside dind
kubectl exec -n gitea "$POD" -c dind -- sh -c "
  docker login -u admin -p admin123 $REGISTRY 2>/dev/null

  echo '=== Building backend ==='
  docker build -t $BACKEND_IMAGE:$GITEA_SHA /tmp/backend
  docker tag $BACKEND_IMAGE:$GITEA_SHA $BACKEND_IMAGE:latest
  docker push $BACKEND_IMAGE:$GITEA_SHA
  docker push $BACKEND_IMAGE:latest

  echo '=== Building worker ==='
  docker build -f /tmp/backend/Dockerfile.worker -t $WORKER_IMAGE:$GITEA_SHA /tmp/backend
  docker tag $WORKER_IMAGE:$GITEA_SHA $WORKER_IMAGE:latest
  docker push $WORKER_IMAGE:$GITEA_SHA
  docker push $WORKER_IMAGE:latest

  echo '=== Building batch ==='
  docker build -f /tmp/backend/Dockerfile.batch -t $BATCH_IMAGE:$GITEA_SHA /tmp/backend
  docker tag $BATCH_IMAGE:$GITEA_SHA $BATCH_IMAGE:latest
  docker push $BATCH_IMAGE:$GITEA_SHA
  docker push $BATCH_IMAGE:latest

  echo '=== Building frontend ==='
  docker build -t $FRONTEND_IMAGE:$GITEA_SHA /tmp/frontend
  docker tag $FRONTEND_IMAGE:$GITEA_SHA $FRONTEND_IMAGE:latest
  docker push $FRONTEND_IMAGE:$GITEA_SHA
  docker push $FRONTEND_IMAGE:latest

  echo '=== Building renderer ==='
  docker build -t $RENDERER_IMAGE:$GITEA_SHA /tmp/renderer
  docker tag $RENDERER_IMAGE:$GITEA_SHA $RENDERER_IMAGE:latest
  docker push $RENDERER_IMAGE:$GITEA_SHA
  docker push $RENDERER_IMAGE:latest
"

# Deploy — apply kustomize with image tags
echo "=== Deploying with kustomize ==="
cd "$WORK_DIR/k8s"
kubectl apply -k .

# Patch images with specific SHA tags
kubectl set image deployment/jinshu-backend -n "$NAMESPACE" \
  "backend=$BACKEND_IMAGE:$GITEA_SHA"
kubectl set image deployment/jinshu-worker -n "$NAMESPACE" \
  "worker=$WORKER_IMAGE:$GITEA_SHA"
kubectl set image deployment/jinshu-batch -n "$NAMESPACE" \
  "batch=$BATCH_IMAGE:$GITEA_SHA"
kubectl set image deployment/jinshu-frontend -n "$NAMESPACE" \
  "frontend=$FRONTEND_IMAGE:$GITEA_SHA"
kubectl set image deployment/jinshu-renderer -n "$NAMESPACE" \
  "renderer=$RENDERER_IMAGE:$GITEA_SHA"

echo "=== Waiting for rollouts ==="
kubectl rollout status deployment/jinshu-backend -n "$NAMESPACE" --timeout=120s || true
kubectl rollout status deployment/jinshu-worker -n "$NAMESPACE" --timeout=120s || true
kubectl rollout status deployment/jinshu-batch -n "$NAMESPACE" --timeout=120s || true
kubectl rollout status deployment/jinshu-frontend -n "$NAMESPACE" --timeout=60s || true
kubectl rollout status deployment/jinshu-renderer -n "$NAMESPACE" --timeout=120s || true

echo "=== Current pods ==="
kubectl get pods -n "$NAMESPACE"

echo "=== Deploy complete ==="
