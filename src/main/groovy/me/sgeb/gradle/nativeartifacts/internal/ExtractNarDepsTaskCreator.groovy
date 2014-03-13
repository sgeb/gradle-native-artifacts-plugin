package me.sgeb.gradle.nativeartifacts.internal

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.language.base.BinaryContainer
import org.gradle.language.base.internal.LanguageSourceSetInternal
import org.gradle.model.ModelRule
import org.gradle.nativebinaries.ProjectNativeBinary
import org.gradle.nativebinaries.internal.ProjectNativeBinaryInternal

class ExtractNarDepsTaskCreator extends ModelRule {

    public static final String NAR_EXTRACT_DEPS_TASK_PREFIX = 'extractNarDeps'
    public static final String NAR_EXTRACT_PATH = "nar-dependencies"

    private final Project project

    public ExtractNarDepsTaskCreator(Project project) {
        this.project = project
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    public void createExtractNarDepsTasks(BinaryContainer binaries) {
        binaries.all { ProjectNativeBinaryInternal binary ->
            Task extractTask = createExtractNarDepsTask(binary)

            Task compileTask = getCompileTask(binary)
            if (compileTask) {
                compileTask.dependsOn(extractTask)
            }
        }
    }

    private Task createExtractNarDepsTask(ProjectNativeBinaryInternal binary) {
        def depsConfName = ConfigurationCreator.getConfigurationName(binary)
        def depsConf = project.configurations[depsConfName]

        String extractDepsTaskName = getExtractNarDepsTaskName(binary)
        def extractDepsTask = project.task(extractDepsTaskName) { Task it ->
            group = BuildNarTaskCreator.NAR_GROUP
            description = "Extracts native artifact dependencies for " +
                    "$binary.namingScheme.description."
            it.dependsOn depsConf

            inputs.files depsConf
            outputs.files { binary.narDepsDir.listFiles() }
            doLast {
                depsConf.grep {
                    it.name.endsWith('.nar') || it.name.endsWith('.zip')
                }.each { narFile ->
                    project.copy {
                        from project.zipTree(narFile)
                        into binary.narDepsDir
                    }
                }
            }
        }

        return extractDepsTask
    }

    static String getNarDepsDirName(ProjectNativeBinary binary) {
        def depsConfName = ConfigurationCreator.getConfigurationName(binary)
        return "$NAR_EXTRACT_PATH/$depsConfName"
    }

    private static String getExtractNarDepsTaskName(ProjectNativeBinaryInternal binary) {
        return binary.namingScheme.getTaskName(NAR_EXTRACT_DEPS_TASK_PREFIX)
    }

    private Task getCompileTask(ProjectNativeBinaryInternal binary) {
        Task result = null
        if (binary.source.size() > 0) {
            String taskName = binary.namingScheme.getTaskName('compile',
                    (binary.source.iterator().next() as LanguageSourceSetInternal).fullName)
            result = project.tasks.findByName(taskName)
        }
        return result
    }

}
