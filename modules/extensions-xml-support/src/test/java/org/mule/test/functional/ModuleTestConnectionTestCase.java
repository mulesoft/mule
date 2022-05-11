/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static java.util.Arrays.asList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class ModuleTestConnectionTestCase extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

  @Parameterized.Parameter
  public String path;

  @Parameterized.Parameters(name = "{index}: Running tests for {0} ")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        // infers out of the box that the petstore:config element is test-contactable
        {"modules/module-test-connection.xml"},
        // takes the first element to do test connection as it's marked with xmlns:connection="true"
        {"modules/module-test-connection-multiple-configs-first.xml"},
        // takes the second element to do test connection as it's marked with xmlns:connection="true"
        {"modules/module-test-connection-multiple-configs-second.xml"}
    });
  }

  @Override
  protected String getModulePath() {
    return path;
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-test-connection-module.xml";
  }

  @Override
  protected boolean shouldValidateXml() {
    return true;
  }

  @Test
  public void testConnection() throws Exception {
    assertConnectionOn("theConfigurationNameFromTheAppThatWontBeMacroExpanded");
    assertConnectionOn("anotherConfigurationToShowThereIsNoClashOnMacroExpansion");
  }

  private void assertConnectionOn(String beanName) throws MuleException {
    ConfigurationInstance config = muleContext.getExtensionManager().getConfiguration(beanName, testEvent());
    assertThat(config, is(notNullValue()));
    assertThat(config.getConnectionProvider().isPresent(), is(true));
    final ConnectionProvider connectionProvider = config.getConnectionProvider().get();
    final Object connect = connectionProvider.connect();
    final ConnectionValidationResult connectionValidationResult = connectionProvider.validate(connect);
    assertThat(connectionValidationResult.isValid(), is(true));
    connectionProvider.disconnect(connect);
  }

  @Override
  public boolean mustRegenerateComponentBuildingDefinitionRegistryFactory() {
    // returns true because not same extensions are loaded by all tests.
    return true;
  }
}
