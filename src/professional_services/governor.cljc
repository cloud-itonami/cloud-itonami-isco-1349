(ns professional-services.governor
  "ProfessionalServicesGovernor — the independent safety/traceability
  layer for the ISCO-08 1349 independent professional-services-
  management actor. Wired as its own `:govern` node in
  `professional-services.actor`'s StateGraph, downstream of `:advise`
  — the Advisor has no notion of engagement provenance or client-fund/
  regulatory risk, so this MUST be a separate system able to reject a
  proposal (itonami actor pattern, per ADR-2607011000 / CLAUDE.md
  Actors section).

  `check` is a pure function of (request, context, proposal, store) ->
  verdict; it never mutates the store. The StateGraph's `:decide` node
  routes on the verdict:
    :hard? true                → :hold  (irreversible, no write)
    :escalate? true            → :request-approval (interrupt-before)
    otherwise                  → :commit

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. engagement provenance  — the request's engagement must be
       registered.
    2. no-actuation             — proposal :effect must be :propose.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off, per the
  README robotics-premise: client fund handling and regulatory filing
  submission always require human sign-off):
    3. :op :handle-client-funds.
    4. :op :submit-regulatory-filing.
    5. low confidence (< `confidence-floor`)."
  (:require [professional-services.store :as store]))

(def confidence-floor 0.6)
(def ^:private escalating-ops #{:handle-client-funds :submit-regulatory-filing})

(defn- hard-violations [{:keys [proposal]} engagement-record]
  (cond-> []
    (nil? engagement-record)
    (conj {:rule :no-engagement :detail "未登録 engagement"})

    (not= :propose (:effect proposal))
    (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `professional-services.store/Store`. Returns
  `{:ok? bool :violations [...] :confidence n :hard? bool :escalate? bool}`."
  [request context proposal store]
  (let [engagement-record (store/engagement store (:engagement-id request))
        hard (hard-violations {:proposal proposal} engagement-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        risky-op? (contains? escalating-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not risky-op?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? risky-op?))}))
