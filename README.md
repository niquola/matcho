# matcho

[![Build Status](https://travis-ci.org/niquola/matcho.svg)](https://travis-ci.org/niquola/matcho)
[![Clojars Project](https://img.shields.io/clojars/v/matcho.svg)](https://clojars.org/matcho)

One file library for data driven tests

## Usage

```clj
(testing "Matches"
  (match 1 1)
  (match [1] [1])
  (match  {:a 1 :b 2} {:a 1})
  (match  {:a 1 :b 2} {:a odd?})
  (match {:a 2} {:a pos?})
  (match  {:a [1 2 3]} {:a #(= 3 (count %))})

  (match {:a {:b [{:c 1 :x 5} {:c 2 :x 6}]}}
          {:a {:b [{:c 1} {:c 2}]}}))

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
          [{:path [:a 1 :c], :expected "4", :but 3}])))
          
          
(s/def ::pos-coll (s/coll-of pos?))
(match (match*  {:a [1 -2 3]} {:a ::pos-coll})
        [{:path [:a]
          :expected "confirms to spec :matcho.core-test/pos-coll"
          :but "In: [1] val: -2 fails spec: :matcho.core-test/pos-coll predicate: pos?\n"}])
```

## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
