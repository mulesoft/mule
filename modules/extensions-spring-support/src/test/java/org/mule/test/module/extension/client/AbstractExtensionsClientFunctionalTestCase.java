/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.client;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
@Feature("EXTENSIONS_CLIENT")
public abstract class AbstractExtensionsClientFunctionalTestCase extends AbstractExtensionFunctionalTestCase {

  private static String MESSAGE_TO_ECHO = "Echo this message!";
  private static String ANOTHER_MESSAGE_TO_ECHO = "Echo this another message!";
  private static String HEISENBERG = "Heisenberg";
  private static String PETSTORE = "Petstore";
  private static String DYNAMIC_DEALER_NAME = "dynamic dealer";
  private static String ANOTHER_DYNAMIC_DEALER_NAME = "another dynamic dealer";
  private static String STATIC_DEALER_NAME_ONE = "Domingo 'Krazy-8' Molina";
  private static String STATIC_DEALER_NAME_TWO = "Skinny Pete";
  private static String DYNAMIC_CONFIG_NAME = "dynamic-heisenberg";
  private static String STATIC_CONFIG_NAME_ONE = "static-heisenberg1";
  private static String STATIC_CONFIG_NAME_TWO = "static-heisenberg2";
  private static String MESSAGELESS_ECHO = " echoed by Heisenberg";
  private static int NUMBER_OF_CALLS = 1000;


  @Parameterized.Parameter(0)
  public String parameterizationName;

  @Parameterized.Parameter(1)
  public String configName;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"Using Extensions API", "extensions-client-config.xml"},
        {"Using SDK API", "sdk-extensions-client-config.xml"},
    });
  }

  @Override
  protected String getConfigFile() {
    return configName;
  }

  @Test
  @Description("Verifies the call to the echo operation in the Heisenberg Extension")
  public void testHeisenbergEcho() throws Exception {
    String echoedMessage =
        (String) flowRunner("echoHeisenberg").withVariable("message", MESSAGE_TO_ECHO).run().getMessage().getPayload().getValue();
    assertThat(echoedMessage, containsString(MESSAGE_TO_ECHO));
    assertThat(echoedMessage, containsString(HEISENBERG));
  }

  @Test
  @Description("Verifies the call to the echo operation in the Pet Store Extension")
  public void testPetStoreEcho() throws Exception {
    String echoedMessage =
        (String) flowRunner("echoPetStore").withVariable("message", MESSAGE_TO_ECHO).run().getMessage().getPayload().getValue();
    assertThat(echoedMessage, containsString(MESSAGE_TO_ECHO));
    assertThat(echoedMessage, containsString(PETSTORE));
  }

  @Test
  @Description("Verifies calling two operations with the same name but from different extensions")
  public void testOperationsWithTheSameName() throws Exception {
    testHeisenbergEcho();
    testPetStoreEcho();
    testHeisenbergEcho();
    testPetStoreEcho();
  }

  @Test
  @Description("Verifies the call to an operation using a dynamic configuration Extension")
  public void testDynamicConfig() throws Exception {
    String nameSaid =
        (String) flowRunner("sayMyName").withVariable("configName", DYNAMIC_CONFIG_NAME)
            .withVariable("dealerName", DYNAMIC_DEALER_NAME).run().getMessage().getPayload().getValue();
    assertThat(nameSaid, is(DYNAMIC_DEALER_NAME));
  }

  @Test
  @Description("Verifies the call to an operation using another a dynamic configuration Extension")
  public void testAnotherDynamicConfig() throws Exception {
    String nameSaid =
        (String) flowRunner("sayMyName").withVariable("configName", DYNAMIC_CONFIG_NAME)
            .withVariable("dealerName", ANOTHER_DYNAMIC_DEALER_NAME).run().getMessage().getPayload().getValue();
    assertThat(nameSaid, is(ANOTHER_DYNAMIC_DEALER_NAME));
  }

  @Test
  @Description("Verifies the call to the same operation using the same configuration but resolves to different values")
  public void testDynamicConfigCalledTwice() throws Exception {
    testDynamicConfig();
    testAnotherDynamicConfig();
    testDynamicConfig();
    testAnotherDynamicConfig();
  }

  @Test
  @Description("Verifies a call to an operation with a static configuration")
  public void testStaticConfig() throws Exception {
    String nameSaid =
        (String) flowRunner("sayMyName").withVariable("configName", STATIC_CONFIG_NAME_ONE).run().getMessage().getPayload()
            .getValue();
    assertThat(nameSaid, is(STATIC_DEALER_NAME_ONE));
  }

  @Test
  @Description("Verifies a call to an operation with another static configuration")
  public void testAnotherStaticConfig() throws Exception {
    String nameSaid =
        (String) flowRunner("sayMyName").withVariable("configName", STATIC_CONFIG_NAME_TWO).run().getMessage().getPayload()
            .getValue();
    assertThat(nameSaid, is(STATIC_DEALER_NAME_TWO));
  }

  @Test
  @Description("Verifies calls to an operation twice with different static configurations")
  public void testCallTwiceWithDifferentStaticConfigs() throws Exception {
    testStaticConfig();
    testAnotherStaticConfig();
    testStaticConfig();
    testAnotherStaticConfig();
  }

  @Test
  @Description("Calls the client to execute an operation with a parameter that do not support expressions")
  public void testParameterThatDoNotSupportExpressions() throws Exception {
    String echoedMessage =
        (String) flowRunner("echoStaticMessage").withVariable("message", MESSAGE_TO_ECHO).run().getMessage().getPayload()
            .getValue();
    assertThat(echoedMessage, containsString(MESSAGE_TO_ECHO));
  }

  @Test
  @Description("Calls the client to execute an operation with a parameter that do not support expressions")
  public void testAnotherParameterThatDoNotSupportExpressions() throws Exception {
    String echoedMessage =
        (String) flowRunner("echoStaticMessage").withVariable("message", ANOTHER_MESSAGE_TO_ECHO).run().getMessage().getPayload()
            .getValue();
    assertThat(echoedMessage, containsString(ANOTHER_MESSAGE_TO_ECHO));
  }

  @Test
  @Description("Calls the client to execute an operation with a parameter that do not support expressions")
  public void testParameterThatDoNotSupportExpressionsTwice() throws Exception {
    testParameterThatDoNotSupportExpressions();
    testAnotherParameterThatDoNotSupportExpressions();
    testParameterThatDoNotSupportExpressions();
    testAnotherParameterThatDoNotSupportExpressions();
  }

  @Test
  @Description("Verifies calling an operation without an optional parameter")
  public void testOptionalParameter() throws Exception {
    String echoedMessage =
        (String) flowRunner("echoWithoutMessage").run().getMessage().getPayload().getValue();
    assertThat(echoedMessage, is(MESSAGELESS_ECHO));
  }

  @Test
  @Description("Verifies that the client remains consistent after multiple calls")
  public void testMultipleCalls() throws Exception {
    for (int i = 0; i < NUMBER_OF_CALLS; i++) {
      testCallTwiceWithDifferentStaticConfigs();
      testDynamicConfigCalledTwice();
      testOperationsWithTheSameName();
      testParameterThatDoNotSupportExpressionsTwice();
      testOptionalParameter();
    }
  }

}
