package me.sgeb.gradle.nativeartifacts.internal

import me.sgeb.gradle.NativeArtifactsPlugin
import me.sgeb.gradle.nativeartifacts.NativeComponent
import me.sgeb.gradle.nativeartifacts.NativeComponentContainer
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.Zip
import org.gradle.nativebinaries.Executable
import org.gradle.nativebinaries.Library
import org.gradle.nativebinaries.SharedLibraryBinary
import org.gradle.nativebinaries.StaticLibraryBinary
import org.gradle.nativebinaries.toolchain.internal.ToolChainInternal
import org.gradle.nativebinaries.toolchain.internal.ToolSearchResult
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import static me.sgeb.gradle.nativeartifacts.TestHelpers.one
import static me.sgeb.gradle.nativeartifacts.internal.BuildNarTaskCreator.*

class BuildNarTaskCreatorTest extends Specification {
    def project
    def nativeComponents
    def nativeComponent

    private Zip aZipTask
    private Executable anExecutable
    private Library aLibrary

    void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: NativeArtifactsPlugin
        project.model { toolChains { add toolChainAny('any') } }

        nativeComponents = project.extensions.getByType(NativeComponentContainer)
        nativeComponent = aNativeComponent()

        aZipTask = aZipTask()
        anExecutable = anExecutable()
        aLibrary = aLibrary()
    }

    void 'configures native artifact building tasks'() {
        given:
        nativeComponent.from(anExecutable)

        when:
        project.evaluate()

        then:
        project.tasks.findByName(NAR_LIFECYCLE_TASK_NAME)?.group == NAR_GROUP
        project.tasks.findByName(NAR_BUILD_TASK_PREFIX + 'MyExeExecutable')?.group == NAR_GROUP
    }

    void 'all native artifact building tasks depend on lifecycle task'() {
        given:
        nativeComponent.from(anExecutable)
        nativeComponent.from(aLibrary)

        when:
        project.evaluate()

        then:
        Task lifecycleTask = project.tasks.findByName(NAR_LIFECYCLE_TASK_NAME)
        lifecycleTask.dependsOn.containsAll(project.tasks.findAll { it.name =~ "$NAR_BUILD_TASK_PREFIX." })
    }

    void 'does not change an ArchiveTask'() {
        when:
        nativeComponent.from(aZipTask)
        project.evaluate()

        then:
        one(nativeComponent.artifacts).archiveTask == aZipTask
    }

    void 'creates ArchiveTask wrapper for an Executable'() {
        when:
        nativeComponent.from(anExecutable)
        project.evaluate()

        then:
        with (one(nativeComponent.artifacts).archiveTask.rootSpec.children[0]) {
            children*.destPath.pathString.flatten() == ['bin']
            children*.sourcePaths.flatten() == anExecutable.binaries*.primaryOutput
        }
    }

    void 'creates static and shared ArchiveTask wrappers for a Library'() {
        when:
        nativeComponent.from(aLibrary)
        project.evaluate()

        then:
        nativeComponent.artifacts.all {
            assert archiveTask.rootSpec.children[0].children[0].destPath.pathString == 'lib'
            assert archiveTask.rootSpec.children[0].children[1].destPath.pathString == 'include'
        }
        nativeComponent.artifacts.matching { it.archiveTask.name =~ /StaticLibrary/ }.all {
            assert archiveTask.rootSpec.children[0].children[0].sourcePaths.toArray() ==
                    aLibrary.binaries.withType(StaticLibraryBinary).primaryOutput
        }
        nativeComponent.artifacts.matching { it.archiveTask.name =~ /SharedLibrary/ }.all {
            assert archiveTask.rootSpec.children[0].children[0].sourcePaths.toArray() ==
                    aLibrary.binaries.withType(SharedLibraryBinary).primaryOutput
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

    private NativeComponent aNativeComponent() {
        project.extensions.getByType(NativeComponentContainer).create('myComponent')
    }

    private Zip aZipTask() {
        return project.tasks.create('aZipTask', Zip)
    }

    private Executable anExecutable() {
        return project.executables.create('myExe')
    }

    private Library aLibrary() {
        return project.libraries.create('myLib')
    }

}
