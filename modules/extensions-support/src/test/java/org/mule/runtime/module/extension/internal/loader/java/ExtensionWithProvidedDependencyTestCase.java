/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.loadExtension;
import static org.mule.test.allure.AllureConstants.Sdk.MinMuleVersion.MIN_MULE_VERSION;
import static org.mule.test.allure.AllureConstants.Sdk.SDK;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.test.provided.dependency.ProvidedDependencyExtension;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(SDK)
@Story(MIN_MULE_VERSION)
@Issue("W-14645134")
public class ExtensionWithProvidedDependencyTestCase {

  @Test
  @Description("Tests that the model of an extension that has a custom type that relies on types from a dependency" +
      "declared as provided, and has that custom type used as a parameter in an operation, is correctly loaded and" +
      "the Min Mule Version of the component has the right value")
  public void extensionWithParametersRelyingOnProvidedDependencyTypesIsLoaded() {
    ExtensionModel extension = loadExtension(ProvidedDependencyExtension.class);

    assertThat(extension.getName(), is(ProvidedDependencyExtension.NAME));
    assertThat(extension.getMinMuleVersion().isPresent(), is(true));
    assertThat(extension.getMinMuleVersion().get().toString(), is("4.1.1"));

    assertThat(extension.getConfigurationModel("config").isPresent(), is(true));
    ConfigurationModel configurationModel = extension.getConfigurationModel("config").get();

    OperationModel operationModel =
        configurationModel.getOperationModel("dummyOperation")
            .orElseThrow(() -> new RuntimeException("'dummyOperation' not found"));
    assertThat(operationModel.getMinMuleVersion().isPresent(), is(true));
    assertThat(operationModel.getMinMuleVersion().get().toString(), is("4.1.1"));
  }

}
