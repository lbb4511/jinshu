#!/bin/bash
set -e

# Markdown文档格式化脚本
# 功能：
# 1. 规范化标题层级
# 2. 自动生成/更新目录
# 3. 检查并修复相对链接
# 4. 列表格式化
# 5. 代码块语言标记统一
# 6. Markdown lint检查

# 参数解析
PATH_ARG=""
CHECK_MODE=false
GENERATE_TOC=false
FIX_LINKS=false
LINT_MODE=false
FIX_MODE=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --check)
            CHECK_MODE=true
            shift
            ;;
        --toc)
            GENERATE_TOC=true
            shift
            ;;
        --links)
            FIX_LINKS=true
            shift
            ;;
        --lint)
            LINT_MODE=true
            shift
            ;;
        --fix)
            FIX_MODE=true
            shift
            ;;
        *)
            PATH_ARG="$1"
            shift
            ;;
    esac
done

if [ -z "$PATH_ARG" ]; then
    echo "错误：请指定要格式化的文件或目录路径"
    exit 1
fi

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查是否安装了必要工具
check_dependencies() {
    local missing=0

    if ! command -v prettier &> /dev/null; then
        log_warn "prettier 未安装，部分格式化功能将不可用"
        log_info "安装命令: npm install -g prettier"
        missing=1
    fi

    if ! command -v markdownlint &> /dev/null; then
        log_warn "markdownlint 未安装，lint检查功能将不可用"
        log_info "安装命令: npm install -g markdownlint-cli"
        missing=1
    fi

    return $missing
}

# 规范化标题层级
normalize_headers() {
    local file="$1"
    local temp_file=$(mktemp)

    # 确保H1只出现一次且在文件开头
    local h1_count=$(grep -c '^# ' "$file" || true)

    if [ "$h1_count" -gt 1 ]; then
        if [ "$FIX_MODE" = true ]; then
            log_info "$file: 发现 $h1_count 个H1标题，自动将多余H1降级为H2"
            # 第一个H1保留，其余H1(#)变成H2(##)
            awk '
                /^# / && !found_first_h1 { found_first_h1=1; print; next }
                /^# / { print "##" substr($0, 2); next }
                { print }
            ' "$file" > "$temp_file"
            mv "$temp_file" "$file"
        else
            log_warn "$file: 发现 $h1_count 个H1标题，使用 --fix 可自动修复"
        fi
    fi

    # 检查标题层级是否正确（不能跳跃）
    local last_level=0
    local line_num=0
    while IFS= read -r line; do
        line_num=$((line_num + 1))
        if [[ "$line" =~ ^(#+)\  ]]; then
            local level=${#BASH_REMATCH[1]}
            if [ $((level - last_level)) -gt 1 ] && [ $last_level -gt 0 ]; then
                log_warn "$file:$line_num: 标题层级跳跃：从 H$last_level 跳到 H$level"
            fi
            last_level=$level
        fi
    done < "$file"
}

# 生成目录
generate_toc() {
    local file="$1"
    local temp_file=$(mktemp)

    log_info "正在生成目录..."

    # 使用Python生成TOC（更好的中文支持）
    python3 - "$file" << 'PYTHON'
import sys
import re

filepath = sys.argv[1]

with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# 生成锚点
def gen_anchor(title):
    anchor = title.lower()
    anchor = re.sub(r'[^\w一-鿿-]', '-', anchor)
    anchor = re.sub(r'-+', '-', anchor)
    anchor = anchor.strip('-')
    return anchor

# 提取所有标题（H2及以下），排除"目录"标题本身
toc_lines = []
for match in re.finditer(r'^(#{2,}) (.*)$', content, re.MULTILINE):
    hashes = match.group(1)
    title = match.group(2)
    if title == '目录':
        continue
    level = len(hashes) - 2
    indent = '  ' * level
    anchor = gen_anchor(title)
    toc_lines.append(f'{indent}- [{title}](#{anchor})')

toc = '## 目录\n\n' + '\n'.join(toc_lines)

# 替换或插入TOC：先删除所有旧TOC，再在第一个H1后面插入新TOC
toc_pattern = r'<!-- TOC START -->.*?<!-- TOC END -->'
toc_replacement = f'<!-- TOC START -->\n\n{toc}\n\n<!-- TOC END -->'

# 删除所有旧TOC标记
content = re.sub(toc_pattern, '', content, flags=re.DOTALL)
# 在第一个H1之后插入新TOC
content = re.sub(r'^# (.*)$', rf'# \1\n\n{toc_replacement}', content, count=1, flags=re.MULTILINE)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
PYTHON

    log_info "目录已生成"
}

# 检查并修复相对链接
fix_links() {
    local file="$1"
    local file_dir=$(dirname "$file")

    log_info "检查链接..."

    # 使用Python更准确地解析markdown链接（支持路径中包含括号）
    python3 - "$file" "$file_dir" << 'PYTHON'
import sys
import re
import os

filepath = sys.argv[1]
file_dir = sys.argv[2]

with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# 匹配 [text](url) 格式的链接
# 注意：url 可能包含括号（如 (ADR)）
for match in re.finditer(r'\[.*?\]\((.*?)\)(?=\s|$)', content, re.MULTILINE):
    link = match.group(1).strip()

    # 跳过外部链接和锚点
    if link.startswith(('http://', 'https://', 'mailto:', '#')):
        continue

    # 移除非路径部分（如 "title" 在 "path "title"" 中）
    if ' "' in link:
        link = link.split(' "')[0]

    abs_path = os.path.normpath(os.path.join(file_dir, link))

    if not os.path.exists(abs_path):
        print(f"[WARN] {filepath}: 链接不存在: {link}")
PYTHON
}

# 使用prettier格式化
format_with_prettier() {
    local file="$1"

    if command -v prettier &> /dev/null; then
        if [ "$CHECK_MODE" = true ]; then
            prettier --check "$file" 2>/dev/null || log_warn "$file: 需要格式化"
        else
            prettier --write "$file" 2>/dev/null
            log_info "$file: 已格式化"
        fi
    fi
}

# 使用markdownlint检查
lint_markdown() {
    local file="$1"

    if command -v markdownlint &> /dev/null; then
        log_info "执行markdownlint检查..."
        markdownlint "$file" || true
    fi
}

# 处理单个文件
process_file() {
    local file="$1"

    log_info "处理文件: $file"

    # 规范化标题
    normalize_headers "$file"

    # 生成目录
    if [ "$GENERATE_TOC" = true ]; then
        generate_toc "$file"
    fi

    # 检查链接
    if [ "$FIX_LINKS" = true ]; then
        fix_links "$file"
    fi

    # 使用prettier格式化
    format_with_prettier "$file"

    # Lint检查
    if [ "$LINT_MODE" = true ]; then
        lint_markdown "$file"
    fi
}

# 主函数
main() {
    check_dependencies || true

    if [ -f "$PATH_ARG" ]; then
        # 处理单个文件
        process_file "$PATH_ARG"
    elif [ -d "$PATH_ARG" ]; then
        # 处理目录下所有md文件
        find "$PATH_ARG" -name "*.md" -type f | while read -r file; do
            process_file "$file"
        done
    else
        log_error "路径不存在: $PATH_ARG"
        exit 1
    fi

    log_info "格式化完成！"
}

main
