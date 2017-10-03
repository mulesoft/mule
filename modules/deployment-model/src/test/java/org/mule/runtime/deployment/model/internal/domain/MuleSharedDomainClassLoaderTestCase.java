/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.domain;

import static java.util.Collections.emptyList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainFolder;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.DEFAULT_DOMAIN_NAME;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.util.FileUtils;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemPropertyTemporaryFolder;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;

@SmallTest
@RunWith(PowerMockRunner.class)
@PrepareForTest(MuleArtifactClassLoader.class)
@PowerMockIgnore("javax.management.*")
public class MuleSharedDomainClassLoaderTestCase extends AbstractMuleTestCase {

  public static final String RESOURCE_FILE_NAME = "file.properties";

  @Rule
  public SystemPropertyTemporaryFolder temporaryFolder =
      new SystemPropertyTemporaryFolder(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY);
  private final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);

  @Before
  public void setUp() throws Exception {
    mockStatic(MuleArtifactClassLoader.class);
  }

  @Test
  public void findResourcesInProvidedUrls() throws Exception {
    createDomainFolder(DEFAULT_DOMAIN_NAME);
    final File resourceFile = createDomainResource(DEFAULT_DOMAIN_NAME, RESOURCE_FILE_NAME);
    final List<URL> urls = Collections.singletonList(resourceFile.toURI().toURL());

    MuleSharedDomainClassLoader classLoader = new MuleSharedDomainClassLoader(new ArtifactDescriptor(DEFAULT_DOMAIN_NAME),
                                                                              getClass().getClassLoader(), lookupPolicy, urls,
                                                                              emptyList());

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
}
