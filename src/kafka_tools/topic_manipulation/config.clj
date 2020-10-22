(ns kafka-tools.topic-manipulation.config
  (:require [kafka-tools.util :as util]))

(defn kafka-config []
  {"bootstrap.servers"                     (util/get-env-required "KAFKA_HOST")
   "application.id"                        (util/get-env-required "KAFKA_GROUP")
   "group.id"                              (util/get-env-required "KAFKA_GROUP")
   "client.id"                             (util/get-env-required "KAFKA_GROUP")
   "ssl.endpoint.identification.algorithm" "https"
   "compression.type"                      "gzip"
   "request.timeout.ms"                    "20000"
   "retry.backoff.ms"                      "500"
   "key.serializer"                        "org.apache.kafka.common.serialization.StringSerializer"
   "value.serializer"                      "org.apache.kafka.common.serialization.StringSerializer"
   "key.deserializer"                      "org.apache.kafka.common.serialization.StringDeserializer"
   "value.deserializer"                    "org.apache.kafka.common.serialization.StringDeserializer"
   "security.protocol"                     "SASL_SSL"
   "sasl.mechanism"                        "PLAIN"
   "sasl.jaas.config"                      (format
                                             "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";"
                                             (util/get-env-required "KAFKA_USER")
                                             (util/get-env-required "KAFKA_PASSWORD"))
   })