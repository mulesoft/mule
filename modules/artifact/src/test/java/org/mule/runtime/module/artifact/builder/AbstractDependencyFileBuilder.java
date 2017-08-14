/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.SystemUtils.JAVA_IO_TMPDIR;
import static org.mule.runtime.module.artifact.api.classloader.MuleMavenPlugin.MULE_MAVEN_PLUGIN_ARTIFACT_ID;
import static org.mule.runtime.module.artifact.api.classloader.MuleMavenPlugin.MULE_MAVEN_PLUGIN_GROUP_ID;
import org.mule.runtime.api.exception.MuleRuntimeException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Base class for all kind of artifact that may exists in mule.
 * <p>
 * Provides all the support needed for maven.
 *
 * @param <T> the actual type of the builder.
 */
public abstract class AbstractDependencyFileBuilder<T extends AbstractDependencyFileBuilder<T>> {

  public static final String COMPILE_SCOPE = "compile";
  private static final String MULE_MAVEN_PLUGIN_VERSION = "1.0.0";
  private final String artifactId;
  private final List<AbstractDependencyFileBuilder> dependencies = new ArrayList<>();
  private final List<AbstractDependencyFileBuilder> sharedLibraries = new ArrayList<>();
  private String groupId = "org.mule.test";
  private String version = "1.0.0";
  private String type = "jar";
  private String classifier;
  private File artifactPomFile;
  private File tempFolder;

  /**
   * @param artifactId the maven artifact id
   */
  public AbstractDependencyFileBuilder(String artifactId) {
    checkArgument(artifactId != null, "artifact id cannot be null");
    this.artifactId = artifactId;
  }

  protected String getTempFolder() {
    if (tempFolder == null) {
      tempFolder = new File(JAVA_IO_TMPDIR, getArtifactId() + currentTimeMillis());
      tempFolder.deleteOnExit();
      if (tempFolder.exists()) {
        tempFolder.delete();
      }
      tempFolder.mkdir();
    }

    return tempFolder.getAbsolutePath();
  }

  /**
   * Sets the temporary folder to be used to create the artifact file.
   *
   * @param tempFolder temporary folder to use to create the artifact file.
   * @return the same builder instance
   */
  public T tempFolder(File tempFolder) {
    checkArgument(tempFolder != null, "tempFolder cannot be null");
    checkArgument(tempFolder.isDirectory(), "tempFolder must be a directory");
    this.tempFolder = tempFolder;
    return getThis();
  }

  public abstract File getArtifactFile();

  public File getArtifactPomFile() {
    if (artifactPomFile == null) {
      checkArgument(!isEmpty(artifactId), "Filename cannot be empty");

      final File tempFile = new File(getTempFolder(), artifactId + ".pom");
      tempFile.deleteOnExit();

      Model model = new Model();
      model.setGroupId(getGroupId());
      model.setArtifactId(getArtifactId());
      model.setVersion(getVersion());
      model.setModelVersion("4.0.0");
      if (!sharedLibraries.isEmpty()) {
        model.setBuild(new Build());
        model.getBuild().setPlugins(singletonList(createMuleMavenPlugin()));
      }

      for (AbstractDependencyFileBuilder fileBuilderDependency : dependencies) {
        model.addDependency(fileBuilderDependency.getAsMavenDependency());
      }

      artifactPomFile = new File(tempFile.getAbsolutePath());
      try (FileOutputStream fileOutputStream = new FileOutputStream(artifactPomFile)) {
        new MavenXpp3Writer().write(fileOutputStream, model);
      } catch (IOException e) {
        throw new MuleRuntimeException(e);
      }
    }
    return artifactPomFile;
  }

  private Plugin createMuleMavenPlugin() {
    Plugin plugin = new Plugin();
    plugin.setGroupId(MULE_MAVEN_PLUGIN_GROUP_ID);
    plugin.setArtifactId(MULE_MAVEN_PLUGIN_ARTIFACT_ID);
    plugin.setVersion(MULE_MAVEN_PLUGIN_VERSION);
    Xpp3Dom configuration = new Xpp3Dom("configuration");
    plugin.setConfiguration(configuration);
    Xpp3Dom sharedLibrariesDom = new Xpp3Dom("sharedLibraries");
    configuration.addChild(sharedLibrariesDom);
    dependencies.stream().filter(sharedLibraries::contains)
        .forEach(sharedLibrary -> {
          Xpp3Dom sharedLibraryDom = new Xpp3Dom("sharedLibrary");
          sharedLibrariesDom.addChild(sharedLibraryDom);
          Xpp3Dom groupIdDom = new Xpp3Dom("groupId");
          groupIdDom.setValue(sharedLibrary.getGroupId());
          sharedLibraryDom.addChild(groupIdDom);
          Xpp3Dom artifactIdDom = new Xpp3Dom("artifactId");
          artifactIdDom.setValue(sharedLibrary.getArtifactId());
          sharedLibraryDom.addChild(artifactIdDom);
        });
    return plugin;
  }

  public T dependingOn(AbstractDependencyFileBuilder dependencyFileBuilder) {
    dependencies.add(dependencyFileBuilder);
    return getThis();
  }

  /**
   * Adds a new dependency that will be visible to other plugins within the artifact.
   *
   * @param dependencyFileBuilder shared dependency.
   * @return the same builder instance
   */
  public T dependingOnSharedLibrary(AbstractDependencyFileBuilder dependencyFileBuilder) {
    dependencies.add(dependencyFileBuilder);
    sharedLibraries.add(dependencyFileBuilder);
    return getThis();
  }

  /**
   * @param groupId the maven group id
   * @return the same builder instance
   */
  public T withGroupId(String groupId) {
    this.groupId = groupId;
    return getThis();
  }

  /**
   * @param version the maven version
   * @return the same builder instnace
   */
  public T withVersion(String version) {
    this.version = version;
    return getThis();
  }

  /**
   * @param classifier the maven classifier
   * @return the same builder instance
   */
  public T withClassifier(String classifier) {
    this.classifier = classifier;
    return getThis();
  }

  /**
   * @return maven group id
   */
  public String getGroupId() {
    return groupId;
  }

  /**
   * @return maven artifact id
   */
  public String getArtifactId() {
    return artifactId;
  }

  /**
   * @return maven version
   */
  public String getVersion() {
    return version;
  }

  /**
   * @return maven type
   */
  public String getType() {
    return type;
  }

  /**
   * @return maven classifier
   */
  public String getClassifier() {
    return classifier;
  }

  /**
   * @return the path within the maven repository this artifact is located in.
   */
  public String getArtifactFileRepositoryPath() {
    return getGroupId().replace(".", File.separator) + File.separator
        + Paths
            .get(getArtifactId(), getVersion(),
                 getArtifactId() + "-" + getVersion() + (getClassifier() != null ? "-" + getClassifier() : "") + "." + type)
            .toString();
  }

  /**
   * @return the path of the folder within the maven repository where this artifact is located in.
   */
  public String getArtifactFileRepositoryFolderPath() {
    return getGroupId().replace(".", File.separator) + File.separator
        + Paths
            .get(getArtifactId(), getVersion())
            .toString();
  }

  /**
   * @return the path within the maven repository this artifact pom is located in.
   */
  public String getArtifactFilePomRepositoryPath() {
    return getGroupId().replace(".", File.separator) + File.separator
        + Paths.get(getArtifactId(), getVersion(), getArtifactId() + "-" + getVersion() + ".pom").toString();
  }

  /**
   * @return the path within the artifact file where the pom file is bundled
   */
  public String getArtifactFileBundledPomPath() {
    return Paths.get("META-INF", "maven", getGroupId(), getArtifactId(), "pom.xml").toString();
  }

  /**
   * Creates a {@link Dependency} object from this artifact with scope compile.
   *
   * @return a maven
   */
  public Dependency getAsMavenDependency() {
    Dependency dependency = new Dependency();
    dependency.setVersion(getVersion());
    dependency.setGroupId(getGroupId());
    dependency.setArtifactId(getArtifactId());
    dependency.setClassifier(getClassifier());
    dependency.setType(getType());
    dependency.setScope(COMPILE_SCOPE);
    return dependency;
  }

  /**
   * @return current instance. Used just to avoid compilation warnings.
   */
  protected abstract T getThis();

  /**
   * @return the collection of dependencies of this artifact.
   */
  public List<AbstractDependencyFileBuilder> getDependencies() {
    return dependencies;
  }

  /**
   * @return a collection of all the compile dependencies of this artifact including the transitive ones
   */
  public List<AbstractDependencyFileBuilder> getAllCompileDependencies() {
    List<AbstractDependencyFileBuilder> allCompileDependencies = new ArrayList<>();
    for (AbstractDependencyFileBuilder dependency : dependencies) {
      if (dependency.getAsMavenDependency().getScope().equals(COMPILE_SCOPE)) {
        allCompileDependencies.addAll(dependency.getAllCompileDependencies());
        allCompileDependencies.add(dependency);
      }
    }
    return allCompileDependencies;
  }
}
