/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.temporary;

import static com.google.common.collect.Lists.newArrayList;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.internal.AbstractArtifactClassLoaderBuilder;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel.ClassLoaderModelBuilder;
import org.mule.runtime.module.artifact.util.FileJarExplorer;
import org.mule.runtime.module.artifact.util.JarExplorer;
import org.mule.runtime.module.artifact.util.JarInfo;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link ArtifactClassLoader} builder for class loaders used by mule artifacts such as domains or applications.
 *
 * Allows to construct a classloader when using a set of artifact plugins and takes into account default plugins provided by the
 * runtime and the shared libraries configured for the plugins.
 *
 * @since 4.0
 */
public class TemporaryArtifactClassLoaderBuilder extends AbstractArtifactClassLoaderBuilder<TemporaryArtifactClassLoaderBuilder> {

  private ArtifactClassLoader parentClassLoader;
  private List<URL> urls = newArrayList();
  private DeployableArtifactClassLoaderFactory artifactClassLoaderFactory;

  /**
   * Creates an {@link TemporaryArtifactClassLoaderBuilder}.
   * 
   * @param artifactPluginClassLoaderFactory factory for creating class loaders for artifact plugins. Must be not null.
   * @param artifactClassLoaderFactory creates artifact class loaders from descriptors
   */
  public TemporaryArtifactClassLoaderBuilder(ArtifactClassLoaderFactory<ArtifactPluginDescriptor> artifactPluginClassLoaderFactory,
                                             DeployableArtifactClassLoaderFactory<ApplicationDescriptor> artifactClassLoaderFactory) {
    super(artifactPluginClassLoaderFactory);

    this.artifactClassLoaderFactory = artifactClassLoaderFactory;
  }

  @Override
  protected ArtifactClassLoader createArtifactClassLoader(String artifactId, RegionClassLoader regionClassLoader) {
    return artifactClassLoaderFactory.create(artifactId, regionClassLoader, artifactDescriptor, artifactPluginClassLoaders);
  }

  /**
   * @param parentClassLoader parent class loader for the artifact class loader.
   * @return the builder
   */
  public TemporaryArtifactClassLoaderBuilder setParentClassLoader(ArtifactClassLoader parentClassLoader) {
    this.parentClassLoader = parentClassLoader;
    return this;
  }

  /**
   * Adds a {@link URL} to the {@link ArtifactDescriptor}.
   *
   * @param url {@link URL} to be included as part of the {@link ArtifactDescriptor} definition.
   * @return the builder
   */
  public TemporaryArtifactClassLoaderBuilder addUrl(URL url) {
    this.urls.add(url);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MuleDeployableArtifactClassLoader build() throws IOException {
    final ApplicationDescriptor artifactDescriptor = new ApplicationDescriptor("temp");
    if (!urls.isEmpty()) {
      ClassLoaderModelBuilder classLoaderModelBuilder = new ClassLoaderModelBuilder();

      JarInfo jarInfo = createJarInfo(this.urls);
      classLoaderModelBuilder.exportingPackages(jarInfo.getPackages())
          .exportingResources(jarInfo.getResources());

      this.urls.stream().forEach(url -> classLoaderModelBuilder.containing(url));

      artifactDescriptor.setClassLoaderModel(classLoaderModelBuilder.build());
    }
    setArtifactDescriptor(artifactDescriptor);
    return (MuleDeployableArtifactClassLoader) super.build();
  }

  private JarInfo createJarInfo(List<URL> urls) {
    Set<String> packages = new HashSet<>();
    Set<String> resources = new HashSet<>();
    final JarExplorer jarExplorer = new FileJarExplorer();

    for (URL library : urls) {
      final JarInfo jarInfo = jarExplorer.explore(library);
      packages.addAll(jarInfo.getPackages());
      resources.addAll(jarInfo.getResources());
    }

    return new JarInfo(packages, resources);
  }

  @Override
  protected String getArtifactId(ArtifactDescriptor artifactDescriptor) {
    return "temp/" + artifactDescriptor.getName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ArtifactClassLoader getParentClassLoader() {
    return parentClassLoader;
  }
}
