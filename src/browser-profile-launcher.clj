#!/usr/bin/env bb
(ns browser-profile-launcher.core
  (:require [clojure.edn :as edn]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as str]))

(def keycode-alt 64)

(defn holding-key? [key-code]
  (let [bash-cmd (format "xinput --list --id-only | xargs -I{} xinput query-state {} 2>/dev/null | grep down | grep %s | wc -l" key-code)
        keydown-output (->> (apply sh ["bash" "-c" bash-cmd])
                            :out
                            str/split-lines
                            first)]
    (= keydown-output "1")))

(defn ask-for-profile-name! [profiles]
  (let [profile-names (keys profiles)
        {:keys [exit out]} (apply sh (concat ["yad" "--list" "--title=Choose browser" "--column=profile" "--no-headers" "--width=250" "--height=150"] profile-names))]
    (when (= exit 0)
      (-> out
          str/split-lines
          first
          (str/split #"\|")
          first))))

(defn profile-for-url [url profiles]
  (->> profiles
       (filter (fn [[_ {:keys [url-regexes]}]]
                 (->> url-regexes
                      (filter (fn [regex]
                                (re-find (re-pattern regex) url)))
                      not-empty)))
       ffirst))

(defn fallback-profile-name [profiles]
  (->> profiles
       (some (fn [[_ {:keys [name fallback?]}]]
               (when fallback? name)))))

(defn replace-kw-placeholders [s placehoders-map]
  (->> (reduce
        (fn [s replacement] (str/replace s (str (first replacement)) (str (second replacement))))
        s
        placehoders-map)))

(defn apply-params-on-placeholders [exec-command-parts params]
  (->> exec-command-parts
       (map (fn [cmd-part]
              (if (string? cmd-part)
                (replace-kw-placeholders cmd-part params)
                cmd-part)))))

(defn build-sh-command-args [profile-name url profiles browsers]
  (let [profile (get profiles profile-name)
        params (-> (get profile :params {})
                   (assoc :url url))
        {:keys [exec-command]} (get browsers (:browser profile))]
    (-> exec-command
        (apply-params-on-placeholders params))))

(defn open-browser! [url profiles browsers]
  (when-let [profile-name (if (holding-key? keycode-alt)
                            (ask-for-profile-name! profiles)
                            (or (profile-for-url url profiles)
                                (fallback-profile-name profiles)))]
    (apply sh (build-sh-command-args profile-name url profiles browsers))))

(when-let [url (first *command-line-args*)]
  (let [config-file-path (str (System/getProperty "user.home") "/.browser-profile-launcher/profiles.edn")
        {:keys [profiles browsers]} (edn/read-string (slurp config-file-path))]
    (open-browser! url profiles browsers)))
