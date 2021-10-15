# Browser profile launcher

Browser profile launcher is a CLI/UI utility written in clojure/babashka. It targets Linux + xorg. I'm successfully using it on Ubuntu Budgie, but it should run on pretty much any distribution.

This utility acts as a simple browser startup wrapper, which opens a specific browser profile (new tab or window) based on the actual URL that you're trying to open.

This utility is supposed to be registered as system's "Default application" (default web browser). This way, whenever you click any http(s) link which in any application, `Browser profile launcher` will be executed instead of the actual browser. Once executed, it will decide in which chrome profile window the URL should be opened and it opens it in that profile's browser window/tab.

Originally this utility only supported a single browser (google chrome), but later I added a generic support for other browsers as well, for example firefox. See the configuration example bellow.

# Why is this useful?

I'm using a google chrome with 2 profiles. Each has its own history, sets of bookmarks, sets of plugins, cookies/sessions/local stores etc. One profile is dedicated to my personal stuff, the other one is dedicated for work. By default, whenever I used to click any link (eg. link to my customer's JIRA ticket in my customer's Slack), it got opened in the chrome window (profile) that I most recently used. This is very naive and annoying strategy. I often had to copy the url manually and then paste it into correct chrome window.

With Browser profile launcher, I simply define a whitelist of URL regex patterns for each profile and those URLs will be opened in correct profile (window) automatically. They either open as new tab or new window, based on whether I already have some browser window open for that profile or not. The rest of URLs (which don't match any URL pattern) are opened in "fallback" profile (ie. my personal profile). 

In case I have some URL for which I want to manually choose the profile to be used, I can simply hold down the `alt` key on my keyboard when mouse-clicking the link. This brings up a simple dialog which allows me to choose the profile to be used.

# Requirements

- [babashka](https://babashka.org/) (ie. `bb` binary somewhere on your path)
- [xinput](https://github.com/freedesktop/xorg-xinput) (you should have this tool installed if you have xserver based distribution)
- [YAD (yet another dialog)](https://sourceforge.net/projects/yad-dialog/) (only required if you want to show the profile choice dialog window by holding `alt` key)

# Installation

1) Put `src/browser-profile-launcher.clj` somewhere, make it executable `chmod +x browser-profile-launcher.clj`. In my case, I've put a sym-link of mentioned file into `/home/brdloush/bin/browser-profile-launcher`.

```bash
ln -s ~/projects/browser-profile-launcher/src/browser-profile-launcher.clj ~/bin/browser-profile-launcher
chmod +x /home/brdloush/bin/browser-profile-launcher
```

2) Provide some configuration in `~/.browser-profile-launcher/profiles.edn` file. Here's a sample content:

```edn
{:browsers {:chrome {:exec-command ["google-chrome" "--profile-directory=:dir" ":url"]}
            :firefox {:exec-command ["firefox" "--new-tab" ":url"]}}
 :profiles {"personal" {:name "personal"
                        :params {:dir "Profile 1"}
                        :fallback? true     
                        :url-regexes []}
            "work" {:name "work"
                    :params {:dir "Profile 2"}
                    :url-regexes ["http[s]?://[^/]+\\.mycustomer\\.io"
                                  "http[s]?://mycustomer\\.slack\\.com"
                                  "http[s]?://mycustomer\\.splunkcloud\\.com"
                                  "http[s]?://github.com/mycustomer"
                                  "http[s]?://mycustomer.atlassian.net"]}
            "firefox" {:name "firefox"
                       :browser :firefox
                       :url-regexes ["http[s]?://some-url-to-be-opened-in-firefox.com"]}}}
```

Note: `:dir` parameter value for chrome `--profile-directory` should be a name of profile subdirectory in `~/.config/google-chrome` (the location may differ on some linux distributions). You might need to do some investigation to find out which directory holds which chrome profile.

3) Create a `/usr/share/applications/browser-profile-launcher.desktop` file. I used `google-chrome.desktop` as a reference, but only provided a simple content:

```
[Desktop Entry]
Version=1.0
Name=Browser Profile Launcher
GenericName=Browser Profile Launcher
Comment=Launch specific browser/profile based on provided URL
Exec=/home/brdloush/bin/browser-profile-launcher %U
StartupNotify=true
Terminal=false
Icon=google-chrome
Type=Application
Categories=Network;WebBrowser;
MimeType=application/pdf;application/rdf+xml;application/rss+xml;application/xhtml+xml;application/xhtml_xml;application/xml;image/gif;image/jpeg;image/png;image/webp;text/html;text/xml;x-scheme-handler/ftp;x-scheme-handler/http;x-scheme-handler/https;
```

4) update a desktop file database:
```bash
sudo update-desktop-database
```

5) Go into your system's `Default applications` and choose `Google Chrome Launcher` as your default browser (sometimes configured as `Web` application).

6) Try to click any link, which would normally open in your previous default browser. Optionally, hold `alt` key before clicking the link - in that case a GUI chooser window will pop up and ask you for the browser/profile you want the link to be opened with.
