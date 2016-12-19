(ns matcho.core-test
  (:require [clojure.test :refer :all]
            [clojure.spec :as s]
            [matcho.core :refer :all]))

(defn count-4? [xs]
  (= 2 (count xs)))

(s/def ::pos-coll (s/coll-of pos?))

(deftest matcho-test
  (testing "Matches"
    (match 1 1)
    (match {:a 1 :b 2} {:a 1})
    (match [1] [1])
    (match  {:a 1 :b 2} {:a 1})
    (match  {:a 1 :b 2} {:a odd?})
    (match {:a 2} {:a pos?})
    (match  {:a [1 2 3]} {:a #(= 3 (count %))})

    (match [1 2 3] [1 2])

    (match {:a {:b [{:c 1 :x 5} {:c 2 :x 6}]}}
            {:a {:b [{:c 1} {:c 2}]}})

    (match  {:a [1 2 3]} {:a ::pos-coll})

    (match '(1 2 3) [1 2 3])


    #_(match {:a {:b [{:c 1 :x 5} {:c 2 :x 6}]}}
           {:a {:b #{{:c odd?}}}})

    )

  (testing "Many predicates"
    (match [{:a 1} {:b 2}]
           #(= 2 (count %))
           [{:a odd?} {:b even?}])

    (match (match* [{:a 2} {:b 1}]
             #(= 2 (count %))
             [{:a odd?} {:b even?}])
           [{:path [0 :a]} {:path [1 :b]}]))
    
  (testing "spec integration"

    (match [1 2 3] (s/coll-of number?))
    (match (match* [1 2 3] (s/coll-of even?))
           [{:path []}]))

  (testing "Errors"

    (match (match* {:a 1} [1 2])
           #(not (empty? %)))

    (match (match*  {:a [1 2 3]} {:a count-4?})
           [{:path [:a] :expected #"count" :but [1 2 3]}])

    (match (match*  {:a [1 -2 3]} {:a ::pos-coll})
           [{:path [:a]
             :expected "confirms to spec :matcho.core-test/pos-coll"
             :but map?}])
    
    (match (match* {:a 2} {:a 1})
           [{:path [:a], :expected 1, :but 2}])

    (match (match* {:a 2} {:a neg?})
           [{:path [:a], :expected #"neg" :but 2}])

    (match (match* {} {:x ""})
           [{:path [:x], :expected "", :but nil}] )

    (match (match* {:a [1]} {:a [2]})
           [{:path [:a 0], :expected 2, :but 1}])

    (match (match* {:a {:b "baaa"}}
                   {:a {:b #"^a"}} )
           [{:path [:a :b], :expected "Match regexp: ^a", :but "baaa"}])

    (match (match* {:a [1 {:c 3}]} {:a [1 {:c 4}]})
           [{:path [:a 1 :c], :expected 4, :but 3}])))



(deftest test-matcho
  (matcho {:a 1} {:a odd?})

  (matcho [1 2 3] (s/coll-of int?))

  (match
   (matcho* [{:a 2}]
            [{:a odd?}])
   #:clojure.spec{:problems [{:path [keyword? :a], :pred '(pattern-to-spec odd?), :val 2}]})

  (match (matcho* {:a -2 :b {:c {:d 5}}}
                  {:a neg? :b {:c {:d even?}}})
         #:clojure.spec{:problems [{:path [:b :c :d]}]}))
