/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.extension.internal.loader.enricher.BooleanParameterDeclarationEnricher.DONT_SET_DEFAULT_VALUE_TO_BOOLEAN_PARAMS;
import static org.mule.runtime.module.extension.internal.loader.java.CraftedExtensionModelLoader.TYPE_PROPERTY_NAME;

import static java.util.Optional.of;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionLoadingDelegate;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.qameta.allure.Issue;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class CraftedExtensionModelLoaderTestCase extends AbstractMuleTestCase {

  private static final String EXTENSION_NAME = "crafted extension";

  private ExtensionModelLoader loader = new CraftedExtensionModelLoader();
  private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

  @Rule
  public ExpectedException expectedException = none();

  @Mock
  private DslResolvingContext dslResolvingContext;

  private final Map<String, Object> attributes = new HashMap<>();

  @Before
  public void before() throws Exception {
    attributes.put(TYPE_PROPERTY_NAME, TestExtensionLoadingDelegate.class.getName());
    // TODO MULE-14517: This workaround should be replaced for a better and more complete mechanism
    attributes.put("COMPILATION_MODE", true);
  }

  @Test
  public void load() throws Exception {
    ExtensionModel extensionModel = loader.loadExtensionModel(classLoader, dslResolvingContext, attributes);
    assertThat(extensionModel, is(notNullValue()));
    assertThat(extensionModel.getName(), is(EXTENSION_NAME));
  }

  @Test
  public void typeNotPresent() throws Exception {
    attributes.clear();
    expectedException.expect(IllegalArgumentException.class);
    load();
  }

  @Test
  public void typeIsBlank() throws Exception {
    attributes.put(TYPE_PROPERTY_NAME, "");
    expectedException.expect(IllegalArgumentException.class);
    load();
  }

  @Test
  public void typeIsNotInstantiable() throws Exception {
    attributes.put(TYPE_PROPERTY_NAME, ExtensionLoadingDelegate.class.getName());
    expectedException.expect(IllegalArgumentException.class);
    load();
  }

  @Test
  public void typeIsOfWrongType() throws Exception {
    attributes.put(TYPE_PROPERTY_NAME, String.class.getName());
    expectedException.expect(IllegalArgumentException.class);
    load();
  }

  @Test
  @Issue("W-12003688")
  public void dontSetDefaultValueToBooleanParams() throws Exception {
    AtomicReference<ExtensionLoadingContext> contextRef = new AtomicReference<>();

    loader = new CraftedExtensionModelLoader() {

      @Override
      protected ExtensionModel doCreate(ExtensionLoadingContext context) {
        contextRef.set(context);
        return super.doCreate(context);
      }
    };

    load();
    assertThat(contextRef.get().getParameter(DONT_SET_DEFAULT_VALUE_TO_BOOLEAN_PARAMS), is(of(true)));
  }

  public static class TestExtensionLoadingDelegate implements ExtensionLoadingDelegate {

    @Override
    public void accept(ExtensionDeclarer extensionDeclarer, ExtensionLoadingContext context) {
      extensionDeclarer.named(EXTENSION_NAME)
          .describedAs("Crafted Extension")
          .onVersion("1.0.0")
          .withCategory(COMMUNITY)
          .fromVendor("Mulesoft");
    }
  }
}
