(ns com.machinemetrics.airtable.import
  (:require
    [cljs.core.async :refer [<! >! chan onto-chan timeout close!]]
    [cljs-http.client :as http]
    [clojure.string :as string]
    [clojure.tools.cli :refer [parse-opts]]
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

(defn retrieve-machines [{:keys [url-base offset limit access-token]} ch]
  (go-loop [offset offset]
           (println (str "FETCHING records between " offset " and " (+ offset limit)))
           (let [response (<! (http/request {:method  :get
                                             :url     (str url-base "/dmz/airtable/import-machines?offset=" offset "&limit=" limit)
                                             :headers {"DMZ-Access-Token" access-token}}))]
             (when (= (:status response) 200)
               (let [machines (:body response)]
                 (onto-chan ch machines false)
                 (recur (+ offset limit)))))))

(defn process-machines [{:keys [result-file]} ch]
  (let [header "record-id,timestamp,machine-id,temp\n"]
    (.writeFileSync fs result-file header "utf-8")
    (go-loop []
             (when-let [line (<! ch)]
               (.appendFileSync fs result-file line "utf-8")
               (recur)))))

(def cli-options
  [["-u" "--url-base <urlBase>" "Url base" :default "http://localhost:8888"]
   ["-a" "--access-token <accessToken>" "Access Token" :default ""]
   ["-o" "--offset [offset]" "Offset" :default 0 :parse-fn #(js/parseInt %)]
   ["-l" "--limit [limit]" "Limit" :default 100 :parse-fn #(js/parseInt %)]
   ["-f" "--result-file [file]" "File to write result to" :default "./import-results.csv"]])

(defn main [& cli-args]
  (let [opts       (:options (parse-opts cli-args cli-options))
        process-ch (process-channel)]
    (retrieve-machines opts process-ch)
    (process-machines opts process-ch)))

