(ns com.matthiasnehlsen.inspect.index
  (:gen-class)
  (:require
    [hiccup.core :refer [html]]))

(defn index-page
  "Generates index page HTML with the specified page title."
  [title]
  (html
    [:html
     {:lang "en"}
     [:head 
      [:meta {:content "width=device-width, initial-scale=1", :name "viewport"}]
      [:title title]
      [:link {:href "/inspect/bower_components/pure/pure-min.css", :media "screen", :rel "stylesheet"}]
      [:link {:href "/inspect/css/inspect.css", :media "screen", :rel "stylesheet"}]
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
        [:h1.splash-head "inspect"]
        [:p.splash-subhead "println no more"]]]
      [:div.content-wrapper
       [:div.content
        [:div.pure-g
         [:div.l-box-lrg
          [:div.pure-u-1-2
           [:div#types]]
          [:div.pure-u-1-2 [:div#selection]]]]
        [:div.pure-g [:div.l-box-lrg [:div#code.pure-u-1]]]]]
      [:script {:src "/inspect/js/build/inspect-opt.js"}]]]))
