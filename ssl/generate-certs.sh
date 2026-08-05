#!/usr/bin/env bash
# Generate SSL keystore (server) and truststore (client) for HRM RMI.
# Run from project root: bash ssl/generate-certs.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
STORE_PASS="${SSL_STORE_PASS:-changeit}"
VALIDITY_DAYS="${SSL_VALIDITY_DAYS:-3650}"
DNAME="${SSL_DNAME:-CN=localhost, OU=HRM, O=BHEL, L=City, ST=State, C=MY}"

cd "$SCRIPT_DIR"

echo "Generating server-keystore.jks ..."
keytool -genkeypair \
  -alias hrmserver \
  -keyalg RSA \
  -keysize 2048 \
  -validity "$VALIDITY_DAYS" \
  -keystore server-keystore.jks \
  -storepass "$STORE_PASS" \
  -keypass "$STORE_PASS" \
  -dname "$DNAME"

echo "Exporting server certificate ..."
keytool -exportcert \
  -alias hrmserver \
  -keystore server-keystore.jks \
  -storepass "$STORE_PASS" \
  -file server.cer

echo "Creating client-truststore.jks ..."
keytool -importcert \
  -alias hrmserver \
  -file server.cer \
  -keystore client-truststore.jks \
  -storepass "$STORE_PASS" \
  -noprompt

echo
echo "Done. Files in: $SCRIPT_DIR"
echo "  server-keystore.jks   (server)"
echo "  client-truststore.jks (client)"
echo "  server.cer            (exported cert)"
echo
echo "Default store password: $STORE_PASS"
echo "Update config.properties ssl.* paths if needed."
