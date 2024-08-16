(ns net.devdude.lispcraft.runtime.ConsoleRuntime
  (:import (java.util.concurrent LinkedBlockingQueue TimeUnit)
           (net.devdude.lispcraft.runtime CharacterScreen))
  (:gen-class
    :state state
    :init init
    :prefix "-"
    :methods [[start [net.devdude.lispcraft.runtime.CharacterScreen] void]
              [sendTestEvent [] void]]
    :main false))


(defn -init []
  [[] (atom {:event-channel (new LinkedBlockingQueue)})])

(defn -set
  [this key value]
  (swap! (.state this) into {key value}))

(defn -get
  [this key]
  (@(.state this) key))

(defn -get-event-channel
  [this]
  (-get this :event-channel))

(defn -send-event
  [this event]
  (.offer (-get-event-channel this) event, 0, TimeUnit/SECONDS))

(defn -next-event
  [this]
  (.take (-get-event-channel this)))

(defn -start
  "Entrypoint for the console block's runtime"
  [this ^CharacterScreen screen]
  (Thread/startVirtualThread #(while true
                                (let [event (-next-event this)]
                                  (when (= event :test)
                                    (.print screen 0 0 "Event happened"))
                                  ))))


(defn -sendTestEvent
  [this]
  (-send-event this :test))