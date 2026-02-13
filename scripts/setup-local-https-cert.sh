#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CERT_DIR="$ROOT_DIR/src/main/resources/certs"
P12_PATH="$CERT_DIR/local-dev.p12"
CERT_PEM="$CERT_DIR/localhost.pem"
KEY_PEM="$CERT_DIR/localhost-key.pem"
ALIAS="local-dev"
PASSWORD="${SSL_KEYSTORE_PASSWORD:-changeit}"

if ! command -v mkcert >/dev/null 2>&1; then
  echo "mkcert가 설치되어 있지 않습니다."
  echo "macOS(Homebrew): brew install mkcert nss && mkcert -install"
  exit 1
fi

if ! command -v openssl >/dev/null 2>&1; then
  echo "openssl이 필요합니다."
  exit 1
fi

mkdir -p "$CERT_DIR"

mkcert -install
mkcert -cert-file "$CERT_PEM" -key-file "$KEY_PEM" localhost 127.0.0.1 ::1

openssl pkcs12 -export \
  -in "$CERT_PEM" \
  -inkey "$KEY_PEM" \
  -out "$P12_PATH" \
  -name "$ALIAS" \
  -password "pass:$PASSWORD"

echo "완료: $P12_PATH"
echo "실행: SSL_KEYSTORE_PASSWORD=$PASSWORD ./gradlew bootRun --args='--spring.profiles.active=local,local-https'"
