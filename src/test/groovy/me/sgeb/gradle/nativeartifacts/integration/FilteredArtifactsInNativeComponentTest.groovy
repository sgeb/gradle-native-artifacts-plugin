package me.sgeb.gradle.nativeartifacts.integration

import me.sgeb.gradle.NativeArtifactsPlugin
import org.gradle.internal.os.OperatingSystem
import org.gradle.nativebinaries.StaticLibraryBinary
import org.gradle.nativebinaries.platform.Platform
import org.gradle.nativebinaries.toolchain.internal.ToolChainAvailability
import org.gradle.nativebinaries.toolchain.internal.ToolChainInternal
import org.gradle.nativebinaries.toolchain.internal.ToolSearchResult
import org.gradle.nativebinaries.toolchain.internal.gcc.AbstractGccCompatibleToolChain
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import static me.sgeb.gradle.nativeartifacts.TestHelpers.one

class FilteredArtifactsInNativeComponentTest extends Specification {

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
                    add toolChainAny('always')
                }
            }

            libraries {
                myLib
            }

            nativeArtifacts {
                my_component {
                    from libraries.myLib
                }
            }
        }

        when:
        project.evaluate()

        then:
        project.nativeArtifacts.my_component.artifacts.size() == 2
    }

    public void 'does not add any native artifacts to component on always-false filter'() {
        given:
        project.configure(project) {
            model {
                toolChains {
                    add toolChainAny('any')
                }
            }

            libraries {
                myLib
            }

            nativeArtifacts {
                my_component {
                    from (libraries.myLib) { false }
                }
            }
        }

        when:
        project.evaluate()

        then:
        project.nativeArtifacts.my_component.artifacts.empty
    }

    public void 'adds only filtered native artifacts to component'() {
        given:
        project.configure(project) {
            model {
                toolChains {
                    add toolChainAny('always')
                }
            }

            libraries {
                myLib
            }

            nativeArtifacts {
                my_component {
                    from (libraries.myLib) {
                        it instanceof StaticLibraryBinary
                    }
                }
            }
        }

        when:
        project.evaluate()

        then:
        project.nativeArtifacts.my_component.artifacts.size() == 1
        one(project.nativeArtifacts.my_component.artifacts).archiveTask.name =~ /StaticLibrary/
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
