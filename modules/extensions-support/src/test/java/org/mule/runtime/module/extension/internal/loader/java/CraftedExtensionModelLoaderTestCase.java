/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.module.extension.api.loader.java.CraftedExtensionModelLoader.TYPE_PROPERTY_NAME;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionLoadingDelegate;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.api.loader.java.CraftedExtensionModelLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class CraftedExtensionModelLoaderTestCase extends AbstractMuleTestCase {

  private static final String EXTENSION_NAME = "crafted extension";

  private ExtensionModelLoader loader = new CraftedExtensionModelLoader();
  private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

  @Rule
  public ExpectedException expectedException = none();

  @Mock
  private DslResolvingContext dslResolvingContext;

  private Map<String, Object> attributes = new HashMap<>();

  @Before
  public void before() throws Exception {
    attributes.put(TYPE_PROPERTY_NAME, TestExtensionLoadingDelegate.class.getName());
  }

  @Test
  public void load() throws Exception {
    ExtensionModel extensionModel = loader.loadExtensionModel(classLoader, dslResolvingContext, attributes);
    assertThat(extensionModel, is(notNullValue()));
    assertThat(extensionModel.getName(), is(EXTENSION_NAME));
  }

  @Test
  public void spiDiscoverable() throws Exception {
    ServiceRegistry registry = new SpiServiceRegistry();
    Optional<ExtensionModelLoader> craftedLoader = registry.lookupProviders(ExtensionModelLoader.class, classLoader).stream()
        .filter(p -> p instanceof CraftedExtensionModelLoader)
        .findAny();
    assertThat(craftedLoader.isPresent(), is(true));
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
