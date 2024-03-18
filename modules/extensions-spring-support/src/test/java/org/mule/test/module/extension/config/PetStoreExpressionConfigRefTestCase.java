/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.config;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.functional.junit4.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_DYNAMIC_CONFIG_REF_PROPERTY;
import static org.mule.test.allure.AllureConstants.Sdk.Parameters.EXPRESSIONS_ON_CONFIG_REF;
import static org.mule.test.allure.AllureConstants.Sdk.SDK;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.PetStoreConnector;

import java.util.List;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.ClassRule;
import org.junit.Test;

@Feature(SDK)
@Story(EXPRESSIONS_ON_CONFIG_REF)
public class PetStoreExpressionConfigRefTestCase extends AbstractExtensionFunctionalTestCase {

  @ClassRule
  public static SystemProperty enableDynamicConfigRef = new SystemProperty(ENABLE_DYNAMIC_CONFIG_REF_PROPERTY, "true");

  @Override
  protected String getConfigFile() {
    return "petstore-expression-config-ref.xml";
  }

  @Test
  @Description("When using a regular static reference it works as always")
  public void getPetsWithReference() throws Exception {
    CoreEvent response = runFlow("getPetsWithReference");
    List<String> pets = (List<String>) response.getMessage().getPayload().getValue();

    ConfigurationInstance config = muleContext.getExtensionManager().getConfiguration("paw-patrol-store", testEvent());
    assertThat(config, is(notNullValue()));

    PetStoreConnector configValue = (PetStoreConnector) config.getValue();
    assertThat(pets, containsInAnyOrder(configValue.getPets().toArray()));
  }

  @Test
  @Description("When using an expression involving variables from the event, it resolves the right configuration")
  public void getPetsWithExpression() throws Exception {
    CoreEvent response = flowRunner("getPetsWithExpression").withVariable("storeName", "paw-patrol").run();
    List<String> pets = (List<String>) response.getMessage().getPayload().getValue();

    ConfigurationInstance config = muleContext.getExtensionManager().getConfiguration("paw-patrol-store", testEvent());
    assertThat(config, is(notNullValue()));

    PetStoreConnector configValue = (PetStoreConnector) config.getValue();
    assertThat(pets, containsInAnyOrder(configValue.getPets().toArray()));
  }

  @Test
  @Description("When using an expression resolving to a global element that is not a config it fails (in runtime) with a proper error message")
  public void getPetsWithExpressionResolvingToNonConfigFails() throws Exception {
    String expectedMessage = "Error resolving configuration for component 'getPetsWithExpressionResolvingToNonConfig'";
    String expectedCauseMessage = "There is no registered configurationProvider under name 'some-non-config'";
    flowRunner("getPetsWithExpressionResolvingToNonConfig")
        .runExpectingException(allOf(instanceOf(IllegalArgumentException.class), hasMessage(expectedMessage),
                                     hasCause(hasMessage(expectedCauseMessage))));
  }

  @Test
  @Description("When using an expression resolving to a global element that is a config of an incompatible type it fails (in runtime) with a proper error message")
  public void getPetsWithExpressionResolvingToIncompatibleConfigFails() throws Exception {
    String expectedMessage = "Root component 'getPetsWithExpressionResolvingToIncompatibleConfig' defines an usage of " +
        "operation 'getPets' which points to configuration 'some-incompatible-config'. The selected config does not " +
        "support that operation.";
    flowRunner("getPetsWithExpressionResolvingToIncompatibleConfig")
        .runExpectingException(allOf(instanceOf(IllegalArgumentException.class), hasMessage(expectedMessage)));
  }

  @Test
  @Description("When using an expression resolving to an empty string it fails (in runtime) with a proper error message")
  public void getPetsWithExpressionResolvingToEmptyStringFails() throws Exception {
    String expectedMessage = "cannot get configuration from a blank provider name";
    flowRunner("getPetsWithExpressionResolvingToEmptyString")
        .runExpectingException(allOf(instanceOf(IllegalArgumentException.class), hasMessage(expectedMessage)));
  }

  @Override
  protected boolean mustRegenerateExtensionModels() {
    return true;
  }
}
