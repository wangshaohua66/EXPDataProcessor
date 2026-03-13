# DataProcessor 测试报告

## 项目信息
- **项目名称**: DataProcessor
- **版本**: 1.0.0
- **测试日期**: 2026-03-13
- **Java 版本**: OpenJDK 11+

---

## 一、单元测试报告

### 1.1 测试概览

| 指标 | 数值 |
|------|------|
| 测试用例总数 | 14 |
| 通过 | 14 |
| 失败 | 0 |
| 错误 | 0 |
| 跳过 | 0 |
| 通过率 | 100% |

### 1.2 测试用例详情

| 序号 | 测试名称 | 描述 | 状态 |
|------|----------|------|------|
| 1 | testInputDirNotExist | 输入目录不存在时应打印错误并退出 | ✓ 通过 |
| 2 | testOutputDirAutoCreate | 输出目录不存在时自动创建 | ✓ 通过 |
| 3 | testFileExtensionFilter | 只处理 .log 和 .data 文件，忽略其他文件 | ✓ 通过 |
| 4 | testLogFileProcessing | .log 文件处理 - 筛选 ERROR 行并添加时间戳 | ✓ 通过 |
| 5 | testLogFileNoErrors | .log 文件处理 - 无 ERROR 行时创建空文件 | ✓ 通过 |
| 6 | testLogFileEmpty | .log 文件处理 - 空文件 | ✓ 通过 |
| 7 | testDataFileProcessing | .data 文件处理 - 计算总和和平均值 | ✓ 通过 |
| 8 | testDataFileEmpty | .data 文件处理 - 空文件 | ✓ 通过 |
| 9 | testDataFileWithNegativeAndFloats | .data 文件处理 - 包含负数和小数 | ✓ 通过 |
| 10 | testDataFileWithEmptyLines | .data 文件处理 - 包含空行 | ✓ 通过 |
| 11 | testOriginalFileNotModified | 原始文件不被修改 | ✓ 通过 |
| 12 | testCommandLineArgs | 命令行参数解析 | ✓ 通过 |
| 13 | testFileWithoutExtension | 无扩展名文件处理 | ✓ 通过 |
| 14 | testCaseInsensitiveExtension | 大小写不敏感的扩展名匹配 | ✓ 通过 |

### 1.3 单元测试执行记录

```
[INFO] Running com.experiment.DataProcessorTest
Processed: mixed.data -> mixed_processed_20260313.data
Processing completed successfully.
Processed: clean.log -> clean_processed_20260313.log
Processing completed successfully.
Processed: app.log -> app_processed_20260313.log
Processing completed successfully.
Processed: empty.data -> empty_processed_20260313.data
Processing completed successfully.
Processed: test.DATA -> test_processed_20260313.DATA
Processed: test.LOG -> test_processed_20260313.LOG
Processing completed successfully.
Processed: test.log -> test_processed_20260313.log
Processing completed successfully.
Skipping file (invalid extension): noextension
Processed: valid.log -> valid_processed_20260313.log
Skipping file (invalid extension): invalid.tmp
Processed: valid.data -> valid_processed_20260313.data
Skipping file (invalid extension): invalid.bak
Processing completed successfully.
Processed: original.log -> original_processed_20260313.log
Processing completed successfully.
Skipping file (invalid extension): noextension
Processing completed successfully.
Processed: numbers.data -> numbers_processed_20260313.data
Processing completed successfully.
Processed: test.log -> test_processed_20260313.log
Processing completed successfully.
Processed: with_empty_lines.data -> with_empty_lines_processed_20260313.data
Processing completed successfully.
Processed: empty.log -> empty_processed_20260313.log
Processing completed successfully.
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
```

---

## 二、功能测试报告

### 2.1 测试概览

| 测试项 | 状态 | 说明 |
|--------|------|------|
| 编译项目 | ✓ 通过 | 项目编译成功 |
| 输入目录不存在 | ✓ 通过 | 程序正确退出，退出码为 1 |
| 输出目录自动创建 | ✓ 通过 | 输出目录已自动创建 |
| 文件扩展名过滤 | ✓ 通过 | 只处理了 2 个有效文件 |
| .log 文件处理 - 筛选 ERROR 行 | ✓ 通过 | 正确筛选 ERROR 行并添加时间戳 |
| .log 文件无 ERROR 行 | ✓ 通过 | 空文件正确创建 |
| .data 文件处理 - 计算 | ✓ 通过 | 总和和平均值计算正确 |
| .data 文件为空 | ✓ 通过 | 空文件处理正确 |
| 原始文件不被修改 | ✓ 通过 | 原始文件未被修改 |
| 默认路径 | - | 需要在实际环境中测试 |

### 2.2 路径约束测试

#### 2.2.1 输入目录不存在
- **测试目的**: 验证当输入目录不存在时程序是否正确退出
- **测试步骤**:
  1. 指定一个不存在的输入目录
  2. 运行程序
- **预期结果**: 程序打印错误信息并退出，退出码为 1
- **实际结果**: ✓ 程序打印 "Error: Input directory does not exist" 并退出，退出码为 1

#### 2.2.2 输出目录自动创建
- **测试目的**: 验证输出目录不存在时自动创建
- **测试步骤**:
  1. 指定一个不存在的输出目录
  2. 运行程序
- **预期结果**: 输出目录自动创建，包括所有父目录
- **实际结果**: ✓ 输出目录已自动创建

### 2.3 文件格式约束测试

#### 2.3.1 文件扩展名过滤
- **测试目的**: 验证只处理 .log 和 .data 文件
- **测试文件**:
  - valid.log (应处理)
  - valid.data (应处理)
  - invalid.tmp (应忽略)
  - invalid.bak (应忽略)
  - noextension (应忽略)
- **预期结果**: 只处理 2 个有效文件
- **实际结果**: ✓ 正确过滤了文件，只处理了 2 个有效文件

### 2.4 .log 文件处理测试

#### 2.4.1 筛选 ERROR 行并添加时间戳
- **输入内容**:
  ```
  INFO: Starting process
  ERROR: Connection failed
  DEBUG: Debug info
  ERROR: Timeout occurred
  INFO: Process completed
  ```
- **预期输出**: 只包含 ERROR 行，每行前添加时间戳 `[yyyy-MM-dd HH:mm:ss]`
- **实际输出**:
  ```
  [2026-03-13 09:42:27] ERROR: Connection failed
  [2026-03-13 09:42:27] ERROR: Timeout occurred
  ```
- **结果**: ✓ 通过
  - ERROR 行数: 2 (期望: 2) ✓
  - INFO 行数: 0 (期望: 0) ✓
  - 时间戳行数: 2 (期望: 2) ✓

#### 2.4.2 无 ERROR 行时创建空文件
- **输入内容**: 只包含 INFO 和 DEBUG 行
- **预期输出**: 空文件
- **实际结果**: ✓ 空文件正确创建

#### 2.4.3 空 .log 文件
- **输入内容**: 空文件
- **预期输出**: 空文件
- **实际结果**: ✓ 空文件正确创建

### 2.5 .data 文件处理测试

#### 2.5.1 计算总和和平均值
- **输入内容**:
  ```
  10.5
  20.0
  30.5
  ```
- **预期输出**: `{"sum": 61.00, "average": 20.33}`
- **实际输出**: `{"sum": 61.00, "average": 20.33}`
- **结果**: ✓ 通过

#### 2.5.2 空 .data 文件
- **输入内容**: 空文件
- **预期输出**: `{"sum": 0.00, "average": 0.00}`
- **实际输出**: `{"sum": 0.00, "average": 0.00}`
- **结果**: ✓ 通过

### 2.6 不可修改区域约束测试

#### 2.6.1 原始文件不被修改
- **测试目的**: 验证程序以只读方式处理原始文件
- **测试步骤**:
  1. 记录原始文件的 MD5 和时间戳
  2. 运行程序
  3. 检查原始文件的 MD5 和时间戳
- **预期结果**: 原始文件内容和元数据保持不变
- **实际结果**: ✓ 原始文件未被修改（MD5 和时间戳均未变化）

### 2.7 命名规范约束测试

- **测试目的**: 验证输出文件命名格式 `<原始文件名>_processed_<日期>.<扩展名>`
- **测试结果**: ✓ 所有输出文件都按照正确的格式命名
  - `app.log` → `app_processed_20260313.log`
  - `numbers.data` → `numbers_processed_20260313.data`

---

## 三、约束条件验证

| 约束条件 | 验证结果 | 说明 |
|----------|----------|------|
| 路径约束 - 输入目录检查 | ✓ 通过 | 输入目录不存在时程序退出 |
| 路径约束 - 输出目录创建 | ✓ 通过 | 输出目录自动创建 |
| 路径约束 - 不修改输入目录 | ✓ 通过 | 原始文件未被修改 |
| 文件格式约束 | ✓ 通过 | 只处理 .log 和 .data 文件 |
| 命名规范约束 | ✓ 通过 | 输出文件名格式正确 |
| 不可修改区域约束 | ✓ 通过 | 只读方式处理原始文件 |
| 处理逻辑 - .log 文件 | ✓ 通过 | 正确筛选 ERROR 行并添加时间戳 |
| 处理逻辑 - .data 文件 | ✓ 通过 | 正确计算总和和平均值 |

---

## 四、测试结论

### 4.1 总体评估

所有测试用例均已通过，程序满足所有需求约束条件：

1. **路径约束**: 程序正确处理输入目录不存在的情况，自动创建输出目录
2. **文件格式约束**: 只处理 .log 和 .data 文件，正确忽略其他文件
3. **命名规范约束**: 输出文件名格式正确
4. **不可修改区域约束**: 以只读方式处理原始文件，不修改输入目录
5. **处理逻辑**: 
   - .log 文件正确筛选 ERROR 行并添加时间戳
   - .data 文件正确计算总和和平均值

### 4.2 测试统计

| 测试类型 | 用例数 | 通过 | 失败 | 通过率 |
|----------|--------|------|------|--------|
| 单元测试 | 14 | 14 | 0 | 100% |
| 功能测试 | 9 | 9 | 0 | 100% |
| **总计** | **23** | **23** | **0** | **100%** |

### 4.3 建议

1. 在实际生产环境中测试默认路径 `/home/user/experiment/raw_data` 和 `/home/user/experiment/processed_data`
2. 考虑添加日志记录功能，便于问题排查
3. 考虑添加配置文件支持，便于灵活配置

---

## 五、附录

### 5.1 运行单元测试

```bash
mvn clean test
```

### 5.2 运行功能测试

```bash
./test_functional.sh
```

### 5.3 编译并运行程序

```bash
# 编译
mvn compile

# 运行（使用默认路径）
java -cp target/classes com.experiment.DataProcessor

# 运行（指定路径）
java -cp target/classes com.experiment.DataProcessor --inputDir /path/to/input --outputDir /path/to/output
```

### 5.4 打包可执行 JAR

```bash
mvn package
# 生成的 JAR 文件位于 target/data-processor-1.0.0-jar-with-dependencies.jar
```
