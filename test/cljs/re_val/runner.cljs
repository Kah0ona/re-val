(ns re-val.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [re-val.core-test]))

(doo-tests 're-val.core-test)
