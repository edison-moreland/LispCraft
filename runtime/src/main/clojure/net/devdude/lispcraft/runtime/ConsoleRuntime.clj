(ns net.devdude.lispcraft.runtime.ConsoleRuntime
  (:import (java.io OutputStream)
           (java.util.concurrent LinkedBlockingQueue TimeUnit)
           (net.devdude.lispcraft.runtime ConsoleEvent ConsoleEvent$Write))
  (:gen-class
    :state state
    :init init
    :prefix "-"
    :methods [[start [java.io.OutputStream] void]
              [sendConsoleEvent [net.devdude.lispcraft.runtime.ConsoleEvent] void]]
    :main false))

(defn -init []
  [[] (atom {:event-channel (new LinkedBlockingQueue)
             :event-count   0})])

(defn -set
  [this key value]
  (swap! (.state this) into {key value})
  value)

(defn -get
  [this key]
  (@(.state this) key))

(defn -swap
  [this key f]
  (-set this key (f (-get this key))))

(defn -get-event-count
  [this]
  (-get this :event-count))

(defn -swap-event-count
  [this f]
  (-swap this :event-count f))

(defn ^LinkedBlockingQueue -get-event-channel
  [this]
  (-get this :event-channel))

(defn -send-event
  [this ^ConsoleEvent event]
  (.offer (-get-event-channel this) event, 0, TimeUnit/SECONDS))

(defn ^ConsoleEvent -next-event
  [this]
  (.take (-get-event-channel this)))

(defmulti -event-handler class)

(defmethod -event-handler ConsoleEvent$Write
  [event] (fn [this ^OutputStream console]
            (.write console (.bytes event))
            (.flush console)))

(defn -do-event-loop
  [this console]
  (while true
    ((-event-handler (-next-event this)) this console)))

(defn -start
  "Entrypoint for the console block's runtime"
  [this ^OutputStream console]
  (assert (not (nil? console)))
  (Thread/startVirtualThread
    #(try
       (-do-event-loop this console)
       (catch Exception e
         (.write console (.getBytes (.getMessage e)))
         (.flush console)))))

(defn -sendConsoleEvent
  [this ^ConsoleEvent event]
  (-send-event this event))

