(ns net.devdude.lispcraft.runtime.EvalTest
  (:gen-class
    :prefix "-"
    :methods [^:static [hello [] void]
              ^:static [eval [String] String]]
    :main false))

(defn -hello
  "Print hello!"
  []
  (println "Hello world!"))

(defn -eval
  "Evaluates the source string, returning the results as a string"
  [src]
  (str (eval (read-string src))))
