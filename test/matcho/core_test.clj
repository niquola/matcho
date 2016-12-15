(ns matcho.core-test
  (:require [clojure.test :refer :all]
            [matcho.core :refer :all]))

(defn count-4? [xs]
  (= 2 (count xs)))

(deftest matcho-test
  (testing "Matches"
    (match 1 1)
    (match [1] [1])
    (match  {:a 1 :b 2} {:a 1})
    (match  {:a 1 :b 2} {:a odd?})
    (match {:a 2} {:a pos?})
    (match  {:a [1 2 3]} {:a #(= 3 (count %))})

    (match {:a {:b [{:c 1 :x 5} {:c 2 :x 6}]}}
           {:a {:b [{:c 1} {:c 2}]}})

    #_(match {:a {:b [{:c 1 :x 5} {:c 2 :x 6}]}}
           {:a {:b #{{:c odd?}}}})

    )

  (testing "Errors"

    (is (= [{:path [:a],
             :expected "#function[matcho.core-test/count-4?]"
             :but [1 2 3]}]
           (match*  {:a [1 2 3]} {:a count-4?})))
    (is (= [] (match* {} [])))

    (is (= (match* {:a 2} {:a 1})
           [{:path [:a], :expected "1", :but 2}] ))

    (is (= (match* {:a 2} {:a neg?})
           [{:path [:a], :expected "#function[clojure.core/neg?]", :but 2}]))

    (is (= (match* {} {:x ""})
           [{:path [:x], :expected "\"\"", :but nil}] ))

    (is (= (match* {:a [1]} {:a [2]})
           [{:path [:a 0], :expected "2", :but 1}]))

    (is (= (match* {:a {:b "baaa"}}
                   {:a {:b #"^a"}} )
           [{:path [:a :b], :expected "#\"^a\"", :but "baaa"}]))

    (is (= (match* {:a [1 {:c 3}]} {:a [1 {:c 4}]})
           [{:path [:a 1 :c], :expected "4", :but 3}]))


    )

  )
