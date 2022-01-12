# Dashjoin Demo Application

This application showcases various features of the Dashjoin Platform.
The demo includes the northwind sample database. The import scripts are already
included in the binaries and can be loaded into the database using the 
the initScripts configuration field (see the definition in model/dj-database).

To install it, you can use the upload feature on the configuration 
database (/resource/config/dj-database/dj%2Fconfig) or copy the files into
your model folder manually.
You can also have the platform install the app right from github
upon startup. The easiest way to do so is by using docker:

```
docker run -p 8080:8080 -e DJ_ADMIN_PASS=djdjdj -e DASHJOIN_HOME=dashjoin-demo -e DASHJOIN_APPURL=https://github.com/dashjoin/dashjoin-demo dashjoin/platform
```

The container is started with three environment variables:

* DJ_ADMIN_PASS: sets the default admin password to "djdjdj"
* DASHJOIN_HOME: moves the location of the model folder to ./dashjoin-demo
* DASHJOIN_APPURL: clones this repository upon startup

If you are using the executable platform version, the DASHJOIN_HOME directory is fixed to userhome/.dashjoin.
You can check out the repository manually into this folder, set the environment and then run the platform. Consider the following example for Windows:

```
git clone https://github.com/dashjoin/dashjoin-demo .dashjoin
set DASHJOIN_APPURL=https://github.com/dashjoin/dashjoin-demo
set DJ_ADMIN_PASS=djdjdj
Dashjoin.exe
```

You can also check out a read-only copy by visiting https://demo.my.dashjoin.com/ and logging in with the guest user.
