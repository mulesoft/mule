/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mule.tck.ZipUtils.compress;
import org.mule.tck.ZipUtils.ZipResource;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.core.util.FilenameUtils;
import org.mule.runtime.core.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Defines a builder to create files for mule artifacts.
 * <p/>
 * Instances can be configured using the methods that follow the builder pattern until the artifact file is accessed. After that
 * point, builder methods will fail to update the builder state.
 *
 * @param <T> class of the implementation builder
 */
public abstract class AbstractArtifactFileBuilder<T extends AbstractArtifactFileBuilder<T>> implements TestArtifactDescriptor {

  private final String fileName;
  private final String id;
  private File artifactFile;
  protected List<ZipResource> resources = new LinkedList<>();
  protected boolean corrupted;

  /**
   * Creates a new builder
   *
   * @param id artifact identifier. Non empty.
   */
  public AbstractArtifactFileBuilder(String id) {
    checkArgument(!StringUtils.isEmpty(id), "ID cannot be empty");
    this.id = id;
    this.fileName = id + ".zip";
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
    checkArgument(!StringUtils.isEmpty(jarFile), "Jar file cannot be empty");
    resources.add(new ZipResource(jarFile, "lib/" + FilenameUtils.getName(jarFile)));

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

  /**
   * @return current instance. Used just to avoid compilation warnings.
   */
  protected abstract T getThis();

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
  public File getArtifactFile() throws Exception {
    if (artifactFile == null) {
      checkArgument(!StringUtils.isEmpty(fileName), "Filename cannot be empty");

      final File tempFile = new File(getTempFolder(), fileName);
      tempFile.deleteOnExit();

      if (corrupted) {
        buildBrokenZipFile(tempFile);
      } else {
        final List<ZipResource> zipResources = new LinkedList<>(resources);
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

  protected ZipResource createPropertiesFile(Properties props, String propertiesFileName) throws IOException {
    ZipResource result = null;

    if (!props.isEmpty()) {
      final File applicationPropertiesFile = new File(getTempFolder(), propertiesFileName);
      applicationPropertiesFile.deleteOnExit();
      createPropertiesFile(applicationPropertiesFile, props);

      result = new ZipResource(applicationPropertiesFile.getAbsolutePath(), propertiesFileName);
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

  protected String getTempFolder() {
    return System.getProperty("java.io.tmpdir");
  }

  private void buildBrokenZipFile(File tempFile) throws IOException {
    FileUtils.write(tempFile, "This is content represents invalid compressed data");
  }

  protected abstract List<ZipResource> getCustomResources() throws Exception;
}
