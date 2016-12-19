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
                    ev  (nth (vec example) k nil)]
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

(defmacro pattern-to-spec [pattern]
  (cond
    (symbol? pattern) pattern
    (list? pattern) pattern
    (instance? clojure.spec.Specize pattern)  (throw (Exception. "ups")) ;;pattern
    (fn? pattern) pattern
    (map? pattern)
    (let [nns (name (gensym "n"))
          nks (mapv #(keyword nns (name %)) (keys pattern))
          ks  (map (fn [[k v]] (list 's/def (keyword nns (name k)) (list 'pattern-to-spec v))) pattern)]
      `(do ~@ks (s/keys :req-un ~nks)))

    (vector? pattern)
    (let [nns (name (gensym "n"))
          cats (loop [i 0
                      [p & ps] pattern
                      cats []]
                 (if p
                   (recur (inc i)
                          ps
                          (conj cats (keyword nns (str "i" i)) (list 'pattern-to-spec p)))
                   cats))]
      `(s/cat ~@cats))

    :else `(conj #{} ~pattern)))

(defmacro matcho* [example pattern]
  `(let [sp# (pattern-to-spec ~pattern)]
     (s/explain-data sp# ~example)))

(defmacro matcho [example pattern]
  `(let [sp# (pattern-to-spec ~pattern)
         res# (s/valid? sp#  ~example)
         es# (s/explain-str sp# ~example)]
     (is res# (str (pr-str ~example) "\n" es#))))

(comment
  (matcho* [1 -2 3] [neg? neg? neg?])
  (pattern-to-spec [neg? neg? neg?])

  (matcho* [1 2] (s/coll-of keyword?))

  (pattern-to-spec (s/coll-of keyword?))


  )

