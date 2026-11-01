#!/usr/bin/env bash
# Downloads the archive the user shared and checks that it really holds Downhill Domination.
# The zip is 2.42 GB and unpacks to a 2.77 GB ISO - keep ~6 GB free.
set -euo pipefail
OUT="${1:-downhill-domination.zip}"
TRANSFER_ID="202611018n8sG1cN"
FILE_ID="udQuP9"

if [ ! -f "$OUT" ]; then
  URL=$(curl -s "https://www.transfernow.net/api/transfer/downloads/link?transferId=$TRANSFER_ID&fileId=$FILE_ID" \
        | python3 -c "import json,sys;print(json.load(sys.stdin)['url'])")
  echo "baixando de $URL"
  curl -L --retry 3 -o "$OUT" "$URL"
fi
ls -l "$OUT"
python3 - "$OUT" <<'PY'
import sys, zipfile
z = zipfile.ZipFile(sys.argv[1])
for i in z.infolist():
    print(f"{i.filename}  {i.compress_size/2**30:.2f} GB zip -> {i.file_size/2**30:.2f} GB")
PY
