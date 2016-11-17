/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.domain;

import static java.lang.String.format;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.util.FileUtils.newFile;

import org.mule.runtime.module.deployment.impl.internal.AbstractSplashScreenTestCase;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;

import java.io.File;
import java.io.IOException;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.BeforeClass;

public class DomainStartedSplashScreenTestCase extends AbstractSplashScreenTestCase<DomainStartedSplashScreen> {

  private static final String DOMAIN_NAME = "simpleDomain";
  private static final String DOMAIN_LIB_PATH = format("domains/%s/lib", DOMAIN_NAME);
  private static final String SOME_JAR = "some.jar";
  private static final String MY_ZIP = "myZip.zip";
  private DomainDescriptor descriptor = mock(DomainDescriptor.class);

  @BeforeClass
  public static void setUpLibrary() throws IOException {
    File libFile = newFile(workingDirectory.getRoot(), DOMAIN_LIB_PATH);
    libFile.mkdirs();
    newFile(workingDirectory.getRoot(), getDomainPathFor(SOME_JAR)).mkdir();
    newFile(workingDirectory.getRoot(), getDomainPathFor(MY_ZIP)).mkdir();
  }

  @Before
  public void setUp() {
    splashScreen = new DomainStartedSplashScreen();
    when(descriptor.getName()).thenReturn(DOMAIN_NAME);
  }

  @Override
  protected Matcher<String> getSimpleLogMatcher() {
    return is("\n**********************************************************************\n" + "* Started domain '" + DOMAIN_NAME
        + "'                                      *\n"
        + "**********************************************************************");
  }

  @Override
  protected Matcher<String> getComplexLogMatcher() {
    return is("\n**********************************************************************\n" + "* Started domain '" + DOMAIN_NAME
        + "'                                      *\n"
        + "* Domain libraries:                                                  *\n" + "*  - " + SOME_JAR
        + "                                                        *\n"
        + "**********************************************************************");
  }

  @Override
  protected void setUpSplashScreen() {
    splashScreen.createMessage(descriptor);
  }

  private static String getDomainPathFor(String fileName) {
    return format("domains/%s/lib/%s", DOMAIN_NAME, fileName);
  }
}
