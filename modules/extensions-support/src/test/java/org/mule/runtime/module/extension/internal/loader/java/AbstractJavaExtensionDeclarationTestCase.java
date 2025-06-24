/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.mule.runtime.config.internal.dsl.utils.DslConstants.CONFIG_ATTRIBUTE_NAME;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.extension.api.ExtensionConstants.REDELIVERY_POLICY_PARAMETER_NAME;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.assertType;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
import org.mule.runtime.extension.api.declaration.type.RedeliveryPolicyTypeBuilder;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.module.extension.internal.util.ExtensionDeclarationTestUtils;

import java.util.List;

public abstract class AbstractJavaExtensionDeclarationTestCase extends AbstractMuleTestCase {

  private ExtensionDeclarer declarer;

  protected ExtensionDeclarer getDeclarer() {
    return declarer;
  }

  protected void setDeclarer(ExtensionDeclarer declarer) {
    this.declarer = declarer;
  }

  protected ExtensionDeclarer declarerFor(final Class<?> type) {
    return ExtensionDeclarationTestUtils.declarerFor(type, getProductVersion());
  }

  protected ExtensionDeclarer declarerFor(final Class<?> type, String version) {
    return ExtensionDeclarationTestUtils.declarerFor(type, version);
  }

  protected ExtensionDeclarer declarerFor(Class<?> type, ExtensionLoadingContext ctx) {
    return declarerFor(type, getProductVersion(), ctx);
  }

  protected ExtensionDeclarer declarerFor(Class<?> type, String version, ExtensionLoadingContext ctx) {
    return ExtensionDeclarationTestUtils.declarerFor(type, version, ctx);
  }

  protected ExtensionDeclarer declareExtension() {
    return declarer;
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

  protected void assertRedeliveryPolicyParameter(ParameterModel redelivery) {
    assertThat(redelivery.getName(), is(REDELIVERY_POLICY_PARAMETER_NAME));
    assertThat(redelivery.getType(), is(equalTo(new RedeliveryPolicyTypeBuilder().buildRedeliveryPolicyType())));
  }
}
