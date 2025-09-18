#!/bin/bash
echo "Starting CarSeek with dependencies..."

# Set classpath to include all JAR files in lib directory
export CLASSPATH="src:lib/*"

# Compile the Java files with UTF-8 encoding
echo "Compiling Java files..."
javac -encoding UTF-8 -cp "$CLASSPATH" -source 8 -target 8 src/*.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

echo "Compilation successful!"
echo "Starting application..."

# Run the main application
java -cp "$CLASSPATH" Main 