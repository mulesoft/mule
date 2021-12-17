/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.internal.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.assertType;

import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithOperationsDeclaration;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.delegate.DefaultExtensionModelLoaderDelegate;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.List;

public abstract class AbstractJavaExtensionDeclarationTestCase extends AbstractMuleTestCase {

  private DefaultExtensionModelLoaderDelegate loader;

  protected DefaultExtensionModelLoaderDelegate getLoader() {
    return loader;
  }

  protected void setLoader(DefaultExtensionModelLoaderDelegate loader) {
    this.loader = loader;
  }

  protected DefaultExtensionModelLoaderDelegate loaderFor(final Class<?> type) {
    return new DefaultExtensionModelLoaderDelegate(type, getProductVersion());
  }

  protected ExtensionDeclarer declareExtension() {
    return declareExtension(new DefaultExtensionLoadingContext(getClass().getClassLoader(), getDefault(emptySet())));
  }

  protected ExtensionDeclarer declareExtension(ExtensionLoadingContext context) {
    return getLoader().declare(context);
  }

  protected ConfigurationDeclaration getConfiguration(ExtensionDeclaration extensionDeclaration, final String configurationName) {
    return extensionDeclaration.getConfigurations()
        .stream()
        .filter(config -> config.getName().equals(configurationName))
        .findAny()
        .orElse(null);
  }

  protected OperationDeclaration getOperation(WithOperationsDeclaration declaration, final String operationName) {
    List<OperationDeclaration> operations = declaration.getOperations();
    return operations.stream()
        .filter(operation -> operation.getName().equals(operationName))
        .findAny()
        .orElse(null);
  }

  protected Pair<ParameterGroupDeclaration, ParameterDeclaration> findParameterInGroup(ParameterizedDeclaration<?> declaration,
                                                                                       String name) {
    return declaration.getParameterGroups().stream()
        .map(group -> {
          ParameterDeclaration parameter = findParameter(group.getParameters(), name);
          return parameter != null ? new Pair<>(group, parameter) : null;
        })
        .filter(pair -> pair != null)
        .findFirst().orElse(null);
  }

  protected ParameterDeclaration findParameter(List<ParameterDeclaration> parameters, final String name) {
    return parameters.stream()
        .filter(param -> param.getName().equals(name))
        .findAny()
        .orElse(null);
  }

  protected void assertConfigRefParam(ParameterModel configRef) {
    assertThat(configRef.getName(), is(CONFIG_ATTRIBUTE_NAME));
    assertType(configRef.getType(), ConfigurationProvider.class, ObjectType.class);
  }
}
