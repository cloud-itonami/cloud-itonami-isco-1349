(ns professional-services.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [professional-services.actor :as actor]
            [professional-services.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-engagement! st {:engagement-id "engagement-1" :name "Acme Sole Trader"})
    st))

(deftest commits-a-clean-low-risk-request
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:engagement-id "engagement-1" :op :coordinate :stake :low}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "engagement-1"))))))

(deftest holds-on-unregistered-engagement-without-committing
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:engagement-id "no-such-engagement" :op :coordinate :stake :low}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :done (:status result)))
    (is (nil? (get-in result [:state :record])))
    (is (empty? (store/records-of st "no-such-engagement")))
    (is (= :hold (:disposition (:state result))))))

(deftest interrupts-then-commits-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        ;; client-fund handling always escalates (governor invariant)
        request {:engagement-id "engagement-1" :op :handle-client-funds :stake :high}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "engagement-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (some? (get-in resumed [:state :record])))
      (is (= 1 (count (store/records-of st "engagement-1")))))))
