SPLIT_PATTERN="s-*-bulk.*"

ES_HOST=localhost
ES_PORT=9200
ES_INDEX=my_index_name-restore-shell

# sed -i 's/\(\"_id\":\)\([ ]*\)\([a-z0-9-]*[^}^\s*]\)/\1\"\3\"/' demo.txt
if [ -z "$1" ]; then
  echo "Please input files..."
  exit 1
fi

for var in "$@"
do
  split -10000 "${var}" s-"${var}"-bulk.
done

if [ "$?" -ne "0" ]; then
  rm -f ${SPLIT_PATTERN}
  exit 1
fi

for var in ${SPLIT_PATTERN}
do
  echo "curl -H \"Content-Type: application/x-ndjson\" -XPOST \"http://${ES_HOST}:${ES_PORT}/${ES_INDEX}/_bulk?pretty\" --data-binary \"@${var}\""
  curl -H "Content-Type: application/x-ndjson" -XPOST "http://${ES_HOST}:${ES_PORT}/${ES_INDEX}/_bulk?pretty" --data-binary "@${var}" 1> /dev/null 2>&1
done

rm -f ${SPLIT_PATTERN}
