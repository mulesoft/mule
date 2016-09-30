/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ExtensionPluginMetadataGeneratorTestCase {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private Artifact plugin = new DefaultArtifact("org.foo:foo-core:1.0-SNAPSHOT");

  @Test
  public void scanningClassPathShouldNotIncludeSpringStuff() throws IOException {
    ExtensionPluginMetadataGenerator generator = new ExtensionPluginMetadataGenerator(temporaryFolder.newFolder());
    Class scanned = generator.scanForExtensionAnnotatedClasses(plugin, newArrayList(
                                                                                    this.getClass().getProtectionDomain()
                                                                                        .getCodeSource().getLocation()));

    assertThat(scanned, is(nullValue()));
  }

}
