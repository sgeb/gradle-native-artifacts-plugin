package me.sgeb.gradle.nativeartifacts.internal

import me.sgeb.gradle.NativeArtifactsPlugin
import me.sgeb.gradle.nativeartifacts.NativeComponent
import me.sgeb.gradle.nativeartifacts.NativeComponentContainer
import org.gradle.nativebinaries.Executable
import org.gradle.nativebinaries.Library
import org.gradle.nativebinaries.toolchain.internal.ToolChainInternal
import org.gradle.nativebinaries.toolchain.internal.ToolSearchResult
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import static me.sgeb.gradle.nativeartifacts.internal.BuildNarTaskCreator.NAR_GROUP

class ExtractNarDepsTaskCreatorTest extends Specification {
    def project
    def nativeComponents
    def nativeComponent

    private Executable anExecutable
    private Library aLibrary

    void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'cpp'
        project.apply plugin: NativeArtifactsPlugin
        project.model { toolChains { add toolChainAny('any') } }
        project.sources { myExe { cpp { builtBy(new Object()) } } }
        project.sources { myLib { cpp { builtBy(new Object()) } } }

        nativeComponents = project.extensions.getByType(NativeComponentContainer)
        nativeComponent = aNativeComponent()

        anExecutable = anExecutable()
        aLibrary = aLibrary()
    }

    void 'configures native artifact dependencies extracting task for an Executable'() {
        given:
        nativeComponent.from(anExecutable)

        when:
        project.evaluate()

        then:
        with(project.tasks.findByName('extractNarDepsMyExeExecutable')) {
            it != null
            it.group == NAR_GROUP
            it.dependsOn.contains(project.configurations.findByName('compileMyExeDebug'))
            project.tasks.findByName('compileMyExeExecutableMyExeCpp').dependsOn.contains(it)
        }
    }

    void 'configures native artifact dependencies extracting task for a Library'() {
        given:
        nativeComponent.from(aLibrary)

        when:
        project.evaluate()

        then:
        with(project.tasks.findByName('extractNarDepsMyLibSharedLibrary')) {
            it != null
            it.group == NAR_GROUP
            it.dependsOn.contains(project.configurations.findByName('compileMyLibDebug'))
            project.tasks.findByName('compileMyLibSharedLibraryMyLibCpp').dependsOn.contains(it)
        }
        with(project.tasks.findByName('extractNarDepsMyLibStaticLibrary')) {
            it != null
            it.group == NAR_GROUP
            it.dependsOn.contains(project.configurations.findByName('compileMyLibDebug'))
            project.tasks.findByName('compileMyLibStaticLibraryMyLibCpp').dependsOn.contains(it)
        }
    }

    void 'sets narDepsDir on binaries'() {
        given:
        nativeComponent.from(anExecutable)
        nativeComponent.from(aLibrary)

        when:
        project.evaluate()

        then:
        project.binaries.all {
            it.narDepsDir =~ "$project.buildDir/nar-dependencies/.+"
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

    private Executable anExecutable() {
        return project.executables.create('myExe')
    }

    private Library aLibrary() {
        return project.libraries.create('myLib')
    }

}
