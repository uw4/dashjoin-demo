# Dashjoin Demo Application

This application contains some demos and tutorials which can be deployed into a dashjoin platform.
The easiest way to do so is by using docker:

```
docker run -p 8080:8080 -e DJ_ADMIN_PASS=djdjdj -e DASHJOIN_HOME=dashjoin-demo -e DASHJOIN_APPURL=https://github.com/dashjoin/dashjoin-demo dashjoin/platform
```

The container is started with three environment variables:

* DJ_ADMIN_PASS: sets the default admin password to "djdjdj"
* DASHJOIN_HOME: moves the location of the model folder to ./dashjoin-demo
* DASHJOIN_APPURL: clones this repository upon startup

