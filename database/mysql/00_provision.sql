 CREATE DATABASE IF NOT EXISTS auth_gateway
      CHARACTER SET utf8mb4
      COLLATE utf8mb4_0900_ai_ci;

  -- 数据库账号由管理员单独创建。
  -- 不要把真实密码写在这个文件中。

  GRANT SELECT, INSERT, UPDATE, DELETE,
        CREATE, ALTER, INDEX, DROP, REFERENCES
  ON auth_gateway.*
  TO 'auth_app'@'localhost';