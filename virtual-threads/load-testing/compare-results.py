#!/usr/bin/env python3
"""
Compare load testing results from multiple test runs
Parses 'hey' text output files and creates a comparison report
"""

import os
import re
import json
import sys
from pathlib import Path
from datetime import datetime
from collections import defaultdict

def parse_hey_output(file_path):
    """Parse hey output file and extract key metrics"""
    metrics = {}
    
    try:
        with open(file_path, 'r') as f:
            content = f.read()
        
        # Extract metrics using regex
        patterns = {
            'total_requests': r'Total:\s+([\d.]+)\s+secs',
            'avg_response_time': r'Average:\s+([\d.]+)\s+secs',
            'fastest': r'Fastest:\s+([\d.]+)\s+secs',
            'slowest': r'Slowest:\s+([\d.]+)\s+secs',
            'rps': r'Requests/sec:\s+([\d.]+)',
            'total_data': r'Total data:\s+([\d.]+)\s+MB',
            'status_200': r'[Status 200:\s+(\d+)',
            'status_non_200': r'Bad requests:\s+(\d+)',
        }
        
        for key, pattern in patterns.items():
            match = re.search(pattern, content)
            if match:
                metrics[key] = float(match.group(1))
            else:
                metrics[key] = None
        
        # Extract percentiles
        percentiles = {}
        percentile_pattern = r'\[(\d+)\]\s+([\d.]+)\s+secs'
        for match in re.finditer(percentile_pattern, content):
            percentiles[f'p{match.group(1)}'] = float(match.group(2))
        
        metrics['percentiles'] = percentiles
        
        return metrics
    
    except Exception as e:
        print(f"Error parsing {file_path}: {e}", file=sys.stderr)
        return None

def generate_comparison_report(results_dir="./results"):
    """Generate comparison report from all test files"""
    
    results_path = Path(results_dir)
    if not results_path.exists():
        print(f"Results directory '{results_dir}' not found!")
        print("Run load tests first using run-load-test.ps1")
        sys.exit(1)
    
    # Find all .txt files (hey output)
    test_files = sorted(results_path.glob("*.txt"))
    
    if not test_files:
        print(f"No test files found in '{results_dir}'")
        print("Run load tests first using run-load-test.ps1")
        sys.exit(1)
    
    print("=" * 80)
    print("LOAD TEST COMPARISON REPORT")
    print("=" * 80)
    print(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"Results Directory: {results_path.absolute()}")
    print("=" * 80)
    print()
    
    # Parse all test files
    all_results = {}
    for test_file in test_files:
        test_name = test_file.stem
        metrics = parse_hey_output(str(test_file))
        if metrics:
            all_results[test_name] = metrics
    
    if not all_results:
        print("No valid test results found!")
        sys.exit(1)
    
    # Display individual test results
    print("\n" + "=" * 80)
    print("DETAILED TEST RESULTS")
    print("=" * 80)
    
    for test_name in sorted(all_results.keys()):
        metrics = all_results[test_name]
        print(f"\n📊 Test: {test_name}")
        print("-" * 80)
        
        if metrics.get('rps'):
            print(f"  Throughput (Requests/sec):  {metrics['rps']:.2f} req/s")
        if metrics.get('avg_response_time'):
            print(f"  Average Response Time:      {metrics['avg_response_time']*1000:.2f} ms")
        if metrics.get('fastest'):
            print(f"  Fastest Response:           {metrics['fastest']*1000:.2f} ms")
        if metrics.get('slowest'):
            print(f"  Slowest Response:           {metrics['slowest']*1000:.2f} ms")
        if metrics.get('total_data'):
            print(f"  Total Data Transferred:     {metrics['total_data']:.2f} MB")
        
        # Display percentiles if available
        if metrics.get('percentiles'):
            print(f"\n  Response Time Percentiles:")
            for p in sorted(metrics['percentiles'].keys()):
                val = metrics['percentiles'][p]
                print(f"    {p:>4}: {val*1000:.2f} ms")
    
    # Generate comparison if there are multiple tests
    if len(all_results) > 1:
        print("\n" + "=" * 80)
        print("COMPARISON SUMMARY")
        print("=" * 80)
        
        # Find best/worst for each metric
        tests_by_rps = sorted(all_results.items(), 
                             key=lambda x: x[1].get('rps', 0) or 0, 
                             reverse=True)
        
        tests_by_latency = sorted(all_results.items(), 
                                 key=lambda x: x[1].get('avg_response_time', float('inf')) or float('inf'))
        
        if tests_by_rps[0][1].get('rps'):
            print(f"\n🏆 Best Throughput: {tests_by_rps[0][0]}")
            print(f"   {tests_by_rps[0][1]['rps']:.2f} req/s")
        
        if tests_by_latency[0][1].get('avg_response_time'):
            print(f"\n⚡ Lowest Latency: {tests_by_latency[0][0]}")
            print(f"   {tests_by_latency[0][1]['avg_response_time']*1000:.2f} ms average")
        
        # Calculate improvements
        if len(tests_by_rps) >= 2:
            best_rps = tests_by_rps[0][1].get('rps', 0)
            worst_rps = tests_by_rps[-1][1].get('rps', 0)
            if best_rps and worst_rps:
                improvement = ((best_rps - worst_rps) / worst_rps) * 100
                print(f"\n📈 Throughput Improvement: {improvement:.1f}%")
                print(f"   {tests_by_rps[0][0]} vs {tests_by_rps[-1][0]}")
        
        if len(tests_by_latency) >= 2:
            best_latency = tests_by_latency[0][1].get('avg_response_time', 0)
            worst_latency = tests_by_latency[-1][1].get('avg_response_time', 0)
            if best_latency and worst_latency:
                improvement = ((worst_latency - best_latency) / worst_latency) * 100
                print(f"\n⏱️  Latency Improvement: {improvement:.1f}%")
                print(f"   {tests_by_latency[0][0]} vs {tests_by_latency[-1][0]}")
    
    # Generate CSV for further analysis
    csv_file = results_path / "comparison.csv"
    print(f"\n" + "=" * 80)
    print(f"📄 CSV Export: {csv_file}")
    print("=" * 80)
    
    with open(csv_file, 'w') as f:
        f.write("Test Name,Throughput (req/s),Avg Latency (ms),Min (ms),Max (ms),P50 (ms),P95 (ms),P99 (ms)\n")
        
        for test_name in sorted(all_results.keys()):
            metrics = all_results[test_name]
            rps = metrics.get('rps', 'N/A')
            avg_lat = metrics.get('avg_response_time', 0) * 1000 if metrics.get('avg_response_time') else 'N/A'
            min_lat = metrics.get('fastest', 0) * 1000 if metrics.get('fastest') else 'N/A'
            max_lat = metrics.get('slowest', 0) * 1000 if metrics.get('slowest') else 'N/A'
            p50 = metrics.get('percentiles', {}).get('p50', 0) * 1000 if metrics.get('percentiles', {}).get('p50') else 'N/A'
            p95 = metrics.get('percentiles', {}).get('p95', 0) * 1000 if metrics.get('percentiles', {}).get('p95') else 'N/A'
            p99 = metrics.get('percentiles', {}).get('p99', 0) * 1000 if metrics.get('percentiles', {}).get('p99') else 'N/A'
            
            f.write(f"{test_name},{rps},{avg_lat},{min_lat},{max_lat},{p50},{p95},{p99}\n")
    
    print(f"✓ Comparison saved to: {csv_file}")
    print()

if __name__ == "__main__":
    generate_comparison_report()
