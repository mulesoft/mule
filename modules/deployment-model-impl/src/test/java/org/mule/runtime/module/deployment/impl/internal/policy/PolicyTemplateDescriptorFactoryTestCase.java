/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import static java.io.File.createTempFile;
import static java.io.File.separator;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.util.FileUtils.unzip;
import static org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateDescriptorFactory.CLASSES_DIR;
import static org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateDescriptorFactory.LIB_DIR;
import static org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateDescriptorFactory.MISSING_POLICY_PROPERTIES_FILE;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginRepository;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.deployment.impl.internal.builder.PolicyFileBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.util.CompilerUtils;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class PolicyTemplateDescriptorFactoryTestCase extends AbstractMuleTestCase {

  public static final String POLICY_NAME = "testPolicy";
  public static final String JAR_FILE_NAME = "test.jar";

  private static final File echoTestJarFile =
      new CompilerUtils.JarCompiler().compiling(getResourceFile("/org/foo/EchoTest.java"))
          .compile(JAR_FILE_NAME);

  private static File getResourceFile(String resource) {
    return new File(PolicyTemplateDescriptorFactoryTestCase.class.getResource(resource).getFile());
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ArtifactPluginRepository applicationPluginRepository;

  @Before
  public void setUp() throws Exception {
    applicationPluginRepository = mock(ArtifactPluginRepository.class);
    when(applicationPluginRepository.getContainerArtifactPluginDescriptors()).thenReturn(emptyList());
  }

  @Test
  public void verifiesThatPolicyDescriptorIsPresent() throws Exception {
    PolicyFileBuilder policyFileBuilder = new PolicyFileBuilder(POLICY_NAME).usingLibrary(echoTestJarFile.getAbsolutePath());
    File tempFolder = createTempFolder();
    unzip(policyFileBuilder.getArtifactFile(), tempFolder);

    PolicyTemplateDescriptorFactory descriptorFactory = new PolicyTemplateDescriptorFactory();

    expectedException.expect(ArtifactDescriptorCreateException.class);
    expectedException.expectMessage(MISSING_POLICY_PROPERTIES_FILE);
    descriptorFactory.create(tempFolder);
  }

  @Test
  public void readsRuntimeLibs() throws Exception {
    PolicyFileBuilder policyFileBuilder = new PolicyFileBuilder(POLICY_NAME).configuredWith("policy.name", POLICY_NAME)
        .usingLibrary(echoTestJarFile.getAbsolutePath());
    File tempFolder = createTempFolder();
    unzip(policyFileBuilder.getArtifactFile(), tempFolder);

    PolicyTemplateDescriptorFactory descriptorFactory = new PolicyTemplateDescriptorFactory();
    PolicyTemplateDescriptor desc = descriptorFactory.create(tempFolder);

    assertThat(desc.getClassLoaderModel().getUrls().length, equalTo(2));
    assertThat(desc.getClassLoaderModel().getUrls()[0].getFile(), equalTo(new File(tempFolder, CLASSES_DIR).toString()));
    assertThat(desc.getClassLoaderModel().getUrls()[1].getFile(),
               equalTo(new File(tempFolder, LIB_DIR + separator + JAR_FILE_NAME).toString()));
  }

  private File createTempFolder() throws IOException {
    File tempFolder = createTempFile("tempPolicy", null);
    assertThat(tempFolder.delete(), is(true));
    assertThat(tempFolder.mkdir(), is(true));
    return tempFolder;
  }
}
