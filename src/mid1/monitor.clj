(ns mid1.monitor
  (:require
    [clojure.pprint :as pp])
  (:import
    [javax.sound.midi MidiMessage ShortMessage]))

(comment
  ;; need to compile on the REPL to generate class file?
  (compile 'mid1.monitor)
  )

(gen-class
  :name mid1.monitor.Monitor
  :implements [javax.sound.midi.Receiver]
  :prefix mon-
  :state state
  :init init
  )

(def empty-state
  {:on-map {}
   :events []})

(def note-alignment-factor 100)
(def pedal-collapse-factor 250)
(def step-resolution 600)

(def ctlno-pedal 64)
(defn- pedal-on? [val] (> val 63))
(defn msec [usec] (quot usec 1000))

(defn- pedal-event
  [ts on?]
  [(msec ts) (if on? :pedal-on :pedal-off)])

(defn- note-event
  [ts note velocity length]
  [(msec ts) :note note velocity (msec length)])

(defn- append-event
  [st ev]
  (let [evs (:events st)]
    (assoc st :events (conj evs ev))))

(defn- append-pedal-event
  [st ts ctlno para]
  (if (= ctlno ctlno-pedal)
    (append-event st (pedal-event ts (pedal-on? para)))))

(defn- keep-on
  [st ts note velocity]
  (let [on-m (:on-map st)]
    (assoc st :on-map (assoc on-m note [ts velocity]))))

(defn- resolve-note
  [st ts note]
  (let [on-m (:on-map st)
        evs (:events st)
        [on-ts velo] (get on-m note)
        on-m (dissoc on-m note)]
    (if on-ts
      (assoc st
             :on-map (dissoc on-m note)
             :events (conj evs (note-event on-ts note velo (- ts on-ts))))
      st)))

(defn- process-events
  [events ts-factor process-fn]
  (loop [processed []
         prev-ts 0
         events events]
    (if (seq events)
      (let [event (first events)
            ts (first event)
            to-process? (< (- ts prev-ts) ts-factor)
            [new-processed new-ts]
            (if to-process?
              (process-fn processed event prev-ts ts)
              [(conj processed event) ts])
            ]
        (recur new-processed new-ts (rest events)))
      processed)))

(defn- align-notes
  [notes]
  (process-events
    notes
    note-alignment-factor
    (fn [processed event prev-ts ts]
      [(conj processed (assoc event 0 prev-ts)) prev-ts]
      )))

(defn- collapse-pedal-off
  [pedals]
  (process-events
    pedals
    pedal-collapse-factor
    (fn [processed event prev-ts ts]
      [(assoc processed (dec (count processed)) event) ts])))

(defn- merge-pedals
  [note-m peddals]
  (let [tss (keys note-m)]
    (reduce
      (fn [note-m pedal]
        (let [ts (first pedal)
              note-ts (or
                        (last (filter #(< % ts) tss))
                        (first tss))]
          (update note-m note-ts conj (assoc pedal 0 note-ts))))
      note-m
      peddals)))

(defn- mk-note
  [[_ _ note-no _ length]]
  {:note-no note-no
   :hand :left
   :finger-no 1
   :length (inc (quot length step-resolution))})

(defn- filter-events
  [type-set events]
  (filter #(type-set (second %)) events))

(defn- mk-step
  [evs]
  (let [notes (filter-events #{:note} evs)
        ped-on? (seq (filter-events #{:pedal-on} evs))
        ped-off? (seq (filter-events #{:pedal-off} evs))
        step (if ped-on? {:bar-top? true :pedal :on}
               (if ped-off? {:pedal :off} {}))]
    (assoc step :notes (mapv mk-note notes))))

(defn render-for-mpnote
  [events]
  (let [notes (filter-events #{:note} events)
        pedals (filter-events #{:pedal-on :pedal-off} events)
        note-m (->> notes
                    align-notes
                    (group-by first)
                    (into (sorted-map)))
        pedals (->> pedals
                    collapse-pedal-off)
        event-m (merge-pedals note-m pedals)
        steps (mapv mk-step (vals event-m))]
    {:title "unknown"
     :url ""
     :tempo 120
     :steps steps}))

(defn mon-init
  []
  [[] (atom empty-state)])

(defn mon-close
  [this]
  (let [state (.state this)]
    (reset! state empty-state)))

(defn mon-send
  [this ^ShortMessage msg ts]
  (let [state (.state this)
        status (.getStatus msg)
        val1 (.getData1 msg)
        val2 (.getData2 msg)]
    #_(prn @state)
    (cond
      (.equals status ShortMessage/NOTE_ON) (swap! state keep-on ts val1 val2)
      (.equals status ShortMessage/NOTE_OFF) (swap! state resolve-note ts val1)
      (.equals status  ShortMessage/CONTROL_CHANGE) (swap! state append-pedal-event ts val1 val2)
      :else nil)))

(defn render
  [this]
  (let [state (.state this)
        events (:events @state)]
    (format "%d events sent" (count events))))

(defn events
  [this]
  (let [state (.state this)]
    (:events @state)))

(defn save-for-mpnote!
  [this path]
  (let [state (.state this)
        events (sort-by first (:events @state))]
    (spit path (with-out-str (pp/pprint (render-for-mpnote events))))))
