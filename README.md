# inspect

This Electron-based application lets you look inside systems built with the **[systems-toolbox](https://github.com/matthiasn/systems-toolbox)**. You need a system that is configured to offload its firehose to a Kafka topic. Once that is set up, you can connect this application to your Kafka host and watch it draw your system from the messages flowing through. Other than that, this application has zero knowledge about the system under observation. This particular screenshot comes from observing my **[iWasWhere](https://github.com/matthiasn/iWasWhere)** journaling application, where **inspect** has already helped quite a bit in making sense of message flows.

![Screenshot](./doc/screenshot.png)


## Download

If you don't want to build this application yourself, you can download an already packaged application:

* **[Mac](https://s3.eu-central-1.amazonaws.com/matthiasn-inspect/inspect-0.2.10.dmg)**
* **[Linux](https://s3.eu-central-1.amazonaws.com/matthiasn-inspect/inspect-0.2.10-x86_64.AppImage)**
* **[Windows](https://s3.eu-central-1.amazonaws.com/matthiasn-inspect/inspect+Setup+0.2.10.exe)**

The Mac and Windows versions will notify you when there's an update. For Linux, that feature is still missing, unfortunately. If you believe that should be different, maybe you can help out with the [electron-builder](https://github.com/electron-userland/electron-builder/issues/1138) project.


## Building inspect

    $ lein cljsbuild auto main
    $ lein cljsbuild auto view
    $ lein cljsbuild auto updater
    $ lein sass auto
    $ npm start


## Publishing inspect

    $ AWS_ACCESS_KEY_ID=<...> AWS_SECRET_ACCESS_KEY=<...> ./publish_beta.sh
    $ AWS_ACCESS_KEY_ID=<...> AWS_SECRET_ACCESS_KEY=<...> ./publish.sh


## Contributions

Contributions always welcome. Please help to make this project more useful, prettier, and just generally more awesome. Thanks! 


## License

Copyright © 2016, 2017 Matthias Nehlsen

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
