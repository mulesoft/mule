/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.artifact;

import static java.lang.String.format;
import static org.mule.maven.client.api.MavenClientProvider.discoverProvider;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.MAVEN;
import static org.mule.runtime.globalconfig.api.GlobalConfigLoader.getMavenConfig;
import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.MavenClientProvider;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.runtime.core.config.bootstrap.ArtifactType;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModelLoader;
import org.mule.runtime.module.artifact.descriptor.InvalidDescriptorLoaderException;
import org.mule.runtime.module.deployment.impl.internal.application.DeployableMavenClassLoaderModelLoader;
import org.mule.runtime.module.deployment.impl.internal.plugin.PluginMavenClassLoaderModelLoader;

import java.io.File;
import java.util.Map;

/**
 * This class is responsible of returning the {@link BundleDescriptor} of a given plugin's location and also creating a
 * {@link ClassLoaderModel}
 * <p>
 * TODO(fernandezlautaro): MULE-11094 this class is the default implementation for discovering dependencies and URLs, which
 * happens to be Maven based. There could be other ways to look for dependencies and URLs (probably for testing purposes where the
 * plugins are done by hand and without maven) which will imply implementing the jira pointed out in this comment.
 *
 * @since 4.0
 */
// TODO MULE-11878 - consolidate with other aether usages in mule.
public class MavenClassLoaderModelLoader implements ClassLoaderModelLoader {

  private final DeployableMavenClassLoaderModelLoader deployableMavenClassLoaderModelLoader;
  private final PluginMavenClassLoaderModelLoader pluginMavenClassLoaderModelLoader;

  public MavenClassLoaderModelLoader() {
    MavenConfiguration mavenRuntimeConfig = getMavenConfig();
    MavenClientProvider mavenClientProvider = discoverProvider(MavenClientProvider.class.getClassLoader());
    MavenClient mavenClient = mavenClientProvider.createMavenClient(mavenRuntimeConfig);

    deployableMavenClassLoaderModelLoader =
        new DeployableMavenClassLoaderModelLoader(mavenClient, mavenClientProvider.getLocalRepositorySuppliers());
    pluginMavenClassLoaderModelLoader =
        new PluginMavenClassLoaderModelLoader(mavenClient, mavenClientProvider.getLocalRepositorySuppliers());
  }

  @Override
  public String getId() {
    return MAVEN;
  }

  @Override
  public ClassLoaderModel load(File artifactFile, Map<String, Object> attributes, ArtifactType artifactType)
      throws InvalidDescriptorLoaderException {
    if (deployableMavenClassLoaderModelLoader.supportsArtifactType(artifactType)) {
      return deployableMavenClassLoaderModelLoader.load(artifactFile, attributes, artifactType);
    }
    if (pluginMavenClassLoaderModelLoader.supportsArtifactType(artifactType)) {
      return pluginMavenClassLoaderModelLoader.load(artifactFile, attributes, artifactType);
    }
    throw new IllegalStateException(format("Artifact type %s not supported", artifactType));
  }

  @Override
  public boolean supportsArtifactType(ArtifactType artifactType) {
    return deployableMavenClassLoaderModelLoader.supportsArtifactType(artifactType)
        || pluginMavenClassLoaderModelLoader.supportsArtifactType(artifactType);
  }

}
