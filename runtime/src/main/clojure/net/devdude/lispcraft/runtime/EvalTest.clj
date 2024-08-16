(ns net.devdude.lispcraft.runtime.EvalTest
  (:import (net.devdude.lispcraft.runtime Printer))
  (:gen-class
    :prefix "-"
    :methods [^:static [hello [net.devdude.lispcraft.runtime.Printer] void]
              ^:static [eval [String] String]]
    :main false)
  )

(defn -hello
  "Print hello!"
  [^Printer printer]
  (.print printer "Hello world!"))

(defn -eval
  "Evaluates the source string, returning the results as a string"
  [src]
  (str (eval (read-string src))))
