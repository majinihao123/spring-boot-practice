# 简单网关与登录鉴权示例

这是一个用于学习 Spring Cloud Gateway、JWT 登录鉴权和 MySQL 数据访问的双应用示例。

当前项目包含：

- 后端服务 `demo`：处理登录、业务接口和数据库访问。
- 网关服务 `gateway`：统一接收客户端请求并转发到后端。
- MySQL 数据库 `auth_gateway`：保存用户账号和后续业务数据。

## 架构

```text
客户端
  │
  │ http://localhost:8080/api/**
  ▼
Spring Cloud Gateway（8080）
  ├─ 路由转发：已完成
  └─ JWT 统一鉴权：待完成
  │
  │ StripPrefix=1
  ▼
Spring Boot Backend（8081）
  ├─ Controller
  ├─ Service
  ├─ Repository
  ├─ JWT 签发与校验
  └─ MySQL
```

示例路由：

```text
GET http://localhost:8080/api/hello
                    ↓
GET http://localhost:8081/hello
```

网关不直接访问数据库。所有数据库操作都必须通过后端接口完成。

## 使用技术

| 技术 | 版本或用途 |
|---|---|
| Java | 21 |
| Maven Wrapper | 项目构建和启动 |
| Spring Boot | 4.1.0 |
| Spring Web MVC | 后端 REST API |
| Spring Cloud | 2025.1.2 |
| Spring Cloud Gateway Server WebFlux | 网关与路由转发 |
| Project Reactor / Netty | 网关响应式运行时 |
| Spring Security | 后端和网关安全控制 |
| OAuth2 Resource Server | JWT Bearer Token 校验 |
| JWT | HS256 签名，默认有效期 2 小时 |
| BCrypt | 用户密码哈希 |
| Spring Data JPA / Hibernate | 数据库访问 |
| MySQL | 用户及业务数据存储 |

依赖版本由 Spring Boot Parent 和 Spring Cloud BOM 统一管理，不要随意给单个 Spring 依赖指定版本。

## 项目结构

```text
demo/
├── pom.xml
├── src/
│   └── main/
│       ├── java/com/example/demo/
│       │   ├── DemoApplication.java
│       │   ├── HelloController.java
│       │   └── auth/
│       │       ├── AdminUserInitializer.java
│       │       ├── AppUser.java
│       │       ├── AppUserRepository.java
│       │       ├── AuthController.java
│       │       ├── AuthService.java
│       │       ├── LoginRequest.java
│       │       ├── LoginResponse.java
│       │       └── SecurityConfig.java
│       └── resources/
│           └── application.properties
├── gateway/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/gateway/
│       │   └── GatewayApplication.java
│       └── resources/
│           └── application.yml
└── database/
    └── mysql/
        ├── 00_provision.sql
        └── 01_schema.sql
```

## 分层约定

后端代码采用以下调用顺序：

```text
Controller → Service → Repository → MySQL
```

- `Controller`：接收 HTTP 参数并返回响应，不编写数据库操作。
- `Service`：处理登录、密码校验、JWT 签发等业务逻辑。
- `Repository`：使用 Spring Data JPA 查询数据库，相当于 MyBatis 项目中的 Mapper。
- `Entity`：映射数据库表。
- `Request/Response`：定义接口输入和输出，不直接暴露 Entity。

当前项目只有一种认证实现，因此直接使用 `AuthService` 类。只有出现多个实现或明确的接口边界时，才需要拆成 `AuthService` 和 `AuthServiceImpl`。

## 数据库

数据库名称：

```text
auth_gateway
```

应用账号：

```text
auth_app@localhost
```

用户表：

```text
app_user
```

主要字段：

| 字段 | 用途 |
|---|---|
| `id` | 用户主键 |
| `username` | 唯一登录名 |
| `password_hash` | BCrypt 密码哈希，不保存明文密码 |
| `role` | 用户角色，例如 `ADMIN` |
| `enabled` | 账号是否启用 |
| `created_at` | 创建时间 |
| `updated_at` | 更新时间 |

数据库脚本：

- `database/mysql/00_provision.sql`：创建数据库并给已存在的 `auth_app` 账号授权。
- `database/mysql/01_schema.sql`：创建 `app_user` 表。

## 本地密钥配置

本地使用根目录下的文件：

```text
.env.local.properties
```

该文件已加入 `.gitignore`，禁止提交到 Git。格式如下：

```properties
DB_USERNAME=auth_app
DB_PASSWORD=本地数据库密码
JWT_SECRET=Base64编码的32字节以上随机密钥
APP_ADMIN_USERNAME=admin
APP_ADMIN_PASSWORD=本地管理员初始密码
```

注意：

- 不要提交、打印或复制真实密钥到 README、日志、聊天记录。
- 后端与网关必须使用完全相同的 `JWT_SECRET`。
- JWT 密钥变化后，旧令牌会立即失效。
- `APP_ADMIN_PASSWORD` 只用于首次创建管理员，不会在每次启动时重置密码。

当前还需要在后端和网关配置中导入 `.env.local.properties`，这是下一步工作。

## 后端鉴权设计

后端已经具备以下代码：

- 从 `app_user` 表查询用户。
- 使用 BCrypt 比对密码。
- 登录成功后签发 HS256 JWT。
- JWT 包含 `iss`、`iat`、`exp`、`sub` 和 `role`。
- `POST /auth/login` 允许匿名访问。
- `/error` 允许框架内部错误处理。
- 其他后端接口要求有效 JWT。
- 使用无状态安全策略，不创建 HTTP Session。
- 启动时可创建初始管理员，已存在时不会重置。

登录接口：

```text
POST /auth/login
```

请求体：

```json
{
  "username": "admin",
  "password": "本地管理员密码"
}
```

响应结构：

```json
{
  "tokenType": "Bearer",
  "accessToken": "JWT",
  "expiresIn": 7200
}
```

## 网关设计

网关当前已经完成：

- 独立 Spring Boot 应用。
- 监听 `8080`。
- 匹配 `/api/**`。
- 使用 `StripPrefix=1` 删除 `/api`。
- 转发到 `http://localhost:8081`。

网关下一步需要完成：

- 添加 OAuth2 Resource Server 依赖。
- 导入本地 JWT 密钥。
- 创建响应式 `SecurityWebFilterChain`。
- 创建与后端一致的 `ReactiveJwtDecoder`。
- 匿名放行 `POST /api/auth/login`。
- 要求其他所有请求携带有效 Bearer Token。
- 验证 JWT 签名、签发者和过期时间。
- 对未登录请求返回 `401 Unauthorized`。

网关不得添加 JPA 或 MySQL 依赖，也不得查询 `app_user` 表。

## 目标请求流程

### 登录

```text
POST /api/auth/login
  → 网关匿名放行
  → 后端查询 MySQL
  → BCrypt 校验密码
  → 后端签发 JWT
  → 返回客户端
```

### 访问受保护接口

```text
GET /api/hello
Authorization: Bearer <JWT>
  → 网关验证 JWT
  → 后端再次验证 JWT
  → 执行业务代码
  → 返回结果
```

网关和后端各验证一次 JWT：

- 网关可以尽早拒绝非法流量。
- 后端验证可以防止客户端绕过网关直接访问 `8081`。

## 启动命令

所有命令都在项目根目录执行。

启动后端：

```powershell
./mvnw.cmd spring-boot:run
```

启动网关：

```powershell
./mvnw.cmd -f gateway/pom.xml spring-boot:run
```

编译后端：

```powershell
./mvnw.cmd -DskipTests compile
```

编译网关：

```powershell
./mvnw.cmd -f gateway/pom.xml -DskipTests compile
```

## 接口测试目标

网关鉴权完成后需要依次验证：

1. 不携带 JWT 访问 `/api/hello`，返回 `401`。
2. 使用错误用户名或密码登录，返回 `401`。
3. 使用正确账号登录，返回 JWT。
4. 携带有效 JWT 访问 `/api/hello`，返回 `Hello Spring Boot!`。
5. 携带伪造或过期 JWT，返回 `401`。
6. 不携带 JWT 直接访问 `http://localhost:8081/hello`，返回 `401`。
7. 确认网关项目没有数据库依赖。

## 当前进度

- [x] 创建后端服务并监听 `8081`
- [x] 创建网关服务并监听 `8080`
- [x] 配置 `/api/**` 路由转发
- [x] 创建 MySQL 数据库和 `app_user` 表
- [x] 添加后端 JPA、MySQL 和 Security 依赖
- [x] 创建 Entity 和 Repository
- [x] 创建登录请求与响应对象
- [x] 创建登录 Service 和 Controller
- [x] 创建后端 JWT 编码器与解码器
- [x] 创建 BCrypt PasswordEncoder
- [x] 创建初始管理员初始化器
- [x] 创建并忽略本地密钥文件
- [ ] 在应用配置中导入本地密钥文件
- [ ] 启动后端并确认管理员写入数据库
- [ ] 测试后端登录接口
- [ ] 给网关添加 JWT 鉴权
- [ ] 通过网关完成完整登录与接口测试

## 后续代码生成约束

后续修改代码时遵循以下规则：

1. 网关只负责路由和统一鉴权，不连接 MySQL。
2. 登录、用户和业务数据操作放在后端。
3. 密码只能保存 BCrypt 哈希，禁止保存或记录明文密码。
4. JWT 密钥只能来自外部配置，禁止写死在 Java、YAML 或 Git 中。
5. 登录接口是唯一的匿名业务入口，其他接口默认需要认证。
6. 后端继续保留 JWT 校验，不能只依赖网关。
7. DTO、Entity 和数据库访问对象保持职责分离。
8. 新增数据库结构必须通过 `database/mysql` 中的版本化 SQL 脚本维护。
9. 新增路由统一使用 `/api/**` 作为外部路径。
10. 修改后至少执行后端和网关编译，并完成 `401 → 登录 → 带 Token 成功` 测试。
