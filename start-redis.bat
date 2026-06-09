@echo off
REM === 房产管理系统 — Redis 启动脚本 ===
REM 运行前请确保 D:\redis\bin\redis-server.exe 存在

echo [房产管理系统] 正在启动 Redis...
start /B /MIN D:\redis\bin\redis-server.exe D:\redis\bin\redis.windows.conf
timeout /t 2 /nobreak >nul

echo [房产管理系统] 验证 Redis 连接...
D:\redis\bin\redis-cli.exe ping
if %ERRORLEVEL% equ 0 (
    echo [房产管理系统] Redis 已成功启动 ^(端口 6379^)
) else (
    echo [房产管理系统] Redis 启动失败，请检查配置
)

echo.
echo 启动后端: cd 后端服务代码/Demo ^&^& mvnw spring-boot:run
echo 启动前端: cd 前端代码 ^&^& npm run dev
echo.
pause
