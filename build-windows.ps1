# & "C:\Program Files\Microsoft SDKs\Windows\v7.1\Bin\SetEnv.cmd"

# Get the version from the git command
& sbt -error -batch "run -c git-mkver.conf patch"
$VERSION = & sbt -error -batch "run -c git-mkver.conf next"

& sbt assembly
Set-Location -Path target\scala-2.12
& native-image -jar "git-mkver-assembly-$VERSION.jar" --no-fallback
Move-Item -Path "git-mkver-assembly-$VERSION.exe" -Destination git-mkver.exe
Compress-Archive -Path 'git-mkver.exe' -DestinationPath 'git-mkver-windows-amd64-%VERSION%.zip'
Get-FileHash git-mkver-windows-amd64-%VERSION%.zip | %% Hash
