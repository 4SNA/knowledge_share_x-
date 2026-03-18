@echo off
title KnowledgeGraphX Server

echo ===================================================
echo   Starting KnowledgeGraphX Environment...
echo ===================================================

:: Ensure persistence across reboots by saving to AppData
set "PERSIST_DIR=%APPDATA%\KnowledgeGraphX"
set "APP_DIR=%TEMP%\knowledgegraphx_run_env"

echo Creating playable sandbox to bypass D: permissions...
:: Mirror the code to Temp so we can compile it without Permission Access Denied warnings
robocopy "%~dp0." "%APP_DIR%" /MIR /XD target data .git .idea /NFL /NDL /NJH /NJS /nc /ns /np >nul

pushd "%APP_DIR%"

echo Compiling and Running Application...
:: Use AppData to ensure the database and uploaded documents are preserved properly!
call mvn spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:h2:file:%PERSIST_DIR%/data/kgx --app.upload.dir=%PERSIST_DIR%/uploads"

popd
pause
