/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static java.io.File.separator;
import static java.util.Optional.empty;
import static org.apache.commons.io.FileUtils.write;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getName;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mule.tck.ZipUtils.compress;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.tck.ZipUtils.ZipResource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Defines a builder to create files for mule artifacts.
 * <p/>
 * Instances can be configured using the methods that follow the builder pattern until the artifact file is accessed. After that
 * point, builder methods will fail to update the builder state.
 *
 * @param <T> class of the implementation builder
 */
public abstract class AbstractArtifactFileBuilder<T extends AbstractArtifactFileBuilder<T>>
    extends AbstractDependencyFileBuilder<T>
    implements TestArtifactDescriptor {

  private final boolean upperCaseInExtension;
  private File artifactFile;
  protected List<ZipResource> resources = new LinkedList<>();
  protected boolean corrupted;

  /**
   * Creates a new builder
   *
   * @param artifactId artifact identifier. Non empty.
   * @param upperCaseInExtension whether the extension is in uppercase
   */
  public AbstractArtifactFileBuilder(String artifactId, boolean upperCaseInExtension) {
    super(artifactId);
    this.upperCaseInExtension = upperCaseInExtension;
    checkArgument(!isEmpty(artifactId), "ID cannot be empty");
  }

  /**
   * Creates a new builder
   *
   * @param artifactId artifact identifier. Non empty.
   */
  public AbstractArtifactFileBuilder(String artifactId) {
    this(artifactId, false);
  }

  /**
   * Template method to redefine the file extension
   *
   * @return the file extension of the file name for the artifact.
   */
  protected String getFileExtension() {
    return ".jar";
  }

  /**
   * Creates a new builder from another instance.
   *
   * @param source instance used as template to build the new one. Non null.
   */
  public AbstractArtifactFileBuilder(T source) {
    this(source.getArtifactId(), source);
  }

  /**
   * Create a new builder from another instance and different ID.
   *
   * @param id artifact identifier. Non empty.
   * @param source instance used as template to build the new one. Non null.
   */
  public AbstractArtifactFileBuilder(String id, T source) {
    this(id);
    this.resources.addAll(source.resources);
    this.corrupted = source.corrupted;
  }

  /**
   * Adds a jar file to the artifact lib folder.
   *
   * @param jarFile jar file from a external file or test resource.
   * @return the same builder instance
   */
  public T usingLibrary(String jarFile) {
    checkImmutable();
    checkArgument(!isEmpty(jarFile), "Jar file cannot be empty");
    resources.add(new ZipResource(jarFile, "lib/" + getName(jarFile)));

    return getThis();
  }

  /**
   * Adds a class file to the artifact classes folder.
   *
   * @param classFile class file to include. Non null.
   * @param alias path where the file must be added inside the app file
   * @return the same builder instance
   */
  public T containingClass(File classFile, String alias) {
    checkImmutable();
    checkArgument(classFile != null, "Class file cannot be null");
    resources.add(new ZipResource(classFile.getAbsolutePath(), alias));

    return getThis();
  }

  /**
   * Adds a resource file to the plugin root folder.
   *
   * @param resourceFile resource file from a external file or test resource.
   * @return the same builder instance
   */
  public T containingResource(String resourceFile, String alias) {
    checkImmutable();
    checkArgument(!isEmpty(resourceFile), "Resource file cannot be empty");
    resources.add(new ZipResource(resourceFile, alias));

    return getThis();
  }

  /**
   * Indicates that the generated artifact file must be a corrupted ZIP.
   *
   * @return the same builder instance
   */
  public T corrupted() {
    checkImmutable();
    this.corrupted = true;

    return getThis();
  }

  @Override
  public String getId() {
    return getBaseName(getArtifactFileName());
  }

  @Override
  public String getZipPath() {
    if (artifactFile == null) {
      throw new IllegalStateException("Must generate the artifact file before invoking this method");
    }
    return separator + artifactFile.getName();
  }

  @Override
  public String getDeployedPath() {
    if (artifactFile == null) {
      throw new IllegalStateException("Must generate the artifact file before invoking this method");
    }
    if (corrupted) {
      return artifactFile.getName();
    } else {
      return getBaseName(artifactFile.getName());
    }
  }

  @Override
  public File getArtifactFile() {
    if (artifactFile == null) {

      String fileName = getArtifactFileName();
      final File tempFile = new File(getTempFolder(), fileName);
      tempFile.deleteOnExit();

      if (corrupted) {
        buildBrokenJarFile(tempFile);
      } else {
        final List<ZipResource> zipResources = new LinkedList<>(resources);
        zipResources.add(new ZipResource(getArtifactPomFile().getAbsolutePath(), getArtifactFileBundledPomPath()));
        zipResources.addAll(getCustomResources());
        compress(tempFile, zipResources.toArray(new ZipResource[0]));
      }

      artifactFile = new File(tempFile.getAbsolutePath());
    }

    return artifactFile;
  }

  private String getArtifactFileName() {
    String fileName = getArtifactId();
    String artifactNameSeparator = "-";
    if (getVersion() != null) {
      fileName = fileName + artifactNameSeparator + getVersion();
    }

    if (getClassifier() != null) {
      fileName = fileName + artifactNameSeparator + getClassifier();
    }

    fileName = fileName + ((upperCaseInExtension) ? getFileExtension().toUpperCase() : getFileExtension().toLowerCase());
    return fileName;
  }

  protected final void checkImmutable() {
    assertThat("Cannot change attributes once the artifact file was built", artifactFile, is(nullValue()));
  }

  protected ZipResource createPropertiesFile(Properties props, String propertiesFileName, String zipAlias) {
    ZipResource result = null;

    if (!props.isEmpty()) {
      final File applicationPropertiesFile = new File(getTempFolder(), propertiesFileName);
      applicationPropertiesFile.deleteOnExit();
      createPropertiesFile(applicationPropertiesFile, props);

      result = new ZipResource(applicationPropertiesFile.getAbsolutePath(), zipAlias);
    }

    return result;
  }

  protected void createPropertiesFile(File file, Properties props) {
    try {
      OutputStream out = new FileOutputStream(file);
      props.store(out, "Generated application properties");
    } catch (Exception e) {
      throw new IllegalStateException("Cannot create properties", e);
    }
  }

  private void buildBrokenJarFile(File tempFile) throws UncheckedIOException {
    try {
      write(tempFile, "This content represents invalid compressed data");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * @return a collection with custom {@link ZipResource}s to add to the artifact file.
   */
  protected abstract List<ZipResource> getCustomResources();

  /**
   * @return the descriptor loader for the artifact.
   */
  protected Optional<MuleArtifactLoaderDescriptor> getBundleDescriptorLoader() {
    return empty();
  }

  @Override
  public String getGroupId() {
    return (String) getBundleDescriptorLoader().map(descriptorLoader -> descriptorLoader.getAttributes().get("groupId"))
        .orElse(super.getGroupId());
  }

  @Override
  public String getArtifactId() {
    return (String) getBundleDescriptorLoader().map(descriptorLoader -> descriptorLoader.getAttributes().get("artifactId"))
        .orElse(super.getArtifactId());
  }

  @Override
  public String getClassifier() {
    return (String) getBundleDescriptorLoader().map(descriptorLoader -> descriptorLoader.getAttributes().get("classifier"))
        .orElse(super.getClassifier());
  }

  @Override
  public String getType() {
    return (String) getBundleDescriptorLoader().map(descriptorLoader -> descriptorLoader.getAttributes().get("type"))
        .orElse(super.getType());
  }

  @Override
  public String getVersion() {
    return (String) getBundleDescriptorLoader().map(descriptorLoader -> descriptorLoader.getAttributes().get("version"))
        .orElse(super.getVersion());
  }
}
