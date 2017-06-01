/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Optional.empty;
import static org.apache.commons.io.FileUtils.write;
import static org.apache.commons.io.FilenameUtils.getName;
import static org.apache.commons.lang.StringUtils.isEmpty;
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

  private final String fileName;
  private final String id;
  private File artifactFile;
  protected List<ZipResource> resources = new LinkedList<>();
  protected boolean corrupted;

  /**
   * Creates a new builder
   *
   * @param id artifact identifier. Non empty.
   * @param upperCaseInExtension whether the extension is in uppercase
   */
  public AbstractArtifactFileBuilder(String id, boolean upperCaseInExtension) {
    super(id);
    checkArgument(!isEmpty(id), "ID cannot be empty");
    this.id = id;
    this.fileName = upperCaseInExtension ? (id + getFileExtension().toUpperCase()) : (id + getFileExtension().toLowerCase());
  }

  /**
   * Creates a new builder
   *
   * @param id artifact identifier. Non empty.
   */
  public AbstractArtifactFileBuilder(String id) {
    this(id, false);
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
    this(source.getId(), source);
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
    resources.add(new ZipResource(classFile.getAbsolutePath(), "classes/" + alias));

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
    return id;
  }

  @Override
  public String getZipPath() {
    return "/" + fileName;
  }

  @Override
  public String getDeployedPath() {
    if (corrupted) {
      return fileName;
    } else {
      return id;
    }
  }

  @Override
  public File getArtifactFile() {
    if (artifactFile == null) {
      checkArgument(!isEmpty(fileName), "Filename cannot be empty");

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

  protected final void checkImmutable() {
    assertThat("Cannot change attributes once the artifact file was built", artifactFile, is(nullValue()));
  }

  protected ZipResource createPropertiesFile(Properties props, String propertiesFileName, String zipAlias) {
    ZipResource result = null;

    if (!props.isEmpty()) {
      File classesFolder = new File(getTempFolder(), "classes");
      classesFolder.mkdirs();
      final File applicationPropertiesFile = new File(classesFolder, propertiesFileName);
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
