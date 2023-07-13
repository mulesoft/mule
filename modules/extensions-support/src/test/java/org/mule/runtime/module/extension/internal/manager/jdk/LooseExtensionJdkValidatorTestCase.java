/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager.jdk;

import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.internal.util.JdkVersionUtils;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
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
    System.out.println("MESSAEG: >>>>" + captor.getValue());
  }
}
