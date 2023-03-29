/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

import static com.google.common.collect.ImmutableSet.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.fail;

import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.core.api.util.IOUtils.toByteArray;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.VERSION;
import static org.mule.runtime.module.extension.internal.loader.parser.java.utils.ResolvedMinMuleVersion.FIRST_MULE_VERSION;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.AGE;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.impl.DefaultObjectType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.core.api.extension.MuleExtensionModelProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest;
import org.mule.runtime.extension.api.model.config.ImmutableConfigurationModel;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.VoidOperations;
import org.mule.sdk.api.annotation.Configurations;
import org.mule.sdk.api.annotation.Extension;
import org.mule.sdk.api.annotation.Import;
import org.mule.sdk.api.annotation.MinMuleVersion;
import org.mule.sdk.api.annotation.PrivilegedExport;
import org.mule.sdk.api.runtime.parameter.Literal;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;
import org.mule.test.heisenberg.extension.model.PersonalInfo;
import org.mule.test.oauth.TestOAuthExtension;
import org.mule.test.petstore.extension.PetStoreConnector;
import org.mule.test.vegan.extension.VeganCookBook;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class JavaExtensionModelParserTestCase {

  public static final String COMPILATION_MODE = "COMPILATION_MODE";
  protected static Map<String, ExtensionModel> EXTENSION_MODELS = new HashMap<>();
  protected static final ExtensionModelLoader JAVA_LOADER = new DefaultJavaExtensionModelLoader();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  @Issue("W-12622240")
  @Description("Verify that ExtensionModel for an Extension with java data types such as LocalDateTime, loads the correct MetadataType i.e. DateTimeType and works in Java 17")
  public void getParameterizedWithJavaFieldsExtensionUsingSdkApi() {
    ExtensionModel model = loadExtension(ParameterizedWithJavaTypeExtension.class, JAVA_LOADER, null);
    ConfigurationModel configModel = model.getConfigurationModels().iterator().next();
    ParameterModel parameterModel = configModel.getAllParameterModels().iterator().next();
    DefaultObjectType objectType = (DefaultObjectType) parameterModel.getType();
    assertThat(objectType.toString(),
               is("org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserTestCase.SimplePojoWithTime"));
    Collection<ObjectFieldType> fields = objectType.getFields();
    List<String> dateTimeParameters = fields.stream().map(f -> f.getValue().getClass().getName()).collect(toList());

    assertThat(dateTimeParameters, hasItem(equalTo("org.mule.metadata.api.model.impl.DefaultDateTimeType")));
  }

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
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion(), is(FIRST_MULE_VERSION));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Extension SimpleExtension has min mule version 4.1.1 because it is the default value."));
  }

  @Test
  public void getMMVForSdkExtension() {
    JavaExtensionModelParser parser = getParser(ExtensionUsingSdkApi.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5.0"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Extension SimpleExtension has min mule version 4.5.0 because it if annotated with the new sdk api @Extension."));
  }

  @Test
  public void getMMVForExtensionWithSuperExtension() {
    JavaExtensionModelParser parser = getParser(ExtensionWithSuperExtension.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Extension ExtensionWithSuperExtension has min mule version 4.4 because of its super class ParameterizedExtension. Extension ParameterizedExtension has min mule version 4.4 because of its field extensionParameter. Field extensionParameter has min mule version 4.4 because it is annotated with Parameter. Parameter was introduced in Mule 4.4."));
  }

  @Test
  public void getMMVForParameterizedExtension() {
    JavaExtensionModelParser parser = getParser(ParameterizedExtension.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Extension MixedConfigurationsAnnotationExtension has min mule version 4.4 because of its field extensionParameter. Field extensionParameter has min mule version 4.4 because it is annotated with Parameter. Parameter was introduced in Mule 4.4."));
  }

  @Test
  public void getMMVForExtensionWithConfiguration() {
    JavaExtensionModelParser parser = getParser(ExtensionWithConfiguration.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion(), is(FIRST_MULE_VERSION));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Extension MixedConfigurationsAnnotationExtension has min mule version 4.1.1 because it is the default value."));
  }

  @Test
  public void getMMVForExtensionAnnotatedWithMMV() {
    JavaExtensionModelParser parser = getParser(ExtensionAnnotatedWithMMV.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Calculated Min Mule Version is 4.4 which is greater than the one set at the extension class level 4.3. Overriding it. Extension ExtensionAnnotatedWithMMV has min mule version 4.4 because of its method extensionMethod. Method extensionMethod has min mule version 4.4 because it is the one set at the method level through the @MinMuleVersion annotation."));
  }

  @Test
  public void getMMVForExtensionAnnotatedWithHighMMV() {
    JavaExtensionModelParser parser = getParser(ExtensionAnnotatedWithHighMMV.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.7"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
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

  @org.mule.runtime.extension.api.annotation.Extension(name = "MixedConfigurationsAnnotationExtension")
  public static class ParameterizedWithJavaTypeExtension {

    @Parameter
    private SimplePojoWithTime simplePojoWithTime;

    public ParameterizedWithJavaTypeExtension() {
      this.simplePojoWithTime = simplePojoWithTime;
    }
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


  @org.mule.runtime.extension.api.annotation.Extension(name = "SimplePojoWithTime")
  @Operations(VoidOperations.class)
  public static class SimplePojoWithTime {

    @Parameter
    private String name;

    @Parameter
    private Integer age;

    @Parameter
    private LocalDateTime dateTime;
  }

  protected static ExtensionModel loadExtension(Class<?> clazz, ExtensionModelLoader loader, ArtifactCoordinates coordinates) {
    Map<String, Object> params = SmallMap.of(TYPE_PROPERTY_NAME, clazz.getName(),
                                             VERSION, getProductVersion(),
                                             COMPILATION_MODE, true);
    final DslResolvingContext dslResolvingContext = getDefault(new LinkedHashSet<>(EXTENSION_MODELS.values()));

    final String basePackage = clazz.getPackage().toString();
    final ClassLoader pluginClassLoader = new ClassLoader(clazz.getClassLoader()) {

      @Override
      protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.startsWith(basePackage)) {
          byte[] classBytes;
          try {
            classBytes =
                toByteArray(this.getClass().getResourceAsStream("/" + name.replaceAll("\\.", "/") + ".class"));
            return this.defineClass(null, classBytes, 0, classBytes.length);
          } catch (Exception e) {
            return super.loadClass(name);
          }
        } else {
          return super.loadClass(name, resolve);
        }
      }
    };

    ExtensionModelLoadingRequest.Builder builder = builder(pluginClassLoader, dslResolvingContext).addParameters(params);
    if (coordinates != null) {
      builder.setArtifactCoordinates(coordinates);
    }

    return loader.loadExtensionModel(builder.build());
  }
}
