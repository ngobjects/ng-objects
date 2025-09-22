![](https://github.com/ngobjects/ng-objects/workflows/build/badge.svg)

# ng-objects

ng-objects is an open source web framework heavily based on concepts from Apple's WebObjects (WO) framework. It aims to:

1. Extract the best concepts of WO and project Wonder (including the Ajax-framework, with it's partial component rendering) and use them in a new, modern library.

2. Not be a WO clone but _familiar_ to a WO programmer and thus easy to migrate existing WO code to. While WO is great, there have been two decades of improvements in software design and web development since WO's last official release.

3. Be compatible with WO's deployment environment, so apps can transparently integrate into an existing WO deployment infrastructure, easing transition of existing apps and environments.

## Status

The framework is still in development, but we currently have working implementations of HTTP request handling, templating, very, very basic routing, resource loading and management, property/configuration management, sessions, and stateful actions.

"Working implementation" is doing a lot of heavy lifting in that sentence though, since we're now going through the process of cleaning up and enriching the APIs and making them a joy to use. And there's a lot of work to do. But the framework has been deployed for testing on a couple of production sites, since nothing is quite as valuable as dogfooding when it comes to developing a framework - and remarkably it works very, very well. First actual release is scheduled for 2025.

## Trying the test application

We haven't made a release yet, so to try the framework you need to either clone the repo and either import the projects into your IDE and run the "Application" class in the ng-testapp project, or, using maven on the command line...

```
	$ git clone git@github.com:ngobjects/ng-objects.git
	$ cd ng-objects
	$ mvn install
	$ cd ng-testapp
	$ mvn package
	$ ./target/ng-testapp-0.1.0-SNAPSHOT.woa/ng-testapp
```

Then point your browser to [localhost:1200](http://localhost:1200/). Yay!