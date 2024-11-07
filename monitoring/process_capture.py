import matplotlib.pyplot as plt
from datetime import datetime

def convert_to_microseconds(timestamp):
    seconds, nanoseconds = timestamp.split(".")
    return int(seconds) * 1_000_000 + int(nanoseconds) // 1_000  

# Read the data from both capture files
def read_capture_file(filename):
    capture_data = {}
    with open(filename, 'r') as file:
        for line in file:
            
            fields = line.strip().split('\t')
            if len(fields) < 4:
                continue  

            timestamp = fields[0] 
            seq_number = fields[1] 
            src_ip = fields[2]  
            dst_ip = fields[3]  

            capture_data[seq_number] = {
                "timestamp": timestamp,
                "src_ip": src_ip,
                "dst_ip": dst_ip,
            }
    return capture_data

def process_capture_files(capture_file_9003, capture_file_9001):
    capture_data_9003 = read_capture_file(capture_file_9003)
    capture_data_9001 = read_capture_file(capture_file_9001)

    seq_numbers = []
    latencies = []

    for seq in capture_data_9003:
        if seq in capture_data_9001:
            timestamp_9003 = capture_data_9003[seq]['timestamp']
            timestamp_9001 = capture_data_9001[seq]['timestamp']
            
            # Convert timestamps to microseconds
            timestamp_9003_us = convert_to_microseconds(timestamp_9003)
            timestamp_9001_us = convert_to_microseconds(timestamp_9001)

            time_diff_us = timestamp_9003_us - timestamp_9001_us

            seq_numbers.append(int(seq))
            latencies.append(time_diff_us)

            print(f"Seq: {seq}, Time Diff: {time_diff_us} µs (Start: {timestamp_9003}, End: {timestamp_9001})")
    
    plt.figure(figsize=(10, 6))
    plt.plot(seq_numbers, latencies ,color='b', linestyle='-')
    plt.title("Latency vs Sequence Number")
    plt.xlabel("Sequence Number")
    plt.ylabel("Latency (µs)")
    plt.grid()

    plt.savefig("latency_vs_sequence.png")
    print("Plot saved as 'latency_vs_sequence.png'")

    # plt.show()

capture_file_9003 = 'capture_port_9003.txt'
capture_file_9001 = 'capture_port_9001.txt'

process_capture_files(capture_file_9003, capture_file_9001)
