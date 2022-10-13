(ns mid1.html
  (:require
    [clojure.pprint :as pp]
    [hiccup.core :refer [html]]))

(def note-no-c4 60)

(defn html-note
  [[ts _ note-no velocity length]]
  [note-no (quot ts 10) (inc (quot length 500)) velocity])

(defn note-event
  [[_ ts length] ts-max]
  (for [ix (range length)]
    (let [bottom (- (quot ts-max 10) ts (* ix 22))
          on? (zero? ix)
          note-type (if on? "note" "dummy-note")]
      [:div {:class (str note-type " left-note")
             :style (str "bottom: " bottom "px;")}
       (when on? "0")])))

(defn octave
  [first-note-no num-notes ev-m ts-max]
  [:div {:class (str "octave " "keys-" num-notes)}
   (for [ix (range num-notes)]
     (let [note-no (+ first-note-no ix)
           bw (if (odd? ix) "black" "white")
           c4 (if (= note-no note-no-c4) "c4-key" "")]
       [:div {:class (str "key-1 " bw "-key " c4)
              :data-note-no (str note-no)}
        (when ev-m
          (when-let [evs (get ev-m note-no)]
            (mapcat #(note-event % ts-max) evs)))]))])

(defn octaves
  []
  [[21 3]
   [24 12] [36 12] [48 12]
   [60 12] [72 12] [84 12]
   [96 12]
   [108 1]])

(defn body
  [ev-m ts-max]
  [:div {:id "app"}
   [:div {:class "app"}
    [:div {:class "main-container"}
     [:div {:class "indicator-col"}
      [:div {:class "pedal pedal-on", :style "top: -1718px;"}]]
     [:div {:class "main-col"}
      [:div {:class "timeline jsTimeline"}
       (for [[f n] (octaves)]
         (octave f n ev-m ts-max))
       [:div {:style "bottom: 20px;", :class "cur-step"}]]
      [:div {:class "keys-88"}
       (for [[f n] (octaves)]
         (octave f n nil nil))]]
     [:div {:class "annotation-col"}]
     [:div {:style "transform: translate(0px);", :class "control-panel"}
      [:a {:href "#", :class "btn rewind"}]
      [:a {:href "#", :class "btn fast-forward"}]]]]])

(defn render-notes
  [ev-m ts-max]
  (let [inner-body (html (body ev-m ts-max))]
    (str "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>mid1</title><link rel=\"stylesheet\" type=\"text/css\" href=\"css/reset.css\"><link rel=\"stylesheet\" type=\"text/css\" href=\"css/mid1.css\"></head><body>" inner-body "</body></html>")))

(comment
  
  (html (octave-for-timeline 24 12))
  (html (note-event 100 3))
  (let [ev-m {
              60 [[100 1][200 2][300 3]]}]
    (spit "html/hoge.html" (render-notes ev-m)))
  )

