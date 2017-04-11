/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static java.util.Collections.emptySet;
import static org.assertj.core.util.Arrays.isNullOrEmpty;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.api.meta.model.tck.TestCoreExtensionDeclarer.CHOICE_OPERATION_NAME;
import static org.mule.runtime.api.meta.model.tck.TestCoreExtensionDeclarer.EXTENSION_DESCRIPTION;
import static org.mule.runtime.api.meta.model.tck.TestCoreExtensionDeclarer.EXTENSION_NAME;
import static org.mule.runtime.api.meta.model.tck.TestCoreExtensionDeclarer.FOREACH_EXPRESSION_PARAMETER_NAME;
import static org.mule.runtime.api.meta.model.tck.TestCoreExtensionDeclarer.FOREACH_OPERATION_NAME;
import static org.mule.runtime.api.meta.model.tck.TestCoreExtensionDeclarer.OTHERWISE_ROUTE_NAME;
import static org.mule.runtime.api.meta.model.tck.TestCoreExtensionDeclarer.VENDOR;
import static org.mule.runtime.api.meta.model.tck.TestCoreExtensionDeclarer.VERSION;
import static org.mule.runtime.api.meta.model.tck.TestCoreExtensionDeclarer.WHEN_ROUTE_NAME;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.assertType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.Stereotype;
import org.mule.runtime.api.meta.model.operation.RouteModel;
import org.mule.runtime.api.meta.model.operation.RouterModel;
import org.mule.runtime.api.meta.model.operation.ScopeModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.tck.TestCoreExtensionDeclarer;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionDeclarationTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class CoreExtensionDeclarationTestCase extends AbstractJavaExtensionDeclarationTestCase {

  private ExtensionModel extensionModel;

  @Before
  public void before() {
    extensionModel = new ExtensionModelLoader() {

      @Override
      public String getId() {
        return "test";
      }

      @Override
      protected void declareExtension(ExtensionLoadingContext context) {
        new TestCoreExtensionDeclarer().declareOn(context.getExtensionDeclarer());
      }
    }.loadExtensionModel(getClass().getClassLoader(), getDefault(emptySet()), new HashMap<>());
  }

  @Test
  public void assertDeclaration() {
    assertThat(extensionModel.getName(), is(EXTENSION_NAME));
    assertThat(extensionModel.getDescription(), is(EXTENSION_DESCRIPTION));
    assertThat(extensionModel.getVersion(), is(VERSION));
    assertThat(extensionModel.getConfigurationModels(), hasSize(0));
    assertThat(extensionModel.getVendor(), is(VENDOR));
    assertThat(extensionModel.getOperationModels(), hasSize(2));
    assertThat(extensionModel.getConnectionProviders(), is(empty()));
    assertThat(extensionModel.getSourceModels(), is(empty()));
  }

  @Test
  public void choiceRouter() {
    RouterModel choice = (RouterModel) extensionModel.getOperationModel(CHOICE_OPERATION_NAME).get();
    assertThat(choice.getAllParameterModels(), hasSize(0));

    List<RouteModel> routes = choice.getRouteModels();
    assertThat(routes, hasSize(2));
    assertRoute(routes.get(0), WHEN_ROUTE_NAME, 1, null);
    assertRoute(routes.get(1), OTHERWISE_ROUTE_NAME, 0, 1);
  }

  @Test
  public void foreachScope() {
    ScopeModel foreach = (ScopeModel) extensionModel.getOperationModel(FOREACH_OPERATION_NAME).get();
    assertThat(foreach.getAllParameterModels(), hasSize(1));
    ParameterModel parameter = foreach.getAllParameterModels().get(0);

    assertThat(parameter.getName(), is(FOREACH_EXPRESSION_PARAMETER_NAME));
    assertType(parameter.getType(), String.class, StringType.class);

  }

  private void assertRoute(RouteModel route, String name, int minOccurs, Integer maxOccurs, Stereotype... stereotypes) {
    assertThat(route.getName(), is(name));
    assertThat(route.getMinOccurs(), is(minOccurs));
    if (maxOccurs != null) {
      assertThat(route.getMaxOccurs().get(), is(maxOccurs));
    } else {
      assertThat(route.getMaxOccurs().isPresent(), is(false));
    }

    if (isNullOrEmpty(stereotypes)) {
      assertThat(route.getAllowedStereotypes().isPresent(), is(false));
    } else {
      assertThat(route.getAllowedStereotypes().get(), containsInAnyOrder(stereotypes));
    }
  }
}
