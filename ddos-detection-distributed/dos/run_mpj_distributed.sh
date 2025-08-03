#!/bin/bash

# MPJ Distributed Packet Processing Runner
# This script sets up and runs the MPJ-based distributed packet processing

echo "=== MPJ Distributed Packet Processing Runner ==="
echo

# Set default values
NUM_PROCESSES=${1:-4}
MPJ_HOME=${MPJ_HOME:-"/usr/local/mpj"}

echo "Configuration:"
echo "  Number of processes: $NUM_PROCESSES"
echo "  MPJ_HOME: $MPJ_HOME"
echo

# Check if MPJ is available
if [ ! -d "$MPJ_HOME" ]; then
    echo "⚠️  MPJ not found at $MPJ_HOME"
    echo "Please install MPJ Express or set MPJ_HOME environment variable"
    echo "For simulation purposes, you can run the benchmark instead:"
    echo "  ./run_benchmark.sh"
    exit 1
fi

# Compile the project
echo "Compiling project..."
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo "❌ Compilation failed!"
    exit 1
fi

echo "✅ Compilation successful!"
echo

# Set MPJ classpath
export CLASSPATH="target/classes:lib/mpj.jar:$MPJ_HOME/lib/mpj.jar"

# Run MPJ distributed processing
echo "Starting MPJ distributed processing with $NUM_PROCESSES processes..."
echo "Note: This requires proper MPJ Express installation and configuration."
echo

# Example MPJ run command (adjust based on your MPJ installation)
if [ -f "$MPJ_HOME/bin/mpjrun.sh" ]; then
    $MPJ_HOME/bin/mpjrun.sh -np $NUM_PROCESSES org.example.mpj.MPJDistributedMain
else
    echo "❌ MPJ runtime not found. Expected: $MPJ_HOME/bin/mpjrun.sh"
    echo "Running simulation instead..."
    java -cp "$CLASSPATH" org.example.mpj.BenchmarkRunner 10000 $NUM_PROCESSES
fi

echo
echo "MPJ processing completed!"
