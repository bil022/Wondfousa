if exist "C:\Progra~1\Java\jdk1.7.0_79\bin\javac.exe" (
"C:\Progra~1\Java\jdk1.7.0_79\bin\javac.exe" Magic4.java
"C:\Progra~1\Java\jdk1.7.0_79\bin\java.exe" Magic4
) else if exist "C:\Progra~1\Java\jdk1.8.0_101\bin\javac.exe" (
"C:\Progra~1\Java\jdk1.8.0_101\bin\javac.exe" Magic4.java
"C:\Progra~1\Java\jdk1.8.0_101\bin\java.exe" Magic4
) else (
javac.exe Magic4.java
java Magic4
)
pause
