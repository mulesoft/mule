/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.builder;

import org.mule.runtime.module.artifact.builder.AbstractArtifactFileBuilder;
import org.mule.tck.ZipUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

/**
 * Creates Mule Domain bundle files.
 */
public class DomainBundleFileBuilder extends AbstractArtifactFileBuilder<DomainBundleFileBuilder> {

  private static final String MULE_DOMAIN_BUNDLE_CLASSIFIER = "domain-bundle";
  private final File domainFile;
  private final List<File> applications = new LinkedList<>();

  /**
   * Creates a new builder
   *
   * @param domainFileBuilder creates the file for the domain artifact contained on the bundle.
   */
  public DomainBundleFileBuilder(DomainFileBuilder domainFileBuilder) {
    this(domainFileBuilder.getArtifactId(), domainFileBuilder.getArtifactFile());
  }

  /**
   * Creates a new builder
   *
   * @param artifactId identifier for the domain bunle
   * @param domain file for the domain artifact contained on the bundle.
   */
  public DomainBundleFileBuilder(String artifactId, File domain) {
    super(artifactId);
    this.domainFile = domain;
  }

  /**
   * Adds an application to the bundle
   *
   * @param applicationFileBuilder creates the application file to add.
   * @return same builder instance
   */
  public DomainBundleFileBuilder containing(ApplicationFileBuilder applicationFileBuilder) {
    return this.containing(applicationFileBuilder.getArtifactFile());
  }

  /**
   * Adds an application to the bundle
   *
   * @param applicationFile the application file to add.
   * @return same builder instance
   */
  public DomainBundleFileBuilder containing(File applicationFile) {
    applications.add(applicationFile);

    return this;
  }

  @Override
  protected List<ZipUtils.ZipResource> getCustomResources() {
    final List<ZipUtils.ZipResource> customResources = new LinkedList<>();

    customResources.add(new ZipUtils.ZipResource(domainFile.getAbsolutePath(),
                                                 Paths.get("domain",
                                                           domainFile.getName())
                                                     .toString()));

    for (File application : applications) {

      customResources.add(new ZipUtils.ZipResource(application.getAbsolutePath(),
                                                   Paths.get("applications",
                                                             application.getName())
                                                       .toString()));
    }

    return customResources;
  }

  @Override
  protected DomainBundleFileBuilder getThis() {
    return this;
  }

  @Override
  public String getConfigFile() {
    return null;
  }

  @Override
  protected String getFileExtension() {
    return ".zip";
  }

  @Override
  public String getClassifier() {
    return MULE_DOMAIN_BUNDLE_CLASSIFIER;
  }
}
