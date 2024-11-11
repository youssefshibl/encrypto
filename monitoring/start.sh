#!/bin/bash

# Prompt user to enter values
read -p "Enter Monitoring Time (default: 100): " Monitoring_Time
Monitoring_Time=${Monitoring_Time:-100}

read -p "Enter Packet Number Per Scond (default: 500 p/s): " PACKET_NUMBER
PACKET_NUMBER=${PACKET_NUMBER:-500}

read -p "Enter Time duration for the test (default: 5s): " Packet_Generation_Time
Packet_Generation_Time=${Packet_Generation_Time:-5}




# Configuration
rm -f ./capture_port_9001_.txt ./capture_port_9001_.txt ./capture_port_9001.txt ./capture_port_9003.txt
touch ./capture_port_9001_.txt ./capture_port_9001_.txt
# kill process listening on port 9001 9002 9003

PID_NC=$(lsof -t -i:9001)
if [ -n "$PID_NC" ]; then
    echo "Killing process listening on port 9001 with PID: $PID_NC"
    kill -9 $PID_NC
fi
PID_SERVER=$(lsof -t -i:9002)   
if [ -n "$PID_SERVER" ]; then
    echo "Killing process listening on port 9002 with PID: $PID_SERVER"
    kill -9 $PID_SERVER
fi
PID_CLIENT=$(lsof -t -i:9003)
if [ -n "$PID_CLIENT" ]; then
    echo "Killing process listening on port 9003 with PID: $PID_CLIENT"
    kill -9 $PID_CLIENT
fi


# run command in background

nc -l 127.0.0.1 9001  > /dev/null 2>&1 &
PID_NC=$!
echo "Netcat started with PID: $PID_NC"

# Generate TLS files
# ./generatetlsfiles.sh

sleep 2
# Start the service
java -Xms128m -Xmx128m -XX:TieredStopAtLevel=1 -XX:+EnableDynamicAgentLoading -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints -jar encrypto-1.0-SNAPSHOT.jar server 9002 127.0.0.1 9001 > ./server.log 2>&1 &
PID_SERVER=$!
echo "Server started with PID: $PID_SERVER"
sleep 2 
java -Xms128m -Xmx128m -XX:TieredStopAtLevel=1 -XX:+UnlockDiagnosticVMOptions  -XX:+DebugNonSafepoints -jar encrypto-1.0-SNAPSHOT.jar client 9003 127.0.0.1 9002 > ./client.log 2>&1 &
PID_CLIENT=$!
echo "Client started with PID: $PID_CLIENT"

#  sleep for 5 seconds
sleep 5

# start monitoring network traffic
sudo tshark -i lo -f "tcp port 9003" -Y "tcp and not tcp.len == 0" -T fields -e frame.time_epoch -e tcp.seq -e data.text -o data.show_as_text:TRUE -o tcp.desegment_tcp_streams:FALSE  -a duration:$Monitoring_Time> capture_port_9003_.txt &
PID_CLIENT_MONITOR=$!
echo "Client monitor started with PID: $PID_CLIENT_MONITOR"
sudo tshark -i lo -f "tcp port 9001" -Y "tcp and not tcp.len == 0" -T fields -e frame.time_epoch -e tcp.seq -e data.text -o data.show_as_text:TRUE -o tcp.desegment_tcp_streams:FALSE -a duration:$Monitoring_Time > capture_port_9001_.txt  &
PID_SERVER_MONITOR=$!
echo "Server monitor started with PID: $PID_SERVER_MONITOR"


./async-profiler/bin/asprof -i 100us  -e cpu -d $Monitoring_Time -f cpu_profile_server.html $PID_SERVER > /dev/null 2>&1 &
PID_SERVER_PROFILER=$!
echo "Server profiler started with PID: $PID_SERVER_PROFILER"
./async-profiler/bin/asprof -i 100us -e cpu -d $Monitoring_Time -f cpu_profile_client.html $PID_CLIENT > /dev/null 2>&1 &
PID_CLIENT_PROFILER=$!
echo "Client profiler started with PID: $PID_CLIENT_PROFILER"

# cleanup on exit
trap "kill $PID_NC $PID_SERVER $PID_CLIENT $PID_CLIENT_MONITOR $PID_SERVER_MONITOR $PID_SERVER_PROFILER $PID_CLIENT_PROFILER" EXIT

#  sleep for 5 seconds
sleep 5
python3 client_test.py $PACKET_NUMBER $Packet_Generation_Time

#  wait for the client to finish
echo "Waiting for client to finish"
wait $PID_CLIENT_MONITOR
wait $PID_SERVER_MONITOR
wait $PID_SERVER_PROFILER
wait $PID_CLIENT_PROFILER

# #  kill the server
# kill $PID_SERVER
# kill $PID_CLIENT
# kill $PID_NC

python3 process_capture.py


# rm -f ./client.log ./server.log ./capture_port_9001.txt ./capture_port_9003.txt

echo "### Monitoring Complete ###"
















