ImagePoll
---------

ImagePoll is an image based poll creator. For more information see our [wiki](https://github.com/kimagure/ImagePoll/wiki/). 


Front End
=========

The angularjs app is located in the `app/client/` directory. In order to build and deploy the static files you will need the following:
 * NodeJS
 * bower (installed globally)
 * grunt (installed globally)
 * Ruby
 * Compass 

###Building the Angularjs app

`cd app/client`
`npm install`
`bower install`

Running the grunt build task will deploy minified, production ready versions of the angular app in `public/javascripts/dist`.

`grunt build`

###Deploying the Front End Locally

This will deploy the current angular app source via a live-reloading test server. This is helpful if you want your workflow to only build the static files
once they are correct.

`grunt serve`


###Testing the front end

`grunt test`


Back End
========

The Scala Play! app is located in the root directory of the git repo. In order to build and deploy the scala API + the staticly built angular files in `public/` you will need sbt installed.
You will also need mongo to setup the database.

###Running the Play! API + most recently built angular files

`sbt run`


###Testing the Back End

`sbt test`