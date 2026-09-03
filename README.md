![](https://github.com/ngobjects/ng-objects/workflows/build/badge.svg)

# ng-objects

ng-objects is an open source web framework heavily based on concepts from Apple's WebObjects (WO) framework. It aims to:

1. Extract the best concepts of WO and project Wonder (including the Ajax-framework, with it's partial component rendering) and use them in a new, modern library.

2. Not be a WO clone but _familiar_ to a WO programmer and thus easy to migrate existing WO code to. While WO is great, there have been two decades of improvements in software design and web development since WO's last official release.

3. Be compatible with WO's deployment environment, so apps can transparently integrate into an existing WO deployment infrastructure, easing transition of existing apps and environments.

## Status

The framework is under active development, with interim development releases (the 0.1.x series) published to Maven Central under the `is.rebbi.ng` groupId. Java 21 or later is required.

Working today: HTTP request handling, templating, stateful component actions with partial page updates (Ajax update containers), a thoroughly reworked page cache, sessions, resource loading and management, property/configuration management, basic routing, WO deployment integration (wotaskd/Monitor lifebeats) and a growing set of development tools (live console/log access, an eval endpoint, dev-server integration for tooling).

The APIs are still being cleaned up, enriched and reshaped — the framework is actively dogfooded on production sites, and what that teaches us drives the design. Expect changes between interim releases.

## Trying the test application

Clone the repo and either import the projects into your IDE and run the "Application" class in the ng-testapp project, or, using maven on the command line...

```
	$ git clone git@github.com:ngobjects/ng-objects.git
	$ cd ng-objects
	$ mvn install
	$ cd ng-testapp
	$ mvn package
	$ ./target/ng-testapp-*.woa/ng-testapp
```

Then point your browser to [localhost:1200](http://localhost:1200/). Yay!
