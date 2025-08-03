# MPJ Distributed Packet Processing

This project implements MPJ (Message Passing Java) for distributed packet processing and benchmarking packet processing delays across multiple clients.

## Overview

The system provides:
- **Distributed Packet Processing**: Uses MPJ to distribute packet analysis across multiple processes
- **Performance Benchmarking**: Measures and compares single-threaded vs distributed processing
- **Delay Analysis**: Tracks processing delays and throughput metrics
- **Comprehensive Statistics**: Detailed performance analysis and reporting

## Architecture

### Components

1. **DistributedPacketProcessor**: Main MPJ coordinator that distributes packets to workers
2. **PacketProcessingResult**: Serializable result object for MPJ communication
3. **PacketProcessingBenchmark**: Benchmarking utilities for performance measurement
4. **BenchmarkRunner**: Standalone benchmark comparison tool
5. **MPJDistributedMain**: Entry point for MPJ-based processing

### Process Roles

- **Master Process (Rank 0)**: Distributes packets and collects results
- **Worker Processes (Rank 1+)**: Process packets and return results

## Installation & Setup

### Prerequisites

1. **Java 21+** (as specified in pom.xml)
2. **Maven** for building
3. **MPJ Express** (optional, for true distributed processing)

### MPJ Express Installation (Optional)

```bash
# Download MPJ Express
wget https://sourceforge.net/projects/mpjexpress/files/releases/mpj-v0_44.tar.gz
tar -xzf mpj-v0_44.tar.gz
sudo mv mpj-v0_44 /usr/local/mpj

# Set environment variables
export MPJ_HOME=/usr/local/mpj
export PATH=$MPJ_HOME/bin:$PATH
```

### Build Project

```bash
mvn clean compile
```

## Usage

### 1. Quick Benchmark (Recommended for Testing)

Run a simulation benchmark to compare single-threaded vs multi-threaded processing:

```bash
./run_benchmark.sh [packet_count] [processor_count]
```

Examples:
```bash
# Default: 10,000 packets, 4 processors
./run_benchmark.sh

# Custom: 50,000 packets, 8 processors
./run_benchmark.sh 50000 8

# Large scale: 100,000 packets, 16 processors
./run_benchmark.sh 100000 16
```

### 2. True MPJ Distributed Processing

If you have MPJ Express installed:

```bash
./run_mpj_distributed.sh [num_processes]
```

Examples:
```bash
# Default: 4 processes
./run_mpj_distributed.sh

# 8 processes
./run_mpj_distributed.sh 8
```

### 3. Manual Java Execution

```bash
# Benchmark simulation
java -cp "target/classes:lib/mpj.jar" org.example.mpj.BenchmarkRunner 10000 4

# MPJ distributed (requires MPJ setup)
mpjrun.sh -np 4 org.example.mpj.MPJDistributedMain
```

## Benchmark Metrics

The system measures several key performance metrics:

### Processing Metrics
- **Total Execution Time**: End-to-end processing time
- **Average Processing Time per Packet**: Mean processing delay
- **Throughput**: Packets processed per second
- **Distribution Overhead**: Communication costs in distributed processing

### Performance Analysis
- **Speedup**: Performance improvement ratio (distributed vs single-threaded)
- **Parallel Efficiency**: How well the system utilizes additional processors
- **Throughput Improvement**: Percentage increase in packet processing rate

### Per-Processor Statistics
- **Packets Processed**: Number of packets handled by each processor
- **Min/Max/Average Times**: Processing time distribution per processor
- **Load Balance**: Distribution of work across processors

## Example Output

```
=== COMPREHENSIVE PACKET PROCESSING BENCHMARK ===
Packet count: 10000
Processor count: 4

Running single-threaded benchmark...
Running multi-threaded benchmark with 4 threads...

=== BENCHMARK COMPARISON RESULTS ===

Method                    | Total Time (ms) | Packets         | Avg Time/Packet (ms) | Throughput (pkt/s)
-------------------------------------------------------------------------------------------
Single-threaded           | 10234.56       | 10000           | 1.023                | 977.12
Multi-threaded (4 threads)| 2845.23        | 10000           | 1.102                | 3515.67

=== PERFORMANCE METRICS ===
Speedup: 3.60x
Throughput improvement: 259.8%
Parallel efficiency: 90.0%
✅ Good parallel efficiency achieved!
```

## File Outputs

### Benchmark Results CSV
The system exports detailed results to timestamped CSV files:
- `benchmark_results_YYYY-MM-DD_HH-mm-ss.csv`

### CSV Format
```csv
Metric,Value
Total Execution Time (ms),2845.23
Total Packets,10000
Average Processing Time (ms),1.102
Throughput (packets/sec),3515.67
Total Distribution Overhead (ms),45.67

Processor,Packets Processed,Min Time (ms),Max Time (ms),Avg Time (ms)
1,2500,0.987,1.234,1.098
2,2500,0.945,1.187,1.105
3,2500,0.976,1.298,1.099
4,2500,0.923,1.176,1.106
```

## Configuration

### Packet Processing Parameters
- Modify `SharedData.hardLimit` to change blocking thresholds
- Adjust processing delays in `processPacket()` methods
- Configure thread pool sizes in `BenchmarkRunner`

### MPJ Configuration
- Set `MPJ_HOME` environment variable
- Configure process counts via command line arguments
- Adjust MPI tags and communication patterns in `DistributedPacketProcessor`

## Troubleshooting

### Common Issues

1. **MPJ Not Found**
   ```
   Solution: Install MPJ Express or run simulation benchmark instead
   ```

2. **Compilation Errors**
   ```bash
   # Clean and rebuild
   mvn clean compile
   ```

3. **Permission Denied on Scripts**
   ```bash
   chmod +x run_benchmark.sh run_mpj_distributed.sh
   ```

4. **Out of Memory for Large Packet Counts**
   ```bash
   # Increase JVM heap size
   export JAVA_OPTS="-Xmx4g -Xms1g"
   ```

## Performance Tips

1. **Optimal Processor Count**: Usually 2-8 processors for best efficiency
2. **Packet Batch Size**: Process packets in batches for better throughput
3. **Network Configuration**: Use dedicated network for MPJ communication
4. **JVM Tuning**: Adjust heap size and GC settings for large workloads

## Integration with Existing System

The MPJ components integrate with your existing DoS detection system:

- Uses existing `SharedData` for packet queues and statistics
- Compatible with `PacketCapture` and `PacketConsumer`
- Maintains existing UI components and monitoring
- Extends `PerformanceTimer` for consistent timing

## Next Steps

1. **Run initial benchmarks** to establish baseline performance
2. **Experiment with different processor counts** to find optimal configuration
3. **Analyze CSV output** to identify bottlenecks
4. **Configure MPJ Express** for true distributed processing across machines
5. **Integrate with production traffic** for real-world testing
