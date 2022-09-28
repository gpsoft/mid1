(ns mid1.core
  (:require
    [clojure.java.io :as io]
    [mid1.midi :as midi])
  (:import
    [javax.sound.midi MidiSystem ShortMessage Sequencer Synthesizer Sequence])
  (:gen-class))

(defn open!
  []
  (let [pcspkr (midi/open-pcspkr!)
        recorder (midi/open-recorder!)
        d1-in (midi/open-d1-in!)]
    [pcspkr recorder d1-in]))

(defn close!
  [devices]
  (dorun (for [dev devices]
           (when dev (midi/close-dev! dev)))))

(comment
  (let [devs (open!)
        [pcspkr recorder d1-in] devs
        [empty-media track] (midi/new-media! :scratch nil)
        path "/var/tmp/sample.mid"]
    (when d1-in
      (midi/connect! d1-in pcspkr)
      (midi/connect! d1-in recorder)
      (midi/prepare-rec! recorder empty-media)
      (midi/start-rec!
        recorder 60000
        #(do
           (midi/save! recorder path)
           (close! devs)))))
  (let [devs (open!)
        [pcspkr recorder] devs
        path "/var/tmp/sample.mid"]
    (let [m (midi/new-media! :file path)]
      (midi/connect! recorder pcspkr)
      (midi/play!
        recorder m 60000
        #(close! devs))))
  )



(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
