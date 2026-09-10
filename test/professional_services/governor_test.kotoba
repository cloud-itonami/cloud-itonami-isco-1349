(ns professional-services.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [professional-services.store :as store]
            [professional-services.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-engagement! st {:engagement-id "engagement-1" :name "Acme Sole Trader"})
    st))

(deftest ok-on-clean-coordinate
  (let [st (fresh-store)
        proposal {:op :coordinate :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:engagement-id "engagement-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest hard-on-unregistered-engagement
  (let [st (fresh-store)
        proposal {:op :coordinate :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:engagement-id "no-such-engagement"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-engagement (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        proposal {:op :coordinate :effect :direct-write :confidence 0.9 :stake :low}
        v (governor/check {:engagement-id "engagement-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest escalates-on-client-fund-handling
  (let [st (fresh-store)
        proposal {:op :handle-client-funds :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:engagement-id "engagement-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-regulatory-filing-submission
  (let [st (fresh-store)
        proposal {:op :submit-regulatory-filing :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:engagement-id "engagement-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-low-confidence
  (let [st (fresh-store)
        proposal {:op :coordinate :effect :propose :confidence 0.2 :stake :low}
        v (governor/check {:engagement-id "engagement-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest store-records-and-ledger-append-only
  (let [st (fresh-store)]
    (store/commit-record! st {:engagement-id "engagement-1" :op :review})
    (store/append-ledger! st {:disposition :commit})
    (is (= 1 (count (store/records-of st "engagement-1"))))
    (is (= 1 (count (store/ledger st))))))
