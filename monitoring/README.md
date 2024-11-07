



```bash
sudo tshark -i lo -f "tcp port 9003" -Y "tcp and not tcp.len == 0" -T fields -e frame.time_epoch -e tcp.seq -e ip.src -e ip.dst > capture_port_9003.txt
sudo tshark -i lo -f "tcp port 9001" -Y "tcp and not tcp.len == 0" -T fields -e frame.time_epoch -e tcp.seq -e ip.src -e ip.dst > capture_port_9001.txt

```

