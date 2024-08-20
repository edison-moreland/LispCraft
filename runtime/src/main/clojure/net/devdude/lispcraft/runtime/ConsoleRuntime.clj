(ns net.devdude.lispcraft.runtime.ConsoleRuntime
  (:import (java.io InputStream OutputStream))
  (:gen-class
    :state state
    :init init
    :prefix "-"
    :methods [[start [java.io.InputStream java.io.OutputStream] void]
              [stop [] void]]
    :main false))

(defn -init []
  [[] (atom {:done true})])

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

;TODO: Make this into a repl instead of echoing
(defn -start
  "Entrypoint for the console block's runtime"
  [this ^InputStream input ^OutputStream output]
  (Thread/startVirtualThread
    #(try
       (-set this :done false)
       (while (false? (-get this :done))
         (.write output (.read input))
         (.flush output))
       (catch Exception e
         (.write output (.getBytes (.getMessage e)))
         (.flush output)))))

(defn -stop
  [this]
  (-set this :done true))