(ns com.matthiasnehlsen.inspect.index
  (:gen-class)
  (:require
    [hiccup.core :refer [html]]
    [garden.core :refer [css]]
    [garden.units :as u :refer [px]]))

(defn index-css
  "Generate index page CSS in Clojure using Garden."
  []
  (let [small-font (u/em 0.7)
        string-color "#6A8759"
        strong-blue "#4471FF"]
    (css {:pretty-print? false}
         [:.event
          [:h4 {:margin-top 0
                :margin-bottom 0
                :color "#63809C"}]
          [:pre {:margin-top (px 2)
                 :background-color "#202F41"
                 :color "#A9B7C6"
                 :overflow :scroll-x
                 :border-radius (px 2)
                 :padding (px 10)}
           [:code {:margin-top 0
                   :margin-bottom (px 10)
                   :font-size small-font
                   :font-family "Monaco, monospace"
                   :overflow :scroll-x}]]]
         [:.received {:margin-top 0
                      :margin-bottom 0
                      :font-size small-font
                      :float :right}]
         [:table :th :td {:text-align :right :background-color "#EEE"}]
         [:th {:background-color "#DDD"}]
         [:.table-small     {:font-size small-font}]
         [:.button-xsmall   {:font-size small-font :margin (px 2)}]
         [:.active          {:font-weight :bold :color :black}]
         [:.delimiter       {:font-weight :bold :color :red}]
         [:.number          {:color "#6897BB"}]
         [:.tag             {:color :red}]
         [:.boolean         {:color :green}]
         [:.class-delimiter {:color strong-blue}]
         [:.string          {:font-weight :bold :color string-color}]
         [:.character       {:font-weight :bold :color string-color}]
         [:.keyword         {:font-weight :bold :color "#CC7832"}]
         [:.class-name      {:font-weight :bold :color strong-blue}]
         [:.function-symbol {:font-weight :bold :color strong-blue}]
         [:.nil             {:font-weight :bold :color "#CC7832"}]
         [:.symbol])))

(defn index-page
  "Generates index page HTML with the specified page title."
  [conf]
  (html
    [:html
     {:lang "en"}
     [:head 
      [:meta {:content "width=device-width, initial-scale=1", :name "viewport"}]
      [:title (:title conf)]
      [:link {:href "/inspect/bower_components/pure/pure-min.css", :media "screen", :rel "stylesheet"}]
      [:link {:href "/inspect/css/inspect.css", :media "screen", :rel "stylesheet"}]
      [:style (index-css)]
      [:link {:href "http://fonts.googleapis.com/css?family=Lato&subset=latin,latin-ext" :rel "stylesheet" :type "text/css"}]
      [:link {:href "/inspect/images/favicon.png", :rel "shortcut icon", :type "image/png"}]]
     [:body
      [:div.header
       [:div.home-menu.pure-menu.pure-menu-open.pure-menu-horizontal.pure-menu-fixed
        [:a.pure-menu-heading {:href ""} "inspect"]
        [:ul
         [:li [:a {:href "https://github.com/matthiasn/inspect"} "GitHub"]]
         [:li [:a {:href "http://matthiasnehlsen.com/blog/2014/11/14/Inspect/"} "About"]]]]]
      [:div.splash-container
       [:div.splash 
        [:h1.splash-head (:header conf)]
        [:p.splash-subhead (:subheader conf)]]]
      [:div.content-wrapper
       [:div.content
        [:div.pure-g
         [:div.l-box-lrg
          [:div.pure-u-1-2
           [:div#types]]
          [:div.pure-u-1-2 [:div#selection]]]]
        [:div.pure-g [:div.l-box-lrg [:div#code.pure-u-1]]]]]
      [:script {:src "/inspect/js/build/inspect.js"}]]]))
