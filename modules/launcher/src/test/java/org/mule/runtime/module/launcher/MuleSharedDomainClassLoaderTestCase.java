/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.reboot.MuleContainerBootstrapUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.size.SmallTest;
import org.mule.runtime.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SmallTest
public class MuleSharedDomainClassLoaderTestCase extends AbstractMuleTestCase {

  public static final String RESOURCE_FILE_NAME = "file.properties";
  public static final String DEFAULT_DOMAIN_NAME = "default";
  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Rule
  public final SystemProperty muleHomeSystemProperty =
      new SystemProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY, temporaryFolder.getRoot().getCanonicalPath());
  private final File muleHomeFolder;
  private final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);

  public MuleSharedDomainClassLoaderTestCase() throws IOException {
    muleHomeFolder = temporaryFolder.getRoot();
  }

  @Before
  public void setUp() throws IOException {
    temporaryFolder.delete();
    temporaryFolder.create();
  }

  @Test
  public void findResourcesInProvidedUrls() throws Exception {
    createDomainFolder(DEFAULT_DOMAIN_NAME);
    final File resourceFile = createDomainResource(DEFAULT_DOMAIN_NAME, RESOURCE_FILE_NAME);
    final List<URL> urls = Collections.singletonList(resourceFile.toURI().toURL());

    MuleSharedDomainClassLoader classLoader =
        new MuleSharedDomainClassLoader(DEFAULT_DOMAIN_NAME, getClass().getClassLoader(), lookupPolicy, urls);

    assertThat(classLoader.findResource(RESOURCE_FILE_NAME), notNullValue());
  }

  private File createDomainResource(String domainName, String resourceFile) throws Exception {
    final File file = new File(getDomainFolder(domainName), resourceFile);
    assertThat(FileUtils.createFile(file.getAbsolutePath()).exists(), is(true));

    return file;
  }

  private void createDomainFolder(String domainName) {
    assertThat(getDomainFolder(domainName).mkdirs(), is(true));
  }

  private File getDomainFolder(String domainName) {
    return new File(muleHomeFolder, MuleContainerBootstrapUtils.MULE_DOMAIN_FOLDER + File.separator + domainName); // To change
                                                                                                                   // body of
                                                                                                                   // created
                                                                                                                   // methods use
                                                                                                                   // File |
                                                                                                                   // Settings |
                                                                                                                   // File
                                                                                                                   // Templates.
  }
}
