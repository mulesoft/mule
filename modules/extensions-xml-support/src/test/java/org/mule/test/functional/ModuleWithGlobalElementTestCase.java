/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mule.extension.http.api.request.validator.ResponseValidatorTypedException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

@RunnerDelegateTo(Parameterized.class)
public class ModuleWithGlobalElementTestCase extends AbstractModuleWithHttpTestCase {

  @Parameterized.Parameter
  public String configFile;

  @Parameterized.Parameter(1)
  public String[] paths;

  @Parameterized.Parameter(2)
  public boolean shouldValidate;

  @Parameterized.Parameters(name = "{index}: Running tests for {0} (validating XML [{2}]) ")
  public static Collection<Object[]> data() {
    return asList(simpleScenario(true),
                  simpleScenario(false),
                  nestedScenario(true),
                  nestedScenario(false),
                  nestedNestedScenario(true),
                  nestedNestedScenario(false),
                  literalAndExpressionScenario(true),
                  literalAndExpressionScenario(false));
  }

  private static Object[] simpleScenario(boolean shouldValidate) {
    //simple scenario
    return new Object[] {"flows/flows-using-module-global-elements.xml", new String[] {MODULE_GLOBAL_ELEMENT_XML},
        shouldValidate};
  }

  private static Object[] nestedScenario(boolean shouldValidate) {
    //nested modules scenario
    return new Object[] {"flows/nested/flows-using-module-global-elements-proxy.xml",
        new String[] {MODULE_GLOBAL_ELEMENT_XML, MODULE_GLOBAL_ELEMENT_PROXY_XML}, shouldValidate};
  }

  private static Object[] nestedNestedScenario(boolean shouldValidate) {
    //nested^2 modules scenario
    return new Object[] {"flows/nested/flows-using-module-global-elements-another-proxy.xml",
        new String[] {MODULE_GLOBAL_ELEMENT_XML, MODULE_GLOBAL_ELEMENT_PROXY_XML, MODULE_GLOBAL_ELEMENT_ANOTHER_PROXY_XML},
        shouldValidate};
  }

  private static Object[] literalAndExpressionScenario(boolean shouldValidate) {
    //using literals and expressions that will be resolved accordingly scenario
    return new Object[] {"flows/flows-using-module-global-elements-with-expressions.xml",
        new String[] {MODULE_GLOBAL_ELEMENT_XML}, shouldValidate};
  }

  @Override
  protected String[] getModulePaths() {
    return paths;
  }

  @Override
  protected String getConfigFile() {
    return configFile;
  }

  @Override
  protected boolean shouldValidateXml() {
    return shouldValidate;
  }

  @Test
  public void testConnection() throws Exception {
    ConfigurationInstance config = muleContext.getExtensionManager().getConfiguration("la-plata-config", testEvent());
    assertThat(config, is(notNullValue()));
    assertThat(config.getConnectionProvider().isPresent(), is(true));
    final ConnectionProvider connectionProvider = config.getConnectionProvider().get();
    final Object connect = connectionProvider.connect();
    final ConnectionValidationResult connectionValidationResult = connectionProvider.validate(connect);
    assertThat(connectionValidationResult.isValid(), is(true));
    connectionProvider.disconnect(connect);
  }

  @Test
  public void testHttpDoLogin() throws Exception {
    assertFlowForUsername("testHttpDoLogin", "userLP");
  }

  @Test
  public void testHttpDontLoginThrowsException() throws Exception {
    try {
      flowRunner("testHttpDontLogin").run();
      fail("Should not have reach here");
    } catch (MessagingException me) {
      Throwable cause = me.getEvent().getError().get().getCause();
      assertThat(cause, instanceOf(ResponseValidatorTypedException.class));
      assertThat(cause.getMessage(), containsString("failed: unauthorized (401)"));
    }
  }

  @Test
  public void testHttpDoLoginGonnet() throws Exception {
    assertFlowForUsername("testHttpDoLoginGonnet", "userGonnet");
  }
}
