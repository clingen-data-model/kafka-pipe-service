
offset=0
group=broad-test-group
topic=broad-dsp-clinvar

kafka-consumer-groups --command-config kafka-prod.properties \
  --bootstrap-server pkc-4yyd6.us-east1.gcp.confluent.cloud:9092 \
  --reset-offsets \
  --to-offset $offset \
  --group $group --topic $topic \
  --execute
