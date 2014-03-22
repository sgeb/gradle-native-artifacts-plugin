# Gradle native artifacts plugin

[![Build Status](https://travis-ci.org/sgeb/gradle-native-artifacts-plugin.png?branch=master)](https://travis-ci.org/sgeb/gradle-native-artifacts-plugin)

A gradle plugin to package C/C++ native executables and libraries into artifacts
publishable to Ivy or Maven, with support for transitive dependencies.

## When is this plugin useful?

*TL;DR: This plugin is most useful on a C/C++ project. It publishes the
project's native artifacts to Ivy and Maven. It also resolves direct and
transitive dependencies such as third-party libraries without the need to
compile them on every developer machine.*

In a Java project you usually pull in direct and transitive dependencies through
Ivy and Maven. These JAR dependencies are OS and architecture agnostic because
they run in the Java Virtual Machine. How do you achieve a similar convenience
for native projects where dependencies are OS and architecture specific?

Think of a C/C++ project that depends on a third party library (e.g.
[Boost](http://www.boost.org/)) and on a testing framework to run unit tests
(e.g. [gtest](https://code.google.com/p/googletest/)). There are usually two
ways to pull in dependencies:

* Include the dependencies' source code into your project and compile them along
  with your code on each platform. Every initial compilation on each platform
  must build all dependencies (and potentially its transitive dependencies)
  which can take a long time for bigger libraries such as Boost. In addition it
  is not always straightforward to integrate external libraries into an existing
  build system.  Most libraries require an external setup phase and separate
  compilation steps.

* Install the pre-compiled libraries into your system. If you're lucky the
  libraries are already available through the standard package manager (think
  `apt-get`, `rpm`, `brew`, etc). Otherwise you'd have to compile and install
  the dependencies. There is usually no way around compiling the dependencies
  yourself if cross-compilation is part of the mix

This plugin provides a third option. It brings the convenience of dependency
management to gradle-based C/C++ projects. Each dependency (and transitive
dependency) is compiled once per platform, packaged into an artifact and
published to Ivy or Maven. The publication includes configuration and dependency
information so that transitive dependencies don't need to be added manually to
each project.

Multiple versions of an artifact can co-exist in Ivy / Maven, simplifying
testing of a C/C++ project against different versions of third party libraries.
No need to clean and rebuild a dependency; updating the version number of the
dependency in the gradle config is enough. Dynamic version dependencies (e.g.
"depend on the latest version of the 2.1 branch") are supported as well.

The continuous integration of different modules is greatly simplified as well.
Imagine a CI server building `libA` on every green commit and publishing it to
the artifact repository. A project `TastyApp` with a dependency on `libA` can
define the dependency pointing to the latest integration build of `libA` (via
dynamic versions described above). Then `TastyApp` is guaranteed to always be
tested with the latest and greatest version of `libA` and can identify
integration issues as early as possible.

## How?

Here is an example on how to configure the plugin. The important part is the
`nativeArtifacts` block.

*Documentation is currently sparse, I will consider writing more in the future.*

```groovy
buildscript {
    repositories {
        maven {
            url "https://sgeb.github.io/maven_repo/"
        }
    }

    dependencies {
        classpath "me.sgeb.gradle:gradle-native-artifacts-plugin:1.+"
    }
}

apply plugin: 'cpp'
apply plugin: 'native-artifacts'
apply plugin: 'ivy-publish'

// Set up source sets here for libA and tastyApp

libraries {
    libA
}

executables {
    tastyApp {
        binaries.all {
            lib libraries.libA.static
            linker.args "-lboost"
        }
    }

    tastyAppTest {
        binaries.all {
            lib libraries.libA.static
            linker.args "-lboost", "-lgtest"
        }
    }
}

binaries.all {
    cppCompiler.args "-I${it.narDepsDir}/include"
    linker.args "-L${it.narDepsDir}/lib"
}

// Set up your repository here

dependencies {
    compileLibA 'org.example.vendor:boost:1.55.+@nar'
    compileTastyAppTest 'org.example.vendor:gtest:1.+@nar'
}

model {
    buildTypes {
        debug
        release
    }
}

nativeArtifacts {
    my_liba {
        from (libraries.libA) { it instanceof StaticLibraryBinary }
    }
}

publishing {
    publications {
        ivy(IvyPublication) {
            from nativeArtifacts.my_liba
        }
    }
}
```

## FAQ

#### How do I create native artifacts from third-party libraries?

Gradle-native-artifacts-plugin publishes native artifacts from binaries built by
Gradle. It won't build projects using other build tools such as Makefile and
CMake.

That said there is an alternative way to create native artifacts compatible with
this plugin. The following steps describe the process:

1. Download the source code
2. Apply custom patches (optional, usually not necessary)
3. Configure the build for the target platform and other misc options such as a
   temporary installation directory (e.g.  `./configure --host=amd64-pc-linux
   --prefix=$(pwd)/install`)
4. Compile the project (e.g. `make all`)
5. Install the binaries, headers files or other needed files into a temporary
   directory (e.g. `make install`)
6. Zip up the temporary directory into an artifact. In case you're targeting
   multiple platforms,  tag the artifact using Maven classifiers.
7. Create or adjust the publication descriptor (usually specific to either Maven
   or Ivy)
8. Repeat steps 3-7 for each target platform
9. Publish the result to a Maven or Ivy repository

This process could be implemented using standard bash scripting but Gradle makes
it easier, especially when creating Maven or Ivy publications.

I've built a similar workflow in
[opencash/vendor](https://github.com/opencash/vendor), see
[build.gradle](https://github.com/opencash/vendor/blob/master/build.gradle).
Feel free to copy the bits relevant to your project.

#### How do I cross-compile to different platforms?

Targeting different platforms is handled by Gradle itself, not by this plugin.
If your build is set up to cross-compile, gradle-native-artifacts-plugin will
automatically include an artifact for each platform, tagged with
platform-specific Maven classifiers. The choice of target platforms can be
restricted using closure filters, such as in this example:

```groovy
nativeArtifacts {
    my_lib {
        from (libraries.myLib) {
            // Assuming platforms "linux-amd64" and "android-arm" have been
            // set up in model { platforms { ... } }

            it.targetPlatform.name == "linux-amd64" ||
            it.targetPlatform.name == "android-arm" ||
            it.targetPlatform.operatingSystem.macOsX
        }
    }
}
```

Two things to note here:

1. The parentheses around `libraries.myLib` are mandatory.

    In fact the method `from()` takes two parameters: the component to package
    and the optional closure filter. Groovy, the language used by Gradle config
    files, dictates the use of parentheses around the non-closure parameters in
    order to treat the closure itself as a parameter as well.

    Unfortunately you won't get an error if you forget the parentheses. Instead
    the closure filter will be silently ignored.

2. The closure filter is executed in the context of a candidate binary, not in
   the context of the native component.

    In the example, `libraries.myLib` is a native component made up of binaries,
    namely one binary per targetPlatform-buildType-flavor-linkType combination.
    The linkType dimension only applies to library components (shared / static).

    The closure filter is applied at that binary level. It is executed for each
    binary in turn. If the closure evaluates to `true` for a given binary, then
    that binary will be included in the native artifact. Otherwise it is
    ignored.

You can find an example using at
[opencash/libopencash](https://github.com/opencash/libopencash), see
[build.gradle](https://github.com/opencash/libopencash/blob/master/build.gradle).

#### Can additional compiler/linker flags be specified in a native artifact?

Currently this is not supported and projects depending on native artifacts have
to specify all relevant compilation flags themselves. This also applies to
linker flags for direct and transitive dependencies.

The process of formalizing compiler flags is not trivial due to the variety of
compiler families, versions and available options. But it is a valuable feature
and I am considering it for a future release. In the meantime it is captured in
[issue #2](https://github.com/sgeb/gradle-native-artifacts-plugin/issues/2).

#### Are there any examples or real-life projects using this plugin?

There are no "official" examples. I have developed this plugin out of need in
a couple of my own projects, hopefully they provide a good starting point:

- [opencash/vendor](https://github.com/opencash/vendor): creates native
  artifacts from third-party libraries
  [[build.gradle](https://github.com/opencash/vendor/blob/master/build.gradle)]

- [opencash/libopencash](https://github.com/opencash/libopencash): compiles and
  publishes native artifacts, pulling in third-party dependencies from
  opencash/vendor
  [[build.gradle](https://github.com/opencash/libopencash/blob/master/build.gradle)]

These examples create native artifacts targeting osx-x86_64, linux-amd64 and
android-arm (Android).

## Contributing

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request
