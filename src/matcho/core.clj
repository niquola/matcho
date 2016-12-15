(ns matcho.core
  (:require
   [clojure.test :refer :all]))

(defn simple-value? [x]
  (not (or (map? x) (vector? x) (set? x))))

(re-find #"rego" "reggie")

(defn match-compare [p s]
  (cond (fn? p) (p s)
        (= java.util.regex.Pattern (type p)) (when (string? s) (re-find p s))
        :else (= p s)))

(defn match-recur [errors path example pattern]
  (cond
    (and (not (or (map? pattern) (vector? pattern)))
         (= pattern example)) errors

    (not (or (and (map? pattern) (map? example))
             (and (vector? pattern) (seqable? example))))
    (conj errors {:path path :error (str "unmatched types: expected " (type pattern) ", but " (type example) "; " (pr-str example))})

    (map? pattern) (reduce (fn [errors [k v]]
                             (let [path  (conj path k)
                                    ev (get example k)]
                               (if (simple-value? v)
                                 (if (not (match-compare v ev))
                                   (conj errors {:path path :expected (pr-str v) :but ev})
                                   errors)
                                 (match-recur errors path ev v))))
                           errors pattern)

    (vector? pattern) (reduce (fn [errors [k v]]
                                (let [path (conj path k)
                                      ev (nth example k nil)]
                                  (if (simple-value? v)
                                    (if (not (match-compare v ev))
                                      (conj errors {:path path :expected (pr-str v) :but ev})
                                      errors)
                                    (match-recur errors path ev v))))
                              errors
                              (map (fn [x i] [i x]) pattern (range)))

    :else (assert false "Unexpected input")))

(defn match* [example pattern]
  (match-recur [] [] example pattern))

(defmacro match [example pattern]
  `(let [errors# (match-recur [] [] ~example ~pattern)]
     (if-not (empty? errors#)
       (is false (pr-str errors#))
       (is true))))

