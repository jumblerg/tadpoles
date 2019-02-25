# tadpoles media download script
download images from tadpoles in bulk

1. launch the repl from the project root.

```bash
boot repl
```

2. obtain a session cookie by logging in via the browser, then grab it from the
request.


3. load the tadpole namespace, change to it, then define the session cookie in
the repl.  call the `download-session-data` function to download all the media
associated with the account corresponding to the cookie.

```clojure
=> (require 'tadpoles :reload-all)
=> (in-ns 'tadpoles)
=> (def cookie "...")
=> (download-session-media! "media" cookie time)
```
