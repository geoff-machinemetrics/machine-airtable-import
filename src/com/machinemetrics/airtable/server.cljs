(ns com.machinemetrics.airtable.server
  (:require
    [mount.core :as mount :refer [defstate]]
    ["http" :as http]
    ["url" :as url]))

(def machines
  (->> (range)
       (map (fn [index]
              {:record-id  index
               :machine-id (rand-int 10)
               :temp       (rand-int 200)
               :timestamp  (js/Date.now)}))
       (take 2010)
       (vec)))

(defn query-machines [offset limit]
  (take limit (drop offset machines)))

(defn handler [req res]
  (let [query  (.-query (.parse url (.-url req) true))
        offset (js/parseInt (.-offset query))
        limit  (js/parseInt (.-limit query))]
    (if-let [data (seq (query-machines offset limit))]
      (js/setTimeout #(doto res
                        (.writeHead 200 (clj->js {"Content-Type" "application/json"}))
                        (.write (js/JSON.stringify (clj->js data)))
                        (.end))
                     (rand-int 300))
      (doto res (.writeHead 404) (.end)))))

(defstate server
          :start (doto
                   (.createServer http handler)
                   (.listen 8888))
          :stop (some-> @server .close))

(defn main [& cli-args] (mount/start))
