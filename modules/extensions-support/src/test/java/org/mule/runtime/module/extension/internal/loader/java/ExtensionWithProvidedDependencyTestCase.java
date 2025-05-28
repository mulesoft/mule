/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;
import static org.mule.runtime.manifest.api.MuleManifest.getMuleManifest;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.VERSION;
import static org.mule.test.allure.AllureConstants.Sdk.SDK;
import static org.mule.test.allure.AllureConstants.Sdk.MinMuleVersion.MIN_MULE_VERSION;

import static java.util.Collections.singleton;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider;
import org.mule.test.provided.dependency.ProvidedDependencyExtension;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(SDK)
@Story(MIN_MULE_VERSION)
@Issue("W-14645134")
public class ExtensionWithProvidedDependencyTestCase {

  @Test
  @Description("Tests that a component that internally relies on a class from a provided dependency doesn't break MMV " +
      "resolution due to the dependency not being available at design time")
  public void extensionWithParametersRelyingOnProvidedDependencyTypesIsLoaded() {
    ExtensionModel extension = loadExtension(ProvidedDependencyExtension.class, new SmallMap<>());

    assertThat(extension.getName(), is(ProvidedDependencyExtension.NAME));
    assertThat(extension.getMinMuleVersion().isPresent(), is(true));
    assertThat(extension.getMinMuleVersion().get().toString(), is("4.1.0"));

    assertThat(extension.getConfigurationModel("config").isPresent(), is(true));
    ConfigurationModel configurationModel = extension.getConfigurationModel("config").get();

    OperationModel operationModel =
        configurationModel.getOperationModel("dummyOperation")
            .orElseThrow(() -> new RuntimeException("'dummyOperation' not found"));
    assertThat(operationModel.getMinMuleVersion().isPresent(), is(true));
    assertThat(operationModel.getMinMuleVersion().get().toString(), is("4.1.0"));
  }

  public static ExtensionModel loadExtension(Class<?> clazz, Map<String, Object> params) {
    params.put(TYPE_PROPERTY_NAME, clazz.getName());
    params.put(VERSION, getMuleManifest().getProductVersion());
    // TODO MULE-11797: as this utils is consumed from
    // org.mule.runtime.module.extension.internal.capability.xml.schema.AbstractXmlResourceFactory.generateResource(org.mule.runtime.api.meta.model.ExtensionModel),
    // this util should get dropped once the ticket gets implemented.
    final DslResolvingContext dslResolvingContext = getDefault(singleton(MuleExtensionModelProvider.getExtensionModel()));
    return new DefaultJavaExtensionModelLoader().loadExtensionModel(builder(clazz.getClassLoader(), dslResolvingContext)
        .setResolveMinMuleVersion(true)
        .addParameters(params)
        .build());
  }

}
