package me.sgeb.gradle.nativeartifacts

import me.sgeb.gradle.NativeArtifactsPlugin
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class NativePublishArtifactsPluginTest extends Specification {

    def project

    void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: NativeArtifactsPlugin
    }

    void 'plugin applies itself'() {
        expect:
        project.plugins.hasPlugin('native-artifacts')
    }

    void 'plugin applies native-binaries plugin'() {
        expect:
        project.plugins.hasPlugin('native-binaries')
    }

    void 'plugin adds nativeArtifacts extension'() {
        expect:
        project.extensions.getByName("nativeArtifacts") instanceof NativeComponentContainer
        project.nativeArtifacts instanceof NativeComponentContainer
    }
}
