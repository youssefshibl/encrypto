import matplotlib.pyplot as plt
import numpy as np
import argparse


def convert_to_milliseconds(timestamp):
    seconds, nanoseconds = timestamp.split(".")
    return int(seconds) * 1_000 + int(nanoseconds) // 1_000_000  

# Read the data from both capture files
def read_capture_file(filename):
    capture_data = {}
    with open(filename, 'r') as file:
        for line in file:
            fields = line.strip().split('\t')
            if len(fields) < 3:
                continue  

            timestamp = fields[0] 
            seq_number = fields[1] 
            uuid = fields[2]  

            capture_data[uuid] = {
                "timestamp": timestamp,
                "seq_number": seq_number,
            }
    return capture_data

def process_capture_files(capture_file_9003, capture_file_9001,num_packets):
    capture_data_9003 = read_capture_file(capture_file_9003)
    capture_data_9001 = read_capture_file(capture_file_9001)

    seq_numbers = []
    latencies = []
    missing_seq_numbers = []

    for uuid in capture_data_9003 :
        if uuid in capture_data_9001:
            timestamp_9003 = capture_data_9003[uuid]['timestamp']
            timestamp_9001 = capture_data_9001[uuid]['timestamp']
            seq_number = capture_data_9003[uuid]['seq_number']
            
            # Convert timestamps to milliseconds
            timestamp_9003_ms = convert_to_milliseconds(timestamp_9003)
            timestamp_9001_ms = convert_to_milliseconds(timestamp_9001)

            time_diff_ms = timestamp_9001_ms - timestamp_9003_ms

            seq_numbers.append(int(seq_number))
            latencies.append(time_diff_ms)

            # print(f"Seq Number: {seq_number}, Time Diff: {time_diff_ms} ms (Start: {timestamp_9003}, End: {timestamp_9001})")
        else:
            missing_seq_numbers.append(capture_data_9003[uuid]['seq_number'])

    
    # Calculate statistics
    avg_latency = np.mean(latencies)
    p95_latency = np.percentile(latencies, 95)
    p99_latency = np.percentile(latencies, 99)

    plt.figure(figsize=(10, 6))
    plt.scatter(seq_numbers, latencies, color='b', s=10)  # Use scatter plot for scalability
    # plt.plot(seq_numbers, latencies, color='b', linestyle='-')  # Add lines between points

    # mark missing sequence numbers as red points
    if missing_seq_numbers:
        missing_seq_numbers_x = [int(seq_number) for seq_number in missing_seq_numbers]
        missing_seq_numbers_y = [0] * len(missing_seq_numbers)
        plt.plot(missing_seq_numbers_x, missing_seq_numbers_y, 'ro', label='Missing ' + str(len(missing_seq_numbers)) + ' seq numbers')



    # Add lines for average, P95, and P99 latencies
    plt.axhline(y=avg_latency, color='g', linestyle='--', label=f'Average: {avg_latency:.2f} ms')
    plt.axhline(y=p95_latency, color='r', linestyle='--', label=f'P95: {p95_latency:.2f} ms')
    plt.axhline(y=p99_latency, color='m', linestyle='--', label=f'P99: {p99_latency:.2f} ms')

    #  add total number of packets label
    plt.text(0.5, 0.5, f'Total Packets: {len(capture_data_9003)} \n Packets Rate: {num_packets}P/s', horizontalalignment='center', verticalalignment='center', transform=plt.gca().transAxes)
    

    plt.title("Latency vs Sequence Number")
    plt.xlabel("Sequence Number")
    plt.ylabel("Latency (ms)")
    plt.grid()

    # Adjust x-axis ticks for better readability
    plt.xticks(rotation=45)
    plt.locator_params(axis='x', nbins=10)

    plt.legend()
    plt.savefig("latency_vs_seq_number.png")
    print("Plot saved as 'latency_vs_seq_number.png'")

    # plt.show()
def process_file(input_file, output_file):
    with open(input_file, 'r') as infile, open(output_file, 'w') as outfile:
        new_seq_number = 1  

        for line in infile:
            parts = line.strip().split("\t")
            if len(parts) < 3:
                continue
            timestamp = parts[0]
            original_seq = parts[1]
            uuids = parts[2].split("$")
            for uuid in filter(bool, uuids):
                outfile.write(f"{timestamp}\t{new_seq_number}\t{uuid}\n")
                new_seq_number += 1  # Increment sequence number for the new file

unprocessed_file_9003 = 'capture_port_9003_.txt'
capture_file_9003 = 'capture_port_9003.txt'
unprocessed_file_9001 = 'capture_port_9001_.txt'
capture_file_9001 = 'capture_port_9001.txt'
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Send UUIDs to a server.")
    parser.add_argument("num_packets", type=int, help="Number of packets to send")
    args = parser.parse_args()
    process_file(unprocessed_file_9003, capture_file_9003)
    process_file(unprocessed_file_9001, capture_file_9001)
    process_capture_files(capture_file_9003, capture_file_9001,args.num_packets)