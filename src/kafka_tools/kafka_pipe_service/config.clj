(ns kafka-tools.kafka-pipe-service.config
  (:require [kafka-tools.util :as util])
  (:gen-class))

(def log-level :debug)

(def app-config
  {
   ; Define clusters and client configurations for each
   :clusters {:broad-confluent-prod
               {"bootstrap.servers"                     (util/get-env-required "KAFKA_HOST")
                ;"application.id"                        (util/get-env-required "KAFKA_GROUP")
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
                }
              ;"aws-kafka"
              ; {"bootstrap.servers" (util/get-env-required "DX_MIGRATION_SOURCE_HOST")
              ;  "application.id" (util/get-env-required "DX_MIGRATION_GROUP")
              ;  "enable.auto.commit" "true"
              ;  "compression.type" "gzip"
              ;  "auto.commit.interval.ms" "1000"
              ;  "key.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"
              ;  "value.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"
              ;  "security.protocol" "SSL"
              ;  "ssl.truststore.location" (util/get-env-required "DX_MIGRATION_TRUSTSTORE")
              ;  "ssl.truststore.password" (util/get-env-required "DX_MIGRATION_TRUSTSTORE_PASS")
              ;  "ssl.keystore.location" (util/get-env-required "DX_MIGRATION_KEYSTORE")
              ;  "ssl.keystore.password" (util/get-env-required "DX_MIGRATION_KEYSTORE_PASS")
              ;  "ssl.key.password" (util/get-env-required "DX_MIGRATION_KEY_PASS")
              ;  "ssl.endpoint.identification.algorithm" "" ; Disable host name / CN identification
              ;  }
               }
   ; Define the pipes to construct and run
   :pipes    [
               {
                :name           "broad-dsp-clinvar-backup"
                ; :subscribe-from can be an integer or :latest
                ; :latest means to start from the current offset for the consumer group
                :subscribe-from :latest
                :source         {:cluster    "broad-confluent-prod"
                                 :topic-name "broad-dsp-clinvar"}
                :destination    {:cluster    "broad-confluent-prod"
                                 :topic-name "broad-dsp-clinvar-backup"}
                }
               ]
   })
