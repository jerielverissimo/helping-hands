(ns clj.helping-hands-consumer.persistence
  "Persistence Port and Adapter for Consumer Service"
  (:require [datomic.api :as d]))

;; --------------------------------------------------
;; Consumer Persistence Port for Adapters to Plug-in
;; --------------------------------------------------
(defprotocol ConsumerDB
  "Abstraction for consumer database"
  
  (upsert [this id name address mobile email geo]
          "Adds/Updates a consumer entity")
  
  (entity [this id flds]
          "Gets the specified consumer with all or requested fields")
  
  (delete [this id]
          "Deletes the specified consumer entity"))

;; -------------------------------------------------
;; Datomic Adapter Implementation for Consumer Port
;; -------------------------------------------------
(defn- get-entity-id
  [conn id]
  (-> (d/q '[:find ?e
             :in $ ?id
             :where [?e :consumer/id ?id]] (d/db conn) (str id))
      ffirst))

(defn- get-entity
  [conn id]
  (let [eid (get-entity-id conn id)]
    (->> (d/entity (d/db conn) eid) seq (into {}))))

(defrecord ConsumerDBDatomic [conn]
  ConsumerDB
  
  (upsert [this id name address mobile email geo]
    (d/transact conn
                (vector (into {} (filter (comp some? val)
                                         {:db/id id
                                          :consumer/id id
                                          :consumer/name name
                                          :consumer/address address
                                          :consumer/mobile mobile
                                          :consumer/email email 
                                          :consumer/geo geo})))))

  (entity [this id flds]
    (when-let [consumer (get-entity conn id)]
      (if (empty? flds)
        consumer
        (select-keys consumer (map keyword flds)))))

  (delete [this id]
    (when-let [eid (get-entity-id conn id)]
      (d/transact conn [[:db.fn/retractEntity eid]]))))


