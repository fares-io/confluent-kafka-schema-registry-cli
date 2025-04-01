#!/usr/bin/env bash

KEY_DIR=src/main/resources/certificates

mkdir -p ${KEY_DIR}
rm -f ${KEY_DIR}/*

echo "generate server key and certificate"

openssl req -x509 -nodes -newkey rsa:4096 \
            -keyout ${KEY_DIR}/server.key \
            -out ${KEY_DIR}/server.pem \
            -subj "/CN=localhost" \
            -days 3650 \
            -config openssl.conf \
            -extensions 'v3_req'

echo "package server key and certificate"

openssl pkcs12 -export -inkey ${KEY_DIR}/server.key -in ${KEY_DIR}/server.pem -out ${KEY_DIR}/server.pfx -passout pass:  -name "server"

echo "verify server certificate"

openssl x509 -in ${KEY_DIR}/server.pem -noout -text

openssl x509 -in ${KEY_DIR}/server.pem -out ${KEY_DIR}/server.der -outform DER

# verify server private key
#openssl rsa -noout -text -in ${KEY_DIR}/server.key

cat ${KEY_DIR}/server.der | base64 -w 0 > ${KEY_DIR}/server.b64
