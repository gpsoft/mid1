(ns mid1.core
  (:require
    [clojure.java.io :as io]
    [uncomplicate.clojure-sound.core :as sound]
    [uncomplicate.clojure-sound.midi :as midi]
    [uncomplicate.commons.core :refer [close!]])
  (:import
    [com.sun.media.sound SF2Soundbank])
  (:gen-class))

(def maple (midi/sequence (io/resource "maple.mid")))
(def fluid (midi/soundbank (io/resource "FluidR3_GM.sf2")))
(def salamander (midi/soundbank (io/resource "SalamanderGrandPiano.sf2")))

(def sqcr (midi/sequencer))

(comment
  (do (def synth (midi/synthesizer))
      (sound/open! synth)
      #_(midi/load! synth fluid)
      (midi/load! synth salamander)
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
