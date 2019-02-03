(ns com.machinemetrics.airtable.import
  (:require
    [cljs.core.async :refer [<! >! chan onto-chan timeout close!]]
    [cljs-http.client :as http]
    [clojure.string :as string]
    ["xhr2" :as xhr2]
    ["fs" :as fs])
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]]))

(set! js/XMLHttpRequest xhr2)

(defn process-channel []
  (chan 10
        (comp
          (filter #(> (:temp %) 110))
          (map #((juxt :record-id :timestamp :machine-id :temp) %))
          (map #(string/join "," (conj % "\n"))))))

(defn retrieve-machines [domain access-token offset limit ch]
  (go-loop [offset offset]
           (println (str "FETCHING records between " offset " and " (+ offset limit)))
           (let [response (<! (http/request {:method  :get
                                             :url     (str domain "/dmz/airtable/import-machines?offset=" offset "&limit=" limit)
                                             :headers {"DMZ-Access-Token" access-token}
                                             :body    nil}))]
             (when (= (:status response) 200)
               (let [machines (:body response)]
                 (onto-chan ch machines false)
                 (recur (+ offset limit)))))))

(defn process-machines [file ch]
  (let [header "record-id,timestamp,machine-id,temp\n"]
    (.writeFileSync fs file header "utf-8")
    (go-loop []
             (when-let [line (<! ch)]
               (.appendFileSync fs file line "utf-8")
               (recur)))))

(defn main [& cli-args]
  (let [file "import-results.csv"
        process-ch (process-channel)]
    (retrieve-machines "http://localhost:8888" "token" 0 100 process-ch)
    (process-machines file process-ch)))

