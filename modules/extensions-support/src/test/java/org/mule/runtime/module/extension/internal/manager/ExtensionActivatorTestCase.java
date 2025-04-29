/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager;

import static org.mule.metadata.api.builder.BaseTypeBuilder.create;
import static org.mule.metadata.java.api.JavaTypeLoader.JAVA;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.VERSION;
import static org.mule.tck.util.MuleContextUtils.verifyRegistration;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.metadata.api.annotation.TypeAliasAnnotation;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionModule;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.privileged.el.GlobalBindingContextProvider;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import org.mockito.ArgumentCaptor;

import io.qameta.allure.Issue;

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
                                                                 getDefault(emptySet()),
                                                                 attributes);

    ExtensionActivator extensionActivator = new ExtensionActivator(mock(MuleRegistry.class),
                                                                   mock(Injector.class),
                                                                   mock(ExtendedExpressionManager.class),
                                                                   mock(ServerNotificationManager.class),
                                                                   this.getClass().getClassLoader());
    extensionActivator.activateExtension(extensionModel);
    assertThat(extensionActivator.getEnumTypes(), not(emptyIterable()));

    extensionActivator.stop();
    assertThat(extensionActivator.getEnumTypes(), emptyIterable());
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
    final var registry = mock(MuleRegistry.class);

    // When the extension activator activates that extension model
    ExtensionActivator extensionActivator = new ExtensionActivator(registry,
                                                                   mock(Injector.class),
                                                                   mock(ExtendedExpressionManager.class),
                                                                   mock(ServerNotificationManager.class),
                                                                   this.getClass().getClassLoader());
    extensionActivator.activateExtension(extensionModel);

    // Then a global binding context is registered in the mule context registry, and it contains the type
    ArgumentCaptor<GlobalBindingContextProvider> bcProviderCaptor = forClass(GlobalBindingContextProvider.class);
    String registryKey = MOCK_EXTENSION_NAME + "GlobalBindingContextProvider";
    verifyRegistration(registry, registryKey, bcProviderCaptor);
    BindingContext bindingContext = bcProviderCaptor.getValue().getBindingContext();

    assertThat(bindingContext.modules(), iterableWithSize(1));
    ExpressionModule module = bindingContext.modules().stream().findAny().get();
    assertThat(module.declaredTypes(), contains(mockType));
  }

  private static ExtensionModel extensionWithTypes(Set<ObjectType> metadataTypes) {
    ExtensionModel mockExtensionModel = mock(ExtensionModel.class, RETURNS_MOCKS);
    XmlDslModel dslModel = XmlDslModel.builder().setPrefix(MOCK_EXTENSION_PREFIX).build();
    when(mockExtensionModel.getXmlDslModel()).thenReturn(dslModel);
    when(mockExtensionModel.getName()).thenReturn(MOCK_EXTENSION_NAME);
    when(mockExtensionModel.getTypes()).thenReturn(metadataTypes);
    when(mockExtensionModel.getModelProperty(any())).thenReturn(empty());
    return mockExtensionModel;
  }
}
