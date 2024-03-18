/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentLocationStory.COMPONENT_LOCATION;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.SentientSource;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SOURCES)
@Story(COMPONENT_LOCATION)
public class SourceWithComponentLocationTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "source/source-with-component-location-config.xml";
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    super.doSetUpBeforeMuleContextCreation();
    reset();
  }

  @Override
  protected void doTearDown() throws Exception {
    super.doTearDown();
    reset();
  }

  @Test
  public void injectedComponentLocation() throws Exception {
    new PollingProber(5000, 50)
        .check(new JUnitLambdaProbe(() -> {
          ComponentLocation location = SentientSource.capturedLocation;
          assertThat(location, is(notNullValue()));
          assertThat(location.getRootContainerName(), equalTo("sentient"));
          return true;
        }));
  }

  private void reset() {
    SentientSource.capturedLocation = null;
  }
}
