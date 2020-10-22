(ns kafka-tools.topic-manipulation.topic
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as spec]
            [jackdaw.client :as jc]
            [kafka-tools.topic-manipulation.config :as cfg]
            [cheshire.core :as json]
            [jackdaw.data :as jd])
  (:import (java.time Duration)
           (org.apache.kafka.common.header Header))
  (:gen-class))

(def poll-interval (Duration/ofMillis 250))
(def polling true)

(defn download-topic-to-file
  "Download jackdaw-datafied message entries to newline-delimited file.
  consumer-config should be a map compatible with jackdaw.client/consumer"
  [topic-name consumer-config filename]
  (with-open [fwriter (io/writer (io/file filename))]
    (let [consumer (jc/consumer consumer-config)]
      (jc/subscribe consumer [{:topic-name topic-name}])
      (jc/seek-to-beginning-eager consumer)
      (println "starting polling")
      (while polling
        (do
          (let [msgs (jc/poll consumer poll-interval)]
            (println "Writing " (count msgs) " messages to output file")
            (doseq [msg msgs]
              (let [output-msg (-> {}
                                   (assoc :key (:key msg))
                                   (assoc :value (:value msg))
                                   (assoc :timestamp (:timestamp msg))
                                   ;(assoc :topic-name (:topic-name msg))
                                   ;(assoc :partition (:partition msg))
                                   (assoc :headers (into [] (for [^Header hdr (:headers msg)]
                                                              [(.key hdr) (.value hdr)]))))]
                (.write fwriter (str (json/generate-string output-msg) "\n"))))))))))

;(spec/def ::key true)
;(spec/def ::value true)
(spec/def ::timestamp int?)
(spec/def ::headers #(or (nil? %) (seqable? %)))
(spec/def ::jackdaw-message
  (spec/keys :req-un [::key ::value ::timestamp ::headers]))

(defn upload-file-to-topic
  "Upload jackdaw-datafied message entries to newline-delimited file.
  consumer-config should be a map compatible with jackdaw.client/consumer.
  Overrides topic-name of uploaded message to parameter topic-name. Uses default partition assignment."
  [topic-name consumer-config filename]
  ; Validate file
  (with-open [freader (io/reader (io/file filename))]
    (doseq [line (filter not-empty (line-seq freader))]
      (if (not (spec/valid? ::jackdaw-message (json/parse-string line true)))
        (throw (ex-info "Line did not match expected for jackdaw.client/send!"
                        {:spec-errors (spec/explain-data ::jackdaw-message line)
                         :cause line})))))
  ; Upload messages
  (with-open [freader (io/reader (io/file filename))]
    (let [producer (jc/producer consumer-config)]
      (doseq [line (filter not-empty (line-seq freader))]
        (let [msg-map (-> (json/parse-string line true)
                          (assoc :topic-name topic-name))]
          (jc/send! producer (jd/map->ProducerRecord msg-map))))
      )))