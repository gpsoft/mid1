(ns mid1.core
  (:require
    [clojure.java.io :as io]
    [overtone.at-at :as at]
    [mid1.midi :as midi]
    [mid1.monitor :as mon])
  (:gen-class))

(defonce st (atom {:mode :idle :nodes nil}))
(defn st-mode [] (:mode @st))
(defn st-nodes [] (:nodes @st))
(defn st-mode! [mode] (swap! st assoc :mode mode))
(defn st-nodes! [nodes] (swap! st assoc :nodes nodes))
(defn st-idle? [] (= (st-mode) :idle))
(defn st-playing? [] (= (st-mode) :playing))
(defn st-recording? [] (= (st-mode) :recording))

(defn tmp-dir [] (System/getProperty "java.io.tmpdir"))
(defn path-separator [] (System/getProperty "file.separator"))
(defonce savedir-path (let [dir (tmp-dir)
                            sepa (path-separator)
                            dir (if (.endsWith dir sepa) dir (str dir sepa))]
                        (str dir "mid1" sepa)))
(defonce base-path (let [_ (io/make-parents savedir-path)
                         path (str savedir-path "mid1")]
                     path))

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
  (mon/save-events! monitor (str path "-raw.edn"))
  (mon/save-for-mpnote! monitor (str path "-mpnote.edn")))

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
        score (mon/render-for-mpnote events)]
    (spit "/var/tmp/b.edn"
          (with-out-str (pp/pprint score)) ))

  (let [events '([6520 :note 56 42 731]
                 [6559 :note 44 41 1778]
                 [6566 :note 60 46 1714]
                 [7174 :note 51 31 633]
                 [7741 :note 56 36 652]
                 [8368 :note 51 17 434]
                 [8876 :note 49 30 1805]
                 )
        events (->> (str base-path "-raw.edn")
                    slurp
                    edn/read-string
                    (sort-by first))
        html (mon/render-for-html events)]
    (spit (str base-path "-hoge.html") html))
  )


(defn cmd-start-playback
  []
  (if (st-idle?)
    (let [nodes (play! base-path 0)]
      (println "PLAYING")
      (st-mode! :playing)
      (st-nodes! nodes))
    (println "Stop playback/recording first.")))

(defn cmd-start-recording
  []
  (if (st-idle?)
    (let [nodes (record! base-path 0)]
      (println "RECORDING")
      (st-mode! :recording)
      (st-nodes! nodes))
    (println "Stop playback/recording first.")))

(defn cmd-stop
  []
  (if (st-idle?) nil
    (let [nodes (st-nodes)
          path (if (st-recording?) base-path nil)]
      (end! nodes path)
      (st-mode! :idle)
      (st-nodes! nil))))

(defn cmd-open-folder
  []
  (.open (java.awt.Desktop/getDesktop)
         (new java.io.File savedir-path)))

(defn cmd-show-status
  []
  (println "### STATUS ###")
  (if (st-idle?)
    (println "IDLE")
    (show-status (st-nodes))))

(defn cmd-show-menu
  []
  (println "### MENU ###")
  (println "p: start playback")
  (println "r: start recording")
  (println "s: stop playback or recording")
  (println "o: open folder")
  (println "m: show status")
  (println "q: quit")
  (println "?: show menu")
  true)

(defn cmd-auto
  []
  (case (st-mode)
    :playing (cmd-stop)
    :recording (cmd-stop)
    (cmd-show-menu)))

(defn cmd-interval
  []
  #_(show-status (st-nodes))
  (when (st-recording?)
    (let [[_ monitor] (st-nodes)]
      (mon/save-in-html! monitor (str base-path ".html")))))

(defn do-cmd
  [cmd]
  (println "------------------")
  (case cmd
    "" (cmd-auto)
    "p" (cmd-start-playback)
    "r" (cmd-start-recording)
    "s" (cmd-stop)
    "o" (cmd-open-folder)
    "m" (cmd-show-status)
    "q" :quit
    "?" (cmd-show-menu)
    nil))

(defn prompt
  []
  (print ">")
  (flush)
  (read-line))

(defn -main
  [& args]
  (let [pool (at/mk-pool)
        interval (at/every 500 #'cmd-interval pool)]
    (try
      (loop [cmd (prompt)]
        (when (not= (do-cmd cmd) :quit)
          (recur (prompt))))
      (catch Exception e
        (prn e))
      (finally (at/stop interval)
               (at/stop-and-reset-pool! pool)
               #_(shutdown-agents)
               (println "Good bye")
               (flush)))))
