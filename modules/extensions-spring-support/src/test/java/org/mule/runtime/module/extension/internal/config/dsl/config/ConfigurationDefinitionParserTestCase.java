/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.config;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.spy;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.util.ClassUtils.setContextClassLoader;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.core.internal.el.datetime.Date;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition.Builder;
import org.mule.runtime.dsl.api.component.TypeConverter;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.ExtensionModelFactory;
import org.mule.runtime.internal.dsl.DefaultDslResolvingContext;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionParsingContext;
import org.mule.runtime.module.extension.internal.config.dsl.config.extension.SimpleExtension;
import org.mule.runtime.module.extension.internal.loader.delegate.DefaultExtensionModelLoaderDelegate;
import org.mule.tck.classlaoder.TestClassLoader;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.mockito.internal.invocation.InterceptedInvocation;

public class ConfigurationDefinitionParserTestCase {

  @Test
  public void typeConverterUsesTheClassloaderOfTheParser() throws Exception {
    List<ComponentBuildingDefinition> componentBuildingDefinitions;

    Thread thread = Thread.currentThread();
    ClassLoader currentClassLoader = thread.getContextClassLoader();
    TestClassLoader classLoader = spy(getTestClassLoader());
    setContextClassLoader(thread, currentClassLoader, classLoader);
    try {
      Builder<?> definitionBuilder = new Builder<>().withIdentifier("test").withNamespace("namespace");

      ExtensionLoadingContext ctx = new DefaultExtensionLoadingContext(classLoader, getDefault(emptySet()));
      DefaultExtensionModelLoaderDelegate loader = new DefaultExtensionModelLoaderDelegate(SimpleExtension.class, "1.0.0-dev");
      loader.declare(ctx);
      ExtensionModelFactory factory = new ExtensionModelFactory();
      ExtensionModel extensionModel = factory.create(ctx);
      ConfigurationModel configurationModel = extensionModel.getConfigurationModels().get(0);

      DslResolvingContext dslResolvingContext = new DefaultDslResolvingContext(singleton(extensionModel));
      DslSyntaxResolver dslSyntaxResolver = DslSyntaxResolver.getDefault(extensionModel, dslResolvingContext);
      ExtensionParsingContext extensionParsingContext = new ExtensionParsingContext(extensionModel);

      ConfigurationDefinitionParser configurationDefinitionParser =
          new ConfigurationDefinitionParser(definitionBuilder, extensionModel, configurationModel, dslSyntaxResolver,
                                            extensionParsingContext);

      componentBuildingDefinitions = configurationDefinitionParser.parse();
    } finally {
      setContextClassLoader(thread, classLoader, currentClassLoader);
    }

    Optional<TypeConverter<?, ?>> typeConverter = componentBuildingDefinitions.get(1).getTypeConverter();

    int testClassLoaderInvocations = mockingDetails(classLoader).getInvocations().size();
    typeConverter.get().convert(null);
    List<?> invocations = ((List<?>) mockingDetails(classLoader).getInvocations());
    String invocation = ((InterceptedInvocation) invocations.get(invocations.size() - 1)).getArgument(0);

    assertThat(testClassLoaderInvocations + 2, is(invocations.size()));
    assertThat(invocation, is("org.mule.runtime.core.internal.el.datetime.Date"));
  }

  private TestClassLoader getTestClassLoader() {
    TestClassLoader classLoader = new TestClassLoader(null);
    classLoader.addClass("java.util.Map", Map.class);
    classLoader.addClass("org.mule.runtime.core.internal.el.datetime.Date", Date.class);
    return classLoader;
  }

}
