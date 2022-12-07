/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.functional.junit4.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_DYNAMIC_CONFIG_REF_PROPERTY;
import static org.mule.test.allure.AllureConstants.Sdk.Parameters.EXPRESSIONS_ON_CONFIG_REF;
import static org.mule.test.allure.AllureConstants.Sdk.SDK;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.rule.SystemProperty;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.ClassRule;
import org.junit.Test;

@Feature(SDK)
@Story(EXPRESSIONS_ON_CONFIG_REF)
public class ExpressionsOnConfigRefTestCase extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

  @ClassRule
  public static SystemProperty enableDynamicConfigRef = new SystemProperty(ENABLE_DYNAMIC_CONFIG_REF_PROPERTY, "true");

  public static class SomeTestProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      // Does nothing
      return event;
    }
  }

  @Override
  protected String getModulePath() {
    return "modules/module-properties.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-expressions-on-config-ref.xml";
  }

  @Test
  @Description("When using a regular static reference it works as always")
  public void setPayloadWithReference() throws Exception {
    CoreEvent response = runFlow("setPayloadWithReference");
    assertThat(response.getMessage().getPayload().getValue(), is("some config-value-parameter"));
  }

  @Test
  @Description("When using an expression involving variables from the event, it resolves the right configuration")
  public void setPayloadWithExpression() throws Exception {
    CoreEvent response = flowRunner("setPayloadWithExpression").withVariable("configName", "some-other").run();
    assertThat(response.getMessage().getPayload().getValue(), is("some other config-value-parameter"));
  }

  @Test
  @Description("When using an expression resolving to a global element that is not a config it fails (in runtime) with a proper error message")
  public void setPayloadWithExpressionResolvingToNonConfigFails() throws Exception {
    String expectedMessage = "Error resolving configuration for component 'setPayloadWithExpressionResolvingToNonConfig'";
    String expectedCauseMessage = "There is no registered configurationProvider under name 'some-non-config'";
    flowRunner("setPayloadWithExpressionResolvingToNonConfig")
        .runExpectingException(allOf(instanceOf(IllegalArgumentException.class), hasMessage(expectedMessage),
                                     hasCause(hasMessage(expectedCauseMessage))));
  }

  @Test
  @Description("When using an expression resolving to a global element that is a config of an incompatible type it fails (in runtime) with a proper error message")
  public void setPayloadWithExpressionResolvingToIncompatibleConfigFails() throws Exception {
    String expectedMessage = "Root component 'setPayloadWithExpressionResolvingToIncompatibleConfig' defines an usage of " +
        "operation 'set-payload-config-param-value' which points to configuration 'some-incompatible-config'. " +
        "The selected config does not support that operation.";
    flowRunner("setPayloadWithExpressionResolvingToIncompatibleConfig")
        .runExpectingException(allOf(instanceOf(IllegalArgumentException.class), hasMessage(expectedMessage)));
  }

  @Test
  @Description("When using an expression resolving to an empty string it fails (in runtime) with a proper error message")
  public void setPayloadWithExpressionResolvingToEmptyStringFails() throws Exception {
    String expectedMessage = "cannot get configuration from a blank provider name";
    flowRunner("setPayloadWithExpressionResolvingToEmptyString")
        .runExpectingException(allOf(instanceOf(IllegalArgumentException.class), hasMessage(expectedMessage)));
  }
}
