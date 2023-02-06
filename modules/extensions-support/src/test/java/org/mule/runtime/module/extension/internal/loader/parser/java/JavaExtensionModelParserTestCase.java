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
import static org.mule.runtime.api.meta.MuleVersion.FIRST_MULE_VERSION;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
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

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class JavaExtensionModelParserTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void getImportedTypesFromExtensionUsingTheSdkApi() {
    List<MetadataType> importedTypes = getParser(SimpleExtensionUsingSdkApi.class).getImportedTypes();
    assertThat(importedTypes.size(), is(1));
    assertThat(importedTypes.get(0).getAnnotation(ClassInformationAnnotation.class).get().getClassname(),
               is("org.mule.test.heisenberg.extension.model.KnockeableDoor"));
  }

  @Test
  public void getImportedTypesFromExtensionUsingTheLegacyApi() {
    List<MetadataType> importedTypes = getParser(SimpleExtensionUsingLegacyApi.class).getImportedTypes();
    assertThat(importedTypes.size(), is(1));
    assertThat(importedTypes.get(0).getAnnotation(ClassInformationAnnotation.class).get().getClassname(),
               is("org.mule.test.heisenberg.extension.model.KnockeableDoor"));
  }

  @Test
  public void getImportedTypesFromExtensionUsingBothTheLegacyAndTheSdkApi() {
    List<MetadataType> importedTypes = getParser(SimpleMixedApiExtension.class).getImportedTypes();
    assertThat(importedTypes.size(), is(2));
    assertThat(importedTypes.get(1).getAnnotation(ClassInformationAnnotation.class).get().getClassname(),
               is("org.mule.test.heisenberg.extension.model.KnockeableDoor"));
    assertThat(importedTypes.get(0).getAnnotation(ClassInformationAnnotation.class).get().getClassname(),
               is("org.mule.test.vegan.extension.VeganCookBook"));
  }

  @Test
  @Ignore
  public void getExportedTypesFromExtensionUsingTheSdkApi() {
    List<String> exportedTypes = getParser(SimpleExtensionUsingSdkApi.class).getPrivilegedExportedPackages();
    assertThat(exportedTypes.size(), is(1));
  }

  // This behavior needs to be supported to maintain backwards compatibility
  @Test
  public void getSubtypesFromExtensionWithDuplicatedBaseTypes() {
    Map<MetadataType, List<MetadataType>> subTypes = getParser(SubTypesDuplication.class).getSubTypes();
    assertThat(subTypes.size(), is(1));
    assertThat(subTypes.values().iterator().next(), hasSize(2));
  }

  @Test
  public void getMMVForLegacyExtension() {
    JavaExtensionModelParser parser = getParser(ExtensionUsingLegacyApi.class);
    assertThat(parser.getMinMuleVersionResult().getMinMuleVersion(), is(FIRST_MULE_VERSION));
    assertThat(parser.getMinMuleVersionResult().getReason(),
               is("Extension SimpleExtension has min mule version 4.1.1 because it is the default value."));
  }

  @Test
  public void getMMVForSdkExtension() {
    JavaExtensionModelParser parser = getParser(ExtensionUsingSdkApi.class);
    assertThat(parser.getMinMuleVersionResult().getMinMuleVersion().toString(), is("4.5.0"));
    assertThat(parser.getMinMuleVersionResult().getReason(),
               is("Extension SimpleExtension has min mule version 4.5.0 because it if annotated with the new sdk api @Extension."));
  }

  @Test
  public void getMMVForExtensionWithSuperExtension() {
    JavaExtensionModelParser parser = getParser(ExtensionWithSuperExtension.class);
    assertThat(parser.getMinMuleVersionResult().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getMinMuleVersionResult().getReason(),
               is("Extension ExtensionWithSuperExtension has min mule version 4.4 because of its super class ParameterizedExtension. Extension ParameterizedExtension has min mule version 4.4 because of its field extensionParameter. Field extensionParameter has min mule version 4.4 because it is annotated with Parameter. Parameter was introduced in Mule 4.4."));
  }

  @Test
  public void getMMVForParameterizedExtension() {
    JavaExtensionModelParser parser = getParser(ParameterizedExtension.class);
    assertThat(parser.getMinMuleVersionResult().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getMinMuleVersionResult().getReason(),
               is("Extension MixedConfigurationsAnnotationExtension has min mule version 4.4 because of its field extensionParameter. Field extensionParameter has min mule version 4.4 because it is annotated with Parameter. Parameter was introduced in Mule 4.4."));
  }

  @Test
  public void getMMVForExtensionWithConfiguration() {
    JavaExtensionModelParser parser = getParser(ExtensionWithConfiguration.class);
    assertThat(parser.getMinMuleVersionResult().getMinMuleVersion(), is(FIRST_MULE_VERSION));
    assertThat(parser.getMinMuleVersionResult().getReason(),
               is("Extension MixedConfigurationsAnnotationExtension has min mule version 4.1.1 because it is the default value."));
  }

  @Test
  public void getMMVForExtensionAnnotatedWithMMV() {
    JavaExtensionModelParser parser = getParser(ExtensionAnnotatedWithMMV.class);
    assertThat(parser.getMinMuleVersionResult().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getMinMuleVersionResult().getReason(),
               is("Calculated Min Mule Version is 4.4 which is greater than the one set at the extension class level 4.3. Overriding it. Extension ExtensionAnnotatedWithMMV has min mule version 4.4 because of its method extensionMethod. Method extensionMethod has min mule version 4.4 because it is the one set at the method level through the @MinMuleVersion annotation."));
  }

  @Test
  public void getMMVForExtensionAnnotatedWithHighMMV() {
    JavaExtensionModelParser parser = getParser(ExtensionAnnotatedWithHighMMV.class);
    assertThat(parser.getMinMuleVersionResult().getMinMuleVersion().toString(), is("4.7"));
    assertThat(parser.getMinMuleVersionResult().getReason(),
               is("Extension ExtensionAnnotatedWithHighMMV has min mule version 4.7 because it is the one set at the class level through the @MinMuleVersion annotation."));
  }

  protected JavaExtensionModelParser getParser(Class<?> extensionClass) {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(contextClassLoader);
    ExtensionTypeWrapper extensionTypeWrapper = new ExtensionTypeWrapper<>(extensionClass, typeLoader);
    ExtensionLoadingContext ctx = new DefaultExtensionLoadingContext(contextClassLoader, getDefault(emptySet()));
    return new JavaExtensionModelParser(extensionTypeWrapper, ctx);
  }

  @Extension(name = "SimpleExtension")
  @Import(type = KnockeableDoor.class)
  @PrivilegedExport(packages = {"org.mule.runtime.module.extension.internal.loader.parser.java"})
  private static class SimpleExtensionUsingSdkApi {
  }

  @Extension(name = "SimpleExtension")
  public static class ExtensionUsingSdkApi {
  }

  @org.mule.runtime.extension.api.annotation.Extension(name = "SimpleExtension")
  @org.mule.runtime.extension.api.annotation.Import(type = KnockeableDoor.class)
  @org.mule.runtime.extension.api.annotation.PrivilegedExport(
      packages = {"org.mule.runtime.module.extension.internal.loader.parser.java"})
  private static class SimpleExtensionUsingLegacyApi {
  }

  @org.mule.runtime.extension.api.annotation.Extension(name = "SimpleExtension")
  public static class ExtensionUsingLegacyApi {
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
  private static class ExtensionWithSuperExtension extends ParameterizedExtension {
  }

  @org.mule.runtime.extension.api.annotation.Extension(name = "ExtensionAnnotatedWithMMV")
  @MinMuleVersion("4.3")
  private static class ExtensionAnnotatedWithMMV {

    @MinMuleVersion("4.4")
    public void extensionMethod() {}
  }

  @org.mule.runtime.extension.api.annotation.Extension(name = "ExtensionAnnotatedWithHighMMV")
  @MinMuleVersion("4.7")
  private static class ExtensionAnnotatedWithHighMMV {

    @org.mule.sdk.api.annotation.param.Parameter
    String extensionParameter;
  }

}
