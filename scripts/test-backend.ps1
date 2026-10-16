$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$jdkHome = Join-Path $repoRoot '.tools/jdk21/jdk-21.0.12.1+1'
$maven = Join-Path $repoRoot '.tools/apache-maven-3.9.16/bin/mvn.cmd'

if (-not (Test-Path (Join-Path $jdkHome 'bin/java.exe'))) {
    throw "Portable JDK 21 is missing: $jdkHome"
}
if (-not (Test-Path $maven)) {
    throw "Portable Maven is missing: $maven"
}

$env:JAVA_HOME = $jdkHome
$env:PATH = "$(Join-Path $jdkHome 'bin');$env:PATH"
$repoForJava = $repoRoot -replace '\\', '/'
$env:MAVEN_OPTS = "-Dmaven.repo.local=$repoForJava/.tools/m2repo -Djavax.net.ssl.trustStoreType=Windows-ROOT"

& $maven -f (Join-Path $repoRoot 'backend/pom.xml') --no-transfer-progress test
exit $LASTEXITCODE
