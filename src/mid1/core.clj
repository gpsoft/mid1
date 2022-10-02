(ns mid1.core
  (:require
    [mid1.midi :as midi]
    [mid1.monitor :as mon])
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
    (let [m (midi/new-media! :file (str path ".mid"))]
      (midi/connect! recorder pcspkr)
      (midi/play!
        recorder m (* sec 1000)
        #(close! devs)))
    (if (pos? sec) nil nodes)))

(defn save!
  [path recorder monitor]
  (midi/save! recorder (str path ".mid"))
  (mon/save! monitor (str path ".edn")))

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
           (when path
             (save! path recorder monitor))
           (close! devs))))
    (if (pos? sec) nil nodes)))

(defn end!
  [[[_ recorder _ :as devs] monitor] path]
  (midi/stop! recorder)
  (when path
    (save! path recorder monitor))
  (close! devs))

(comment
  (def out-path "/var/tmp/sample" )

  (play! out-path 5)

  (def nodes (play! out-path 0))
  (show-status nodes)
  (do (end! nodes nil)
      (def nodes nil));

  (record! out-path 5)

  (def nodes (record! out-path 0))
  (show-status nodes)
  (do (end! nodes out-path)
      (def nodes nil));

  (require '[clojure.java.io :as io]
           '[clojure.edn :as edn]
           '[clojure.pprint :as pp])
  (let [events (->> "sample.edn"
                    io/resource
                    slurp
                    edn/read-string
                    (sort-by first))
        score (mon/render-events events)]
    (spit "/var/tmp/b.edn"
          (with-out-str (pp/pprint score)) ))

  )



(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
