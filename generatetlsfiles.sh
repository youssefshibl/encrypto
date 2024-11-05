#!/bin/bash

# Configuration
CLIENT_ALIAS="client"
SERVER_ALIAS="server"
KEY_PASSWORD="storepass"
STORE_PASSWORD="storepass"
DAYS_VALID=365
KEYSIZE=2048

# Directory for output
OUTPUT_DIR="./jks"
mkdir -p "$OUTPUT_DIR"

echo "### Generating Client KeyStore (client.jks) ###"
# Generate client keystore
keytool -genkeypair -alias $CLIENT_ALIAS -keyalg RSA -keysize $KEYSIZE -validity $DAYS_VALID \
    -keystore "$OUTPUT_DIR/client.jks" -dname "CN=Client, OU=ClientOrg, O=ClientCompany, L=City, S=State, C=US" \
    -storepass $STORE_PASSWORD -keypass $KEY_PASSWORD

echo "### Generating Server KeyStore (server.jks) ###"
# Generate server keystore
keytool -genkeypair -alias $SERVER_ALIAS -keyalg RSA -keysize $KEYSIZE -validity $DAYS_VALID \
    -keystore "$OUTPUT_DIR/server.jks" -dname "CN=Server, OU=ServerOrg, O=ServerCompany, L=City, S=State, C=US" \
    -storepass $STORE_PASSWORD -keypass $KEY_PASSWORD

echo "### Exporting Public Certificates ###"
# Export client certificate
keytool -export -alias $CLIENT_ALIAS -keystore "$OUTPUT_DIR/client.jks" -storepass $STORE_PASSWORD \
    -file "$OUTPUT_DIR/client.crt"

# Export server certificate
keytool -export -alias $SERVER_ALIAS -keystore "$OUTPUT_DIR/server.jks" -storepass $STORE_PASSWORD \
    -file "$OUTPUT_DIR/server.crt"

echo "### Creating TrustStore (truststore.jks) and Importing Certificates ###"
# Create a truststore and import client certificate
keytool -import -alias "$CLIENT_ALIAS-cert" -file "$OUTPUT_DIR/client.crt" -keystore "$OUTPUT_DIR/trustedCerts.jks" \
    -storepass $STORE_PASSWORD -noprompt

# Import server certificate into truststore
keytool -import -alias "$SERVER_ALIAS-cert" -file "$OUTPUT_DIR/server.crt" -keystore "$OUTPUT_DIR/trustedCerts.jks" \
    -storepass $STORE_PASSWORD -noprompt

echo "### Cleanup: Removing Temporary Certificate Files ###"
rm "$OUTPUT_DIR/client.crt" "$OUTPUT_DIR/server.crt"

echo "### JKS and TrustStore Generation Complete ###"
echo "Client KeyStore: $OUTPUT_DIR/client.jks"
echo "Server KeyStore: $OUTPUT_DIR/server.jks"
echo "TrustStore: $OUTPUT_DIR/truststore.jks"
