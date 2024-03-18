/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager.jdk;

import static org.mule.runtime.api.util.JavaConstants.JAVA_VERSION_11;
import static org.mule.runtime.api.util.JavaConstants.JAVA_VERSION_17;
import static org.mule.runtime.api.util.JavaConstants.JAVA_VERSION_8;

import static java.util.stream.Collectors.toCollection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.internal.util.version.JdkVersionUtils.JdkVersion;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.LinkedHashSet;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public abstract class BaseExtensionJdkValidatorTestCase extends AbstractMuleTestCase {

  protected ExtensionJdkValidator validator;
  protected ExtensionModel extensionModel;

  protected JdkVersion jdk8 = new JdkVersion(JAVA_VERSION_8);
  protected JdkVersion jdk11 = new JdkVersion(JAVA_VERSION_11);
  protected JdkVersion jdk17 = new JdkVersion(JAVA_VERSION_17);
  protected JdkVersion jdk21 = new JdkVersion("21");

  protected abstract ExtensionJdkValidator validatorFor(JdkVersion jdkVersion);

  protected abstract void assertFailure(Runnable task);

  @Before
  public void before() {
    extensionModel = mock(ExtensionModel.class);
    when(extensionModel.getName()).thenReturn("Test Extension");
    mockSupportsJavaVersions(JAVA_VERSION_8, JAVA_VERSION_11, JAVA_VERSION_17);
  }

  @Test
  public void supportedJdks() {
    validatorFor(jdk8).validateJdkSupport(extensionModel);
    validatorFor(jdk11).validateJdkSupport(extensionModel);
    validatorFor(jdk17).validateJdkSupport(extensionModel);
  }

  @Test
  public void unsupportedJdk() {
    assertFailure(() -> validatorFor(jdk21).validateJdkSupport(extensionModel));
  }

  protected void mockSupportsJavaVersions(String... versions) {
    when(extensionModel.getSupportedJavaVersions()).thenReturn(Stream.of(versions).collect(toCollection(LinkedHashSet::new)));
  }

}
