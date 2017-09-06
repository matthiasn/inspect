# inspect desktop app

    $ lein cljsbuild auto main
    $ lein cljsbuild auto view
    $ lein cljsbuild auto updater
    $ lein sass auto
    $ npm start


## Publishing

    $ AWS_ACCESS_KEY_ID=<...> AWS_SECRET_ACCESS_KEY=<...> ./publish_beta.sh
    $ AWS_ACCESS_KEY_ID=<...> AWS_SECRET_ACCESS_KEY=<...> ./publish.sh

