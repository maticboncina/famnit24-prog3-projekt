#!/bin/bash

# Simple Benchmark Runner (No Maven Required)
# This script compiles and runs the packet processing benchmark

echo "=== Simple Packet Processing Benchmark ==="
echo

cd "$(dirname "$0")"

# Create target directory
mkdir -p target/classes

echo "Compiling classes..."

# Compile PerformanceTimer
javac -d target/classes src/main/java/org/example/util/PerformanceTimer.java

# Compile SimpleBenchmarkTest
javac -cp target/classes -d target/classes src/main/java/org/example/mpj/SimpleBenchmarkTest.java

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
    echo
    echo "Running benchmark..."
    echo
    
    # Run the benchmark
    java -cp target/classes org.example.mpj.SimpleBenchmarkTest
    
    echo
    echo "Benchmark completed!"
else
    echo "❌ Compilation failed!"
    exit 1
fi
