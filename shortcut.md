del *.class
./run_with_dependencies.bat
Get-ChildItem -Path . -Filter *.class -Recurse -File | Remove-Item -Force -Verbose