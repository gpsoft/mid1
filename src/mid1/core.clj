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
    [[pcspkr recorder d1-in] monitor]))

(defn close!
  [devs]
  (dorun (for [dev devs]
           (when dev (midi/close-dev! dev)))))

(defn show-status
  [[devs monitor]]
  (dorun
    (map #(println (midi/render-dev %)) devs))
  (println (midi/render-monitor monitor)))

(defn play!
  [path sec]
  (let [[devs :as nodes] (open!)
        [pcspkr recorder] devs]
    (let [m (midi/new-media! :file path)]
      (midi/connect! recorder pcspkr)
      (midi/play!
        recorder m (* sec 1000)
        #(close! devs)))
    (if (pos? sec) nil nodes)))

(defn record!
  [path sec]
  (let [[devs monitor :as nodes] (open!)
        [pcspkr recorder d1-in] devs
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
    (if (pos? sec) nil nodes)))

(defn end!
  [[[_ recorder _ :as devs] monitor] path]
  (midi/stop! recorder)
  (when path
    (midi/save! recorder path))
  (close! devs))

(comment
  (def midi-path "/var/tmp/sample.mid" )

  (play! midi-path 5)

  (def nodes (play! midi-path 0))
  (show-status nodes)
  (do (end! nodes nil)
      (def nodes nil));

  (record! midi-path 5)

  (def nodes (record! midi-path 0))
  (show-status nodes)
  (do (end! nodes midi-path)
      (def nodes nil));

  )



(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
