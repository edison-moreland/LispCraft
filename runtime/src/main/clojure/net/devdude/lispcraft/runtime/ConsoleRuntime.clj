(ns net.devdude.lispcraft.runtime.ConsoleRuntime
  (:import (java.util.concurrent LinkedBlockingQueue TimeUnit)
           (net.devdude.lispcraft.runtime CharacterScreen RuntimeEvent RuntimeEvent$KeyPressed RuntimeEvent$PrintLine))
  (:gen-class
    :state state
    :init init
    :prefix "-"
    :methods [[start [net.devdude.lispcraft.runtime.CharacterScreen] void]
              [sendRuntimeEvent [net.devdude.lispcraft.runtime.RuntimeEvent] void]]
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
  [this ^RuntimeEvent event]
  (.offer (-get-event-channel this) event, 0, TimeUnit/SECONDS))

(defn ^RuntimeEvent -next-event
  [this]
  (.take (-get-event-channel this)))

(defmulti -event-handler class)
(defmethod -event-handler RuntimeEvent$PrintLine
  [event] (fn [this screen]
            (.print screen 0 (-swap-event-count this inc) (format "%s" (.text event)))))

(defmethod -event-handler RuntimeEvent$KeyPressed
  [event] (fn [this screen]
            (.print screen 0 (-swap-event-count this inc) (format "%s" (.keyCode event)))))

(defn -do-event-loop
  [this screen]
  (while true ((-event-handler (-next-event this)) this screen)))

(defn -start
  "Entrypoint for the console block's runtime"
  [this ^CharacterScreen screen]
  (Thread/startVirtualThread
    #(try
       (-do-event-loop this screen)
       (catch Exception e
         (.print screen 0 0 "An error happened!")
         (.print screen 0 1 (.getMessage e))))))

(defn -sendRuntimeEvent
  [this ^RuntimeEvent event]
  (-send-event this event))

