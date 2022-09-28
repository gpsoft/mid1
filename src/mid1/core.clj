(ns mid1.core
  (:require
    [mid1.midi :as midi])
  (:gen-class))

(defn open!
  []
  (let [pcspkr (midi/open-pcspkr!)
        recorder (midi/open-recorder!)
        d1-in (midi/open-d1-in!)
        monitor (midi/open-monitor!)]
    [pcspkr recorder d1-in monitor]))

(defn close!
  [devs]
  (dorun (for [dev devs]
           (when dev (midi/close-dev! dev)))))

(defn show-status
  [devs]
  (dorun
    (map #(println (midi/render-dev %)) devs)))

(defn play!
  [path sec]
  (let [devs (open!)
        [pcspkr recorder] devs]
    (let [m (midi/new-media! :file path)]
      (midi/connect! recorder pcspkr)
      (midi/play!
        recorder m (* sec 1000)
        #(close! devs)))
    (if (pos? sec) nil devs)))

(defn record!
  [path sec]
  (let [devs (open!)
        [pcspkr recorder d1-in monitor] devs
        [empty-media track] (midi/new-media! :scratch nil)]
    (when d1-in
      (midi/connect! d1-in pcspkr)
      (midi/connect! d1-in recorder)
      (midi/connect! d1-in monitor)
      (midi/prepare-rec! recorder empty-media)
      (midi/start-rec!
        recorder (* sec 1000)
        #(do
           (midi/save! recorder path)
           (close! devs))))
    (if (pos? sec) nil devs)))

(defn end!
  [[_ recorder _ :as devs] path]
  (midi/stop! recorder)
  (when path
    (midi/save! recorder path))
  (close! devs))

(comment
  (def midi-path "/var/tmp/sample.mid" )

  (play! midi-path 5)

  (def devs (play! midi-path 0))
  (show-status devs)
  (do (end! devs nil)
      (def devs nil));

  (record! midi-path 5)

  (def devs (record! midi-path 0))
  (show-status devs)
  (do (end! devs midi-path)
      (def devs nil));

  )



(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
