/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;
import org.mule.sdk.api.annotation.Extension;
import org.mule.sdk.api.annotation.Import;
import org.mule.sdk.api.annotation.PrivilegedExport;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;
import org.mule.test.vegan.extension.VeganCookBook;

import java.util.List;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class JavaExtensionModelParserTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void getImportedTypesFromExtensionUsingTheSdkApi() {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(contextClassLoader);
    ExtensionTypeWrapper<SimpleExtensionUsingSdkApi> extensionTypeWrapper =
        new ExtensionTypeWrapper<>(SimpleExtensionUsingSdkApi.class, typeLoader);
    ExtensionLoadingContext ctx = new DefaultExtensionLoadingContext(contextClassLoader, getDefault(emptySet()));
    JavaExtensionModelParser javaExtensionModelParser = new JavaExtensionModelParser(extensionTypeWrapper, ctx);

    List<MetadataType> importedTypes = javaExtensionModelParser.getImportedTypes();

    assertThat(importedTypes.size(), is(1));
    assertThat(importedTypes.get(0).getAnnotation(ClassInformationAnnotation.class).get().getClassname(),
               is("org.mule.test.heisenberg.extension.model.KnockeableDoor"));
  }

  @Test
  public void getImportedTypesFromExtensionUsingTheLegacyApi() {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(contextClassLoader);
    ExtensionTypeWrapper<SimpleExtensionUsingLegacyApi> extensionTypeWrapper =
        new ExtensionTypeWrapper<>(SimpleExtensionUsingLegacyApi.class, typeLoader);
    ExtensionLoadingContext ctx = new DefaultExtensionLoadingContext(contextClassLoader, getDefault(emptySet()));
    JavaExtensionModelParser javaExtensionModelParser = new JavaExtensionModelParser(extensionTypeWrapper, ctx);

    List<MetadataType> importedTypes = javaExtensionModelParser.getImportedTypes();

    assertThat(importedTypes.size(), is(1));
    assertThat(importedTypes.get(0).getAnnotation(ClassInformationAnnotation.class).get().getClassname(),
               is("org.mule.test.heisenberg.extension.model.KnockeableDoor"));
  }

  @Test
  public void getImportedTypesFromExtensionUsingBothTheLegacyAndTheSdkApi() {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(contextClassLoader);
    ExtensionTypeWrapper<SimpleMixedApiExtension> extensionTypeWrapper =
        new ExtensionTypeWrapper<>(SimpleMixedApiExtension.class, typeLoader);
    ExtensionLoadingContext ctx = new DefaultExtensionLoadingContext(contextClassLoader, getDefault(emptySet()));
    JavaExtensionModelParser javaExtensionModelParser = new JavaExtensionModelParser(extensionTypeWrapper, ctx);

    List<MetadataType> importedTypes = javaExtensionModelParser.getImportedTypes();

    assertThat(importedTypes.size(), is(2));
    assertThat(importedTypes.get(1).getAnnotation(ClassInformationAnnotation.class).get().getClassname(),
               is("org.mule.test.heisenberg.extension.model.KnockeableDoor"));
    assertThat(importedTypes.get(0).getAnnotation(ClassInformationAnnotation.class).get().getClassname(),
               is("org.mule.test.vegan.extension.VeganCookBook"));
  }

  @Test
  @Ignore
  public void getExportedTypesFromExtensionUsingTheSdkApi() {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(contextClassLoader);
    ExtensionTypeWrapper<SimpleExtensionUsingSdkApi> extensionTypeWrapper =
        new ExtensionTypeWrapper<>(SimpleExtensionUsingSdkApi.class, typeLoader);
    ExtensionLoadingContext ctx = new DefaultExtensionLoadingContext(contextClassLoader, getDefault(emptySet()));
    JavaExtensionModelParser javaExtensionModelParser = new JavaExtensionModelParser(extensionTypeWrapper, ctx);

    List<String> exportedTypes = javaExtensionModelParser.getPrivilegedExportedPackages();

    assertThat(exportedTypes.size(), is(1));
  }

  @Extension(name = "SimpleExtension")
  @Import(type = KnockeableDoor.class)
  @PrivilegedExport(packages = {"org.mule.runtime.module.extension.internal.loader.parser.java"})
  private static class SimpleExtensionUsingSdkApi {
  }

  @Extension(name = "SimpleExtension")
  @org.mule.runtime.extension.api.annotation.Import(type = KnockeableDoor.class)
  @org.mule.runtime.extension.api.annotation.PrivilegedExport(
      packages = {"org.mule.runtime.module.extension.internal.loader.parser.java"})
  private static class SimpleExtensionUsingLegacyApi {
  }

  @Extension(name = "SimpleExtension")
  @Import(type = KnockeableDoor.class)
  @org.mule.runtime.extension.api.annotation.Import(type = VeganCookBook.class)
  private static class SimpleMixedApiExtension {
  }

}
