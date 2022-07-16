![](https://github.com/ngobjects/ng-objects/workflows/build/badge.svg)

# ng-objects

ng-objects is an open source exploration of Apple's WebObjects (WO) framework. It aims to:

1. Extract the best concepts of WO and use them in a new, modern library.

2. Be compatible with WO's deployment environment, so apps can transparently integrate into an existing WO based deployment infrastructure. This means the apps know how to speak to JavaMonitor/wotaskd and handle requests from the WO Apache adaptor.

3. Not be a WO clone but _familiar_ to a WO programmer and thus easy to migrate existing WO code to. While WO is great, there have been two decades of improvements in software design and web development since WO's last official release.

## To launch the test application (ng-testapp)

### Install dependencies/frameworks

1. Clone this repo.
2. Run `mvn install` at the project's root.

### Build and launch `ng-testapp`:
1. `cd ng-testapp`
2. `mvn package`
3. `./target/ng-testapp-1.0.0-SNAPSHOT.woa/ng-testapp`

Now point your browser to [localhost:1200](http://localhost:1200/) you should be greeted with a test component showing some of the current features. Congratulations!
