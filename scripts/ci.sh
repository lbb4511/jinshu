#!/bin/bash
# CI Pipeline Script — 在 DinD 内用 Docker 容器编译+构建
set -e

SHA="${1:-latest}"
WORK_DIR="/tmp/jinshu-ci-$SHA"

echo "=== CI Pipeline @ $SHA ==="

POD=$(kubectl get pod -n gitea -l app=gitea-runner -o jsonpath='{.items[0].metadata.name}')
echo "Using runner pod: $POD"

echo "=== Copying source to dind ==="
kubectl exec -n gitea "$POD" -c dind -- sh -c "
  rm -rf /tmp/ci/backend /tmp/ci/frontend
  mkdir -p /tmp/ci/backend /tmp/ci/frontend
"
tar czf - -C "$WORK_DIR/backend" . | kubectl exec -i -n gitea "$POD" -c dind -- tar xzf - -C /tmp/ci/backend
tar czf - -C "$WORK_DIR/frontend" . | kubectl exec -i -n gitea "$POD" -c dind -- tar xzf - -C /tmp/ci/frontend

echo "=== Backend: compile (cached) ==="
kubectl exec -n gitea "$POD" -c dind -- sh -c "
  docker run --rm -v /tmp/ci/backend:/app -w /app \
    jinshu/ci-backend \
    gradle compileJava --no-daemon -x test
"

echo "=== Frontend: build (cached) ==="
kubectl exec -n gitea "$POD" -c dind -- sh -c "
  docker run --rm -v /tmp/ci/frontend:/app -w /app \
    jinshu/ci-frontend \
    sh -c 'cp -a /node_modules-cache . && pnpm build'
"

echo "=== CI passed ==="
rm -rf "$WORK_DIR"
