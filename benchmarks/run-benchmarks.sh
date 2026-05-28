#!/bin/bash
# Build benchmarks and generate load test scenarios

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/.."

echo "Building benchmarks module..."
cd "$PROJECT_ROOT"
mvn clean package -pl benchmarks -DskipTests

echo ""
echo "Benchmarks built successfully!"
echo "Benchmark JAR location: benchmarks/target/benchmarks.jar"
echo ""
echo "To run benchmarks:"
echo "  java -jar benchmarks/target/benchmarks.jar"
echo ""
echo "To run specific benchmark:"
echo "  java -jar benchmarks/target/benchmarks.jar TokenBucketBenchmark"
echo ""
echo "To generate JSON output:"
echo "  java -jar benchmarks/target/benchmarks.jar -rf json -rff jmh-results.json"