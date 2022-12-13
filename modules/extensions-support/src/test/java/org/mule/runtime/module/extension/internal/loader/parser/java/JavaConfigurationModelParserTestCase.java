/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.ConfigurationElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;
import org.mule.sdk.api.annotation.Configuration;
import org.mule.sdk.api.annotation.Configurations;
import org.mule.sdk.api.annotation.Extension;
import org.mule.sdk.api.annotation.NoImplicit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.sdk.api.annotation.Operations;
import org.mule.sdk.api.annotation.Sources;
import org.mule.sdk.api.annotation.connectivity.ConnectionProviders;
import org.mule.sdk.api.connectivity.ConnectionProvider;
import org.mule.sdk.api.connectivity.ConnectionValidationResult;
import org.mule.sdk.api.runtime.source.Source;
import org.mule.sdk.api.runtime.source.SourceCallback;

import java.util.Optional;

public class JavaConfigurationModelParserTestCase {

  @Rule
  public ExpectedException expectedException = none();

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
    expectedException.expect(instanceOf(IllegalModelDefinitionException.class));
    expectedException.expectMessage("Annotations org.mule.runtime.extension.api.annotation.Configuration and " +
        "org.mule.sdk.api.annotation.Configuration are both present at the same time on Configuration SimpleWronglyAnnotatedConfiguration");

    JavaConfigurationModelParser javaConfigurationModelParser =
        getParser(SimpleSdkExtensionWithWronglyAnnotatedConfiguration.class, SimpleWronglyAnnotatedConfiguration.class);
    javaConfigurationModelParser.getName();
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
    Optional<MuleVersion> minMuleVersion = javaConfigurationModelParser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get().toString(), is("4.3"));
  }

  @Test
  public void getMMVForSdkImplicitConfiguration() {
    JavaConfigurationModelParser javaConfigurationModelParser =
        getParser(LegacyAnnotationsExtension.class, NoImplicitSdkConfiguration.class);
    Optional<MuleVersion> minMuleVersion = javaConfigurationModelParser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get().toString(), is("4.5.0"));
  }

  @Test
  public void getMMVForLegacyAnnotationConfiguration() {
    JavaConfigurationModelParser javaConfigurationModelParser =
        getParser(LegacyAnnotationsExtension.class, LegacyAnnotationConfiguration.class);
    Optional<MuleVersion> minMuleVersion = javaConfigurationModelParser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get().toString(), is("4.1.1"));
  }

  @Test
  public void getMMVForSdkAnnotationConfiguration() {
    JavaConfigurationModelParser javaConfigurationModelParser =
        getParser(LegacyAnnotationsExtension.class, SdkAnnotationConfiguration.class);
    Optional<MuleVersion> minMuleVersion = javaConfigurationModelParser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get().toString(), is("4.5.0"));
  }

  @Test
  public void getMMVForParameterizedConfiguration() {
    JavaConfigurationModelParser javaConfigurationModelParser =
        getParser(LegacyAnnotationsExtension.class, ParameterizedConfiguration.class);
    Optional<MuleVersion> minMuleVersion = javaConfigurationModelParser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get().toString(), is("4.4"));
  }

  @Test
  public void getMMVForExtendsParameterizedConfiguration() {
    JavaConfigurationModelParser javaConfigurationModelParser =
        getParser(LegacyAnnotationsExtension.class, ExtendsParameterizedConfiguration.class);
    Optional<MuleVersion> minMuleVersion = javaConfigurationModelParser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get().toString(), is("4.4"));
  }

  @Test
  public void getMMVForAnnotatedConfiguration() {
    JavaConfigurationModelParser javaConfigurationModelParser =
        getParser(LegacyAnnotationsExtension.class, AnnotatedConfiguration.class);
    Optional<MuleVersion> minMuleVersion = javaConfigurationModelParser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get().toString(), is("4.1.1"));
  }

  @Test
  public void getMMVForConfigurationFromExtensionWithSdkConfigurationsAnnotation() {
    JavaConfigurationModelParser javaConfigurationModelParser =
        getParser(SimpleLegacyExtension.class, SimpleLegacyConfiguration.class);
    Optional<MuleVersion> minMuleVersion = javaConfigurationModelParser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get().toString(), is("4.5.0"));
  }

  private JavaConfigurationModelParser getParser(Class<?> extension, Class<?> configuration) {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    ExtensionLoadingContext ctx = new DefaultExtensionLoadingContext(contextClassLoader, getDefault(emptySet()));
    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(contextClassLoader);
    ExtensionElement extensionElement = new ExtensionTypeWrapper<>(extension, typeLoader);
    ConfigurationElement configurationElement = extensionElement.getConfigurations().stream()
        .filter(conf -> conf.getTypeName().equals(configuration.getName())).findFirst()
        .orElseThrow(() -> new IllegalStateException(format("Configuration %s was not found among the declared configuration in the extension",
                                                            configuration.getName(), extension.getName())));
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
