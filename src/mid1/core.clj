(ns mid1.core
  (:require
    [clojure.java.io :as io]
    #_[uncomplicate.clojure-sound.core :as sound]
    [uncomplicate.clojure-sound.midi :as midi]
    #_[uncomplicate.commons.core :refer [close!]])
  (:import
    [javax.sound.midi MidiSystem ShortMessage Sequencer Synthesizer Sequence]
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
  (.getDefaultSoundbank defaultSynthesizer)
  ; {:class "SF2Soundbank"
  ;  :description "Emergency generated soundbank"
  ;  :name "Emergency GM sound set"
  ;  :vendor "Generated"
  ;  :version "2.1"}
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
  ;  :status :closed
  ;  :micro-position 0
  ;  :description "Software sequencer"
  ;  :name "Real Time Sequencer"
  ;  :vendor "Oracle Corporation"
  ;  :version "Version 1.0"}

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
    (.setTickPosition sequencer 0)
    (.start sequencer)
    (Thread/sleep 2000)
    (.stop sequencer))
  ; ハープシコード…かなぁ?

  (.unloadAllInstruments defaultSynthesizer (.getDefaultSoundbank defaultSynthesizer ))
  (def fluid (MidiSystem/getSoundbank (io/resource "FluidR3_GM.sf2")))
  (.loadAllInstruments defaultSynthesizer fluid)
  (dorun
    (map (fn [inst]
           (let [prg (.getProgram (.getPatch inst))]
             (future
               (Thread/sleep (* 2200 prg))
               (prn (.getName inst))
               (.programChange (first (.getChannels defaultSynthesizer)) prg)
               #_(.setTickPosition sequencer 0)
               (.start sequencer)
               (Thread/sleep 2000)
               (.stop sequencer))))
         (take 40 (.getLoadedInstruments defaultSynthesizer))))

  (def salamander (MidiSystem/getSoundbank (io/resource "SalamanderGrandPiano.sf2")))
  (.unloadAllInstruments defaultSynthesizer fluid)
  (.loadInstrument defaultSynthesizer (first (.getInstruments salamander)))
  (future
    (.setTickPosition sequencer 0)
    (.start sequencer)
    (Thread/sleep 10000)
    (.stop sequencer))
  )

(defn dev-type
  [dev]
  (let [sqcr (instance? Sequencer dev)
        synth (instance? Synthesizer dev)
        port (not (or sqcr synth))
        in (zero? (.getMaxReceivers dev))]
    (cond
      sqcr :sequencer
      synth :synthesizer
      in :in-port
      :else :out-port)))

(defn render-dev
  [dev]
  (let [klass (.getClass dev)
        id (System/identityHashCode dev)
        info (.getDeviceInfo dev)
        hash (.hashCode info)
        name (.getName info)
        open (.isOpen dev)
        type (dev-type dev)
        rxs (count (.getReceivers dev))
        txs (count (.getTransmitters dev))]
    (format "%s(%s) TX:%d,RX:%d %s" name (if open "open" "close") txs rxs type)))

(defn devices
  []
  (let [infos (MidiSystem/getMidiDeviceInfo)
        devs (map #(MidiSystem/getMidiDevice %) infos)]
    (dorun
      (map-indexed #(println (format "[%d]%s" %1 (render-dev %2))) devs))))

(defn piano
  []
  (let [dev (MidiSystem/getSynthesizer)
        sb (MidiSystem/getSoundbank (io/resource "SalamanderGrandPiano.sf2"))]
    (.open dev)
    (.loadAllInstruments dev sb)
    dev))

(defn in-port
  []
  (let [infos (MidiSystem/getMidiDeviceInfo)
        devs (map #(MidiSystem/getMidiDevice %) infos)
        dev (first (filter #(= (dev-type %) :in-port) devs))]
    (when dev
      (.open dev))
    dev))

(defn connect
  [from to]
  (let [tx (.getTransmitter from)]
    (.setReceiver tx (.getReceiver to))))

(comment
  (let [synth (piano)
        in (in-port)
        sqcr (MidiSystem/getSequencer)
        _ (.open sqcr)
        _ (connect sqcr synth)
        empty-seq (Sequence. Sequence/PPQ 10)
        - (println (MidiSystem/getMidiFileTypes empty-seq))
        track (.createTrack empty-seq)
        _ (.setSequence sqcr empty-seq)
        _ (.recordEnable sqcr track 0)]
    (if in
      (do (connect in sqcr)
          (println "size:" (.size track))
          (.startRecording sqcr)
          (future
            (Thread/sleep 10000)
            (.stopRecording sqcr)
            (println "size:" (.size track))
            (MidiSystem/write empty-seq 1 (io/file "/var/tmp/sample.mid"))
            (.close in)
            (.close sqcr)
            (.close synth)))
      (do
        (.setSequence sqcr (MidiSystem/getSequence (io/file "/var/tmp/sample.mid")))
        (future
          (.setTickPosition sqcr 0)
          (.start sqcr)
          (Thread/sleep 10000)
          (.stop sqcr)
          (.close sqcr)
          (.close synth))))
    )
  )

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
