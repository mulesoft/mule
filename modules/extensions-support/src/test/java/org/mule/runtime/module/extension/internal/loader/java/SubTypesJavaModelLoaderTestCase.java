/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.type.TypeCatalog;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.AnnotationValueFetcher;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.WithDeclaringClass;
import org.mule.runtime.module.extension.internal.loader.delegate.DefaultExtensionModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import org.junit.Before;
import org.junit.Test;

@SmallTest
public class SubTypesJavaModelLoaderTestCase extends AbstractMuleTestCase {

  private static final String BASE_TYPE_ID = BaseType.class.getName();
  private static final String SUBTYPE_ID = SubType.class.getName();

  private DefaultExtensionModelLoaderDelegate loader;
  private ExtensionDeclarer pluginDeclarer;
  private ObjectType baseMetadataType;
  private ObjectType subMetadataType;
  private AnnotationValueFetcher<SubTypeMapping> typeMapping;
  private ExtensionLoadingContext pluginCtx;
  private TypeCatalog typeCatalog;

  @Before
  public void before() {
    pluginDeclarer = spy(new ExtensionDeclarer());

    ExtensionElement extensionElement = mock(ExtensionElement.class, RETURNS_DEEP_STUBS);
    when(extensionElement.getName()).thenReturn("LoaderTest");
    when(extensionElement.getConfigurations()).thenReturn(emptyList());
    when(extensionElement.getConnectionProviders()).thenReturn(emptyList());
    when(extensionElement.getSources()).thenReturn(emptyList());
    when(extensionElement.getOperations()).thenReturn(emptyList());
    when(extensionElement.getOperationContainers()).thenReturn(emptyList());
    when(extensionElement.getFunctionContainers()).thenReturn(emptyList());
    when(extensionElement.getFunctions()).thenReturn(emptyList());

    baseMetadataType = createMetadataType(BASE_TYPE_ID);
    subMetadataType = createMetadataType(SUBTYPE_ID);

    typeMapping = mock(AnnotationValueFetcher.class);

    when(extensionElement.getValueFromAnnotation(SubTypeMapping.class)).thenReturn(of(typeMapping));

    pluginDeclarer.withModelProperty(new ExtensionTypeDescriptorModelProperty(extensionElement));

    typeCatalog = mock(TypeCatalog.class);

    DslResolvingContext dslResolvingCtx = mock(DslResolvingContext.class);
    when(dslResolvingCtx.getTypeCatalog()).thenReturn(typeCatalog);

    pluginCtx = mock(ExtensionLoadingContext.class);
    when(pluginCtx.getExtensionDeclarer()).thenReturn(pluginDeclarer);
    when(pluginCtx.getDslResolvingContext()).thenReturn(dslResolvingCtx);
    when(pluginCtx.getExtensionClassLoader()).thenReturn(getClass().getClassLoader());

    loader = new DefaultExtensionModelLoaderDelegate(extensionElement, "1.0.0");
  }

  @Test
  @Issue("MULE-18581")
  @Description("Simulate the scenario of different plugins declaring subtypes from an underlying lib "
      + "(Plugin A depends on lib X, plugin B also depends ob lib X), assert that the types from the lib are NOT imported, "
      + "since they live twice, on each plugin classloader.")
  public void noImportForSubtypesFromLocalLib() throws ClassNotFoundException {
    final Type baseAType = createType(baseMetadataType, BaseType.class);
    final Type subAType = createType(subMetadataType, SubType.class);

    when(typeMapping.getClassValue(any())).thenReturn(baseAType);
    when(typeMapping.getClassArrayValue(any())).thenReturn(asList(subAType));

    when(typeCatalog.getType(any())).thenReturn(empty());

    loader.declare(pluginCtx);
    verify(pluginDeclarer, never()).withImportedType(any());
  }

  @Test
  @Issue("MULE-18581")
  @Description("Simulate the scenario of a plugins declaring subtypes from another plugin "
      + "(Plugin A depends on plugin B), assert that the types from the plugin B are marked as imported on plugin A.")
  public void importForSubtypesFromOtherPlugin() throws ClassNotFoundException {
    final Type baseAType = createType(baseMetadataType, BaseType.class);
    final Type subAType = createType(subMetadataType, SubType.class);
    when(pluginCtx.getExtensionClassLoader()).thenReturn(mock(ClassLoader.class));

    when(typeMapping.getClassValue(any())).thenReturn(baseAType);
    when(typeMapping.getClassArrayValue(any())).thenReturn(asList(subAType));

    when(typeCatalog.getType(BASE_TYPE_ID)).thenReturn(of(baseMetadataType));
    when(typeCatalog.getType(SUBTYPE_ID)).thenReturn(of(subMetadataType));

    loader.declare(pluginCtx);
    verify(pluginDeclarer, times(2)).withImportedType(any());
  }

  private Type createType(MetadataType metadataType, Class declaringClass) {
    final Type type = mock(Type.class, withSettings().extraInterfaces(WithDeclaringClass.class));
    when(type.asMetadataType()).thenReturn(metadataType);
    when(type.getDeclaringClass()).thenReturn(of(declaringClass));

    return type;
  }

  private ObjectType createMetadataType(String typeId) {
    final ObjectType metadataType = mock(ObjectType.class);

    when(metadataType.getAnnotation(TypeIdAnnotation.class))
        .thenReturn(of(new TypeIdAnnotation(typeId)));

    doAnswer(inv -> {
      MetadataTypeVisitor visitor = inv.getArgument(0);
      visitor.visitObject(metadataType);
      return null;
    }).when(metadataType).accept(any());

    return metadataType;
  }

  public static class BaseType {

  }

  public static class SubType {

  }
}
