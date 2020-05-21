# Development

## Graal Native Image

## Windows

Launch _Windows SDK 7.1 Command Prompt_

set JAVA_HOME=C:\Users\iain\Tools\graalvm-ce-java8-20.0.0\jre
set PATH=%JAVA_HOME%\bin;%PATH%

```
sbt assembly
native-image -jar target\scala-2.12\git-mkver-assembly-0.4.0.jar --no-fallback
```

## MacOs

## Linux
