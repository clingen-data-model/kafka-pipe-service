(ns kafka-tools.util
  (:require [clojure.string :as s])
  (:gen-class))

(defn get-env-required
  "Performs System/getenv variable-name, but throws an exception if response is empty"
  [variable-name]
  (let [a (System/getenv variable-name)]
    (if (not (empty? a)) a
                         (ex-info (format "Environment variable %s must be provided" variable-name) {}))))

(defn to-bool
  [b]
  (not (or (.equalsIgnoreCase "false" (str b))
           (.equals "0" (str b)))))

(defn not-empty?
  [coll]
  (not (empty? coll)))