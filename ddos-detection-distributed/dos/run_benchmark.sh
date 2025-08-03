#!/bin/bash

# Packet Processing Benchmark Runner
# This script compiles and runs the packet processing benchmark

echo "=== Packet Processing Benchmark Runner ==="
echo

# Set default values
PACKET_COUNT=${1:-10000}
PROCESSOR_COUNT=${2:-4}

echo "Configuration:"
echo "  Packet count: $PACKET_COUNT"
echo "  Processor count: $PROCESSOR_COUNT"
echo

# Compile the project
echo "Compiling project..."
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo "❌ Compilation failed!"
    exit 1
fi

echo "✅ Compilation successful!"
echo

# Run the benchmark
echo "Running benchmark..."
java -cp "target/classes:lib/mpj.jar" org.example.mpj.BenchmarkRunner $PACKET_COUNT $PROCESSOR_COUNT

echo
echo "Benchmark completed!"
