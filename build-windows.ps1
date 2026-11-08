# Get the version from the git command
& cargo run --quiet -- -c git-mkver.conf patch
$VERSION = & cargo run --quiet -- -c git-mkver.conf next -f '{Version}'

& cargo build --release
Set-Location -Path target\release
Compress-Archive -Path 'git-mkver.exe' -DestinationPath "git-mkver-windows-amd64-$VERSION.zip" -Force
Get-FileHash "git-mkver-windows-amd64-$VERSION.zip" | % Hash
