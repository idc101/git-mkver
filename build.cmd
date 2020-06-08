call "C:\Program Files\Microsoft SDKs\Windows\v7.1\Bin\SetEnv.cmd"
FOR /F %%i IN ('git mkver next') DO set VERSION=%%i
call sbt assembly
cd target\scala-2.12
call native-image -jar git-mkver-assembly-%VERSION%.jar --no-fallback
del git-mkver.exe
move git-mkver-assembly-%VERSION%.exe git-mkver.exe
PowerShell -Command "Compress-Archive -Path 'git-mkver.exe' -DestinationPath 'git-mkver-windows-amd64-%VERSION%.zip'"
PowerShell -Command "Get-FileHash git-mkver-windows-amd64-%VERSION%.zip | %% Hash"
cd ..\..\
