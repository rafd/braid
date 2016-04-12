(ns chat.server.seed
  (:require [chat.server.db :as db]
            [environ.core :refer [env]]))

(defn drop! []
  (datomic.api/delete-database db/*uri*))

(defn init! []
  (db/init!))

(defn seed! []
  (db/with-conn
    (let [figgis-agency (db/create-group! {:id (db/uuid) :name "The Figgis Agency"})

          malory (db/create-user! {:id (db/uuid)
                                   :email "malory@example.com"
                                   :nickname "malory"
                                   :password "malory"
                                   :avatar "/avatars/malory.png"})
          archer (db/create-user! {:id (db/uuid)
                                   :email "archer@example.com"
                                   :nickname "archer"
                                   :password "archer"
                                   :avatar "/avatars/archer.png"})
          krieger (db/create-user! {:id (db/uuid)
                                    :email "krieger@example.com"
                                    :nickname "krieger"
                                    :password "krieger"
                                    :avatar "/avatars/krieger.png"})
          lana (db/create-user! {:id (db/uuid)
                                 :email "lana@example.com"
                                 :nickname "lana"
                                 :password "lana"
                                 :avatar "/avatars/lana.png"})
          ray (db/create-user! {:id (db/uuid)
                                :email "ray@example.com"
                                :nickname "ray"
                                :password "ray"
                                :avatar "/avatars/ray.png"})
          cheryl (db/create-user! {:id (db/uuid)
                                   :email "cheryl@example.com"
                                   :nickname "cheryl"
                                   :password "cheryl"
                                   :avatar "/avatars/cheryl.png"})
          cyril (db/create-user! {:id (db/uuid)
                                  :email "cyril@example.com"
                                  :nickname "cyril"
                                  :password "cyril"
                                  :avatar "/avatars/cyril.png"})
          pam (db/create-user! {:id (db/uuid)
                                :email "pam@example.com"
                                :nickname "pam"
                                :password "pam"
                                :avatar "/avatars/pam.png"})

          _ (db/user-add-to-group! (malory :id) (figgis-agency :id))
          _ (db/user-add-to-group! (archer :id) (figgis-agency :id))
          _ (db/user-add-to-group! (krieger :id) (figgis-agency :id))
          _ (db/user-add-to-group! (lana :id) (figgis-agency :id))
          _ (db/user-add-to-group! (ray :id) (figgis-agency :id))
          _ (db/user-add-to-group! (cheryl :id) (figgis-agency :id))
          _ (db/user-add-to-group! (cyril :id) (figgis-agency :id))
          _ (db/user-add-to-group! (pam :id) (figgis-agency :id))

          review (db/create-tag! {:id (db/uuid) :group-id (figgis-agency :id) :name "review"})
          marketing (db/create-tag! {:id (db/uuid) :group-id (figgis-agency :id) :name "marketing"})
          watercooler (db/create-tag! {:id (db/uuid) :group-id (figgis-agency :id) :name "watercooler"})
          deane (db/create-tag! {:id (db/uuid) :group-id (figgis-agency :id) :name "deane"})
          longwater (db/create-tag! {:id (db/uuid) :group-id (figgis-agency :id) :name "longwater"})

          _ (db/user-subscribe-to-tag! (malory :id) (review :id))
          _ (db/user-subscribe-to-tag! (malory :id) (marketing :id))
          _ (db/user-subscribe-to-tag! (malory :id) (watercooler :id))
          _ (db/user-subscribe-to-tag! (malory :id) (deane :id))
          _ (db/user-subscribe-to-tag! (malory :id) (longwater :id))

          msg (db/create-message! {:id (db/uuid)
                                   :user-id (pam :id)
                                   :thread-id (db/uuid)
                                   :created-at (java.util.Date.)
                                   :content (str "#" (marketing :id) " What do you think of this poster I made?")
                                   :mentioned-tag-ids [(marketing :id)]})

          _ (db/create-message! {:id (db/uuid)
                     :thread-id (msg :thread-id)
                     :user-id (pam :id)
                     :created-at (java.util.Date.)
                     :content "https://s3.amazonaws.com/chat.leanpixel.com/uploads/570c5662-7ce9-45dc-8d1d-4a36d68a35b1.png"})

          _ (db/create-message! {:id (db/uuid)
                                 :thread-id (msg :thread-id)
                                 :user-id (cheryl :id)
                                 :created-at (java.util.Date.)
                                 :content "Awww, it's so cute! :heart:"})
          _ (db/create-message! {:id (db/uuid)
                                 :thread-id (msg :thread-id)
                                 :user-id (ray :id)
                                 :created-at (java.util.Date.)
                                 :content ":thumbsup:"})
          _ (db/create-message! {:id (db/uuid)
                                 :thread-id (msg :thread-id)
                                 :user-id (archer :id)
                                 :created-at (java.util.Date.)
                                 :content (str "@" (pam :id) ", I swear to god, if I see any of these up I will personally tie you up in a burlap sack and throw you off a bridge...")
                                 :mentioned-user-ids [(pam :id)]})
          _ (db/create-message! {:id (db/uuid)
                                 :thread-id (msg :thread-id)
                                 :user-id (pam :id)
                                 :created-at (java.util.Date.)
                                 :content "Uhhhhh..."})

          msg (db/create-message! {:id (db/uuid)
                                   :user-id (krieger :id)
                                   :thread-id (db/uuid)
                                   :created-at (java.util.Date.)
                                   :content (str "#" (watercooler :id) "Has anyone seen a chicken around lately?")
                                   :mentioned-tag-ids [(watercooler :id)]})

          _ (db/create-message! {:id (db/uuid)
                                 :user-id (malory :id)
                                 :thread-id (msg :thread-id)
                                 :created-at (java.util.Date.)
                                 :content (str "@" (krieger :id) "! Has something escaped the lab again?!")
                                 :mentioned-user-ids [(krieger :id)]})

_ (db/create-message! {:id (db/uuid)
                       :user-id (krieger :id)
                       :thread-id (msg :thread-id)
                       :created-at (java.util.Date.)
                       :content "No, no! Of course not. I was, just, uh, looking for a chicken to use in an experiment, and... wanted to uh, save a trip to the farm, that's all."})


msg (db/create-message! {:id (db/uuid)
                         :user-id (cyril :id)
                         :thread-id (db/uuid)
                         :created-at (java.util.Date.)
                         :content (str "Let’s do a quick pro/cons #" (review :id) " of our last mission, #" (deane :id) " :")
                         :mentioned-tag-ids [(review :id) (deane :id)]})

_ (db/create-message! {:id (db/uuid)
                       :user-id (cyril :id)
                       :thread-id (msg :thread-id)
                       :created-at (java.util.Date.)
                       :content "pro: we got the disk back"
                       :mentioned-tag-ids []})

_ (db/create-message! {:id (db/uuid)
                       :user-id (lana :id)
                       :thread-id (msg :thread-id)
                       :created-at (java.util.Date.)
                       :content "con: we ended up blackmailing the real Veronica Deane"
                       :mentioned-tag-ids []})

_ (db/create-message! {:id (db/uuid)
                       :user-id (ray :id)
                       :thread-id (msg :thread-id)
                       :created-at (java.util.Date.)
                       :content (str "con: we still don't know what #" (longwater :id) " means")
                       :mentioned-tag-ids [(longwater :id)]})

_ (db/create-message! {:id (db/uuid)
                       :user-id (pam :id)
                       :thread-id (msg :thread-id)
                       :created-at (java.util.Date.)
                       :content (str  "@" (archer :id) " got stabbed")
                       :mentioned-tag-ids []
                       :mentioned-user-ids [(archer :id)]})

_ (db/create-message! {:id (db/uuid)
                       :user-id (lana :id)
                       :thread-id (msg :thread-id)
                       :created-at (java.util.Date.)
                       :content "is that a pro or a con? ;)"
                       :mentioned-tag-ids []})

_ (db/create-message! {:id (db/uuid)
                       :user-id (cyril :id)
                       :thread-id (msg :thread-id)
                       :created-at (java.util.Date.)
                       :content "I'll put it down as pro"
                       :mentioned-tag-ids []})

])))
