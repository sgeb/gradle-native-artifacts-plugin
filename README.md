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
for native projects when dependencies *are* OS and architecture dependent?

Think of a C/C++ project that depends on a third party library (e.g.
[Boost](http://www.boost.org/)) and on a testing framework to run unit tests
(e.g. [gtest](https://code.google.com/p/googletest/)). There are usually two
ways to pull in dependencies:

* Include the dependencies' source code into your project and compile them along
  with your code on each platform. Every initial compilation on each platform
  must build all dependencies (and potentially its transitive dependencies)
  which can take a long time for bigger libraries such as Boost. In addition it
  is sometimes complicated to integrate external libraries into an existing
  build system.  Most libraries require an external setup phase and separate
  compilation steps.

* Install the pre-compiled libraries into your system. If you're lucky the
  libraries are already available through the standard package manager (think
  `apt-get`, `rpm`, `brew`, etc). Otherwise you'd have to compile and install
  the dependencies. If cross-compilation is part of the mix then there's usually
  no way around compiling the dependencies yourself.

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

Furthermore the continuous integration of different modules is greatly
simplified as well. Imagine a CI server building `libA` on every green commit
and publishing it to the artifact repository. A project `TastyApp` with a
dependency on `libA` can define the dependency pointing to the latest
integration build of `libA` (via dynamic versions described above). Thereby
`TastyApp` is guaranteed to always be tested with the latest and greatest
version of `libA` and can identify integration issues as early as possible.

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
        from libraries.libA { it instanceof StaticLibrary }
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

## Contributing

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request
