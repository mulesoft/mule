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
import static org.hamcrest.Matchers.hasSize;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.module.extension.internal.loader.parser.java.utils.JavaParserUtils.FIRST_MULE_VERSION;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;
import org.mule.sdk.api.annotation.Configurations;
import org.mule.sdk.api.annotation.Extension;
import org.mule.sdk.api.annotation.Import;
import org.mule.sdk.api.annotation.MinMuleVersion;
import org.mule.sdk.api.annotation.PrivilegedExport;
import org.mule.sdk.api.runtime.parameter.Literal;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;
import org.mule.test.vegan.extension.VeganCookBook;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

  // This behavior needs to be supported to maintain backwards compatibility
  @Test
  public void getSubtypesFromExtensionWithDuplicatedBaseTypes() {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(contextClassLoader);
    ExtensionTypeWrapper<SubTypesDuplication> extensionTypeWrapper =
        new ExtensionTypeWrapper<>(SubTypesDuplication.class, typeLoader);
    ExtensionLoadingContext ctx = new DefaultExtensionLoadingContext(contextClassLoader, getDefault(emptySet()));
    JavaExtensionModelParser javaExtensionModelParser = new JavaExtensionModelParser(extensionTypeWrapper, ctx);

    Map<MetadataType, List<MetadataType>> subTypes = javaExtensionModelParser.getSubTypes();
    assertThat(subTypes.size(), is(1));
    assertThat(subTypes.values().iterator().next(), hasSize(2));
  }

  @Test
  public void getMMVForLegacyExtension() {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(contextClassLoader);
    ExtensionTypeWrapper<SimpleExtensionUsingLegacyApi> extensionTypeWrapper =
        new ExtensionTypeWrapper<>(SimpleExtensionUsingLegacyApi.class, typeLoader);
    ExtensionLoadingContext ctx = new DefaultExtensionLoadingContext(contextClassLoader, getDefault(emptySet()));
    JavaExtensionModelParser javaExtensionModelParser = new JavaExtensionModelParser(extensionTypeWrapper, ctx);

    Optional<MuleVersion> minMuleVersion = javaExtensionModelParser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get(), is(FIRST_MULE_VERSION));
  }

  @Test
  public void getMMVForSdkExtension() {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(contextClassLoader);
    ExtensionTypeWrapper<SimpleExtensionUsingSdkApi> extensionTypeWrapper =
        new ExtensionTypeWrapper<>(SimpleExtensionUsingSdkApi.class, typeLoader);
    ExtensionLoadingContext ctx = new DefaultExtensionLoadingContext(contextClassLoader, getDefault(emptySet()));
    JavaExtensionModelParser javaExtensionModelParser = new JavaExtensionModelParser(extensionTypeWrapper, ctx);

    Optional<MuleVersion> minMuleVersion = javaExtensionModelParser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get().toString(), is("4.5.0"));
  }

  @Test
  public void getMMVForExtensionWithSuperExtension() {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(contextClassLoader);
    ExtensionTypeWrapper<ExtensionWithSuperExtension> extensionTypeWrapper =
            new ExtensionTypeWrapper<>(ExtensionWithSuperExtension.class, typeLoader);
    ExtensionLoadingContext ctx = new DefaultExtensionLoadingContext(contextClassLoader, getDefault(emptySet()));
    JavaExtensionModelParser javaExtensionModelParser = new JavaExtensionModelParser(extensionTypeWrapper, ctx);

    Optional<MuleVersion> minMuleVersion = javaExtensionModelParser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get().toString(), is("4.5.0"));
  }

  @Test
  public void getMMVForParameterizedExtension() {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(contextClassLoader);
    ExtensionTypeWrapper<ParameterizedExtension> extensionTypeWrapper =
        new ExtensionTypeWrapper<>(ParameterizedExtension.class, typeLoader);
    ExtensionLoadingContext ctx = new DefaultExtensionLoadingContext(contextClassLoader, getDefault(emptySet()));
    JavaExtensionModelParser javaExtensionModelParser = new JavaExtensionModelParser(extensionTypeWrapper, ctx);

    Optional<MuleVersion> minMuleVersion = javaExtensionModelParser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get().toString(), is("4.4"));
  }

  @Test
  public void getMMVForExtensionWithConfiguration() {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(contextClassLoader);
    ExtensionTypeWrapper<ExtensionWithConfiguration> extensionTypeWrapper =
        new ExtensionTypeWrapper<>(ExtensionWithConfiguration.class, typeLoader);
    ExtensionLoadingContext ctx = new DefaultExtensionLoadingContext(contextClassLoader, getDefault(emptySet()));
    JavaExtensionModelParser javaExtensionModelParser = new JavaExtensionModelParser(extensionTypeWrapper, ctx);

    Optional<MuleVersion> minMuleVersion = javaExtensionModelParser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get(), is(FIRST_MULE_VERSION));
  }

  @Extension(name = "SimpleExtension")
  @Import(type = KnockeableDoor.class)
  @PrivilegedExport(packages = {"org.mule.runtime.module.extension.internal.loader.parser.java"})
  private static class SimpleExtensionUsingSdkApi {
  }

  @org.mule.runtime.extension.api.annotation.Extension(name = "SimpleExtension")
  @org.mule.runtime.extension.api.annotation.Import(type = KnockeableDoor.class)
  @org.mule.runtime.extension.api.annotation.PrivilegedExport(
      packages = {"org.mule.runtime.module.extension.internal.loader.parser.java"})
  private static class SimpleExtensionUsingLegacyApi {
  }

  @org.mule.runtime.extension.api.annotation.Extension(name = "SimpleExtension")
  @MinMuleVersion("4.2.1")
  private static class SimpleExtensionUsingLegacyApiWithMMV {

    @MinMuleVersion("4.6.1")
    private String someField;
  }

  @Extension(name = "SimpleExtension")
  @Import(type = KnockeableDoor.class)
  @org.mule.runtime.extension.api.annotation.Import(type = VeganCookBook.class)
  private static class SimpleMixedApiExtension {
  }

  @Extension(name = "SubTypesDuplication")
  @SubTypeMapping(baseType = BaseInterface.class,
      subTypes = {ImplementationOne.class, ImplementationTwo.class, ImplementationOne.class})
  private static class SubTypesDuplication {
  }

  private interface BaseInterface {

  }

  private static class ImplementationOne implements BaseInterface {

    @Parameter
    private String parameterOne;

  }

  private static class ImplementationTwo implements BaseInterface {

    @Parameter
    private String parameterTwo;

  }

  @org.mule.runtime.extension.api.annotation.Extension(name = "MixedConfigurationsAnnotationExtension")
  @Configurations(ImplementationOne.class)
  private static class ExtensionWithConfiguration {

    Literal<String> extensionField;
  }

  @org.mule.runtime.extension.api.annotation.Extension(name = "MixedConfigurationsAnnotationExtension")
  private static class ParameterizedExtension {

    @org.mule.sdk.api.annotation.param.Parameter
    String extensionParameter;
  }

  @org.mule.runtime.extension.api.annotation.Extension(name = "ExtensionWithSuperExtension")
  private static class ExtensionWithSuperExtension extends ParameterizedExtension {}

}
