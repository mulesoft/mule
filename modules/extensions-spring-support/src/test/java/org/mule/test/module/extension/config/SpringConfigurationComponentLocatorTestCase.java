/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.DefaultComponentIdentifier;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;

public class SpringConfigurationComponentLocatorTestCase extends AbstractExtensionFunctionalTestCase {

  @Inject
  public ConfigurationComponentLocator locator;

  @Override
  protected String getConfigFile() {
    return "scopes/heisenberg-scope-config.xml";
  }

  @Override
  protected boolean doTestClassInjection() {
    return true;
  }

  @Test
  @Issue("W-16039259")
  @Description("Checks that trying to find by component identifier works for scopes implemented with the Java SDK")
  public void findByComponentIdentifier() {
    ComponentIdentifier identifier =
        new DefaultComponentIdentifier.Builder().name("execute-anything").namespace("heisenberg").build();
    List<Component> components = locator.find(identifier);
    assertThat(components, hasSize(2));
    List<String> locations = components.stream().map(comp -> comp.getLocation().getLocation()).collect(Collectors.toList());
    assertThat(locations, containsInAnyOrder("executeNonBlocking/processors/0", "executeAnything/processors/0"));
  }

}
