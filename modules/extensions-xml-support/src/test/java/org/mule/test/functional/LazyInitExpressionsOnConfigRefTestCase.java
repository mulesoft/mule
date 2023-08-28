/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.functional;

import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.test.allure.AllureConstants.LazyInitializationFeature.LAZY_INITIALIZATION;
import static org.mule.test.allure.AllureConstants.Sdk.Parameters.EXPRESSIONS_ON_CONFIG_REF;
import static org.mule.test.allure.AllureConstants.Sdk.SDK;

import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.config.api.LazyComponentInitializer;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;
import org.junit.Ignore;
import org.junit.Test;

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
