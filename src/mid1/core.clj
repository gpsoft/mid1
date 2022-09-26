(ns mid1.core
  (:require
    [clojure.java.io :as io]
    [uncomplicate.clojure-sound.core :as sound]
    [uncomplicate.clojure-sound.midi :as midi]
    [uncomplicate.commons.core :refer [close!]])
  (:gen-class))

(def maple (midi/sequence (io/resource "maple.mid")))
(def fluid (midi/soundbank (io/resource "FluidR3_GM.sf2")))

(def sqcr (midi/sequencer))

(comment
  (do (def synth (midi/synthesizer))
      (sound/open! synth)
      (midi/load! synth fluid)
      (sound/connect! sqcr synth))
  )

(sound/open! sqcr)
(midi/sequence! sqcr maple)

(sound/start! sqcr)
(sound/stop! sqcr)

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
