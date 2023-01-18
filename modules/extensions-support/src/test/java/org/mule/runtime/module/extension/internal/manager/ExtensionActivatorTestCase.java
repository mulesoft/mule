/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager;

import static org.mule.metadata.api.builder.BaseTypeBuilder.create;
import static org.mule.metadata.java.api.JavaTypeLoader.JAVA;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.VERSION;
import static org.mule.tck.util.MuleContextUtils.mockMuleContext;

import static java.util.Collections.singleton;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import org.mule.metadata.api.annotation.TypeAliasAnnotation;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionModule;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.privileged.el.GlobalBindingContextProvider;
import org.mule.runtime.internal.dsl.DefaultDslResolvingContext;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.qameta.allure.Issue;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

@SmallTest
public class ExtensionActivatorTestCase extends AbstractMuleTestCase {

  private static final String MOCK_EXTENSION_PREFIX = "mock";
  private static final String MOCK_TYPE_ALIAS = "MockType";
  private static final String MOCK_TYPE_ID = "MockTypeId";
  private static final String MOCK_EXTENSION_NAME = "Mock Extension";

  @Test
  public void enumsReleasedWhenStopped() throws Exception {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(TYPE_PROPERTY_NAME, HeisenbergExtension.class.getName());
    attributes.put(VERSION, "1.0.0");
    // TODO MULE-14517: This workaround should be replaced for a better and more complete mechanism
    attributes.put("COMPILATION_MODE", true);

    ExtensionModel extensionModel =
        new DefaultJavaExtensionModelLoader().loadExtensionModel(HeisenbergExtension.class.getClassLoader(),
                                                                 new DefaultDslResolvingContext(Collections.emptySet()),
                                                                 attributes);

    ExtensionActivator extensionActivator = new ExtensionActivator(mockMuleContext());
    extensionActivator.activateExtension(extensionModel);
    assertThat(extensionActivator.getEnumTypes().size(), is(greaterThan(0)));

    extensionActivator.stop();
    assertThat(extensionActivator.getEnumTypes(), hasSize(0));
  }

  @Test
  @Issue("W-11969246")
  public void activatedExtensionTypesAreAddedToGlobalBindings() throws MuleException {
    // Given a type
    ObjectType mockType = create(JAVA).objectType()
        .id(MOCK_TYPE_ID)
        .with(new TypeAliasAnnotation(MOCK_TYPE_ALIAS))
        .build();

    // Given an extension model declaring that type
    ExtensionModel extensionModel = extensionWithTypes(singleton(mockType));

    // Given a mule context
    MuleContextWithRegistry muleContext = mockMuleContext();
    MuleRegistry spiedRegistry = spy(muleContext.getRegistry());
    when(muleContext.getRegistry()).thenReturn(spiedRegistry);

    // When the extension activator activates that extension model
    ExtensionActivator extensionActivator = new ExtensionActivator(muleContext);
    extensionActivator.activateExtension(extensionModel);

    // Then a global binding context is registered in the mule context registry, and it contains the type
    ArgumentCaptor<GlobalBindingContextProvider> bcProviderCaptor = forClass(GlobalBindingContextProvider.class);
    String registryKey = MOCK_EXTENSION_NAME + "GlobalBindingContextProvider";
    verify(spiedRegistry).registerObject(eq(registryKey), bcProviderCaptor.capture());
    BindingContext bindingContext = bcProviderCaptor.getValue().getBindingContext();

    assertThat(bindingContext.modules().size(), is(1));
    ExpressionModule module = bindingContext.modules().stream().findAny().get();
    assertThat(module.declaredTypes(), contains(mockType));
  }

  private static ExtensionModel extensionWithTypes(Set<ObjectType> metadataTypes) {
    ExtensionModel mockExtensionModel = mock(ExtensionModel.class);
    XmlDslModel dslModel = XmlDslModel.builder().setPrefix(MOCK_EXTENSION_PREFIX).build();
    when(mockExtensionModel.getXmlDslModel()).thenReturn(dslModel);
    when(mockExtensionModel.getName()).thenReturn(MOCK_EXTENSION_NAME);
    when(mockExtensionModel.getTypes()).thenReturn(metadataTypes);
    return mockExtensionModel;
  }
}
