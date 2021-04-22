/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static java.util.Arrays.asList;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.functional.junit4.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENABLE_POLLING_SOURCE_LIMIT_PARAMETER;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SourcesStories.POLLING;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

// TODO MULE-19351 migrate this test to InvalidExtensionConfigTestCase
@Feature(SOURCES)
@Story(POLLING)
@RunnerDelegateTo(Parameterized.class)
public class NegativePollingSourceLimitingTestCase extends AbstractExtensionFunctionalTestCase {

  private static final Map<String, Object> EXTENSION_LOADER_CONTEXT_ADDITIONAL_PARAMS = new HashMap<String, Object>() {

    {
      put(ENABLE_POLLING_SOURCE_LIMIT_PARAMETER, true);
    }
  };

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Parameterized.Parameter
  public String parameterizationName;

  @Parameterized.Parameter(1)
  public String configName;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object> modeParameters() {
    return asList(new Object[] {"Negative number", "source/negative-polling-source-limiting-config.xml"},
                  new Object[] {"Zero", "source/zero-polling-source-limiting-config.xml"});
  }

  @Override
  protected Map<String, Object> getExtensionLoaderContextAdditionalParameters() {
    return EXTENSION_LOADER_CONTEXT_ADDITIONAL_PARAMS;
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    // TODO MULE-10061 - Review once the MuleContext lifecycle is clearly defined
    expectedException.expect(InitialisationException.class);
    expectedException
        .expectCause(hasCause(hasCause(hasMessage("The maxItemsPerPoll parameter must have a value greater than 1"))));
  }

  @Override
  protected boolean mustRegenerateExtensionModels() {
    return true;
  }

  @Override
  protected String getConfigFile() {
    return configName;
  }

  @Test
  public void fail() {
    Assert.fail("Config should have failed to parse");
  }
}
