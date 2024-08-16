(ns net.devdude.lispcraft.runtime.ConsoleRuntime
  (:import (java.util.concurrent LinkedBlockingQueue TimeUnit)
           (net.devdude.lispcraft.runtime CharacterScreen RuntimeEvent RuntimeEvent$ButtonPush RuntimeEvent$PrintLine)
           )
  (:gen-class
    :state state
    :init init
    :prefix "-"
    :methods [[start [net.devdude.lispcraft.runtime.CharacterScreen] void]
              [sendRuntimeEvent [net.devdude.lispcraft.runtime.RuntimeEvent] void]]
    :main false))

(defn -init []
  [[] (atom {:event-channel (new LinkedBlockingQueue)
             :event-count 0})])

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

(defn -start-event-loop
  [this ^CharacterScreen screen]
  (while true
    (let* [event (-next-event this)]
      (when (instance? RuntimeEvent$PrintLine event)
        (let [event (cast RuntimeEvent$PrintLine event)
              event-count (-swap-event-count this inc)]
          (.print screen 0 event-count (format "%s" (.text event))))))))

(defn -start
  "Entrypoint for the console block's runtime"
  [this ^CharacterScreen screen]
  (Thread/startVirtualThread
    #(try
        (-start-event-loop this screen)
        (catch Exception e
          (.print screen 0 0 "An error happened!")
          (.print screen 0 1 (.getMessage e))))))

(defn -sendRuntimeEvent
  [this ^RuntimeEvent event]
  (-send-event this event))

