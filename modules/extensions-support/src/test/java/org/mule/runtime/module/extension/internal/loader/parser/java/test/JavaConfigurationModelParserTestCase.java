/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.test;

import static org.mule.runtime.module.extension.internal.loader.parser.java.test.MinMuleVersionTestUtils.ctxResolvingMinMuleVersion;

import static java.lang.String.format;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.ConfigurationElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaConfigurationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParser;
import org.mule.sdk.api.annotation.Configuration;
import org.mule.sdk.api.annotation.Configurations;
import org.mule.sdk.api.annotation.Extension;
import org.mule.sdk.api.annotation.NoImplicit;
import org.mule.sdk.api.annotation.Operations;
import org.mule.sdk.api.annotation.Sources;
import org.mule.sdk.api.annotation.connectivity.ConnectionProviders;
import org.mule.sdk.api.connectivity.ConnectionProvider;
import org.mule.sdk.api.connectivity.ConnectionValidationResult;
import org.mule.sdk.api.runtime.source.Source;
import org.mule.sdk.api.runtime.source.SourceCallback;

import org.junit.Test;

public class JavaConfigurationModelParserTestCase {

  @Test
  public void getConfigurationNameFromConfigurationUsingSdkApi() {
    JavaConfigurationModelParser javaConfigurationModelParser = getParser(SimpleSdkExtension.class, SimpleSdkConfiguration.class);
    assertThat(javaConfigurationModelParser.getName(), is("newSdkConfiguration"));
  }

  @Test
  public void getConfigurationNameFromConfigurationUsingLegacyApi() {
    JavaConfigurationModelParser javaConfigurationModelParser =
        getParser(SimpleLegacyExtension.class, SimpleLegacyConfiguration.class);
    assertThat(javaConfigurationModelParser.getName(), is("oldLegacyConfiguration"));
  }

  @Test
  public void getConfigurationNameFromConfigurationUsingSdkAndLegacyApi() {
    JavaConfigurationModelParser javaConfigurationModelParser =
        getParser(SimpleSdkExtensionWithWronglyAnnotatedConfiguration.class, SimpleWronglyAnnotatedConfiguration.class);
    var thrown = assertThrows(IllegalModelDefinitionException.class, () -> javaConfigurationModelParser.getName());
    assertThat(thrown.getMessage(), containsString("Annotations org.mule.runtime.extension.api.annotation.Configuration and " +
        "org.mule.sdk.api.annotation.Configuration are both present at the same time on Configuration SimpleWronglyAnnotatedConfiguration"));
  }

  @Test
  public void isForceNoImplicitOnConfigurationUsingTheSdkApi() {
    JavaConfigurationModelParser javaConfigurationModelParser = getParser(SimpleSdkExtension.class, SimpleSdkConfiguration.class);

    assertThat(javaConfigurationModelParser.isForceNoImplicit(), is(true));
  }

  @Test
  public void isForceNoImplicitOnConfigurationUsingTheLegacyApi() {
    JavaConfigurationModelParser javaConfigurationModelParser =
        getParser(SimpleLegacyExtension.class, SimpleLegacyConfiguration.class);

    assertThat(javaConfigurationModelParser.isForceNoImplicit(), is(true));
  }

  @Test
  public void getMMVForLegacyImplicitConfiguration() {
    JavaConfigurationModelParser javaConfigurationModelParser =
        getParser(LegacyAnnotationsExtension.class, NoImplicitLegacyConfiguration.class);
    assertThat(javaConfigurationModelParser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.3"));
    assertThat(javaConfigurationModelParser.getResolvedMinMuleVersion().get().getReason(),
               is("Configuration NoImplicitLegacyConfiguration has min mule version 4.3 because it is annotated with NoImplicit. NoImplicit was introduced in Mule 4.3."));
  }

  @Test
  public void getMMVForSdkImplicitConfiguration() {
    JavaConfigurationModelParser javaConfigurationModelParser =
        getParser(LegacyAnnotationsExtension.class, NoImplicitSdkConfiguration.class);
    assertThat(javaConfigurationModelParser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5"));
    assertThat(javaConfigurationModelParser.getResolvedMinMuleVersion().get().getReason(),
               is("Configuration NoImplicitSdkConfiguration has min mule version 4.5 because it is annotated with NoImplicit. NoImplicit was introduced in Mule 4.5."));
  }

  @Test
  public void getMMVForLegacyAnnotationConfiguration() {
    JavaConfigurationModelParser javaConfigurationModelParser =
        getParser(LegacyAnnotationsExtension.class, LegacyAnnotationConfiguration.class);
    assertThat(javaConfigurationModelParser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.1"));
    assertThat(javaConfigurationModelParser.getResolvedMinMuleVersion().get().getReason(),
               is("Configuration LegacyAnnotationConfiguration has min mule version 4.1 because it is the default value."));
  }

  @Test
  public void getMMVForSdkAnnotationConfiguration() {
    JavaConfigurationModelParser javaConfigurationModelParser =
        getParser(LegacyAnnotationsExtension.class, SdkAnnotationConfiguration.class);
    assertThat(javaConfigurationModelParser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5"));
    assertThat(javaConfigurationModelParser.getResolvedMinMuleVersion().get().getReason(),
               is("Configuration SdkAnnotationConfiguration has min mule version 4.5 because it is annotated with Configuration. Configuration was introduced in Mule 4.5."));
  }

  @Test
  public void getMMVForParameterizedConfiguration() {
    JavaConfigurationModelParser javaConfigurationModelParser =
        getParser(LegacyAnnotationsExtension.class, ParameterizedConfiguration.class);
    assertThat(javaConfigurationModelParser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(javaConfigurationModelParser.getResolvedMinMuleVersion().get().getReason(),
               is("Configuration ParameterizedConfiguration has min mule version 4.4 because of its field configField. Field configField has min mule version 4.4 because it is annotated with Parameter. Parameter was introduced in Mule 4.4."));
  }

  @Test
  public void getMMVForExtendsParameterizedConfiguration() {
    JavaConfigurationModelParser javaConfigurationModelParser =
        getParser(LegacyAnnotationsExtension.class, ExtendsParameterizedConfiguration.class);
    assertThat(javaConfigurationModelParser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(javaConfigurationModelParser.getResolvedMinMuleVersion().get().getReason(),
               is("Configuration ExtendsParameterizedConfiguration has min mule version 4.4 because of its super class ParameterizedConfiguration. Configuration ParameterizedConfiguration has min mule version 4.4 because of its field configField. Field configField has min mule version 4.4 because it is annotated with Parameter. Parameter was introduced in Mule 4.4."));
  }

  @Test
  public void getMMVForAnnotatedConfiguration() {
    JavaConfigurationModelParser javaConfigurationModelParser =
        getParser(LegacyAnnotationsExtension.class, AnnotatedConfiguration.class);
    assertThat(javaConfigurationModelParser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.1"));
    assertThat(javaConfigurationModelParser.getResolvedMinMuleVersion().get().getReason(),
               is("Configuration AnnotatedConfiguration has min mule version 4.1 because it is the default value."));
  }

  @Test
  public void getMMVForConfigurationFromExtensionWithSdkConfigurationsAnnotation() {
    JavaConfigurationModelParser javaConfigurationModelParser =
        getParser(SimpleLegacyExtension.class, SimpleLegacyConfiguration.class);
    assertThat(javaConfigurationModelParser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5"));
    assertThat(javaConfigurationModelParser.getResolvedMinMuleVersion().get().getReason(),
               is("Configuration SimpleLegacyConfiguration has min mule version 4.5 because it was propagated from the annotation (either @Configurations or @Config) used to reference this configuration."));
  }

  protected JavaConfigurationModelParser getParser(Class<?> extension, Class<?> configuration) {
    ClassTypeLoader typeLoader =
        ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(Thread.currentThread().getContextClassLoader());
    ExtensionElement extensionElement = new ExtensionTypeWrapper<>(extension, typeLoader);
    ConfigurationElement configurationElement = extensionElement.getConfigurations().stream()
        .filter(conf -> conf.getTypeName().equals(configuration.getName())).findFirst()
        .orElseThrow(() -> new IllegalStateException(format("Configuration %s was not found among the declared configuration in the extension",
                                                            configuration.getName(), extension.getName())));

    ExtensionLoadingContext ctx = ctxResolvingMinMuleVersion();
    JavaExtensionModelParser javaExtensionModelParser = new JavaExtensionModelParser(extensionElement, ctx);
    return new JavaConfigurationModelParser(javaExtensionModelParser, extensionElement, configurationElement, ctx);
  }

  @Extension(name = "SimpleSdkExtension")
  @Configurations({SimpleSdkConfiguration.class})
  private static class SimpleSdkExtension {
  }

  @Configuration(name = "newSdkConfiguration")
  @NoImplicit
  private static class SimpleSdkConfiguration {
  }

  @Extension(name = "SimpleLegacyExtension")
  @Configurations({SimpleLegacyConfiguration.class})
  private static class SimpleLegacyExtension {
  }

  @org.mule.runtime.extension.api.annotation.Configuration(name = "oldLegacyConfiguration")
  @org.mule.runtime.extension.api.annotation.NoImplicit
  private static class SimpleLegacyConfiguration {
  }

  @org.mule.runtime.extension.api.annotation.Extension(name = "SimpleLegacyExtensionWithOperations")
  @org.mule.runtime.extension.api.annotation.Configurations({NoImplicitLegacyConfiguration.class,
      NoImplicitSdkConfiguration.class, LegacyAnnotationConfiguration.class, SdkAnnotationConfiguration.class,
      ParameterizedConfiguration.class, AnnotatedConfiguration.class, ExtendsParameterizedConfiguration.class})
  private static class LegacyAnnotationsExtension {
  }

  @org.mule.runtime.extension.api.annotation.Configuration(name = "NoImplicitLegacyConfiguration")
  @org.mule.runtime.extension.api.annotation.NoImplicit
  private static class NoImplicitLegacyConfiguration {
  }

  @org.mule.runtime.extension.api.annotation.Configuration(name = "NoImplicitSdkConfiguration")
  @NoImplicit
  private static class NoImplicitSdkConfiguration {
  }

  @org.mule.runtime.extension.api.annotation.Configuration(name = "LegacyAnnotationConfiguration")
  private static class LegacyAnnotationConfiguration {
  }

  @Configuration(name = "SdkAnnotationConfiguration")
  private static class SdkAnnotationConfiguration {
  }

  @org.mule.runtime.extension.api.annotation.Configuration(name = "ParameterizedConfiguration")
  private static class ParameterizedConfiguration {

    @org.mule.sdk.api.annotation.param.Parameter
    String configField;
  }

  @org.mule.runtime.extension.api.annotation.Configuration(name = "ExtendsParameterizedConfiguration")
  private static class ExtendsParameterizedConfiguration extends ParameterizedConfiguration {
  }

  @org.mule.runtime.extension.api.annotation.Configuration(name = "ParameterizedConfiguration")
  @Operations(SimpleOperations.class)
  @Sources(SdkSource.class)
  @ConnectionProviders(SdkConnectionProvider.class)
  private static class AnnotatedConfiguration {
  }

  private static class SimpleOperations {
  }

  private static class SdkConnectionProvider implements ConnectionProvider<String> {

    @Override
    public String connect() throws ConnectionException {
      return null;
    }

    @Override
    public void disconnect(String connection) {

    }

    @Override
    public ConnectionValidationResult validate(String connection) {
      return null;
    }
  }

  private static class SdkSource extends Source<String, String> {

    @Override
    public void onStart(SourceCallback<String, String> sourceCallback) throws MuleException {

    }

    @Override
    public void onStop() {

    }
  }

  @Extension(name = "SimpleSdkExtension")
  @Configurations({SimpleWronglyAnnotatedConfiguration.class})
  private static class SimpleSdkExtensionWithWronglyAnnotatedConfiguration {
  }

  @Configuration(name = "wronglySdkConfiguration")
  @org.mule.runtime.extension.api.annotation.Configuration(name = "wronglyLegacyConfiguration")
  private static class SimpleWronglyAnnotatedConfiguration {
  }

}
