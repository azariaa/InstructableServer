@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  InstructableServer startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

@rem Add default JVM options here. You can also use JAVA_OPTS and INSTRUCTABLE_SERVER_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windowz variants

if not "%OS%" == "Windows_NT" goto win9xME_args
if "%@eval[2+2]" == "4" goto 4NT_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*
goto execute

:4NT_args
@rem Get arguments from the 4NT Shell from JP Software
set CMD_LINE_ARGS=%$

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\InstructableServer-1.0.jar;%APP_HOME%\lib\junit-4.11.jar;%APP_HOME%\lib\natty-0.12.jar;%APP_HOME%\lib\stanford-corenlp-3.5.2.jar;%APP_HOME%\lib\stanford-parser-3.6.0.jar;%APP_HOME%\lib\guava-11.0.2.jar;%APP_HOME%\lib\jackson-core-asl-1.1.0.jar;%APP_HOME%\lib\jackson-core-2.7.2.jar;%APP_HOME%\lib\opennlp-tools-1.5.3.jar;%APP_HOME%\lib\json-20090211.jar;%APP_HOME%\lib\commons-lang3-3.0.jar;%APP_HOME%\lib\mail-1.4.7.jar;%APP_HOME%\lib\mysql-connector-java-5.1.35.jar;%APP_HOME%\lib\jklol.jar;%APP_HOME%\lib\google-api-client-1.20.0.jar;%APP_HOME%\lib\google-oauth-client-jetty-1.20.0.jar;%APP_HOME%\lib\google-api-services-calendar-v3-rev125-1.20.0.jar;%APP_HOME%\lib\hamcrest-core-1.3.jar;%APP_HOME%\lib\antlr-runtime-3.5.2.jar;%APP_HOME%\lib\ical4j-1.0.2.jar;%APP_HOME%\lib\xom-1.2.10.jar;%APP_HOME%\lib\joda-time-2.1.jar;%APP_HOME%\lib\jollyday-0.4.7.jar;%APP_HOME%\lib\ejml-0.23.jar;%APP_HOME%\lib\javax.json-api-1.0.jar;%APP_HOME%\lib\jsr305-1.3.9.jar;%APP_HOME%\lib\opennlp-maxent-3.0.3.jar;%APP_HOME%\lib\jwnl-1.3.3.jar;%APP_HOME%\lib\activation-1.1.jar;%APP_HOME%\lib\google-oauth-client-1.20.0.jar;%APP_HOME%\lib\google-http-client-jackson2-1.20.0.jar;%APP_HOME%\lib\guava-jdk5-13.0.jar;%APP_HOME%\lib\google-oauth-client-java6-1.20.0.jar;%APP_HOME%\lib\jetty-6.1.26.jar;%APP_HOME%\lib\commons-logging-1.1.1.jar;%APP_HOME%\lib\commons-codec-1.5.jar;%APP_HOME%\lib\commons-lang-2.6.jar;%APP_HOME%\lib\backport-util-concurrent-3.1.jar;%APP_HOME%\lib\xercesImpl-2.8.0.jar;%APP_HOME%\lib\xalan-2.7.0.jar;%APP_HOME%\lib\jaxb-api-2.2.7.jar;%APP_HOME%\lib\google-http-client-1.20.0.jar;%APP_HOME%\lib\jetty-util-6.1.26.jar;%APP_HOME%\lib\servlet-api-2.5-20081211.jar;%APP_HOME%\lib\httpclient-4.0.1.jar;%APP_HOME%\lib\httpcore-4.0.1.jar;%APP_HOME%\lib\slf4j-api-1.7.12.jar;%APP_HOME%\lib\xml-apis-2.0.2.jar

@rem Execute InstructableServer
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %INSTRUCTABLE_SERVER_OPTS%  -classpath "%CLASSPATH%" testing.CommandLine %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable INSTRUCTABLE_SERVER_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%INSTRUCTABLE_SERVER_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
