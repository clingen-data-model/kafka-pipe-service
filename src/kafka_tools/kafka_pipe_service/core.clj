(ns kafka-tools.kafka-pipe-service.core
  (:require [cli-matic.core :as cli]
            [cheshire.core :as json]
            [jackdaw.client :as jc]
            [kafka-pipe-service.config :as cfg]
            [taoensso.timbre :as log]
            [kafka-pipe-service.util :as util]
            [jackdaw.data :as jd])
  (:import [java.lang Thread]
           (org.apache.kafka.clients.consumer KafkaConsumer)
           (org.apache.kafka.common TopicPartition)
           (java.time Duration))
  )

(defn reset-consumer-offset
  "Resets the consumer offset for all assigned partitions based on the value of subscribe-from,
  which can be either an integer or :latest.
  Is a stateful change on the consumer, but also returns the consumer."
  [^KafkaConsumer consumer subscribe-from]
  (if (= :latest subscribe-from)
    (log/info "Keeping consumer at latest offset")
    (do
      (assert (and (int? subscribe-from) (< 0 subscribe-from)))
      (log/info "Resetting consumer to offset" subscribe-from)
      ; throwaway poll to get assignment
      (jc/poll consumer 0)
      (doseq [^TopicPartition partition (.assignment consumer)]
        (jc/seek consumer partition subscribe-from)
        ))
    )
  consumer)

(def active (atom true))

(defn run-pipe
  [pipe]
  (log/info "Setting up pipe" (:name pipe))
  (let [source (:source pipe)
        destination (:destination pipe)
        source-topic-name (:topic-name source)
        destination-topic-name (:topic-name destination)
        source-cluster-cfg (get-in cfg/app-config [:clusters (keyword (:cluster source))])
        destination-cluster-cfg (get-in cfg/app-config [:clusters (keyword (:cluster destination))])
        subscribe-from (:subscribe-from pipe)
        ]
    ; TODO spec out config structure
    (assert (not-empty source-cluster-cfg) (str "Config not found for cluster " (:cluster source)))
    (assert (not-empty destination-cluster-cfg) (str "Config not found for cluster " (:cluster destination)))
    (let [consumer (jc/consumer source-cluster-cfg)
          producer (jc/producer destination-cluster-cfg)]
      (jc/subscribe consumer [{:topic-name source-topic-name}])
      ; update offset based on :subscribe-from field
      (reset-consumer-offset consumer subscribe-from)
      (log/info "Starting polling from offsets"
                ; position is the next offset that will be read, if exists
                (doall (map #(format "topic %s partition %s position %s"
                                   source-topic-name
                                   (.partition %)
                                   (.position consumer %))
                          (.assignment consumer))))
      (while @active
        (let [msgs (jc/poll consumer (Duration/ofMillis 250))]
          (doseq [msg msgs]
            (log/info "Producing message to output topic"
                      producer
                      {:topic-name destination-topic-name}
                      (:partition msg)
                      (:timestamp msg)
                      (:key msg)
                      (:value msg)
                      (:headers msg))
            (jc/produce! producer
                         {:topic-name destination-topic-name}
                         (:partition msg)
                         (:timestamp msg)
                         (:key msg)
                         (:value msg)
                         (:headers msg))
            )))
      )))

(defn -main
  "Entry point for pipe service"
  [& args]
  (log/set-level! cfg/log-level)
  (let [pipes (:pipes cfg/app-config)]
    (doseq [pipe pipes]
      (.start (Thread. ^Runnable (partial run-pipe pipe)))
      )))