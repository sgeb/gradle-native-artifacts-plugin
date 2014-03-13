package me.sgeb.gradle.nativeartifacts.internal

import org.gradle.api.Named
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.logging.Logger
import org.gradle.language.base.internal.DefaultBinaryNamingScheme
import org.gradle.model.internal.ModelRegistry
import org.gradle.nativebinaries.*
import org.gradle.nativebinaries.platform.Platform
import org.gradle.nativebinaries.platform.PlatformContainer

class ConfigurationCreator {

    public static final String NAR_COMPILE_CONFIGURATION_PREFIX = 'compile'

    private final ConfigurationContainer configurations
    private final ModelRegistry modelRegistry
    private final Logger logger

    public ConfigurationCreator(ConfigurationContainer configurations,
                                ModelRegistry modelRegistry,
                                Logger logger)
    {
        this.configurations = configurations
        this.modelRegistry = modelRegistry
        this.logger = logger
    }

    void createFor(component) {
        allPlatforms.each { platform ->
            allBuildTypes.each { buildType ->
                allFlavors.each { flavor ->
                    configureConfigurationHierarchy(component, platform, buildType, flavor)
                }
            }
        }
    }

    static String getConfigurationName(ProjectNativeBinary binary) {
        getConfigurationNameVar(binary.component, binary.targetPlatform,
                binary.buildType, binary.flavor)
    }

    private void configureConfigurationHierarchy(Named... objects)
    {
        for (int i = objects.size()-1; i >= 0; i--) {
            def parentParams = (i != 0) ? objects[0..i-1] : []
            def confParentName = getConfigurationNameVar(parentParams as Named[])
            def confName = getConfigurationNameVar(objects[0..i] as Named[])

            if (confName != confParentName) {
                def confParent = configurations.maybeCreate(confParentName)
                def conf = configurations.maybeCreate(confName)
                conf.extendsFrom confParent
                logger.info("> Configuration: $confName -> $confParentName")
            }
        }
    }

    private static String getConfigurationNameVar(Named... objects) {
        def params = new ArrayList()
        params.add NAR_COMPILE_CONFIGURATION_PREFIX
        params.addAll(objects.grep { !(it.name in ["default", "current"]) }*.name)

        new DefaultBinaryNamingScheme(null, null, new LinkedList<String>())
                .makeName params.toArray(new String[objects.size()]) as String[]
    }

    // Evaluated at creation time, hence when platforms/buildTypes/flavors have
    // been set up. Evaluating earlier would finalize the model

    private Set<Platform> getAllPlatforms() {
        modelRegistry.get("platforms", PlatformContainer)
    }

    private Set<BuildType> getAllBuildTypes() {
        modelRegistry.get("buildTypes", BuildTypeContainer)
    }

    private Set<Flavor> getAllFlavors() {
        modelRegistry.get("flavors", FlavorContainer)
    }

}
