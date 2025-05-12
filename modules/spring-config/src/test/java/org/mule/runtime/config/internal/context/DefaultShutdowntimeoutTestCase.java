/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context;

import static org.mule.runtime.api.util.MuleSystemProperties.GRACEFUL_SHUTDOWN_DEFAULT_TIMEOUT;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.getExtensionModel;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.GracefulShutdownStory.GRACEFUL_SHUTDOWN_STORY;

import static java.util.Collections.singleton;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.config.utils.AppParserConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Set;

import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(GRACEFUL_SHUTDOWN_STORY)
public class DefaultShutdowntimeoutTestCase extends AbstractMuleContextTestCase {

  @Rule
  public SystemProperty gracefulShutdownDefaultTimeout = new SystemProperty(GRACEFUL_SHUTDOWN_DEFAULT_TIMEOUT, "1234");

  @Override
  protected Set<ExtensionModel> getExtensionModels() {
    return singleton(getExtensionModel());
  }

  @Override
  protected ConfigurationBuilder getBuilder() throws Exception {
    return new AppParserConfigurationBuilder(new String[] {"./simple.xml"});
  }

  @Test
  @Description("Verify that the dsl/extModel do not have a default value that overrides the one from the environment configuration.")
  public void defaultShutdownTimeoutOverride() {
    assertThat(muleContext.getConfiguration().getShutdownTimeout(), is(1234L));
  }

  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }
}
