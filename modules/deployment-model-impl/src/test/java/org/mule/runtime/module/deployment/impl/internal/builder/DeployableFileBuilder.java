/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.deployment.model.api.application.ApplicationDescriptor.REPOSITORY_FOLDER;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.MULE_DOMAIN_CLASSIFIER;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.META_INF;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT;
import static org.mule.runtime.module.deployment.impl.internal.maven.AbstractMavenClassLoaderModelLoader.CLASSLOADER_MODEL_JSON_DESCRIPTOR;
import static org.mule.runtime.module.deployment.impl.internal.maven.AbstractMavenClassLoaderModelLoader.CLASSLOADER_MODEL_JSON_DESCRIPTOR_LOCATION;
import static org.mule.tools.api.classloader.ClassLoaderModelJsonSerializer.serializeToFile;

import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.module.artifact.builder.AbstractArtifactFileBuilder;
import org.mule.runtime.module.artifact.builder.AbstractDependencyFileBuilder;
import org.mule.tck.ZipUtils;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;
import org.mule.tools.api.classloader.model.ClassLoaderModel;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public abstract class DeployableFileBuilder<T extends DeployableFileBuilder<T>> extends AbstractArtifactFileBuilder<T> {

  protected Properties deployProperties = new Properties();
  private final Map<PluginAdditionalDependenciesKey, List<JarFileBuilder>> additionalPluginDependencies = new HashMap<>();

  private boolean useHeavyPackage = true;
  private String classloaderModelVersion = "1.0";

  public DeployableFileBuilder(String artifactId, boolean upperCaseInExtension) {
    super(artifactId, upperCaseInExtension);
  }

  public DeployableFileBuilder(String artifactId) {
    super(artifactId);
  }

  public DeployableFileBuilder(T source) {
    super(source);
  }

  public DeployableFileBuilder(String artifactId, T source) {
    super(artifactId, source);
  }

  /**
   * Adds a new dependency that will be visible only to the plugin defined by the groupId and artifactId of the {@link ArtifactPluginFileBuilder}.
   *
   * @param dependencyFileBuilder shared dependency.
   * @return the same builder instance
   */
  public T additionalPluginDependencies(ArtifactPluginFileBuilder pluginFileBuilder, JarFileBuilder dependencyFileBuilder) {
    PluginAdditionalDependenciesKey pluginAdditionalDependenciesKey =
        new PluginAdditionalDependenciesKey(pluginFileBuilder.getGroupId(), pluginFileBuilder.getArtifactId());
    if (!additionalPluginDependencies.containsKey(pluginAdditionalDependenciesKey)) {
      additionalPluginDependencies.put(pluginAdditionalDependenciesKey, new ArrayList<>());
    }
    additionalPluginDependencies.get(pluginAdditionalDependenciesKey).add(dependencyFileBuilder);
    return getThis();
  }

  /**
   * Adds a property into the application deployment properties file.
   *
   * @param propertyName  name of the property to add. Non empty
   * @param propertyValue value of the property to add. Non null.
   * @return the same builder instance
   */
  public T deployedWith(String propertyName, String propertyValue) {
    checkImmutable();
    checkArgument(!StringUtils.isEmpty(propertyName), "Property name cannot be empty");
    checkArgument(propertyValue != null, "Property value cannot be null");
    deployProperties.put(propertyName, propertyValue);
    return getThis();
  }

  public T usingLightWeightPackage() {
    useHeavyPackage = false;
    return getThis();
  }

  public T withClassloaderModelVersion(String classloaderModelVersion) {
    this.classloaderModelVersion = classloaderModelVersion;
    return getThis();
  }

  @Override
  protected final List<ZipUtils.ZipResource> getCustomResources() {
    final List<ZipUtils.ZipResource> customResources = new LinkedList<>();

    if (useHeavyPackage) {
      for (AbstractDependencyFileBuilder dependencyFileBuilder : getAllCompileDependencies()) {
        if (MULE_DOMAIN_CLASSIFIER.equals(dependencyFileBuilder.getClassifier())) {
          continue;
        }

        customResources.add(new ZipUtils.ZipResource(dependencyFileBuilder.getArtifactFile().getAbsolutePath(),
                                                     Paths.get(REPOSITORY_FOLDER,
                                                               dependencyFileBuilder.getArtifactFileRepositoryPath())
                                                         .toString()));


        if (MULE_PLUGIN_CLASSIFIER.equals(dependencyFileBuilder.getClassifier())) {
          File pluginClassLoaderModel = createClassLoaderModelJsonFile(dependencyFileBuilder);
          customResources.add(new ZipUtils.ZipResource(pluginClassLoaderModel.getAbsolutePath(),
                                                       Paths.get(REPOSITORY_FOLDER,
                                                                 dependencyFileBuilder.getArtifactFileRepositoryFolderPath(),
                                                                 CLASSLOADER_MODEL_JSON_DESCRIPTOR)
                                                           .toString()));
        } else {
          customResources.add(new ZipUtils.ZipResource(dependencyFileBuilder.getArtifactPomFile().getAbsolutePath(),
                                                       Paths.get(REPOSITORY_FOLDER,
                                                                 dependencyFileBuilder.getArtifactFilePomRepositoryPath())
                                                           .toString()));
        }
      }

      customResources.add(new ZipUtils.ZipResource(getClassLoaderModelFile().getAbsolutePath(),
                                                   CLASSLOADER_MODEL_JSON_DESCRIPTOR_LOCATION));
    }

    customResources.addAll(doGetCustomResources());

    return customResources;
  }

  private File createClassLoaderModelJsonFile(AbstractDependencyFileBuilder dependencyFileBuilder) {
    ArtifactCoordinates artifactCoordinates =
        new ArtifactCoordinates(dependencyFileBuilder.getGroupId(), dependencyFileBuilder.getArtifactId(),
                                dependencyFileBuilder.getVersion(), dependencyFileBuilder.getType(),
                                dependencyFileBuilder.getClassifier());
    ClassLoaderModel classLoaderModel = new ClassLoaderModel(classloaderModelVersion, artifactCoordinates);

    List<Artifact> artifactDependencies = new LinkedList<>();
    List<AbstractDependencyFileBuilder> dependencies = dependencyFileBuilder.getDependencies();
    for (AbstractDependencyFileBuilder fileBuilderDependency : dependencies) {
      artifactDependencies.add(getArtifact(fileBuilderDependency, isShared(fileBuilderDependency)));
    }

    classLoaderModel.setDependencies(artifactDependencies);

    Path repository = Paths.get(getTempFolder(), REPOSITORY_FOLDER, dependencyFileBuilder.getArtifactFileRepositoryFolderPath());
    if (repository.toFile().exists()) {
      repository.toFile().delete();
    } else {
      if (!repository.toFile().mkdirs()) {
        throw new IllegalStateException("Cannot create artifact folder inside repository");
      }
    }

    return serializeToFile(classLoaderModel, repository.toFile());
  }

  private File getClassLoaderModelFile() {
    ArtifactCoordinates artifactCoordinates = new ArtifactCoordinates(getGroupId(), getArtifactId(), getVersion());
    ClassLoaderModel classLoaderModel = new ClassLoaderModel(classloaderModelVersion, artifactCoordinates);

    List<Artifact> artifactDependencies = new LinkedList<>();
    for (AbstractDependencyFileBuilder fileBuilderDependency : getDependencies()) {
      artifactDependencies.add(getArtifact(fileBuilderDependency, isShared(fileBuilderDependency)));
    }

    classLoaderModel.setDependencies(artifactDependencies);

    File destinationFolder = Paths.get(getTempFolder()).resolve(META_INF).resolve(MULE_ARTIFACT).toFile();

    if (!destinationFolder.exists()) {
      assertThat(destinationFolder.mkdirs(), is(true));
    }
    return serializeToFile(classLoaderModel, destinationFolder);
  }

  private Artifact getArtifact(AbstractDependencyFileBuilder builder, boolean shared) {
    ArtifactCoordinates artifactCoordinates =
        new ArtifactCoordinates(builder.getGroupId(), builder.getArtifactId(), builder.getVersion(), builder.getType(),
                                builder.getClassifier());
    final Artifact artifact = new Artifact(artifactCoordinates, builder.getArtifactFile().toURI());
    artifact.setShared(shared);
    return artifact;
  }

  protected abstract List<ZipUtils.ZipResource> doGetCustomResources();

  private class PluginAdditionalDependenciesKey {

    private final String groupId;
    private final String artifactId;

    public PluginAdditionalDependenciesKey(String groupId, String artifactId) {
      this.groupId = groupId;
      this.artifactId = artifactId;
    }

    public String getGroupId() {
      return groupId;
    }

    public String getArtifactId() {
      return artifactId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      PluginAdditionalDependenciesKey that = (PluginAdditionalDependenciesKey) o;

      if (!groupId.equals(that.groupId)) {
        return false;
      }
      return artifactId.equals(that.artifactId);
    }

    @Override
    public int hashCode() {
      int result = groupId.hashCode();
      result = 31 * result + artifactId.hashCode();
      return result;
    }
  }
}
