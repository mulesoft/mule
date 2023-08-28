/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager.jdk;

import static org.mule.test.allure.AllureConstants.Sdk.SDK;
import static org.mule.test.allure.AllureConstants.Sdk.SupportedJavaVersions.ENFORCE_EXTENSION_JAVA_VERSION;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.internal.util.JdkVersionUtils;
import org.mule.tck.size.SmallTest;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
@Feature(SDK)
@Story(ENFORCE_EXTENSION_JAVA_VERSION)
public class LooseExtensionJdkValidatorTestCase extends BaseExtensionJdkValidatorTestCase {

  @Mock
  private Logger logger;

  @Override
  public void before() {
    super.before();
    when(logger.isWarnEnabled()).thenReturn(true);
  }

  @Override
  protected ExtensionJdkValidator validatorFor(JdkVersionUtils.JdkVersion jdkVersion) {
    return new LooseExtensionJdkValidator(jdkVersion, logger);
  }

  @Override
  @Test
  public void supportedJdks() {
    super.supportedJdks();
    verify(logger, never()).warn(anyString());
  }

  @Override
  protected void assertFailure(Runnable task) {
    task.run();
    ArgumentCaptor<String> captor = forClass(String.class);
    verify(logger).warn(captor.capture());
    assertThat(captor.getValue(),
               equalTo("Extension 'Test Extension' does not support Java 21. Supported versions are: [1.8, 11, 17]"));
  }
}
