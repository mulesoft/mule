/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.operation;

import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.test.allure.AllureConstants.LazyInitializationFeature.LAZY_INITIALIZATION;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;
import static org.mule.test.marvel.model.Villain.KABOOM;

import static java.util.Optional.empty;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.test.marvel.ironman.IronMan;

import jakarta.inject.Inject;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;

import org.junit.Test;

@Features({@Feature(REUSE), @Feature(LAZY_INITIALIZATION)})
@Story(OPERATIONS)
public class LazyInitMuleOperationExecutionTestCase extends MuleOperationExecutionTestCase {

  @Inject
  private LazyComponentInitializer lazyComponentInitializer;

  @Override
  public boolean enableLazyInit() {
    return true;
  }

  @Test
  @Override
  @Description("Calls a flow that executes the <this:salute-aggressively> operation which is configurable")
  public void executeConfigurableOperation() throws Exception {
    // Because this is a lazy init test case, we change the "no missiles fired" assertion for a "no component with location"
    // assertion
    assertThat(locator.find(builderFromStringRepresentation("ironMan").build()), is(empty()));

    CoreEvent resultEvent = flowRunner("configurableOperationFlow").run();
    String result = (String) resultEvent.getMessage().getPayload().getValue();
    assertThat(result, is(KABOOM));

    // Retrieving the config for the "one missile fired" assertion can be done now that the flow initialization has also
    // initialized the configuration dependency
    ConfigurationInstance config = muleContext.getExtensionManager().getConfiguration("ironMan", testEvent());
    assertThat(config, is(notNullValue()));
    IronMan ironManConfig = (IronMan) config.getValue();
    assertThat(ironManConfig.getMissilesFired(), is(1));
  }

  @Override
  protected FlowRunner flowRunner(String flowName) {
    // Initializes just the flow we are trying to run
    lazyComponentInitializer.initializeComponent(builderFromStringRepresentation(flowName).build());
    return super.flowRunner(flowName);
  }
}
