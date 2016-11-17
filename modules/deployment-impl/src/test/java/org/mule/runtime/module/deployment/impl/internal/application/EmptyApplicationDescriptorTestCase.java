/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.MuleServer;
import org.mule.runtime.module.deployment.impl.internal.application.EmptyApplicationDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SmallTest
public class EmptyApplicationDescriptorTestCase extends AbstractMuleTestCase {

  public static final String APP_NAME = "test-app";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private File appFolder;

  @Before
  public void createAppFolder() throws IOException {
    appFolder = temporaryFolder.newFolder(APP_NAME);
  }

  @Test
  public void defaultValuesAreCorrect() throws IOException {
    EmptyApplicationDescriptor applicationDescriptor = new EmptyApplicationDescriptor(appFolder);
    assertThat(applicationDescriptor.getName(), is(APP_NAME));
    assertThat(applicationDescriptor.getConfigResources()[0], is(MuleServer.DEFAULT_CONFIGURATION));
    String absolutePathForConfigResource = new File(appFolder, MuleServer.DEFAULT_CONFIGURATION).getAbsolutePath();
    assertThat(applicationDescriptor.getAbsoluteResourcePaths()[0], is(absolutePathForConfigResource));
    assertThat(applicationDescriptor.getConfigResourcesFile()[0].getAbsolutePath(), is(absolutePathForConfigResource));
    assertThat(applicationDescriptor.getLogConfigFile(), is(nullValue()));
  }

}
