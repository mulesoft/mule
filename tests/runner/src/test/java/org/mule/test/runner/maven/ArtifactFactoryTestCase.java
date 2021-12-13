/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.runner.maven;

import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.test.runner.maven.ArtifactFactory.createFromPomFile;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.eclipse.aether.artifact.Artifact;

import org.junit.Test;

import io.qameta.allure.Issue;

@Issue("MULE-19417")
public class ArtifactFactoryTestCase {

  private static final String ROOT_RESOURCES_FOLDER = "test-poms";
  private static final String GROUP_ID = "org.mule.tests";
  private static final String ARTIFACT_ID = "simple-pom";
  private static final String VERSION = "1.0.0";
  private static final String PACKAGING = MULE_PLUGIN_CLASSIFIER;

  @Test
  public void pomWithProperties() {
    generateArtifactAndValidate("pom-with-property.xml");
  }

  @Test
  public void pomWithoutProperties() {
    generateArtifactAndValidate("simple-pom.xml");
  }

  @Test
  public void pomWithMissingProperties() {
    File pom = getPom("pom-with-missing-property.xml");
    Artifact artifact = createFromPomFile(pom);
    assertThat(artifact.getGroupId(), is("${missingProperty}"));
  }

  private void generateArtifactAndValidate(String name) {
    File pom = getPom(name);
    Artifact artifact = createFromPomFile(pom);
    assertThat(artifact.getGroupId(), is(GROUP_ID));
    assertThat(artifact.getArtifactId(), is(ARTIFACT_ID));
    assertThat(artifact.getVersion(), is(VERSION));
    assertThat(artifact.getExtension(), is(PACKAGING));
  }

  private File getPom(String name) {
    try {
      return new File(this.getClass().getClassLoader().getResource(
                                                                   Paths.get(ROOT_RESOURCES_FOLDER, name).toString())
          .toURI());
    } catch (URISyntaxException e) {
      throw new RuntimeException("Error parsing URI", e);
    }
  }

}
