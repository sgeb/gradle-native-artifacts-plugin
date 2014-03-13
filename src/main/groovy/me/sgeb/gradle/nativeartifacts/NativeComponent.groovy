package me.sgeb.gradle.nativeartifacts

import me.sgeb.gradle.nativeartifacts.internal.Nar
import org.gradle.api.DomainObjectSet
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Named
import org.gradle.api.Task
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.artifacts.PublishArtifactSet
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.api.internal.artifacts.DefaultPublishArtifactSet
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.Usage
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.nativebinaries.ProjectNativeComponent
import org.gradle.nativebinaries.internal.ProjectNativeBinaryInternal

class NativeComponent implements SoftwareComponentInternal, Named {

    private final String name
    private final PublishArtifactSet artifacts
    private final DomainObjectSet<NativeComponentHint> nativeComponentHints

    public NativeComponent(String name) {
        this.name = name
        artifacts = new DefaultPublishArtifactSet(name, new DefaultDomainObjectSet<>(PublishArtifact))
        nativeComponentHints = new DefaultDomainObjectSet<>(NativeComponentHint)
    }

    public PublishArtifact from(Task task) {
        if (!(task instanceof AbstractArchiveTask)) {
            throw new InvalidUserDataException(
                    "Can only add tasks of type 'AbstractArchiveTask' (e.g. Zip)")
        }

        PublishArtifact publishArtifact =
                new ArchivePublishArtifact(task as AbstractArchiveTask)
        artifacts.add(publishArtifact)
        return publishArtifact
    }

    public void from(ProjectNativeComponent nativeComponent,
                     Closure filter = Closure.IDENTITY) {
        nativeComponentHints.add(new NativeComponentHint(nativeComponent, filter))
    }

    public Set<ProjectNativeBinaryInternal> getBinaries() {
        Set<ProjectNativeBinaryInternal> result = new LinkedHashSet<>()
        nativeComponentHints.all { NativeComponentHint hint ->
            hint.component.binaries.matching(hint.filter).all { ProjectNativeBinaryInternal binary ->
                result.add(binary)
            }
        }
        return result
    }

    @Override
    String getName() {
        return name
    }

    PublishArtifactSet getArtifacts() {
        return artifacts
    }

    @Override
    Set<Usage> getUsages() {
        def result = new LinkedHashSet<Usage>()
        artifacts.all { PublishArtifact artifact ->
            result.add(new NativeUsage(artifact))
        }
        return result
    }

    ///////////////////////////////////////////////////////////////////////////

    private static class NativeComponentHint {
        final ProjectNativeComponent component
        final Closure filter

        NativeComponentHint(ProjectNativeComponent component, Closure filter) {
            this.component = component
            this.filter = filter
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    private static class NativeUsage implements Usage {

        private final PublishArtifact artifact

        NativeUsage(PublishArtifact artifact) {
            this.artifact = artifact
        }

        @Override
        Set<PublishArtifact> getArtifacts() {
            return Collections.singleton(artifact)
        }

        @Override
        Set<ModuleDependency> getDependencies() {
            if (artifact instanceof ArchivePublishArtifact) {
                def archiveArtifact =  artifact as ArchivePublishArtifact
                if (archiveArtifact.archiveTask instanceof Nar) {
                    def nar = archiveArtifact.archiveTask as Nar
                    return nar.conf.allDependencies.withType(ModuleDependency)
                }
            }

            return Collections.emptySet()
        }

        @Override
        String getName() {
            return artifact.classifier
        }
    }
}
