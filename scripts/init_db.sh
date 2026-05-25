#!/bin/bash

set -euo pipefail

echo "正在初始化锦书报表系统数据库..."

DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-jinshu}
DB_USER=${DB_USER:-jinshu}
DB_PASSWORD=${DB_PASSWORD:-jinshu123}

export PGPASSWORD=$DB_PASSWORD

psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres \
    -c "CREATE DATABASE $DB_NAME;" 2>/dev/null || true

psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
    -v ON_ERROR_STOP=1 \
    -f sql/ddl/01_init_schema.sql

echo "数据库初始化完成！"
