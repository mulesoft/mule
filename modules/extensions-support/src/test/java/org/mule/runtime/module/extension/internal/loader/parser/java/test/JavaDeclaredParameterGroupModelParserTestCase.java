/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.test;

import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;

import static java.util.Collections.emptySet;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.ConfigurationElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;
import org.mule.runtime.extension.api.loader.parser.ParameterGroupModelParser.ExclusiveOptionalDescriptor;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterModelParserDecorator;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaDeclaredParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.ParameterDeclarationContext;
import org.mule.sdk.api.annotation.Configuration;
import org.mule.sdk.api.annotation.Configurations;
import org.mule.sdk.api.annotation.Extension;
import org.mule.sdk.api.annotation.param.ExclusiveOptionals;
import org.mule.sdk.api.annotation.param.Optional;
import org.mule.sdk.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.param.ParameterGroup;

import org.junit.Test;

public class JavaDeclaredParameterGroupModelParserTestCase {

  @Test
  public void getExclusiveOptionalDescriptorFromExtensionUsingSdkApi() {
    JavaDeclaredParameterGroupModelParser javaDeclaredParameterGroupModelParser = getParser(SimpleLegacyExtension.class);
    ExclusiveOptionalDescriptor exclusiveOptionalDescriptor = javaDeclaredParameterGroupModelParser.getExclusiveOptionals().get();

    assertThat(exclusiveOptionalDescriptor.isOneRequired(), is(true));
    assertThat(exclusiveOptionalDescriptor.getExclusiveOptionals().size(), is(2));
    assertThat(exclusiveOptionalDescriptor.getExclusiveOptionals(), hasItems("parameter1", "parameter2"));
  }

  @Test
  public void getExclusiveOptionalDescriptorFromExtensionUsingLegacyApi() {
    JavaDeclaredParameterGroupModelParser javaDeclaredParameterGroupModelParser = getParser(SimpleSdkExtension.class);
    ExclusiveOptionalDescriptor exclusiveOptionalDescriptor = javaDeclaredParameterGroupModelParser.getExclusiveOptionals().get();

    assertThat(exclusiveOptionalDescriptor.isOneRequired(), is(true));
    assertThat(exclusiveOptionalDescriptor.getExclusiveOptionals().size(), is(2));
    assertThat(exclusiveOptionalDescriptor.getExclusiveOptionals(), hasItems("parameter1", "parameter2"));
  }

  private JavaDeclaredParameterGroupModelParser getParser(Class<?> extension) {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    ExtensionLoadingContext ctx = new DefaultExtensionLoadingContext(contextClassLoader, getDefault(emptySet()));
    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(contextClassLoader);
    ExtensionElement extensionElement = new ExtensionTypeWrapper<>(extension, typeLoader);
    ConfigurationElement configurationElement = extensionElement.getConfigurations().get(0);
    ExtensionParameter extensionParameter = configurationElement.getParameters().get(0);

    return new JavaDeclaredParameterGroupModelParser(extensionParameter,
                                                     ParameterDeclarationContext.forConfig("config", ctx),
                                                     ParameterModelParserDecorator::new);
  }

  @Extension(name = "SimpleSdkExtension")
  @Configurations({SimpleSdkConfiguration.class})
  private static class SimpleSdkExtension {
  }

  @Configuration(name = "newSdkConfiguration")
  private static class SimpleSdkConfiguration {

    @ParameterGroup(name = "A nice parameter group")
    PojoWithSdkExclusiveOptional pojoWithSdkExclusiveOptional;
  }

  @ExclusiveOptionals(isOneRequired = true)
  private static class PojoWithSdkExclusiveOptional {

    @Parameter
    @Optional
    Object parameter1;

    @Parameter
    @Optional
    Object parameter2;

  }

  @Extension(name = "SimpleLegacyExtension")
  @Configurations({SimpleLegacyConfiguration.class})
  private static class SimpleLegacyExtension {
  }

  @org.mule.runtime.extension.api.annotation.Configuration(name = "oldLegacyConfiguration")
  private static class SimpleLegacyConfiguration {

    @org.mule.runtime.extension.api.annotation.param.ParameterGroup(name = "A nice parameter group")
    PojoWithLegacyExclusiveOptional pojoWithLegacyExclusiveOptional;
  }

  @org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals(isOneRequired = true)
  private static class PojoWithLegacyExclusiveOptional {

    @org.mule.runtime.extension.api.annotation.param.Parameter
    @org.mule.runtime.extension.api.annotation.param.Optional
    Object parameter1;

    @org.mule.runtime.extension.api.annotation.param.Parameter
    @org.mule.runtime.extension.api.annotation.param.Optional
    Object parameter2;

  }
}
