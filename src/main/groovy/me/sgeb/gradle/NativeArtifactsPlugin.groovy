package me.sgeb.gradle

import me.sgeb.gradle.nativeartifacts.NativeComponentContainer
import me.sgeb.gradle.nativeartifacts.internal.BuildNarTaskCreator
import me.sgeb.gradle.nativeartifacts.internal.ConfigurationCreator
import me.sgeb.gradle.nativeartifacts.internal.ExtractNarDepsTaskCreator
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.reflect.Instantiator
import org.gradle.model.ModelRules
import org.gradle.nativebinaries.ExecutableContainer
import org.gradle.nativebinaries.LibraryContainer
import org.gradle.nativebinaries.ProjectNativeBinary
import org.gradle.nativebinaries.ProjectNativeComponent
import org.gradle.nativebinaries.plugins.NativeBinariesPlugin

import javax.inject.Inject

class NativeArtifactsPlugin implements Plugin<Project> {

    static final String EXTENSION_NAME = "nativeArtifacts"

    private final Instantiator instantiator
    private final ModelRules modelRules
    private NativeComponentContainer nativeComponents
    private Project project

    @Inject
    NativeArtifactsPlugin(Instantiator instantiator, ModelRules modelRules) {
        this.instantiator = instantiator
        this.modelRules = modelRules
    }

    @Override
    void apply(Project project) {
        this.project = project
        project.plugins.apply(NativeBinariesPlugin)

        nativeComponents = project.extensions.create(
                EXTENSION_NAME,
                NativeComponentContainer,
                instantiator)

        // Configurations must be created through collection listeners on the
        // binary containers and _not_ a ModelRule or project.afterEvaluate so
        // that the created configurations are available during the build
        // script configuration
        def confCreator = new ConfigurationCreator(project.configurations,
                (project as ProjectInternal).modelRegistry,
                project.logger
        )
        whenExecutableOrLibraryAdded { confCreator.createFor(it) }

        setNarConfName()
        setNarDepsDir()
        modelRules.rule(new BuildNarTaskCreator(nativeComponents, project.tasks, project.configurations))
        modelRules.rule(new ExtractNarDepsTaskCreator(project))
    }

    private void whenExecutableOrLibraryAdded(Closure closure) {
        project.extensions.configure(ExecutableContainer,
                new ClosureBackedAction<ExecutableContainer>({ it.whenObjectAdded(closure) }))
        project.extensions.configure(LibraryContainer,
                new ClosureBackedAction<LibraryContainer>({ it.whenObjectAdded(closure) }))
    }

    private void setNarConfName() {
        def narConfNameClosure = { ProjectNativeComponent component ->
            component.binaries.all { ProjectNativeBinary binary ->
                binary.ext.narConfName = ConfigurationCreator.getConfigurationName(binary)
            }
        }

        // Cannot use project.binaries, see setNarDepsDir
        project.executables.all(narConfNameClosure)
        project.libraries.all(narConfNameClosure)
    }

    private void setNarDepsDir() {
        def narDepsDirClosure = { ProjectNativeComponent component ->
            component.binaries.all { ProjectNativeBinary binary ->
                def depsDirName = ExtractNarDepsTaskCreator.getNarDepsDirName(binary)
                binary.ext.narDepsDir = project.file("$project.buildDir/${depsDirName}")
            }
        }

        // Need to add narDepsDir through project.{executables,libraries} instead
        // of project.binaries otherwise it would only be accessible inside
        // the binaries block (and not inside executables and libraries blocks)
        project.executables.all(narDepsDirClosure)
        project.libraries.all(narDepsDirClosure)
    }
}