#!/bin/bash

# 功能测试脚本 - DataProcessor
# 测试所有功能点

echo "=========================================="
echo "DataProcessor 功能测试"
echo "=========================================="
echo ""

# 设置测试目录
TEST_DIR="/tmp/dataprocessor_test_$$"
INPUT_DIR="$TEST_DIR/raw_data"
OUTPUT_DIR="$TEST_DIR/processed_data"

# 创建测试目录
mkdir -p "$INPUT_DIR"
mkdir -p "$OUTPUT_DIR"

echo "测试目录: $TEST_DIR"
echo ""

# 编译项目
echo "[1/10] 编译项目..."
cd /Users/paul/TraeWorkSpace/EXPDataProcessor2
mvn compile -q
if [ $? -ne 0 ]; then
    echo "编译失败!"
    exit 1
fi
echo "编译成功"
echo ""

# 测试 1: 输入目录不存在
echo "[2/10] 测试: 输入目录不存在时应退出..."
java -cp target/classes com.experiment.DataProcessor --inputDir /non/existent/dir --outputDir "$OUTPUT_DIR" 2>&1
echo "退出码: $?"
echo ""

# 测试 2: 输出目录自动创建
echo "[3/10] 测试: 输出目录不存在时自动创建..."
echo "ERROR: test" > "$INPUT_DIR/test.log"
java -cp target/classes com.experiment.DataProcessor --inputDir "$INPUT_DIR" --outputDir "$TEST_DIR/new_output" 2>&1
if [ -d "$TEST_DIR/new_output" ]; then
    echo "✓ 输出目录已自动创建"
else
    echo "✗ 输出目录未创建"
fi
echo ""

# 测试 3: 文件扩展名过滤
echo "[4/10] 测试: 只处理 .log 和 .data 文件..."
rm -rf "$INPUT_DIR"/* "$OUTPUT_DIR"/*
echo "ERROR: test" > "$INPUT_DIR/valid.log"
echo "10.5" > "$INPUT_DIR/valid.data"
echo "content" > "$INPUT_DIR/invalid.tmp"
echo "content" > "$INPUT_DIR/invalid.bak"
echo "content" > "$INPUT_DIR/noextension"

java -cp target/classes com.experiment.DataProcessor --inputDir "$INPUT_DIR" --outputDir "$OUTPUT_DIR" 2>&1

OUTPUT_COUNT=$(ls -1 "$OUTPUT_DIR" 2>/dev/null | wc -l | tr -d ' ')
if [ "$OUTPUT_COUNT" -eq 2 ]; then
    echo "✓ 正确过滤了文件 (只处理了 2 个有效文件)"
else
    echo "✗ 文件过滤失败 (处理了 $OUTPUT_COUNT 个文件)"
fi
echo ""

# 测试 4: .log 文件处理 - 筛选 ERROR 行
echo "[5/10] 测试: .log 文件处理 - 筛选 ERROR 行..."
rm -rf "$INPUT_DIR"/* "$OUTPUT_DIR"/*
cat > "$INPUT_DIR/app.log" << 'EOF'
INFO: Starting process
ERROR: Connection failed
DEBUG: Debug info
ERROR: Timeout occurred
INFO: Process completed
EOF

java -cp target/classes com.experiment.DataProcessor --inputDir "$INPUT_DIR" --outputDir "$OUTPUT_DIR" 2>&1

DATE_SUFFIX=$(date +%Y%m%d)
OUTPUT_FILE="$OUTPUT_DIR/app_processed_${DATE_SUFFIX}.log"

if [ -f "$OUTPUT_FILE" ]; then
    ERROR_COUNT=$(grep -c "ERROR" "$OUTPUT_FILE" 2>/dev/null || echo 0)
    INFO_COUNT=$(grep -c "INFO" "$OUTPUT_FILE" 2>/dev/null || echo 0)
    TIMESTAMP_COUNT=$(grep -cE '^\[[0-9]{4}-[0-9]{2}-[0-9]{2}' "$OUTPUT_FILE" 2>/dev/null || echo 0)
    
    ERROR_COUNT=$(echo "$ERROR_COUNT" | tr -d ' ')
    INFO_COUNT=$(echo "$INFO_COUNT" | tr -d ' ')
    TIMESTAMP_COUNT=$(echo "$TIMESTAMP_COUNT" | tr -d ' ')
    
    if [ "$ERROR_COUNT" -eq 2 ] && [ "$INFO_COUNT" -eq 0 ] && [ "$TIMESTAMP_COUNT" -eq 2 ]; then
        echo "✓ .log 文件处理正确 (筛选了 ERROR 行并添加了时间戳)"
    else
        echo "✗ .log 文件处理不正确"
        echo "  ERROR 行数: $ERROR_COUNT (期望: 2)"
        echo "  INFO 行数: $INFO_COUNT (期望: 0)"
        echo "  时间戳行数: $TIMESTAMP_COUNT (期望: 2)"
    fi
    echo "  输出内容:"
    cat "$OUTPUT_FILE" | sed 's/^/    /'
else
    echo "✗ 输出文件未创建"
fi
echo ""

# 测试 5: .log 文件处理 - 无 ERROR 行时创建空文件
echo "[6/10] 测试: .log 文件无 ERROR 行时创建空文件..."
rm -rf "$INPUT_DIR"/* "$OUTPUT_DIR"/*
cat > "$INPUT_DIR/clean.log" << 'EOF'
INFO: Starting process
DEBUG: Debug info
INFO: Process completed
EOF

java -cp target/classes com.experiment.DataProcessor --inputDir "$INPUT_DIR" --outputDir "$OUTPUT_DIR" 2>&1

OUTPUT_FILE="$OUTPUT_DIR/clean_processed_${DATE_SUFFIX}.log"
if [ -f "$OUTPUT_FILE" ]; then
    FILE_SIZE=$(stat -f%z "$OUTPUT_FILE" 2>/dev/null || stat -c%s "$OUTPUT_FILE" 2>/dev/null || echo 0)
    FILE_SIZE=$(echo "$FILE_SIZE" | tr -d ' ')
    if [ "$FILE_SIZE" -eq 0 ]; then
        echo "✓ 空文件正确创建"
    else
        echo "✗ 文件不为空 (大小: $FILE_SIZE 字节)"
    fi
else
    echo "✗ 输出文件未创建"
fi
echo ""

# 测试 6: .data 文件处理 - 计算总和和平均值
echo "[7/10] 测试: .data 文件处理 - 计算总和和平均值..."
rm -rf "$INPUT_DIR"/* "$OUTPUT_DIR"/*
cat > "$INPUT_DIR/numbers.data" << 'EOF'
10.5
20.0
30.5
EOF

java -cp target/classes com.experiment.DataProcessor --inputDir "$INPUT_DIR" --outputDir "$OUTPUT_DIR" 2>&1

OUTPUT_FILE="$OUTPUT_DIR/numbers_processed_${DATE_SUFFIX}.data"
if [ -f "$OUTPUT_FILE" ]; then
    CONTENT=$(cat "$OUTPUT_FILE")
    echo "  输出内容: $CONTENT"
    
    if echo "$CONTENT" | grep -q '"sum": 61.00' && echo "$CONTENT" | grep -q '"average": 20.33'; then
        echo "✓ .data 文件处理正确"
    else
        echo "✗ .data 文件处理不正确"
    fi
else
    echo "✗ 输出文件未创建"
fi
echo ""

# 测试 7: .data 文件处理 - 空文件
echo "[8/10] 测试: .data 文件为空时..."
rm -rf "$INPUT_DIR"/* "$OUTPUT_DIR"/*
touch "$INPUT_DIR/empty.data"

java -cp target/classes com.experiment.DataProcessor --inputDir "$INPUT_DIR" --outputDir "$OUTPUT_DIR" 2>&1

OUTPUT_FILE="$OUTPUT_DIR/empty_processed_${DATE_SUFFIX}.data"
if [ -f "$OUTPUT_FILE" ]; then
    CONTENT=$(cat "$OUTPUT_FILE")
    echo "  输出内容: $CONTENT"
    
    if echo "$CONTENT" | grep -q '"sum": 0.00' && echo "$CONTENT" | grep -q '"average": 0.00'; then
        echo "✓ 空 .data 文件处理正确"
    else
        echo "✗ 空 .data 文件处理不正确"
    fi
else
    echo "✗ 输出文件未创建"
fi
echo ""

# 测试 8: 原始文件不被修改
echo "[9/10] 测试: 原始文件不被修改..."
rm -rf "$INPUT_DIR"/* "$OUTPUT_DIR"/*
ORIGINAL_CONTENT="ERROR: Test error
INFO: Test info"
echo "$ORIGINAL_CONTENT" > "$INPUT_DIR/original.log"
BEFORE_MD5=$(md5sum "$INPUT_DIR/original.log" 2>/dev/null || md5 -q "$INPUT_DIR/original.log")
BEFORE_TIME=$(stat -f%m "$INPUT_DIR/original.log" 2>/dev/null || stat -c%Y "$INPUT_DIR/original.log")

sleep 1

java -cp target/classes com.experiment.DataProcessor --inputDir "$INPUT_DIR" --outputDir "$OUTPUT_DIR" 2>&1

AFTER_MD5=$(md5sum "$INPUT_DIR/original.log" 2>/dev/null || md5 -q "$INPUT_DIR/original.log")
AFTER_TIME=$(stat -f%m "$INPUT_DIR/original.log" 2>/dev/null || stat -c%Y "$INPUT_DIR/original.log")
AFTER_CONTENT=$(cat "$INPUT_DIR/original.log")

if [ "$BEFORE_MD5" = "$AFTER_MD5" ] && [ "$BEFORE_TIME" = "$AFTER_TIME" ] && [ "$ORIGINAL_CONTENT" = "$AFTER_CONTENT" ]; then
    echo "✓ 原始文件未被修改"
else
    echo "✗ 原始文件被修改了"
    echo "  MD5 变化: $BEFORE_MD5 -> $AFTER_MD5"
    echo "  时间戳变化: $BEFORE_TIME -> $AFTER_TIME"
fi
echo ""

# 测试 9: 默认路径
echo "[10/10] 测试: 默认路径..."
echo "  默认输入目录: /home/user/experiment/raw_data"
echo "  默认输出目录: /home/user/experiment/processed_data"
echo "  (需要在实际环境中测试)"
echo ""

# 清理
echo "=========================================="
echo "清理测试目录..."
rm -rf "$TEST_DIR"
echo "测试完成!"
echo "=========================================="
