# smart-mybatis
> 智能化的轻量 MyBatis 增强库，主打零 XML CRUD、Lambda 条件构造与自动建模 —— 🇨🇳 默认中文 · [English](#english-version)

## 项目简介
Smart MyBatis 通过 `SmartMapper` 基类、`Where` Lambda DSL 以及自动配置 Starter，统一了实体、Mapper 与 SQL 之间的映射。它是一个对 MyBatis 的增强组件，而非二次封装：代码完全不依赖 MyBatis 的内部实现、也不会代理 MyBatis 的类，所有能力都围绕增强 CRUD 效率展开，保持 MyBatis 原生功能零侵入。

## 关键特性
- **轻量增强**：与 MyBatis 依赖解耦，既不打包 MyBatis 也不替换其任何 Bean，需手动引入官方 MyBatis 依赖，确保升级与排障体验与原生一致。
- **统一 Mapper API**：所有 Mapper 只需继承 `SmartMapper<T extends PO>` 即可拥有 `insert/select/count/executeSql` 等通用方法，同时保留自定义 SQL 能力。
- **Lambda 条件 DSL**：借助 `Where.where().and(Student::getName).like(keyword)` 等语法构造安全的条件表达式，自动处理占位符、防止 SQL 注入。
- **主键与表结构管理**：支持 `AUTO/INPUT/UUID/SNOWFLAKE/SNOWFLAKE_HEX` 等主键策略，可按命名规范自动推导表名、列名，并在 `auto-sync-db=true` 时增量同步字段。
- **Spring Boot Starter**：`spring-boot-starter-smart-mybatis` 自动注册 `SmartMapper`、绑定 `spring.mybatis.smart.*` 配置，并允许自定义 `SmartMapperInitializer`。
- **示例工程**：`spring-boot-starter-smart-mybatis-example` 演示了实体到 REST 应用的闭环，开箱即跑通 MySQL。

## 目录结构
```
smart-mybatis/
├── core/                               # Lambda DSL、SQL Provider、命名工具
├── spring-boot-starter-smart-mybatis/  # 自动配置与配置绑定
└── spring-boot-starter-smart-mybatis-example/
    ├── src/main/java/ink/icoding/...   # Student 实体、Mapper、Service 示例
    └── src/main/resources/application.yaml
```

## 它解决了什么问题
- 摆脱大量 XML Mapper 与模板化 CRUD 代码。
- Lambda 条件让字段编译时可追踪，避免硬编码列名。
- 通过命名规范与表前缀配置，无需手写 `@TableName`/`@TableField` 也能生成语义一致的 SQL。
- `auto-sync-db` 支持按实体自动新增字段，减少手写 `ALTER TABLE`。
- **保持原生能力**：Smart MyBatis 不会接管 SqlSession、MapperFactoryBean 等核心组件，任何 MyBatis 插件、XML 自定义语句都可照常使用。

## 轻量增强原则
1. **依赖解耦**：`core/` 模块不引用 MyBatis 代码，只提供 SQL 构建能力，真正的 MyBatis 依赖需由业务自行添加，例如：
   ```xml
   <dependency>
     <groupId>org.mybatis</groupId>
     <artifactId>mybatis-spring-boot-starter</artifactId>
     <version>3.0.3</version>
   </dependency>
   ```
2. **零替换零代理**：不代理 MyBatis Mapper、不包裹 `SqlSessionFactory`，Starter 仅在 Spring 环境中注册 SmartMapper 并调用官方 API。
3. **按需可拔插**：关闭 `spring.mybatis.smart.enabled` 即刻回退为纯 MyBatis，所有 Smart MyBatis API 都是可选的增益层。

## 架构概览
- **Core**：`MapperUtil` 解析实体元数据、构建 `MapperDeclaration`，`BaseSqlProvider` 依据声明动态生成 SQL，`Where/C` 定义条件树。
- **Starter**：`SmartMybatisInitializer` 在应用启动期绑定配置到 `SmartConfigHolder`，`SmartMybatisAutoConfiguration` 注册 `SmartMapper` 并在 `BeanPostProcessor` 中触发 `SmartMapperInitializer.initMapper`。
- **Example**：`StudentServiceImpl` 展示 DSL 查询、自动插入与 `@PostConstruct` 自检。
- **数据流**：实体定义 → `SmartMapper` 泛型推断 → `MapperUtil` 缓存声明 → `BaseSqlProvider` 输出 SQL → MyBatis 执行 → 可选 `auto-sync-db` 调整表。

## 快速开始(Spring Boot)
> 假设你的项目为 [spring-boot-starter-smart-mybatis-example](spring-boot-starter-smart-mybatis-example) 示例应用, 您可以在这里找示例代码。
1. 在你的项目中确保显式引入官方 MyBatis 依赖 以及本库：
   ```xml
   <!-- Mybatis -->
   <dependency>
     <groupId>org.mybatis</groupId>
     <artifactId>mybatis-spring-boot-starter</artifactId>
     <version>3.0.3</version>
   </dependency>
   <!-- Smart MyBatis -->
   <dependency>
     <groupId>ink.icoding</groupId>
     <artifactId>spring-boot-starter-smart-mybatis</artifactId>
     <version>2.0.2</version><!--version-->
   </dependency>
   ```
2. 在`application.yaml`中配置数据库连接以及`smart mybatis`
   ```yaml
    spring:
      datasource:
         url: jdbc:mysql://localhost:3306/your_db?useSSL=false&serverTimezone=UTC
         username: your_username
         password: your_password
      mybatis:
         smart:
            enabled: true
            auto-sync-db: true
            naming-convention: underline_upper
            table-prefix: sm_
   ```
3. 启动你的项目：
   ```bash
   mvn spring-boot:run
   ```
4. 观察控制台输出的 `StudentServiceImpl#test` 查询结果，或在数据库中查看自动创建/扩展的 `sm_student` 表。

## 示例代码
```java
// 实体：继承 PO 并声明主键策略
public class Student extends PO {
    @ID
    private int id;
    private String name;
    private int age;
    private Sex sex;
}
// Mapper：仅需继承 SmartMapper
@Mapper
public interface StudentMapper extends SmartMapper<Student> {}
// 业务：Where DSL + 内置 CRUD
List<Student> students = studentMapper.select(
    Where.where()
         .and(Student::getName).like(keyword)
         .and(Student::getAge).greaterThan(minAge)
         .and(Student::getSex).equalsFor(sex)
         .limit(20)
);
```

## 配置与命名
| 配置键 | 说明 | 示例 |
| --- | --- | --- |
| `spring.mybatis.smart.enabled` | 是否启用 Smart MyBatis 功能 | `true` |
| `spring.mybatis.smart.auto-sync-db` | 自动将实体新增字段同步至表（仅新增、不删） | `true` |
| `spring.mybatis.smart.naming-convention` | `underline_upper` / `underline_lower` / `as_is` | `underline_upper` |
| `spring.mybatis.smart.table-prefix` | 统一的表前缀 | `sm_` |

命名约定示例：实体 `StudentProfile` 在 `underline_upper + sm_` 模式下将映射为 `SM_STUDENT_PROFILE`，字段 `createdAt` 将生成为 `CREATED_AT` 列。

## 常用命令
- `mvn clean install`：编译全部模块并运行测试。
- `mvn -pl core test`：仅运行 Core 的 DSL/Provider 单测。
- `mvn -pl spring-boot-starter-smart-mybatis-example spring-boot:run`：启动示例服务。
- `mvn -pl spring-boot-starter-smart-mybatis -am package`：构建 Starter 及其依赖。

---

## English Version
> [中文](#smart-mybatis) · English overview

### Overview
Smart MyBatis is a lightweight enhancement for vanilla MyBatis rather than a second wrapper. It ships Lambda DSLs, metadata utilities, and a starter that sit beside MyBatis: you must bring the official MyBatis dependency yourself, and Smart MyBatis never proxies, replaces, or shades any MyBatis class. `SmartMapper`, the conditional DSL, and the Spring Boot starter remove repetitive code while keeping the original surface area fully intact.

### Features
- Lightweight add-on fully decoupled from MyBatis internals; no auto-bundled MyBatis dependency keeps upgrades predictable.
- One base interface `SmartMapper<T extends PO>` adds ready-made CRUD plus `queryBySql/executeSql`.
- Lambda `Where` DSL (e.g., `and(Student::getAge).greaterThan(18)`) maps safely to columns without string literals.
- Multiple primary-key strategies (`AUTO`, `INPUT`, `UUID`, `SNOWFLAKE`, `SNOWFLAKE_HEX`) and automatic table/column naming with configurable prefixes.
- Optional schema synchronization (`auto-sync-db`) that adds missing columns according to entity metadata.
- Spring Boot starter auto-registers mappers and binds `spring.mybatis.smart.*` properties; customize behavior by providing your own `SmartMapperInitializer`.

### Architecture
```
Entity (extends PO)
        ↓
SmartMapper<T> (interface)
        ↓
MapperUtil & MapperDeclaration (metadata cache)
        ↓
BaseSqlProvider (builds SQL)
        ↓
MyBatis / Spring Boot Starter
        ↓
Example app
```

### Lightweight Principles
1. **Bring-your-own MyBatis**: add the official starter manually to your application `pom.xml`:
   ```xml
   <dependency>
     <groupId>org.mybatis</groupId>
     <artifactId>mybatis-spring-boot-starter</artifactId>
     <version>3.0.3</version>
   </dependency>
   ```
2. **No proxies, no replacements**: Smart MyBatis never wraps `SqlSession`, `MapperFactoryBean`, or any MyBatis SPI. Disabling `spring.mybatis.smart.enabled` instantly reverts to pure MyBatis.
3. **Focused additive APIs**: every helper (`SmartMapper`, `Where`, schema sync) layers on top of MyBatis instead of intercepting it, so XML mappers, plugins, and interceptors keep working unchanged.

### Quick Start
> Assume you are using the [spring-boot-starter-smart-mybatis-example](spring-boot-starter-smart-mybatis-example) project as a reference.

1. Add both official MyBatis and Smart MyBatis dependencies to your `pom.xml`:
   ```xml
   <!-- Mybatis -->
   <dependency>
     <groupId>org.mybatis</groupId>
     <artifactId>mybatis-spring-boot-starter</artifactId>
     <version>3.0.3</version>
   </dependency>
   <!-- Smart MyBatis -->
   <dependency>
     <groupId>ink.icoding</groupId>
     <artifactId>spring-boot-starter-smart-mybatis</artifactId>
     <version>2.0.2</version><!--version-->
   </dependency>
   ```
2. Configure database connection and Smart MyBatis in `application.yaml`:
   ```yaml
    spring:
      datasource:
         url: jdbc:mysql://localhost:3306/your_db?useSSL=false&serverTimezone=UTC
         username: your_username
         password: your_password
      mybatis:
         smart:
            enabled: true
            auto-sync-db: true
            naming-convention: underline_upper
            table-prefix: sm_
   ```
3. Run your application:
   ```bash
    mvn spring-boot:run
    ```
4. Check console output for `StudentServiceImpl#test` results or inspect the auto-created/expanded `sm_student` table in your database.

### Usage Snapshot
```java
// Entity: extends PO and declares primary key
public class Student extends PO {
    @ID
    private int id;
    private String name;
    private int age;
    private Sex sex;
}
// Mapper: just extend SmartMapper
@Mapper
public interface StudentMapper extends SmartMapper<Student> {}
// Service: Where DSL + built-in CRUD
List<Student> students = studentMapper.select(
    Where.where()
         .and(Student::getName).like(keyword)
         .and(Student::getAge).greaterThan(minAge)
         .and(Student::getSex).equalsFor(sex)
         .limit(20)
);
```

### Configuration
| Property | Purpose | Example |
| --- | --- | --- |
| `spring.mybatis.smart.enabled` | Turns Smart MyBatis features on/off | `true` |
| `spring.mybatis.smart.auto-sync-db` | Adds new columns based on entity fields | `false` |
| `spring.mybatis.smart.naming-convention` | `underline_upper`, `underline_lower`, `as_is` | `underline_upper` |
| `spring.mybatis.smart.table-prefix` | Prefix prepended to inferred table names | `sm_` |

### Commands
- `mvn clean install` – build all modules.
- `mvn -pl core test` – focus on the DSL/provider layer.
- `mvn -pl spring-boot-starter-smart-mybatis-example spring-boot:run` – run the demo service.
