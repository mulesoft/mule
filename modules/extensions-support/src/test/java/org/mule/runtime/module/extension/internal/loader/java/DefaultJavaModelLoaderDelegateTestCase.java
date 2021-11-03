/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;

import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.meta.model.ImportedTypeModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.sdk.api.annotation.Export;
import org.mule.sdk.api.annotation.Extension;
import org.mule.sdk.api.annotation.Import;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;
import org.mule.test.module.extension.internal.util.extension.SimpleExportedType;
import org.mule.test.module.extension.internal.util.extension.SimpleExtensionUsingLegacyApi;
import org.mule.test.module.extension.internal.util.extension.SimpleExtensionUsingSdkApi;

import java.util.TreeSet;

import org.junit.Test;

public class DefaultJavaModelLoaderDelegateTestCase {

  @Test
  public void verifyExportedTypesAndResourcesFromExtensionUsingSdkApi() {
    DefaultJavaModelLoaderDelegate defaultJavaModelLoaderDelegate =
        new DefaultJavaModelLoaderDelegate(SimpleExtensionUsingSdkApi.class, "1.0.0-dev");

    ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    ExtensionLoadingContext ctx = new DefaultExtensionLoadingContext(currentClassLoader, getDefault(emptySet()));

    ExtensionDeclarer extensionDeclarer = defaultJavaModelLoaderDelegate.declare(ctx);

    assertThat(extensionDeclarer.getDeclaration().getResources().size(), is(1));
    assertThat(extensionDeclarer.getDeclaration().getResources().iterator().next(), equalTo("simpleResource.json"));
    assertThat(extensionDeclarer.getDeclaration().getTypes().size(), is(1));
    assertThat(extensionDeclarer.getDeclaration().getTypes().iterator().next()
        .getAnnotation(ClassInformationAnnotation.class).get().getClassname(),
               equalTo("org.mule.test.module.extension.internal.util.extension.SimpleExportedType"));
  }

  @Test
  public void verifyExportedTypesAndResourcesFromExtensionUsingLegacyApi() {
    DefaultJavaModelLoaderDelegate defaultJavaModelLoaderDelegate =
        new DefaultJavaModelLoaderDelegate(SimpleExtensionUsingLegacyApi.class, "1.0.0-dev");

    ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    ExtensionLoadingContext ctx = new DefaultExtensionLoadingContext(currentClassLoader, getDefault(emptySet()));

    ExtensionDeclarer extensionDeclarer = defaultJavaModelLoaderDelegate.declare(ctx);

    assertThat(extensionDeclarer.getDeclaration().getResources().size(), is(1));
    assertThat(extensionDeclarer.getDeclaration().getResources().iterator().next(), equalTo("simpleResource.json"));
    assertThat(extensionDeclarer.getDeclaration().getTypes().size(), is(1));
    assertThat(extensionDeclarer.getDeclaration().getTypes().iterator().next()
        .getAnnotation(ClassInformationAnnotation.class).get().getClassname(),
               equalTo("org.mule.test.module.extension.internal.util.extension.SimpleExportedType"));
  }

  @Test
  public void verifyImportedTypesFromExtensionUsingTheSdkApi() {
    DefaultJavaModelLoaderDelegate defaultJavaModelLoaderDelegate =
        new DefaultJavaModelLoaderDelegate(SimpleExtensionUsingSdkApi.class, "1.0.0-dev");

    ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    ExtensionLoadingContext ctx = new DefaultExtensionLoadingContext(currentClassLoader, getDefault(emptySet()));

    ExtensionDeclaration extensionDeclaration = defaultJavaModelLoaderDelegate.declare(ctx).getDeclaration();
    ObjectType importedType = ((ImportedTypeModel) ((TreeSet) extensionDeclaration.getImportedTypes()).first()).getImportedType();

    assertThat(extensionDeclaration.getImportedTypes().size(), is(1));
    assertThat(importedType.getAnnotation(ClassInformationAnnotation.class).get().getClassname(),
               is("org.mule.test.heisenberg.extension.model.KnockeableDoor"));
  }

  @Test
  public void verifyImportedTypesFromExtensionUsingTheLegacyApi() {
    DefaultJavaModelLoaderDelegate defaultJavaModelLoaderDelegate =
        new DefaultJavaModelLoaderDelegate(SimpleExtensionUsingLegacyApi.class, "1.0.0-dev");

    ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    ExtensionLoadingContext ctx = new DefaultExtensionLoadingContext(currentClassLoader, getDefault(emptySet()));

    ExtensionDeclaration extensionDeclaration = defaultJavaModelLoaderDelegate.declare(ctx).getDeclaration();
    ObjectType importedType = ((ImportedTypeModel) ((TreeSet) extensionDeclaration.getImportedTypes()).first()).getImportedType();

    assertThat(extensionDeclaration.getImportedTypes().size(), is(1));
    assertThat(importedType.getAnnotation(ClassInformationAnnotation.class).get().getClassname(),
               is("org.mule.test.heisenberg.extension.model.KnockeableDoor"));
  }

}
