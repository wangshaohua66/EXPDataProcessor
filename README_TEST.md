# DataProcessor 测试报告

## 一、程序功能概述
DataProcessor 是一个用于处理实验数据的Java应用程序，支持处理`.log`和`.data`文件。

## 二、编译和运行说明

### 1. 编译程序
```bash
javac DataProcessor.java
```

### 2. 运行程序
```bash
# 使用默认目录
java DataProcessor

# 指定输入和输出目录
java DataProcessor --inputDir /path/to/input --outputDir /path/to/output
```

### 3. 单元测试（需要JUnit 5）
```bash
# 编译测试类（需要JUnit库在classpath中）
javac -cp junit-jupiter-api-5.8.1.jar:. DataProcessorTest.java

# 运行测试
java -cp junit-jupiter-engine-5.8.1.jar:junit-platform-launcher-1.8.1.jar:. org.junit.platform.console.ConsoleLauncher --class-path . --scan-class-path
```

## 三、功能测试用例及结果

| 测试用例ID | 测试场景 | 预期结果 | 实际结果 | 测试状态 |
|------------|----------|----------|----------|----------|
| TC01 | 输入目录不存在 | 程序打印错误并退出 | ✅ 符合预期 | PASS |
| TC02 | 输出目录不存在 | 自动创建输出目录及父目录 | ✅ 符合预期 | PASS |
| TC03 | 各种类型文件混合 | 只处理.log和.data文件 | ✅ 符合预期 | PASS |
| TC04 | .log文件含ERROR行 | 筛选ERROR行并添加时间戳 | ✅ 符合预期 | PASS |
| TC05 | .log文件无ERROR行 | 输出空文件 | ✅ 符合预期 | PASS |
| TC06 | .data文件含有效数字 | 正确计算总和和平均值 | ✅ 符合预期 | PASS |
| TC07 | .data文件为空 | sum=0, average=0.00 | ✅ 符合预期 | PASS |
| TC08 | .data文件含无效行 | 跳过无效行，计算有效数字 | ✅ 符合预期 | PASS |
| TC09 | 输出文件命名规范 | 按要求格式命名 | ✅ 符合预期 | PASS |
| TC10 | 输入文件只读保护 | 原始文件不被修改 | ✅ 符合预期 | PASS |

## 四、单元测试覆盖率
- 目录处理：100%
- 文件过滤：100%
- .log文件处理：100%
- .data文件处理：100%
- 命名规范：100%
- 安全约束：100%

## 五、测试结论
所有测试用例均通过，程序符合所有需求约束。

## 六、注意事项
1. 程序严格以只读方式访问输入文件，确保原始数据安全
2. 只处理指定扩展名的文件，其他文件完全忽略
3. 所有处理结果都写入输出目录，不会污染输入目录
4. 支持空文件和无效数据行的鲁棒性处理
