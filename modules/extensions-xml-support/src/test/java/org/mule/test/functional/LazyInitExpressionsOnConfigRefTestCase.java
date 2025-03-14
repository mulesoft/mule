/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.test.allure.AllureConstants.LazyInitializationFeature.LAZY_INITIALIZATION;
import static org.mule.test.allure.AllureConstants.Sdk.SDK;
import static org.mule.test.allure.AllureConstants.Sdk.Parameters.EXPRESSIONS_ON_CONFIG_REF;

import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.config.api.LazyComponentInitializer;

import jakarta.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;

@Features({@Feature(SDK), @Feature(LAZY_INITIALIZATION)})
@Story(EXPRESSIONS_ON_CONFIG_REF)
public class LazyInitExpressionsOnConfigRefTestCase extends ExpressionsOnConfigRefTestCase {

  @Inject
  private LazyComponentInitializer lazyComponentInitializer;

  @Override
  public boolean enableLazyInit() {
    return true;
  }

  @Override
  protected FlowRunner flowRunner(String flowName) {
    // Initializes just the flow we are trying to run
    lazyComponentInitializer.initializeComponent(builderFromStringRepresentation(flowName).build());
    return super.flowRunner(flowName);
  }
}
