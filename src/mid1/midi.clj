(ns mid1.midi
  (:require
    [clojure.java.io :as io]
    [mid1.monitor :as mon])
  (:import
    [javax.sound.midi
     MidiSystem ShortMessage Sequencer Synthesizer
     Sequence Receiver MidiDevice]
    #_[jp.dip.gpsoft.mid1 Monitor]
    #_[javax.sound.sampled AudioSystem]
    #_[com.sun.media.sound SF2Soundbank]))

;; type of midi device
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

(defmulti render dev-type)
(defmethod render :sequencer [dev]
  (let [rec (.isRecording dev)
        run (.isRunning dev)
        tick (.getTickPosition dev)
        len (.getTickLength dev)
        st (if rec "recording" (if run "playing" "idle"))]
    (format "TICK:%d,LEN:%d %s" tick len st)))
(defmethod render :default [dev]
  "")

(defn render-dev
  [dev]
  (when (and dev (instance? MidiDevice dev))
    (let [klass (.getClass dev)
          id (System/identityHashCode dev)
          info (.getDeviceInfo dev)
          name (.getName info)
          open (.isOpen dev)
          type (dev-type dev)
          rxs (count (.getReceivers dev))
          txs (count (.getTransmitters dev))]
      (format "%25s %s TX:%d,RX:%d %s"
              name (if open "open" "close") txs rxs (render dev)))))

(defn render-monitor
  [monitor]
  (let [events (mon/events monitor)])
  (format "%25s %d event(s)" "Monitor" (count events)))

(defn list-devices
  []
  (let [infos (MidiSystem/getMidiDeviceInfo)
        devs (map #(MidiSystem/getMidiDevice %) infos)]
    (dorun
      (map-indexed #(println (format "[%d]%s" %1 (render-dev %2))) devs))))

(defn open-dev!
  [dev]
  (when dev
    (when-not (.isOpen dev)
      #_(prn "Open: " (System/identityHashCode dev))
      (.open dev))))

(defn close-dev!
  [dev]
  (when (and dev (instance? MidiDevice dev) (.isOpen dev))
    #_(prn "Close " (System/identityHashCode dev))
    (.close dev)))

(defn open-pcspkr!
  []
  (let [dev (MidiSystem/getSynthesizer)
        sb (MidiSystem/getSoundbank
             (io/resource
               #_"FluidR3_GM.sf2"
               "SalamanderGrandPiano.sf2"))]
    (open-dev! dev)
    (.loadAllInstruments dev sb)
    dev))

(defn open-recorder!
  []
  (let [dev (MidiSystem/getSequencer)]
    (open-dev! dev)
    dev))

;; midi-in port *FROM* Korg D1
;; or midi-out port *TO* Korg D1
(defn open-d1-port!
  [in-out]
  (let [infos (MidiSystem/getMidiDeviceInfo)
        devs (map #(MidiSystem/getMidiDevice %) infos)
        dev (first (filter #(= (dev-type %) in-out) devs))]
    (when dev
      (open-dev! dev))
    dev))
(defn open-d1-in!
  []
  (open-d1-port! :in-port))
(defn open-d1-out!
  []
  (open-d1-port! :out-port))

(defn open-monitor!
  []
  (jp.dip.gpsoft.mid1.Monitor.))

(defn connect!
  [from to]
  (when to
    (let [tx (.getTransmitter from)
          rx (if (instance? MidiDevice to)
               (.getReceiver to) to)]
      (.setReceiver tx rx))))

(defn new-media!
  [from path]
  (case from
    :resource (MidiSystem/getSequence (io/resource path))
    :file (MidiSystem/getSequence (io/file path))
    :scratch (let [s (Sequence. Sequence/PPQ 10)
                   t (.createTrack s)]
               [s t])
    nil))

(defn play!
  [recorder s elapse cc]
  (.setSequence recorder s)
  (.setTickPosition recorder 0)
  (.start recorder)
  (when (pos? elapse)
    (future
      (Thread/sleep elapse)
      (.stop recorder)
      (cc))))

(defn prepare-rec!
  [recorder s]
  (.setSequence recorder s)
  (let [t (first (.getTracks s))]
    (.recordEnable recorder t 0))
  (.setTickPosition recorder 0)
  nil)

(defn start-rec!
  [recorder elapse cc]
  (.startRecording recorder)
  (when (pos? elapse)
    (future
      (Thread/sleep elapse)
      (.stopRecording recorder)
      (cc))))

;; stop playback or recording
(defn stop!
  [recorder]
  (when (.isOpen recorder)
    (.stop recorder)))

(defn save!
  [recorder path]
  (let [s (.getSequence recorder)]
    (when s
      (let [file-type (first (MidiSystem/getMidiFileTypes s))]
        (MidiSystem/write s file-type (io/file path))))))


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

  ;; (.getTransmitter sequencer)すると、そのたびに新しいトランスミッタオブジェクトが生成される。
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
