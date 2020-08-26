/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.core.api.util.IOUtils.toByteArray;

import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.utils.MetadataTypeUtils.TypeResolverVisitor;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.type.TypeCatalog;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.AnnotationValueFetcher;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.WithDeclaringClass;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;

public class SubTypesDeclarationEnricherTestCase {

  private SubTypesDeclarationEnricher enricher;

  private ExtensionDeclarer pluginDeclarer;
  private ObjectType baseMetadataType;
  private ObjectType subMetadataType;
  private AnnotationValueFetcher<SubTypeMapping> typeMapping;
  private ExtensionLoadingContext pluginCtx;
  private TypeCatalog typeCatalog;

  @Before
  public void before() {
    enricher = new SubTypesDeclarationEnricher();

    pluginDeclarer = spy(new ExtensionDeclarer());
    Type extensionType = mock(Type.class);

    baseMetadataType = createMetadataType("base");
    subMetadataType = createMetadataType("sub");

    typeMapping = mock(AnnotationValueFetcher.class);

    when(extensionType.getValueFromAnnotation(SubTypeMapping.class))
        .thenReturn(of(typeMapping));

    pluginDeclarer.withModelProperty(new ExtensionTypeDescriptorModelProperty(extensionType));

    typeCatalog = mock(TypeCatalog.class);

    DslResolvingContext dslResolvingCtx = mock(DslResolvingContext.class);
    when(dslResolvingCtx.getTypeCatalog()).thenReturn(typeCatalog);

    pluginCtx = mock(ExtensionLoadingContext.class);
    when(pluginCtx.getExtensionDeclarer()).thenReturn(pluginDeclarer);
    when(pluginCtx.getDslResolvingContext()).thenReturn(dslResolvingCtx);
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

    enricher.enrich(pluginCtx);
    verify(pluginDeclarer, never()).withImportedType(any());
  }

  @Test
  @Issue("MULE-18581")
  @Description("Simulate the scenario of a plugins declaring subtypes from another plugin "
      + "(Plugin A depends on plugin B), assert that the types from the plugin B are marked as imported on plugin A.")
  public void importForSubtypesFromOtherPlugin() throws ClassNotFoundException {
    final ClassLoader otherPluginClassLoader = createOtherPluginClassLoader();
    final Type baseAType = createType(baseMetadataType, otherPluginClassLoader.loadClass("base"));
    final Type subAType = createType(subMetadataType, otherPluginClassLoader.loadClass("sub"));

    when(typeMapping.getClassValue(any())).thenReturn(baseAType);
    when(typeMapping.getClassArrayValue(any())).thenReturn(asList(subAType));

    when(typeCatalog.getType("base")).thenReturn(of(baseMetadataType));
    when(typeCatalog.getType("sub")).thenReturn(of(subMetadataType));

    enricher.enrich(pluginCtx);
    verify(pluginDeclarer, times(2)).withImportedType(any());
  }

  private ClassLoader createOtherPluginClassLoader() {
    final ClassLoader otherPluginClassLoader = new ClassLoader(SubTypesDeclarationEnricherTestCase.class.getClassLoader()) {

      @Override
      protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.equals("base")) {
          byte[] classBytes;
          try {
            classBytes =
                toByteArray(this.getClass()
                    .getResourceAsStream("/org/mule/runtime/module/extension/internal/loader/enricher/SubTypesDeclarationEnricherTestCase$BaseType.class"));
            return this.defineClass(null, classBytes, 0, classBytes.length);
          } catch (Exception e) {
            return super.loadClass(name);
          }
        } else if (name.equals("sub")) {
          byte[] classBytes;
          try {
            classBytes =
                toByteArray(this.getClass()
                    .getResourceAsStream("/org/mule/runtime/module/extension/internal/loader/enricher/SubTypesDeclarationEnricherTestCase$SubType.class"));
            return this.defineClass(null, classBytes, 0, classBytes.length);
          } catch (Exception e) {
            return super.loadClass(name);
          }
        } else {
          return super.loadClass(name, resolve);
        }
      }
    };
    return otherPluginClassLoader;
  }

  private Type createType(MetadataType metadataType, Class declaringClass) {
    final Type type = mock(Type.class, withSettings().extraInterfaces(WithDeclaringClass.class));
    when(type.asMetadataType()).thenReturn(metadataType);
    when(((WithDeclaringClass) type).getDeclaringClass()).thenReturn(of(declaringClass));

    return type;
  }

  private ObjectType createMetadataType(String typeId) {
    final ObjectType metadataType = mock(ObjectType.class);

    when(metadataType.getAnnotation(TypeIdAnnotation.class))
        .thenReturn(of(new TypeIdAnnotation(typeId)));

    doAnswer(inv -> {
      TypeResolverVisitor visitor = inv.getArgument(0);
      visitor.defaultVisit(metadataType);
      return null;
    })
        .when(metadataType).accept(any());

    return metadataType;
  }

  public static class BaseType {

  }

  public static class SubType {

  }
}
