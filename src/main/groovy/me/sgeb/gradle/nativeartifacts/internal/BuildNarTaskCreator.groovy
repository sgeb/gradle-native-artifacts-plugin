package me.sgeb.gradle.nativeartifacts.internal

import me.sgeb.gradle.nativeartifacts.NativeComponent
import me.sgeb.gradle.nativeartifacts.NativeComponentContainer
import org.gradle.api.Task
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.language.base.BinaryContainer
import org.gradle.language.base.internal.BinaryInternal
import org.gradle.model.ModelRule
import org.gradle.nativebinaries.*
import org.gradle.nativebinaries.internal.ProjectNativeBinaryInternal

class BuildNarTaskCreator extends ModelRule {

    public static final String NAR_LIFECYCLE_TASK_NAME = 'buildNar'
    public static final String NAR_BUILD_TASK_PREFIX = 'buildNar'
    public static final String NAR_GROUP = 'Native Artifacts'

    private NativeComponentContainer nativeComponents
    private final TaskContainer tasks
    private final ConfigurationContainer configurations

    private final Task narLifecycleTask

    public BuildNarTaskCreator(NativeComponentContainer nativeComponents,
                               TaskContainer tasks,
                               ConfigurationContainer configurations)
    {
        this.nativeComponents = nativeComponents
        this.tasks = tasks
        this.configurations = configurations

        narLifecycleTask = tasks.maybeCreate(NAR_LIFECYCLE_TASK_NAME).configure {
            description = 'Builds all native artifacts on all buildable platforms.'
            group = NAR_GROUP
        }
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    // The unnamed parameter is required for ModelRule
    public void createBuildNarTasks(BinaryContainer _)
    {
        nativeComponents.all { NativeComponent nativeComponent ->
            nativeComponent.binaries.each { ProjectNativeBinaryInternal binary ->
                def task = createBuildNarTask(binary)
                if (binary.buildable) {
                    nativeComponent.from(task)
                }
            }
        }
    }

    private AbstractArchiveTask createBuildNarTask(ProjectNativeBinaryInternal binary) {
        def narTaskName = getBuildNarTaskName(binary)
        Nar narTask = tasks.maybeCreate(narTaskName, Nar)
        narTask.configure {
            description = "Builds native artifact for $binary.namingScheme.description."
            group = NAR_GROUP
            dependsOn binary.namingScheme.lifecycleTaskName
            conf configurations[ConfigurationCreator.getConfigurationName(binary)]

            from binary.primaryOutput
            into intoZipDirectory(binary)
            baseName = binary.component.name
            classifier = classifierForBinary(binary)
        }

        if (binary.buildable) {
            narLifecycleTask.dependsOn narTask
        }

        return narTask
    }

    private static String getBuildNarTaskName(BinaryInternal binary) {
        return binary.namingScheme.getTaskName(NAR_BUILD_TASK_PREFIX)
    }

    private static String intoZipDirectory(NativeBinary binary) {
        if (binary instanceof ExecutableBinary) {
            return 'bin'
        }
        if (binary instanceof LibraryBinary) {
            return 'lib'
        }
        return ''
    }

    private static String classifierForBinary(NativeBinary binary) {
        return stringIfNotDefault(binary.targetPlatform.name, '') +
                libraryType(binary, '-') +
                stringIfNotDefault(binary.buildType.name) +
                stringIfNotDefault(binary.flavor.name)
    }

    private static String libraryType(NativeBinary binary, String prefix = '-') {
        if (binary instanceof StaticLibraryBinary) {
            return prefix + 'static'
        }
        if (binary instanceof SharedLibraryBinary) {
            return prefix + 'shared'
        }
        return ''
    }

    private static String stringIfNotDefault(String str, String prefix = '-') {
        str != 'default' ? prefix + str : ''
    }

}
