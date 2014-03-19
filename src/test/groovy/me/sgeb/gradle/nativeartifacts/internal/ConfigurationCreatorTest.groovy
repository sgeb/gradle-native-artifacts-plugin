package me.sgeb.gradle.nativeartifacts.internal

import me.sgeb.gradle.NativeArtifactsPlugin
import org.gradle.nativebinaries.toolchain.internal.ToolChainInternal
import org.gradle.nativebinaries.toolchain.internal.ToolSearchResult
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import static me.sgeb.gradle.nativeartifacts.TestHelpers.one

class ConfigurationCreatorTest extends Specification {
    def project

    void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: NativeArtifactsPlugin
    }

    void 'adds Configurations with only defaults'() {
        given:
        project.configure(project) {
            model {
                toolChains {
                    add toolChainAny('any')
                }
            }
            executables {
                cli
            }

            nativeArtifacts {
                myCli {
                    from executables.cli
                }
            }
        }

        when:
        project.evaluate()

        then:
        project.configurations.findByName("compile")
        one(project.configurations.findByName("compileCli")?.extendsFrom) == project.configurations["compile"]
        one(project.configurations.findByName("compileCliDebug")?.extendsFrom) == project.configurations["compileCli"]
    }

    void 'adds Configurations with custom platforms, buildTypes and flavors'() {
        given:
        project.configure(project) {
            model {
                toolChains {
                    add toolChainAny('any')
                }
                platforms {
                    linux {
                        operatingSystem "linux"
                    }
                    "osx-i386" {
                        operatingSystem "mac os x"
                        architecture "i386"
                    }
                    "osx-amd64" {
                        operatingSystem "mac os x"
                        architecture "amd64"
                    }
                }
                buildTypes {
                    debug
                    release
                }
                flavors {
                    english
                    french
                }
            }
            executables {
                cli
                cli2
                cli3
            }

            nativeArtifacts {
                myCli {
                    from executables.cli
                }
            }
        }

        when:
        project.evaluate()

        then:
        project.configurations.findByName("compile")
        one(project.configurations.findByName("compileCli")?.extendsFrom) == project.configurations["compile"]

        one(project.configurations.findByName("compileCliLinux")?.extendsFrom) == project.configurations["compileCli"]
        one(project.configurations.findByName("compileCliLinuxDebug")?.extendsFrom) == project.configurations["compileCliLinux"]
        one(project.configurations.findByName("compileCliLinuxDebugEnglish")?.extendsFrom) == project.configurations["compileCliLinuxDebug"]
        one(project.configurations.findByName("compileCliLinuxDebugFrench")?.extendsFrom) == project.configurations["compileCliLinuxDebug"]
        one(project.configurations.findByName("compileCliLinuxRelease")?.extendsFrom) == project.configurations["compileCliLinux"]
        one(project.configurations.findByName("compileCliLinuxReleaseEnglish")?.extendsFrom) == project.configurations["compileCliLinuxRelease"]
        one(project.configurations.findByName("compileCliLinuxReleaseFrench")?.extendsFrom) == project.configurations["compileCliLinuxRelease"]

        one(project.configurations.findByName("compileCliOsx-i386")?.extendsFrom) == project.configurations["compileCli"]
        one(project.configurations.findByName("compileCliOsx-i386Debug")?.extendsFrom) == project.configurations["compileCliOsx-i386"]
        one(project.configurations.findByName("compileCliOsx-i386DebugEnglish")?.extendsFrom) == project.configurations["compileCliOsx-i386Debug"]
        one(project.configurations.findByName("compileCliOsx-i386DebugFrench")?.extendsFrom) == project.configurations["compileCliOsx-i386Debug"]
        one(project.configurations.findByName("compileCliOsx-i386Release")?.extendsFrom) == project.configurations["compileCliOsx-i386"]
        one(project.configurations.findByName("compileCliOsx-i386ReleaseEnglish")?.extendsFrom) == project.configurations["compileCliOsx-i386Release"]
        one(project.configurations.findByName("compileCliOsx-i386ReleaseFrench")?.extendsFrom) == project.configurations["compileCliOsx-i386Release"]

        one(project.configurations.findByName("compileCliOsx-amd64")?.extendsFrom) == project.configurations["compileCli"]
        one(project.configurations.findByName("compileCliOsx-amd64Debug")?.extendsFrom) == project.configurations["compileCliOsx-amd64"]
        one(project.configurations.findByName("compileCliOsx-amd64DebugEnglish")?.extendsFrom) == project.configurations["compileCliOsx-amd64Debug"]
        one(project.configurations.findByName("compileCliOsx-amd64DebugFrench")?.extendsFrom) == project.configurations["compileCliOsx-amd64Debug"]
        one(project.configurations.findByName("compileCliOsx-amd64Release")?.extendsFrom) == project.configurations["compileCliOsx-amd64"]
        one(project.configurations.findByName("compileCliOsx-amd64ReleaseEnglish")?.extendsFrom) == project.configurations["compileCliOsx-amd64Release"]
        one(project.configurations.findByName("compileCliOsx-amd64ReleaseFrench")?.extendsFrom) == project.configurations["compileCliOsx-amd64Release"]
    }

    void 'sets narConfName on binaries'() {
        given:
        project.configure(project) {
            executables {
                anExe
            }

            libraries {
                aLib
            }
        }

        when:
        project.evaluate()

        then:
        project.binaries.all {
            assert it.narConfName =~ "compile(AnExe|ALib).+"
        }
    }

    //////////////////////////////////////////////////////////////////////////

    def toolChainAny(String name) {
        Stub(ToolChainInternal) {
            getName() >> name
            canTargetPlatform(_) >> Stub(ToolSearchResult) {
                isAvailable() >> true
            }
        }
    }
}
