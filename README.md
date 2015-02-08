# inspect

[![Clojars Project](http://clojars.org/com.matthiasnehlsen/inspect/latest-version.svg)](http://clojars.org/com.matthiasnehlsen/inspect)

Library / tool for inspecting messages. Read more about it in this **[blog post](http://matthiasnehlsen.com/blog/2014/11/14/Inspect/)**. Here's how the UI currently looks like:

<a href="http://inspect.matthiasnehlsen.com" target="_blank"><img src="http://matthiasnehlsen.com/images/inspect.png" /></a>

You can click on the image for a live demo.

## Usage

Using the **inspect** library is super simple. All you need to do is add **inspect** to the dependencies in your project.clj:

    [com.matthiasnehlsen/inspect "0.1.5"]

Then, you probably want to import it where you need it:

    [com.matthiasnehlsen.inspect :as inspect :refer [inspect]]

Next, you want to start it once, from anywhere:

    (inspect/start)

That's all, now you can use it, for example:

    (inspect :interval-put/every-second {:msg "every second"})

Also check out the sample application, which is running as a live demo.

## Project maturity

This project is very young and I'm still exploring the problem space. Therefore anything can change in the next version. That being said, you may probably already find it useful. Please let me know about any problems you encounter and also about any idea that would make this more useful.


## License

Copyright © 2014-2015 Matthias Nehlsen

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
