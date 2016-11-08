@echo off

setlocal

::- Get the JDK Version
set KEY="HKLM\SOFTWARE\JavaSoft\Java Development Kit"
set VALUE=CurrentVersion
reg query %KEY% /v %VALUE% 2>nul || (
    echo JDK not installed 
    pause
    exit /b 1
)
set JDK_VERSION=
for /f "tokens=2,*" %%a in ('reg query %KEY% /v %VALUE% ^| findstr %VALUE%') do (
    set JDK_VERSION=%%b
)

::- echo JDK VERSION: %JDK_VERSION%
::- Get the JavaHome
set KEY="HKLM\SOFTWARE\JavaSoft\Java Development Kit\%JDK_VERSION%"
set VALUE=JavaHome
reg query %KEY% /v %VALUE% 2>nul || (
    echo JavaHome not installed
    pause
    exit /b 1
)

set JAVAHOME=
for /f "tokens=2,*" %%a in ('reg query %KEY% /v %VALUE% ^| findstr %VALUE%') do (
    set JAVAHOME=%%b
)

echo JavaHome: %JAVAHOME%

"%JAVAHOME%\bin\javac.exe" Magic4.java
"%JAVAHOME%\bin\java.exe" Magic4

pause
endlocal