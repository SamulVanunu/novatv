#!/usr/bin/env bash
# Geliştirme/yan yükleme için kendinden imzalı release anahtarı üretir.
#
#   tools/make-keystore.sh
#
# Anahtar ~/.android-keys/novatv.jks, parolalar kök dizindeki
# keystore.properties içine yazılır. Her ikisi de git'e girmez.
# Var olan anahtar silinmez; betik yeniden çalıştırılabilir.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KEY_DIR="${KEY_DIR:-$HOME/.android-keys}"
STORE="$KEY_DIR/novatv.jks"
PROPS="$ROOT/keystore.properties"
ALIAS="novatv"

if [ -f "$STORE" ]; then
  echo "anahtar zaten var: $STORE (dokunulmadı)"
  exit 0
fi

JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
KEYTOOL="$JAVA_HOME/bin/keytool"
if [ ! -x "$KEYTOOL" ]; then
  KEYTOOL="$(command -v keytool || true)"
fi
if [ -z "$KEYTOOL" ]; then
  echo "keytool bulunamadı; JAVA_HOME ayarlayın" >&2
  exit 1
fi

mkdir -p "$KEY_DIR"
chmod 700 "$KEY_DIR"

# Yalnızca harf/rakam: kabuk ve properties dosyasında kaçış gerekmesin.
PASS="$(head -c 48 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 32)"

"$KEYTOOL" -genkeypair \
  -keystore "$STORE" \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -storepass "$PASS" \
  -keypass "$PASS" \
  -dname "CN=NovaTV, OU=Development, O=Anon, L=Istanbul, C=TR"

chmod 600 "$STORE"

umask 077
cat >"$PROPS" <<EOF
# tools/make-keystore.sh tarafından üretildi — git'e eklemeyin.
storeFile=$STORE
storePassword=$PASS
keyAlias=$ALIAS
keyPassword=$PASS
EOF

echo "anahtar: $STORE"
echo "imza ayarları: $PROPS"
