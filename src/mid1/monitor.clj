(ns mid1.monitor
  (:import
    [javax.sound.midi MidiMessage ShortMessage]))

(comment
  ;; need to compile on the REPL to generate class file?
  (compile 'mid1.monitor)
  )

(gen-class
  :name jp.dip.gpsoft.mid1.Monitor
  :implements [javax.sound.midi.Receiver]
  :prefix mon-
  )

(defn mon-close [this])

(defn mon-send [this ^ShortMessage message timeStamp]
  (when (= (.getStatus message) ShortMessage/NOTE_ON)
    (println timeStamp (.getData1 message) (.getData2 message))))
