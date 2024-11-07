import socket
import uuid
import time
import random

server_ip = "127.0.0.1"  
server_port = 9003  


client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

client_socket.connect((server_ip, server_port))

print(f"Connected to server {server_ip} on port {server_port}")

try:
    while True:
        generated_uuid = str(uuid.uuid4())
        
        client_socket.sendall(generated_uuid.encode('utf-8'))
        
        print(f"Sent UUID: {generated_uuid}")
        
        # Sleep for a random time between 50 ms and 500 ms
        time.sleep(random.uniform(0.05, 0.5))
except KeyboardInterrupt:
    print("\nClient terminated by user.")


client_socket.close()
print("Connection closed.")
