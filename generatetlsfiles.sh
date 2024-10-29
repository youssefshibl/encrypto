#!/bin/bash

# Variables
KEYSTORE_NAME="keystore.jks"
TRUSTSTORE_NAME="truststore.jks"
KEYSTORE_PASSWORD="changeit"
TRUSTSTORE_PASSWORD="changeit"
CERT_ALIAS="mycert"
DNAME="CN=MyCert, OU=MyUnit, O=MyOrg, L=MyCity, ST=MyState, C=MyCountry"
VALIDITY_DAYS=365

# Generate KeyStore with a self-signed certificate
echo "Generating KeyStore with a self-signed certificate..."
keytool -genkeypair -alias $CERT_ALIAS -keyalg RSA -keysize 2048 \
    -keystore $KEYSTORE_NAME -storepass $KEYSTORE_PASSWORD \
    -validity $VALIDITY_DAYS -dname "$DNAME"

# Generate TrustStore and import the certificate
echo "Creating TrustStore and importing the certificate..."
keytool -exportcert -alias $CERT_ALIAS -keystore $KEYSTORE_NAME \
    -storepass $KEYSTORE_PASSWORD -file mycert.crt

keytool -importcert -alias $CERT_ALIAS -file mycert.crt \
    -keystore $TRUSTSTORE_NAME -storepass $TRUSTSTORE_PASSWORD -noprompt

# Cleanup
rm mycert.crt

echo "Keystore and Truststore generated successfully:"
echo "Keystore: $KEYSTORE_NAME"
echo "Truststore: $TRUSTSTORE_NAME"
