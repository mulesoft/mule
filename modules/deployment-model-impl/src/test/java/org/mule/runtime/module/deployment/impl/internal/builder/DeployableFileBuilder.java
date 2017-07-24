/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static org.mule.runtime.deployment.model.api.application.ApplicationDescriptor.REPOSITORY_FOLDER;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.deployment.impl.internal.plugin.PluginMavenClassLoaderModelLoader.CLASSLOADER_MODEL_JSON_DESCRIPTOR;
import static org.mule.runtime.module.deployment.impl.internal.plugin.PluginMavenClassLoaderModelLoader.CLASSLOADER_MODEL_JSON_DESCRIPTOR_LOCATION;
import static org.mule.tools.api.packager.PackagerFolders.META_INF;
import static org.mule.tools.api.packager.PackagerFolders.MULE_ARTIFACT;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.module.artifact.builder.AbstractArtifactFileBuilder;
import org.mule.runtime.module.artifact.builder.AbstractDependencyFileBuilder;
import org.mule.tck.ZipUtils;
import org.mule.tools.api.ContentGenerator;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;
import org.mule.tools.api.classloader.model.ClassLoaderModel;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public abstract class DeployableFileBuilder<T extends DeployableFileBuilder<T>> extends AbstractArtifactFileBuilder<T> {

  protected Properties deployProperties = new Properties();

  private boolean useHeavyPackage = true;

  public DeployableFileBuilder(String id, boolean upperCaseInExtension) {
    super(id, upperCaseInExtension);
  }

  public DeployableFileBuilder(String id) {
    super(id);
  }

  public DeployableFileBuilder(T source) {
    super(source);
  }

  public DeployableFileBuilder(String id, T source) {
    super(id, source);
  }

  /**
   * Adds a property into the application deployment properties file.
   *
   * @param propertyName name fo the property to add. Non empty
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

  public T usingLightwayPackage() {
    useHeavyPackage = false;
    return getThis();
  }

  @Override
  protected final List<ZipUtils.ZipResource> getCustomResources() {
    final List<ZipUtils.ZipResource> customResources = new LinkedList<>();


    for (AbstractDependencyFileBuilder dependencyFileBuilder : getAllCompileDependencies()) {
      customResources.add(new ZipUtils.ZipResource(dependencyFileBuilder.getArtifactFile().getAbsolutePath(),
                                                   Paths.get(REPOSITORY_FOLDER,
                                                             dependencyFileBuilder.getArtifactFileRepositoryPath())
                                                       .toString()));

      if (useHeavyPackage && MULE_PLUGIN_CLASSIFIER.equals(dependencyFileBuilder.getClassifier())) {
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

    customResources.addAll(doGetCustomResources());

    if (useHeavyPackage) {
      customResources.add(new ZipUtils.ZipResource(getClassLoaderModelFile().getAbsolutePath(),
                                                   CLASSLOADER_MODEL_JSON_DESCRIPTOR_LOCATION));
    }

    return customResources;
  }

  private File createClassLoaderModelJsonFile(AbstractDependencyFileBuilder dependencyFileBuilder) {
    ArtifactCoordinates artifactCoordinates =
        new ArtifactCoordinates(dependencyFileBuilder.getGroupId(), dependencyFileBuilder.getArtifactId(),
                                dependencyFileBuilder.getVersion(), dependencyFileBuilder.getType(),
                                dependencyFileBuilder.getClassifier());
    ClassLoaderModel classLoaderModel = new ClassLoaderModel("1.0", artifactCoordinates);

    List<Artifact> artifactDependencies = new LinkedList<>();
    List<AbstractDependencyFileBuilder> dependencies = dependencyFileBuilder.getDependencies();
    for (AbstractDependencyFileBuilder fileBuilderDependency : dependencies) {
      artifactDependencies.add(getArtifact(fileBuilderDependency));
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

    return ContentGenerator.createClassLoaderModelJsonFile(classLoaderModel, repository.toFile());
  }

  private File getClassLoaderModelFile() {
    ArtifactCoordinates artifactCoordinates = new ArtifactCoordinates(getGroupId(), getArtifactId(), getVersion());
    ClassLoaderModel classLoaderModel = new ClassLoaderModel("1.0", artifactCoordinates);

    List<Artifact> artifactDependencies = new LinkedList<>();
    for (AbstractDependencyFileBuilder fileBuilderDependency : getDependencies()) {
      artifactDependencies.add(getArtifact(fileBuilderDependency));
    }

    classLoaderModel.setDependencies(artifactDependencies);

    Path rootFolder = Paths.get(getTempFolder());
    File destinationFolder = rootFolder.resolve(META_INF).resolve(MULE_ARTIFACT).toFile();
    return ContentGenerator.createClassLoaderModelJsonFile(classLoaderModel, destinationFolder);
  }

  private Artifact getArtifact(AbstractDependencyFileBuilder builder) {
    ArtifactCoordinates artifactCoordinates =
        new ArtifactCoordinates(builder.getGroupId(), builder.getArtifactId(), builder.getVersion(), builder.getType(),
                                builder.getClassifier());
    return new Artifact(artifactCoordinates, builder.getArtifactFile().toURI());
  }

  protected abstract List<ZipUtils.ZipResource> doGetCustomResources();
}
