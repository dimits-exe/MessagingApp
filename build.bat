clean.bat

DIR /A-D /B /S src\*.java > .files_to_compile

javac -d bin -g:none --release 8 @.files_to_compile

DEL .files_to_compile
