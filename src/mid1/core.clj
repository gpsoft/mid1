(ns mid1.core
  (:require
    [clojure.java.io :as io]
    #_[uncomplicate.clojure-sound.core :as sound]
    [uncomplicate.clojure-sound.midi :as midi]
    #_[uncomplicate.commons.core :refer [close!]])
  (:import
    [javax.sound.midi MidiSystem ShortMessage]
    #_[javax.sound.sampled AudioSystem]
    #_[com.sun.media.sound SF2Soundbank])
  (:gen-class))


(comment
  (def defaultSynthesizer (MidiSystem/getSynthesizer))
  ; {:class "SoftSynthesizer"
  ;  :status :closed
  ;  :micro-position 0
  ;  :description "Software MIDI Synthesizer"
  ;  :name "Gervill"
  ;  :vendor "OpenJDK"
  ;  :version "1.0"}

  (count (.getLoadedInstruments defaultSynthesizer))
  ; 0
  (.open defaultSynthesizer)
  (count (.getLoadedInstruments defaultSynthesizer))
  ; 129
  (.getName (first (.getLoadedInstruments defaultSynthesizer)))
  ; "Acoustic Grand Piano"
  (.getName (nth (.getLoadedInstruments defaultSynthesizer) 6))
  ; "Harpsichord"
  (.getPatch (nth (.getLoadedInstruments defaultSynthesizer ) 6))
  ; {:bank 0, :program 6}

  (.getProgram (first (.getChannels defaultSynthesizer)))
  ; 0
  (.send (.getReceiver defaultSynthesizer) (ShortMessage. ShortMessage/NOTE_ON 0 60 50) -1)
  ; ♪Grand piano sound

  (.programChange (first (.getChannels defaultSynthesizer)) 6)
  (.send (.getReceiver defaultSynthesizer) (ShortMessage. ShortMessage/NOTE_ON 0 60 50) -1)
  ; ♪Harpsichord sound? maybe

  ;; sample midi data
  (def maple (MidiSystem/getSequence (io/resource "maple.mid")))

  (def sequencer (MidiSystem/getSequencer))
  ; {:class "RealTimeSequencer"
  ; :status :closed
  ; :micro-position 0
  ; :description "Software sequencer"
  ; :name "Real Time Sequencer"
  ; :vendor "Oracle Corporation"
  ; :version "Version 1.0"}

  (count (.getTransmitters sequencer))
  ; 1

  (def tx (first (.getTransmitters sequencer)))
  (.getReceiver tx)
  ; {:class "SoftReceiver", :id 2064131581}
  ; どこにつながってるのか良くわからん。

  ;; (.getTransmitter sequencer)すると、そのたびに新しいトランスミッタが生成される。
  ;; 既存のトランスミッタを使うなら、getTransmittersしてnthする方がいいのかな?

  (.open sequencer)

  (.getSequence sequencer)
  ; nil
  (.setSequence sequencer maple)
  (future
    (.start sequencer)
    (Thread/sleep 2000)
    (.stop sequencer))
  ; グランドピアノの音。
  ; どのシンセサイザが音を鳴らしてるのか?

  (.setReceiver tx (.getReceiver defaultSynthesizer))
  ; シーケンサのトランスミッタをdefaultSynthesizerへつなげる
  ; sequencer→synthesizer
  (future
    (.start sequencer)
    (Thread/sleep 2000)
    (.stop sequencer))
  ; ハープシコード…かなぁ?
  )



;;(def maple (midi/sequence (io/resource "maple.mid")))
;;(def fluid (midi/soundbank (io/resource "FluidR3_GM.sf2")))
;;(def salamander (midi/soundbank (io/resource "SalamanderGrandPiano.sf2")))
;;#_(def salamander (SF2Soundbank. (io/file "SalamanderGrandPiano/SalamanderGrandPiano.sfz")))
;;
;;(def sqcr (midi/sequencer))
;;
;;(comment
;;  (do (def synth (midi/synthesizer))
;;      (sound/open! synth)
;;      #_(midi/load! synth fluid)
;;      (midi/load! synth salamander)
;;      (sound/connect! sqcr synth))
;;  )
;;
;;(sound/open! sqcr)
;;(midi/sequence! sqcr maple)
;;
;;(sound/start! sqcr)
;;(sound/stop! sqcr)

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
