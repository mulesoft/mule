/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.domain;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.MuleClassLoaderLookupPolicy;
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
      new SystemProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY, temporaryFolder.getRoot().getCanonicalPath());
  protected final File muleHomeFolder;
  protected final ArtifactClassLoader containerClassLoader =
      new MuleArtifactClassLoader("mule", new URL[0], getClass().getClassLoader(),
                                  new MuleClassLoaderLookupPolicy(emptyMap(), emptySet()));

  public AbstractDomainTestCase() throws IOException {
    muleHomeFolder = temporaryFolder.getRoot();
  }

  protected void createDomainDir(String domainFolder, String domain) {
    assertThat(new File(muleHomeFolder, domainFolder + File.separator + domain).mkdirs(), is(true));
  }
}
