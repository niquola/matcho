(ns matcho.core
 (:require
   [clojure.spec.alpha :as s]
   [clojure.test :refer :all]))

(defn simple-value? [x]
  (not (or (map? x) (vector? x) (set? x))))

(defn match-compare [p s path]
  (cond
    (instance? clojure.spec.alpha.Specize p)
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
  `(let [example# ~example
         patterns# [~@pattern]
         errors# (apply match* example# patterns#)]
     (if-not (empty? errors#)
       (is false (pr-str errors# example# patterns#))
       (is true))))

(defmacro to-spec [pattern]
  (cond
    (symbol? pattern) pattern
    (instance? clojure.lang.Cons pattern) pattern
    (list? pattern) pattern
    (instance? clojure.spec.alpha.Specize pattern)  (throw (Exception. "ups")) ;;pattern
    (fn? pattern) pattern
    (map? pattern)
    (let [nns (name (gensym "n"))
          nks (mapv #(keyword nns (name %)) (keys pattern))
          ks  (map (fn [[k v]] (list 's/def (keyword nns (name k)) (list 'to-spec v))) pattern)]
      `(do ~@ks (s/keys :req-un ~nks)))

    (vector? pattern)
    (let [nns (name (gensym "n"))
          cats (loop [i 0
                      [p & ps] pattern
                      cats []]
                 (if p
                   (recur (inc i)
                          ps
                          (conj cats (keyword nns (str "i" i)) (list 'to-spec p)))
                   cats))]
      `(s/cat ~@cats :rest (s/* (constantly true))))

    :else `(conj #{} ~pattern)))

(defmacro matcho* [example pattern]
  `(let [sp# (to-spec ~pattern)]
     (::s/problems (s/explain-data sp# ~example))))

(defmacro matcho [example pattern]
  `(let [sp# (to-spec ~pattern)
         res# (s/valid? sp#  ~example)
         es# (s/explain-str sp# ~example)]
     (is res# (str (pr-str ~example) "\n" es#))))

(comment

  (match-compare 1 2 [:path])
  (match-compare 1 ::key [:path])
  (match-compare ::key 1 [:path])
  (match-compare neg? 1 [:path])

  (match 1 [])

  (matcho* [1 -2 3] [neg? neg? neg?])
  (to-spec [neg? neg? neg?])

  (matcho* [1 2] (s/coll-of keyword?))

  (to-spec (s/coll-of keyword?))

  )
