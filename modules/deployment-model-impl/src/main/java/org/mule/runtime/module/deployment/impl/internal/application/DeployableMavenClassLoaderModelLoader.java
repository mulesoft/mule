/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.INCLUDE_TEST_DEPENDENCIES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.getPomModelFolder;
import org.mule.maven.client.api.MavenClient;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel.ClassLoaderModelBuilder;
import org.mule.runtime.module.deployment.impl.internal.LightweightClassLoaderModelBuilder;
import org.mule.runtime.module.deployment.impl.internal.maven.AbstractMavenClassLoaderModelLoader;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible of returning the {@link BundleDescriptor} of a given plugin's location and also creating a
 * {@link ClassLoaderModel}
 *
 * @since 4.0
 */
public class DeployableMavenClassLoaderModelLoader extends AbstractMavenClassLoaderModelLoader {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  public DeployableMavenClassLoaderModelLoader(MavenClient mavenClient) {
    super(mavenClient);
  }

  @Override
  public String getId() {
    return MULE_LOADER_ID;
  }

  @Override
  protected void addArtifactSpecificClassloaderConfiguration(File artifactFile, ClassLoaderModelBuilder classLoaderModelBuilder,
                                                             Set<BundleDependency> dependencies) {
    try {
      ((LightweightClassLoaderModelBuilder) classLoaderModelBuilder).sharingLibraries();
      classLoaderModelBuilder.containing(artifactFile.toURI().toURL());
    } catch (MalformedURLException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  protected boolean includeTestDependencies(Map<String, Object> attributes) {
    return Boolean.valueOf((String) attributes.getOrDefault(INCLUDE_TEST_DEPENDENCIES, "false"));
  }

  @Override
  protected Model loadPomModel(File artifactFile) {
    return getPomModelFolder(artifactFile);
  }

  @Override
  protected boolean includeProvidedDependencies(ArtifactType artifactType) {
    return artifactType.equals(APP);
  }

  @Override
  public boolean supportsArtifactType(ArtifactType artifactType) {
    return artifactType.equals(APP) || artifactType.equals(DOMAIN) || artifactType.equals(POLICY);
  }
}
