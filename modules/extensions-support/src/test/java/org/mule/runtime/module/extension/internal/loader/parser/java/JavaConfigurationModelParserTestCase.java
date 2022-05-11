/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;

import org.mule.metadata.api.ClassTypeLoader;
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

public class JavaConfigurationModelParserTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void getConfigurationNameFromConfigurationUsingSdkApi() {
    JavaConfigurationModelParser javaConfigurationModelParser = getParser(SimpleSdkExtension.class);
    assertThat(javaConfigurationModelParser.getName(), is("newSdkConfiguration"));
  }

  @Test
  public void getConfigurationNameFromConfigurationUsingLegacyApi() {
    JavaConfigurationModelParser javaConfigurationModelParser = getParser(SimpleLegacyExtension.class);
    assertThat(javaConfigurationModelParser.getName(), is("oldLegacyConfiguration"));
  }

  @Test
  public void getConfigurationNameFromConfigurationUsingSdkAndLegacyApi() {
    expectedException.expect(instanceOf(IllegalModelDefinitionException.class));
    expectedException.expectMessage("Annotations org.mule.runtime.extension.api.annotation.Configuration and " +
        "org.mule.sdk.api.annotation.Configuration are both present at the same time on Configuration SimpleWronglyAnnotatedConfiguration");

    JavaConfigurationModelParser javaConfigurationModelParser =
        getParser(SimpleSdkExtensionWithWronglyAnnotatedConfiguration.class);
    javaConfigurationModelParser.getName();
  }

  @Test
  public void isForceNoImplicitOnConfigurationUsingTheSdkApi() {
    JavaConfigurationModelParser javaConfigurationModelParser = getParser(SimpleSdkExtension.class);

    assertThat(javaConfigurationModelParser.isForceNoImplicit(), is(true));
  }

  @Test
  public void isForceNoImplicitOnConfigurationUsingTheLegacyApi() {
    JavaConfigurationModelParser javaConfigurationModelParser = getParser(SimpleLegacyExtension.class);

    assertThat(javaConfigurationModelParser.isForceNoImplicit(), is(true));
  }

  private JavaConfigurationModelParser getParser(Class<?> extension) {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    ExtensionLoadingContext ctx = new DefaultExtensionLoadingContext(contextClassLoader, getDefault(emptySet()));
    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(contextClassLoader);
    ExtensionElement extensionElement = new ExtensionTypeWrapper<>(extension, typeLoader);
    ConfigurationElement configurationElement = extensionElement.getConfigurations().get(0);
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

  @Extension(name = "SimpleSdkExtension")
  @Configurations({SimpleWronglyAnnotatedConfiguration.class})
  private static class SimpleSdkExtensionWithWronglyAnnotatedConfiguration {
  }

  @Configuration(name = "wronglySdkConfiguration")
  @org.mule.runtime.extension.api.annotation.Configuration(name = "wronglyLegacyConfiguration")
  private static class SimpleWronglyAnnotatedConfiguration {
  }

}
