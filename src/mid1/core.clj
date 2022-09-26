(ns mid1.core
  (:require
    [clojure.java.io :as io]
    [uncomplicate.clojure-sound.core :as sound]
    [uncomplicate.clojure-sound.midi :as midi]
    [uncomplicate.commons.core :refer [close!]])
  (:gen-class))

(def maple (midi/sequence (io/resource "maple.mid")))

(def sqcr (midi/sequencer))
(sound/open! sqcr)
(midi/sequence! sqcr maple)
(sound/start! sqcr)

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
