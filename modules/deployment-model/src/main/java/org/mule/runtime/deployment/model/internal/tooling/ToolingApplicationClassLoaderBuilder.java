/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal.tooling;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.internal.AbstractArtifactClassLoaderBuilder;
import org.mule.runtime.deployment.model.internal.RegionPluginClassLoadersFactory;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.io.IOException;
import java.net.URL;

/**
 * {@link ArtifactClassLoader} builder for class loaders required by {@link Application} artifacts for Tooling.
 * The main different between this kind of applications vs the ones deployed through the deployment service is that
 * Tooling has to access the {@link ArtifactClassLoader} and it should be simple for Tooling to dispose resources, therefore
 * a {@link ToolingArtifactClassLoader} is created by this builder.
 *
 * @since 4.0
 */
public class ToolingApplicationClassLoaderBuilder
    extends AbstractArtifactClassLoaderBuilder<ToolingApplicationClassLoaderBuilder> {

  private final DeployableArtifactClassLoaderFactory artifactClassLoaderFactory;
  private ArtifactClassLoader parentClassLoader;

  /**
   * Creates a new builder for creating {@link Application} artifacts.
   * <p>
   * The {@code domainRepository} is used to locate the domain that this application belongs to and the
   * {@code artifactClassLoaderBuilder} is used for building the common parts of artifacts.
   *
   * @param artifactClassLoaderFactory factory for the classloader specific to the artifact resource and classes
   * @param pluginClassLoadersFactory creates the class loaders for the plugins included in the application's region. Non null
   */
  public ToolingApplicationClassLoaderBuilder(DeployableArtifactClassLoaderFactory<ApplicationDescriptor> artifactClassLoaderFactory,
                                              RegionPluginClassLoadersFactory pluginClassLoadersFactory) {
    super(pluginClassLoadersFactory);

    this.artifactClassLoaderFactory = artifactClassLoaderFactory;
  }

  /**
   * Creates a new {@code ArtifactClassLoader} using the provided configuration. It will create the proper class loader hierarchy
   * and filters so application classes, resources, plugins and it's domain resources are resolve correctly.
   *
   * @return a {@code MuleDeployableArtifactClassLoader} created from the provided configuration.
   * @throws IOException exception cause when it was not possible to access the file provided as dependencies
   */
  public ToolingArtifactClassLoader build() throws IOException {
    ArtifactClassLoader ownerArtifactClassLoader = super.build();
    ClassLoader parent = ownerArtifactClassLoader.getClassLoader().getParent();
    if (!(parent instanceof RegionClassLoader)) {
      throw new DeploymentException(createStaticMessage(format("The parent of the current owner must be of type '%s' but found '%s'",
                                                               RegionClassLoader.class.getName(), parent.getClass().getName())));
    }
    final RegionClassLoader regionClassLoader = (RegionClassLoader) parent;
    return new ToolingArtifactClassLoader(regionClassLoader, ownerArtifactClassLoader);
  }

  @Override
  protected ArtifactClassLoader createArtifactClassLoader(String artifactId, RegionClassLoader regionClassLoader) {
    return artifactClassLoaderFactory.create(artifactId, regionClassLoader, artifactDescriptor, artifactPluginClassLoaders);
  }

  @Override
  protected String getArtifactId(ArtifactDescriptor artifactDescriptor) {
    return getApplicationId(artifactDescriptor.getName());
  }

  /**
   * @param parentClassLoader parent class loader for the artifact class loader that should have all the {@link URL}s needed from
   *        tooling side when loading the {@link ExtensionModel}. Among those, there will be mule-api, extensions-api,
   *        extensions-support and so on.
   * @return the builder
   */
  public ToolingApplicationClassLoaderBuilder setParentClassLoader(ArtifactClassLoader parentClassLoader) {
    this.parentClassLoader = parentClassLoader;
    return this;
  }

  @Override
  protected ArtifactClassLoader getParentClassLoader() {
    return parentClassLoader;
  }

  /**
   * @param applicationName name of the application. Non empty.
   * @return the unique identifier for the application in the container.
   */
  public static String getApplicationId(String applicationName) {
    checkArgument(!isEmpty(applicationName), "applicationName cannot be empty");

    return "tooling-application/" + applicationName;
  }

}
