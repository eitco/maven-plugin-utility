/*
 * Copyright (c) 2022 EITCO GmbH
 * All rights reserved.
 *
 * Created on 04.03.2022
 *
 */
package de.eitco.cicd.maven.plugin.utility;


import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.*;
import org.apache.maven.shared.transfer.artifact.DefaultArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResult;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.graph.Dependency;

import java.io.File;
import java.io.IOException;
import java.util.List;

public abstract class DependencyMetaDataReader<MetaDataType> {

    private final MavenSession session;
    private final List<ArtifactRepository> remoteRepositories;
    private final ArtifactResolver artifactResolver;
    private final MavenProject project;
    private final RepositorySystemSession repoSession;
    private final ProjectDependenciesResolver projectDependenciesResolver;
    private final String metadataArtifactExtension;
    private final String artifactExtension;

    protected DependencyMetaDataReader(
        MavenSession session,
        List<ArtifactRepository> remoteRepositories,
        ArtifactResolver artifactResolver,
        MavenProject project,
        RepositorySystemSession repoSession,
        ProjectDependenciesResolver projectDependenciesResolver,
        String metadataArtifactExtension,
        String artifactExtension
    ) {
        this.session = session;
        this.remoteRepositories = remoteRepositories;
        this.artifactResolver = artifactResolver;
        this.project = project;
        this.repoSession = repoSession;
        this.projectDependenciesResolver = projectDependenciesResolver;
        this.metadataArtifactExtension = metadataArtifactExtension;
        this.artifactExtension = artifactExtension;
    }


    protected abstract MetaDataType readFrom(File metadataFile) throws IOException;

    public MetaDataType get(Dependency dependency) throws ArtifactResolverException, IOException {

        ProjectBuildingRequest buildingRequest =
            new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());

        buildingRequest.setRemoteRepositories(remoteRepositories);

        DefaultArtifactCoordinate coordinate = new DefaultArtifactCoordinate();
        coordinate.setGroupId(dependency.getArtifact().getGroupId());
        coordinate.setArtifactId(dependency.getArtifact().getArtifactId());
        coordinate.setVersion(dependency.getArtifact().getVersion());
        coordinate.setClassifier(dependency.getArtifact().getClassifier());
        coordinate.setExtension(metadataArtifactExtension);

        ArtifactResult artifactResult = artifactResolver.resolveArtifact(buildingRequest, coordinate);

        return readFrom(artifactResult.getArtifact().getFile());
    }

    @FunctionalInterface
    public interface MetadataConsumer<MetaDataType> {

        void accept(Dependency dependency, MetaDataType metaDataType) throws IOException;
    }

    public void forEachDependency(MetadataConsumer<MetaDataType> consumer) throws DependencyResolutionException, ArtifactResolverException, IOException {

        DefaultDependencyResolutionRequest request = new DefaultDependencyResolutionRequest();

        request.setMavenProject(project);

        request.setRepositorySession(repoSession);

        request.setResolutionFilter((dependencyNode, list) -> {

            Dependency dependency = dependencyNode.getDependency();

            if (dependency == null) {

                return false;
            }

            org.eclipse.aether.artifact.Artifact artifact = dependencyNode.getArtifact();

            String extension = artifact.getExtension();

            return extension.equals(artifactExtension);
        });

        DependencyResolutionResult result = projectDependenciesResolver.resolve(request);

        List<Dependency> dependencies = result.getResolvedDependencies();

        for (Dependency dependency : dependencies) {

            consumer.accept(dependency, get(dependency));
        }
    }

}
