/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationState;
import org.mule.runtime.extension.api.runtime.config.ConfiguredComponent;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Test;

import io.qameta.allure.Feature;

@Feature(SOURCES)
public class ConnectionlessMessageSourceTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "source/connectionless-message-source.xml";
  }

  @Test
  public void obtainDisconnectedSourceConfigParameters() throws Exception {
    Component element = locator.find(Location.builder().globalName("source").addSourcePart().build()).get();
    assertThat(element, is(instanceOf(ConfiguredComponent.class)));

    final ConfigurationInstance configurationInstance = ((ConfiguredComponent) element).getConfigurationInstance().get();
    ConfigurationState configurationState = configurationInstance.getState();

    assertThat(configurationState.getConfigParameters().size(), is(0));
    assertThat(configurationState.getConnectionParameters().size(), is(0));
  }



}
