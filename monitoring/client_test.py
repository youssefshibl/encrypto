import socket
import uuid
import time
import random
import argparse

def main(num_packets, num_seconds):
    server_ip = "127.0.0.1"  
    server_port = 9003  

    client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    client_socket.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
    client_socket.connect((server_ip, server_port))

    print(f"Connected to server {server_ip} on port {server_port}")

    # Convert latency from milliseconds to seconds
    sleep_time = 1.0/num_packets
    total_packets = num_packets * num_seconds
    

    try:
        for _ in range(total_packets):
            generated_uuid = str(uuid.uuid4()) + "$"
            client_socket.sendall(generated_uuid.encode('utf-8'))
            # print(f"Sent UUID: {generated_uuid}")
            time.sleep(sleep_time)
    except KeyboardInterrupt:
        print("\nClient terminated by user.")
    finally:
        client_socket.close()
        print("Connection closed.")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Send UUIDs to a server.")
    parser.add_argument("num_packets", type=int, help="Number of packets to send")
    parser.add_argument("num_seconds", type=int, help="Number of seconds to send packets")
    args = parser.parse_args()
    main(args.num_packets, args.num_seconds)