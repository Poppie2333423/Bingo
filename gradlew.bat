@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem
@rem SPDX-License-Identifier: Apache-2.0
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:execute
@rem Setup the command line

set CLASSPATH=

set WRAPPER_JAR=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
if not exist "%WRAPPER_JAR%" (
    set PROPERTIES_FILE=%APP_HOME%\gradle\wrapper\gradle-wrapper.properties
    if not exist "%PROPERTIES_FILE%" (
        echo ERROR: Gradle wrapper properties not found at %PROPERTIES_FILE% 1>&2
        goto fail
    )

    for /f "usebackq tokens=*" %%i in (`powershell -NoProfile -Command "(Get-Content '%PROPERTIES_FILE%') | Where-Object { $_ -like 'distributionUrl*' } | ForEach-Object { ($_ -split '=',2)[1].Replace('\\:',':') }"`) do set "DISTRIBUTION_URL=%%i"

    if not defined DISTRIBUTION_URL (
        echo ERROR: Could not determine Gradle distribution URL. 1>&2
        goto fail
    )

    powershell -NoProfile -Command ^
        "$ErrorActionPreference = 'Stop';" ^
        "$ProgressPreference = 'SilentlyContinue';" ^
        "$uri = '%DISTRIBUTION_URL%';" ^
        "$tmpZip = [System.IO.Path]::GetTempFileName();" ^
        "$tmpDir = [System.IO.Path]::Combine([System.IO.Path]::GetTempPath(), 'gradle-wrapper-' + [System.Guid]::NewGuid());" ^
        "[System.IO.Directory]::CreateDirectory($tmpDir) | Out-Null;" ^
        "Invoke-WebRequest -Uri $uri -OutFile $tmpZip;" ^
        "Add-Type -AssemblyName System.IO.Compression.FileSystem;" ^
        "$zip = [System.IO.Compression.ZipFile]::OpenRead($tmpZip);" ^
        "$entry = $zip.Entries | Where-Object { $_.FullName -match 'gradle-[^/]+/lib/gradle-wrapper\.jar' } | Select-Object -First 1;" ^
        "if (-not $entry) { throw 'gradle-wrapper.jar not found in distribution'; }" ^
        "$dest = '%WRAPPER_JAR%';" ^
        "[System.IO.Directory]::CreateDirectory([System.IO.Path]::GetDirectoryName($dest)) | Out-Null;" ^
        "$stream = $entry.Open();" ^
        "$outStream = [System.IO.File]::Create($dest);" ^
        "$stream.CopyTo($outStream);" ^
        "$outStream.Close();" ^
        "$stream.Close();" ^
        "$zip.Dispose();" ^
        "Remove-Item $tmpZip -Force;" ^
        "Remove-Item $tmpDir -Recurse -Force;"

    if %ERRORLEVEL% neq 0 goto fail
)

set CLASSPATH=%WRAPPER_JAR%

@rem Execute Gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" -jar "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" %*

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%GRADLE_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
