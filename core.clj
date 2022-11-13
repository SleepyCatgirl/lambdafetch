(ns λfetch.core
  (:gen-class))
;; System type
(def system (System/getProperty "os.name"))



;; ART ASCII
;; TODO Perhaps split by \n and write a fn that to each line, adds spaces to have everywhere equal amount of spaces
(def lambda-ascii "
  .__.
 /_--_.
 /    \\\\
      /\\\\
     / /\\\\
    / /  \\\\
   / /    \\\\
  / /      \\\\
 / /        \\\\
+ +          ++")
(def ASCII
  (list
   lambda-ascii))


;; Linux info
;; Helper fns
(defn parse-info
  "Given a path to text file, extract specific info
  e.g /etc/lsb-release \"DISTRIB_ID\"
  results in \"nixos\""
  [file search]
  (let [vector-file
        (-> (slurp (java.io.FileReader. file))
            (clojure.string/split #"\n"))]
    (loop [line (first vector-file)
           rst (rest vector-file)]
      (if
          (and (clojure.string/includes? line search)
               (not (nil? line)))
        line
        (recur (first rst)
               (rest rst))))))
(defn pretty-str
  "A helper fn to remove \"s"
  [string]
  (apply str
         (re-seq  #"[^\\^\"]" string)))
;; TODO Make a macro or fn out of this

;; 
(def distro
  (pretty-str
   (-> (parse-info "/etc/lsb-release"
                   "DISTRIB_DESCRIPTION")
       (clojure.string/split #"=")
       (second))))

(def proc
  (->
   (parse-info "/proc/cpuinfo"
               "model name")
   (clojure.string/split #":")
   (second)))


;; MemUsed = Memtotal + Shmem - MemFree - Buffers - Cached - SReclaimable
(def memory-map
  (let [
        path "/proc/meminfo"
        MemTotal (parse-info path
                             "MemTotal")
        Shmem (parse-info path
                          "Shmem")
        MemFree (parse-info path
                            "MemFree")
        Buffers (parse-info path
                            "Buffers")
        Cached (parse-info path
                           "Cached")
        SReclaim (parse-info path
                             "SReclaimable")]
    (hash-map :memtotal MemTotal
              :Shmem Shmem
              :MemFree MemFree
              :Buffers Buffers
              :Cached Cached
              :SReclaim SReclaim)))
(def memory-num
  (-> memory-map
      (update-vals #(re-seq #"[0-9]" %))
      (update-vals #(apply str %))
      (update-vals #(Integer. %))))
(def memory
  (let [MemUsed
        (- (+ (:memtotal memory-num)
              (:Shmem memory-num))
           (:MemFree memory-num)
           (:Buffers memory-num)
           (:Cached memory-num)
           (:SReclaim memory-num))]
    (->
     (assoc memory-num :MemUsed MemUsed)
     (update-vals #(/ % 1024.0))
     (update-vals #(Math/round %)))))




;; TODO Overhaul printing
(defn -main []
  (do
    (println lambda-ascii)
    (println "")
    (println (str "Distribution: " distro))
    (println (str "CPU:" proc))
    (println (str "Memory: " (:MemUsed memory) "MB" "/" (:memtotal memory) "MB"))))
