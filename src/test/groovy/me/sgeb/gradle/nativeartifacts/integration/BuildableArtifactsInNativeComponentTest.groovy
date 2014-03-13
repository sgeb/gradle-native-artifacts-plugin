package me.sgeb.gradle.nativeartifacts.integration

import me.sgeb.gradle.NativeArtifactsPlugin
import me.sgeb.gradle.nativeartifacts.internal.BuildNarTaskCreator
import org.gradle.internal.os.OperatingSystem
import org.gradle.nativebinaries.platform.Platform
import org.gradle.nativebinaries.toolchain.internal.ToolChainAvailability
import org.gradle.nativebinaries.toolchain.internal.ToolChainInternal
import org.gradle.nativebinaries.toolchain.internal.ToolSearchResult
import org.gradle.nativebinaries.toolchain.internal.gcc.AbstractGccCompatibleToolChain
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import static me.sgeb.gradle.nativeartifacts.TestHelpers.one

class BuildableArtifactsInNativeComponentTest extends Specification {

    def project

    public void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: NativeArtifactsPlugin
    }

    public void 'adds all native artifacts to component by default'() {
        given:
        project.configure(project) {
            model {
                toolChains {
                    add toolChainAny('any')
                }
            }

            executables {
                myExe
            }

            nativeArtifacts {
                my_component {
                    from executables.myExe
                }
            }
        }

        when:
        project.evaluate()

        then:
        project.nativeArtifacts.my_component.artifacts.size() == 1
    }

    public void 'does not add any native artifacts to component when no toolchain '() {
        given:
        project.configure(project) {
            model {
                toolChains {
                    add toolChainNone('none')
                }
            }

            executables {
                myExe
            }

            nativeArtifacts {
                my_component {
                    from executables.myExe
                }
            }
        }

        when:
        project.evaluate()

        then:
        project.nativeArtifacts.my_component.artifacts.size() == 0
    }

    public void 'adds only buildable native artifacts to component'() {
        given:
        project.configure(project) {
            model {
                toolChains {
                    add toolChainMac('mac')
                    add toolChainNone('linux')
                }
                platforms {
                    "mac" {
                        operatingSystem "mac os x"
                    }
                    "linux" {
                        operatingSystem "linux"
                    }
                }
            }

            executables {
                myExe
            }

            nativeArtifacts {
                my_component {
                    from executables.myExe
                }
            }
        }

        when:
        project.evaluate()

        then:
        project.nativeArtifacts.my_component.artifacts.size() == 1
        one(project.nativeArtifacts.my_component.artifacts).archiveTask.name =~ /Mac/

        with(project.tasks.findByName(BuildNarTaskCreator.NAR_LIFECYCLE_TASK_NAME)) {
            dependsOn.contains(project.tasks.findByName('buildNarMacMyExeExecutable'))
            !dependsOn.contains(project.tasks.findByName('buildNarLinuxMyExeExecutable'))
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    def toolChainAny(String name) {
        Stub(ToolChainInternal) {
            getName() >> name
            canTargetPlatform(_) >> Stub(ToolSearchResult) {
                isAvailable() >> true
            }
        }
    }

    def toolChainNone(String name) {
        Stub(ToolChainInternal) {
            getName() >> name
            canTargetPlatform(_) >> Stub(ToolSearchResult) {
                isAvailable() >> false
            }
        }
    }

    def toolChainMac(String name) {
        new ToolChainMacStub(name)
    }

    ///////////////////////////////////////////////////////////////////////////

    private static class ToolChainMacStub extends AbstractGccCompatibleToolChain {
        ToolChainMacStub(String name) {
            super(name, OperatingSystem.MAC_OS, null, null, null)
        }

        @Override
        ToolSearchResult canTargetPlatform(Platform targetPlatform) {
            def availability = new ToolChainAvailability()
            if (!targetPlatform.operatingSystem.isMacOsX()) {
                availability.unavailable("stub mac, trying $targetPlatform.operatingSystem.displayName")
            }
            return availability
        }

        @Override
        protected String getTypeName() {
            return 'stubber'
        }
    }
}
