(ns matcho.core
  (:require
   [clojure.spec :as s]
   [clojure.test :refer :all]))

(defn simple-value? [x]
  (not (or (map? x) (vector? x) (set? x))))

(re-find #"rego" "reggie")

(defn match-compare [p s path]
  (cond
    (instance? clojure.spec.Specize p)
    (when-not (s/valid? p s)
      {:path path :expected (str "confirms to spec " p) :but (s/explain-data p s)})

    (and (string? s) (= java.util.regex.Pattern (type p)))
    (when-not (re-find p s)
      {:path path :expected (str "Match regexp: " p) :but s})

    (fn? p)
    (when-not (p s)
      {:path path :expected (pr-str p) :but s})

    (and (keyword? p) (s/get-spec p))
    (let [sp (s/get-spec p)]
      (when-not (s/valid? p s)
        {:path path :expected (str "confirms to spec " p) :but (s/explain-data p s)}))


        :else (when-not (= p s)
                {:path path :expected p :but s})))

(match-compare 1 2 [:path])
(match-compare 1 ::key [:path])
(match-compare ::key 1 [:path])
(match-compare neg? 1 [:path])

(defn match-recur [errors path example pattern]
  (cond
    (and (map? example)
         (map? pattern))
    (reduce (fn [errors [k v]]
              (let [path  (conj path k)
                    ev (get example k)]
                (match-recur errors path ev v)))
            errors pattern)

    (and (vector? pattern)
         (seqable? example))
    (reduce (fn [errors [k v]]
              (let [path (conj path k)
                    ev (get example k)]
                (match-recur errors path ev v)))
            errors
            (map (fn [x i] [i x]) pattern (range)))

    :else (if-let [err (match-compare pattern example path)]
            (conj errors err)
            errors)))

(defn match* [example & patterns]
  (reduce (fn [acc pattern] (match-recur acc [] example pattern)) [] patterns))

(defmacro match [example & pattern]
  `(let [errors# (match* ~example ~@pattern)]
     (if-not (empty? errors#)
       (is false (pr-str errors#))
       (is true))))

