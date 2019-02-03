;; We first start an NREPL server
(require '[shadow.cljs.devtools.server :as server])
(server/start!)

;; Then we start the builds and keep watching for changes
(require '[shadow.cljs.devtools.api :as shadow])
(shadow/watch :server)
(shadow/watch :importer)
