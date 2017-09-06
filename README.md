# inspect desktop app

    $ lein cljsbuild auto main
    $ lein cljsbuild auto view
    $ lein cljsbuild auto updater
    $ lein sass auto
    $ npm start


## Publishing

Publishing entire project from parent directory:

    $ AWS_ACCESS_KEY_ID=<...> AWS_SECRET_ACCESS_KEY=<...> ./publish_beta.sh
    $ AWS_ACCESS_KEY_ID=<...> AWS_SECRET_ACCESS_KEY=<...> ./publish.sh

