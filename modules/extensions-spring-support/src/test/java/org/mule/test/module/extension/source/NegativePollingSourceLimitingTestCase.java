/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.functional.junit4.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_SDK_POLLING_SOURCE_LIMIT;

import org.junit.ClassRule;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;

import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.extension.InvalidExtensionConfigTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

@RunnerDelegateTo(Parameterized.class)
public class NegativePollingSourceLimitingTestCase extends InvalidExtensionConfigTestCase {

  @ClassRule
  public static SystemProperty enableLimit = new SystemProperty(ENABLE_SDK_POLLING_SOURCE_LIMIT, "true");

  @Override
  protected boolean mustRegenerateExtensionModels() {
    return true;
  }

  @Parameterized.Parameter
  public String parameterizationName;

  @Parameterized.Parameter(1)
  public String configName;


  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object> modeParameters() {
    return asList(new Object[] {"Negative number", "negative-polling-source-limiting-config.xml"},
                  new Object[] {"Zero", "zero-polling-source-limiting-config.xml"});
  }

  @Override
  protected void additionalExceptionAssertions(ExpectedException expectedException) {
    expectedException
        .expectCause(hasCause(hasCause(hasMessage("The maxItemsPerPoll parameter must have a value greater than 1"))));
  }

  @Override
  protected String getConfigFile() {
    return configName;
  }

}
