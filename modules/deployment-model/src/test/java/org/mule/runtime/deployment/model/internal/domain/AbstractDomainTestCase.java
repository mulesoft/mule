/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.domain;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;

import org.mule.runtime.container.internal.MuleClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class AbstractDomainTestCase extends AbstractMuleTestCase {

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Rule
  public final SystemProperty muleHomeSystemProperty =
      new SystemProperty(MULE_HOME_DIRECTORY_PROPERTY, temporaryFolder.getRoot().getCanonicalPath());
  protected final File muleHomeFolder;
  protected final ArtifactClassLoader containerClassLoader =
      new MuleArtifactClassLoader("mule", new ArtifactDescriptor("mule"), new URL[0],
                                  getClass().getClassLoader(),
                                  new MuleClassLoaderLookupPolicy(emptyMap(), emptySet()));

  public AbstractDomainTestCase() throws IOException {
    muleHomeFolder = temporaryFolder.getRoot();
  }

  protected void createDomainDir(String domainFolder, String domain) {
    assertThat(new File(muleHomeFolder, domainFolder + File.separator + domain).mkdirs(), is(true));
  }
}
