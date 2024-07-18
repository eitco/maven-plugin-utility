/*
 * Copyright (c) 2022 EITCO GmbH
 * All rights reserved.
 *
 * Created on 04.03.2022
 *
 */
package de.eitco.cicd.maven.plugin.utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.eclipse.aether.RepositorySystemSession;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class JsonDependencyMetaDataReader<MetaDataType> extends DependencyMetaDataReader<MetaDataType> {

    private final Class<MetaDataType> clazz;
    private final ObjectMapper objectMapper;

    protected JsonDependencyMetaDataReader(
        MavenSession session,
        List<ArtifactRepository> remoteRepositories,
        ArtifactResolver artifactResolver,
        MavenProject project,
        RepositorySystemSession repoSession,
        ProjectDependenciesResolver projectDependenciesResolver,
        String metadataArtifactExtension,
        String artifactExtension,
        Class<MetaDataType> clazz,
        ObjectMapper objectMapper
    ) {
        super(
            session,
            remoteRepositories,
            artifactResolver,
            project,
            repoSession,
            projectDependenciesResolver,
            metadataArtifactExtension,
            artifactExtension
        );
        this.clazz = clazz;
        this.objectMapper = objectMapper;
    }


    @Override
    protected MetaDataType readFrom(File metadataFile) throws IOException {

        return objectMapper.readValue(metadataFile, clazz);
    }
}
