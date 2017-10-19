(ns re-val.core-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [re-val.core :as core]))

(deftest fake-test
  (testing "fake description"
    (is (= 1 2))))
