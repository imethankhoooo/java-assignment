@echo off
echo ================================================
echo  Java依赖库下载脚�?(兼容�?
echo ================================================

if not exist "lib" mkdir lib
cd lib

echo.
echo 清理旧的依赖�?..
del *.jar /Q

echo.
echo 下载Apache POI依赖 (Excel处理)...
curl -L -o "poi-5.2.4.jar" "https://repo1.maven.org/maven2/org/apache/poi/poi/5.2.4/poi-5.2.4.jar"
curl -L -o "poi-ooxml-5.2.4.jar" "https://repo1.maven.org/maven2/org/apache/poi/poi-ooxml/5.2.4/poi-ooxml-5.2.4.jar"
curl -L -o "poi-ooxml-lite-5.2.4.jar" "https://repo1.maven.org/maven2/org/apache/poi/poi-ooxml-lite/5.2.4/poi-ooxml-lite-5.2.4.jar"

echo.
echo 下载XMLBeans依赖...
curl -L -o "xmlbeans-5.1.1.jar" "https://repo1.maven.org/maven2/org/apache/xmlbeans/xmlbeans/5.1.1/xmlbeans-5.1.1.jar"

echo.
echo 下载Apache Commons依赖...
curl -L -o "commons-collections4-4.4.jar" "https://repo1.maven.org/maven2/org/apache/commons/commons-collections4/4.4/commons-collections4-4.4.jar"
curl -L -o "commons-compress-1.21.jar" "https://repo1.maven.org/maven2/org/apache/commons/commons-compress/1.21/commons-compress-1.21.jar"
curl -L -o "commons-io-2.13.0.jar" "https://repo1.maven.org/maven2/commons-io/commons-io/2.13.0/commons-io-2.13.0.jar"

echo.
echo 下载Log4j依赖 (Apache POI需要)...
curl -L -o "log4j-api-2.20.0.jar" "https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-api/2.20.0/log4j-api-2.20.0.jar"
curl -L -o "log4j-core-2.20.0.jar" "https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-core/2.20.0/log4j-core-2.20.0.jar"

echo.
echo 下载SLF4J依赖...
curl -L -o "slf4j-api-1.7.36.jar" "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar"
curl -L -o "slf4j-simple-1.7.36.jar" "https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.36/slf4j-simple-1.7.36.jar"

echo.
echo 下载iText5依赖 (PDF处理)...
curl -L -o "itextpdf-5.5.13.3.jar" "https://repo1.maven.org/maven2/com/itextpdf/itextpdf/5.5.13.3/itextpdf-5.5.13.3.jar"
curl -L -o "itext-asian-5.2.0.jar" "https://repo1.maven.org/maven2/com/itextpdf/itext-asian/5.2.0/itext-asian-5.2.0.jar"

echo.
echo 下载额外的POI依赖...
curl -L -o "curvesapi-1.07.jar" "https://repo1.maven.org/maven2/com/github/virtuald/curvesapi/1.07/curvesapi-1.07.jar"
curl -L -o "SparseBitSet-1.2.jar" "https://repo1.maven.org/maven2/com/zaxxer/SparseBitSet/1.2/SparseBitSet-1.2.jar"

echo.
echo 下载BouncyCastle依赖 (加密支持)...
curl -L -o "bcprov-jdk15on-1.70.jar" "https://repo1.maven.org/maven2/org/bouncycastle/bcprov-jdk15on/1.70/bcprov-jdk15on-1.70.jar"
curl -L -o "bcpkix-jdk15on-1.70.jar" "https://repo1.maven.org/maven2/org/bouncycastle/bcpkix-jdk15on/1.70/bcpkix-jdk15on-1.70.jar"

cd ..

echo.
echo ================================================
echo  所有依赖库下载完成�?echo ================================================
echo.
echo 已下载的库文件：
dir lib\*.jar /B
echo.
echo 现在可以运行应用程序了！
pause 
