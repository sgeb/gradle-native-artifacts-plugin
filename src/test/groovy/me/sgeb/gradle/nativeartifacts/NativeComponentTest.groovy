package me.sgeb.gradle.nativeartifacts

import me.sgeb.gradle.NativeArtifactsPlugin
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Task
import org.gradle.api.internal.component.Usage
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

class NativeComponentTest extends Specification {
    def project
    def nativeComponents
    def nativeComponent

    private Zip aZipTask
    private Task aGenericTask
    private Executable anExecutable
    private Library aLibrary
    private Zip anotherZipTask

    void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: NativeArtifactsPlugin
        project.model { toolChains { add toolChainAny('any') } }

        nativeComponents = project.extensions.getByType(NativeComponentContainer)
        nativeComponent = aNativeComponent()

        aZipTask = aZipTask()
        aGenericTask = aGenericTask()
        anExecutable = anExecutable()
        aLibrary = aLibrary()
        anotherZipTask = anotherZipTask()
    }

    void 'can add from an AbstractArchiveTask'() {
        when:
        nativeComponent.from(aZipTask)
        project.evaluate()

        then:
        nativeComponent.artifacts.size() == 1
    }

    void 'adding from a generic Task throws InvalidUserDataException'() {
        when:
        nativeComponent.from(aGenericTask)

        then:
        thrown(InvalidUserDataException)
    }

    void 'can add from an Executable'() {
        when:
        nativeComponent.from(anExecutable)
        project.evaluate()

        then:
        nativeComponent.artifacts.size() == 1
    }

    void 'can add from an Executable with a filter'() {
        when:
        nativeComponent.from(anExecutable) { true }
        project.evaluate()

        then:
        nativeComponent.artifacts.size() == 1
    }

    void 'can add from a Library'() {
        when:
        nativeComponent.from(aLibrary)
        project.evaluate()

        then:
        // One static library, one shared library
        nativeComponent.artifacts.size() == 2
    }

    void 'can add from a Library with filter'() {
        when:
        nativeComponent.from(aLibrary) { it instanceof StaticLibraryBinary }
        project.evaluate()

        then:
        nativeComponent.artifacts.size() == 1
    }

    void 'can add from multiple tasks and native binaries'() {
        when:
        nativeComponent.from(aZipTask)
        nativeComponent.from(anExecutable)
        nativeComponent.from(aLibrary) { it instanceof SharedLibraryBinary }
        nativeComponent.from(anotherZipTask)
        project.evaluate()

        then:
        nativeComponent.artifacts.size() == 4
    }

    void 'creates usage from an AbstractArchiveTask'() {
        when:
        nativeComponent.from(aZipTask)
        project.evaluate()

        then:
        one(one(nativeComponent.usages).artifacts).archiveTask == aZipTask
        one(nativeComponent.usages).name == aZipTask.classifier
        one(nativeComponent.usages).dependencies.empty
    }

    void 'creates usage from an Executable'() {
        when:
        project.dependencies.compileMyExe('group:name:version')
        nativeComponent.from(anExecutable)
        project.evaluate()

        then:
        one(one(nativeComponent.usages).artifacts) == one(nativeComponent.artifacts)
        one(nativeComponent.usages).name == one(nativeComponent.artifacts).classifier
        one(nativeComponent.usages).dependencies == project.configurations.compileMyExe.allDependencies
    }

    void 'creates usage from a Library'() {
        when:
        project.dependencies.compileMyLib('group:name:version')
        nativeComponent.from(aLibrary)
        project.evaluate()

        then:
        nativeComponent.usages.size() == nativeComponent.artifacts.size()
        nativeComponent.usages.each { Usage usage ->
            assert nativeComponent.artifacts.contains(one(usage.artifacts))
            assert nativeComponent.artifacts.findAll{ it.classifier == usage.name }.size() == 1
            assert usage.dependencies == project.configurations.compileMyLib.allDependencies
        }
    }

    void 'creates usages from multiple tasks and native binaries'() {
        when:
        nativeComponent.from(aZipTask)
        nativeComponent.from(anExecutable)
        nativeComponent.from(aLibrary)
        nativeComponent.from(anotherZipTask)
        project.evaluate()

        then:
        nativeComponent.usages.size() == 5
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

    private NativeComponent aNativeComponent() {
        return project.nativeArtifacts.create('myComponent')
    }

    private Zip aZipTask() {
        Zip zipTask = project.tasks.create('aZipTask', Zip)
        zipTask.classifier = 'zipClassifier'
        return zipTask
    }

    private Task aGenericTask() {
        return project.tasks.create('task')
    }

    private Zip anotherZipTask() {
        return project.tasks.create('anotherZipTask', Zip)
    }

    private Executable anExecutable() {
        return project.executables.create('myExe')
    }

    private Library aLibrary() {
        return project.libraries.create('myLib')
    }

}