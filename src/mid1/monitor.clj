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
  :state state
  :init init
  )

(def empty-state
  {:on-map {}
   :events []})

(def ctlno-pedal 64)
(defn- pedal-on? [val] (> val 63))

(defn- pedal-event
  [ts on?]
  [ts (if on? :pedal-on :pedal-off)])

(defn- note-event
  [ts note velocity length]
  [ts :note note velocity length])

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
             :events (conj evs (note-event on-ts note velo (- on-ts ts))))
      st)))


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
    (case status
      ShortMessage/NOTE_ON (swap! state keep-on ts val1 val2)
      ShortMessage/NOTE_OFF (swap! state resolve-note ts val1)
      ShortMessage/CONTROL_CHANGE (swap! state append-pedal-event ts val1 val2))))

(defn events
  [this]
  (let [state (.state this)]
    (:events @state)))
