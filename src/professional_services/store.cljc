(ns professional-services.store
  "SSoT for the ISCO-08 1349 independent professional-services-
  management sole-proprietor actor. Store is a protocol injected into
  the `professional-services.actor` StateGraph — `MemStore` is the
  default, deterministic, zero-dep backend; a Datomic/kotoba-server-
  backed implementation can be swapped in without touching the actor or
  governor (itonami actor pattern, per ADR-2607011000 / CLAUDE.md
  Actors section).

  Domain:

    engagement — a registered client engagement (:engagement-id, :name)
    record     — a committed operating record under an engagement
                 (coordination note, review, client-fund handling,
                 regulatory filing) — written ONLY via commit-record!,
                 never mutated in place
    ledger     — an append-only audit trail of every proposal/verdict/
                 disposition, regardless of outcome (commit or hold)")

(defprotocol Store
  (engagement [s engagement-id])
  (records-of [s engagement-id])
  (ledger [s])
  (register-engagement! [s engagement])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (engagement [_ engagement-id] (get-in @a [:engagements engagement-id]))
  (records-of [_ engagement-id] (filter #(= engagement-id (:engagement-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-engagement! [s engagement]
    (swap! a assoc-in [:engagements (:engagement-id engagement)] engagement) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:engagements {} :records [] :ledger []} seed)))))
