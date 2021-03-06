(ns tadpoles
  (:require
    [clojure.java.io    :as io]
    [org.httpkit.client :as http]
    [cheshire.core      :as json]
    [clojure.string   :as string])
  (:import
    [java.net URI]
    [javax.net.ssl SNIHostName SNIServerName SSLEngine SSLParameters]))

;;; utils ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn trunc [s n]
  (subs s 0 (min (count s) n)))

(defn sni-configure [^SSLEngine ssl-engine ^URI uri]
  (let [^SSLParameters ssl-params (.getSSLParameters ssl-engine)]
    (.setServerNames ssl-params [(SNIHostName. (.getHost uri))])
    (.setSSLParameters ssl-engine ssl-params)))

;;; conf ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def url "https://www.tadpoles.com/remote/v1/")

(def headers
  {"Accept"           "application/json, text/javascript, */*; q=0.01"
   "Accept-Language"  "en-US,en;q=0.5"
   "Accept-Encoding"  "gzip, deflate, br"
   "Referer"          "https://www.tadpoles.com/parents"
   "X-Requested-With" "XMLHttpRequest"
   "DNT"              "1"
   "TE"               "Trailers"})

;;; doit ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn download-session-media! [path cookie to]
  (let [client (http/make-client {:ssl-configurer sni-configure})]
    (loop [to to]
      ( let [hdrs (assoc headers "Cookie" cookie)
            from (- to 2629800)
            prms {"earliest_event_time" (str from) "latest_event_time" to "direction" "range" "num_events" "300"}
            resp (http/get (str url "events") {:client client :headers hdrs :query-params prms})
            evts (-> resp deref :body (json/parse-string true) :events)]
       (println "Downloading" (count evts) "events between" from "and" to)
       (when (and evts (> (count evts) 0))
         (doseq [[evt-idx {:keys [event_date location_display members_display new_attachments]}] (map-indexed vector evts)]
           (doseq [[atc-idx {:keys [key mime_type]}] (map-indexed vector new_attachments)]
             (let [loc  (string/lower-case (string/replace location_display #" " "-"))
                   ppl  (string/replace (string/join "-" (map string/lower-case members_display)) #" " "-") 
                   ext  (second (string/split mime_type #"/"))
                   name (str (trunc (str event_date "_" evt-idx atc-idx "_" ppl) 200) "." ext)
                   resp (http/get (str url "attachment") {:client client :headers hdrs :query-params {"key" key}})]
              (println "Downloading object" name)
              (with-open [src (-> resp deref :body)
                              tgt (io/output-stream (io/file path name))]
                (io/copy src tgt)))))
           (recur from))))))
