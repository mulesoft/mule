/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExpressionSupport.REQUIRED;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getNamedObject;
import static org.mule.test.module.extension.internal.util.ExtensionDeclarationTestUtils.declarerFor;

import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.enricher.TargetParameterDeclarationEnricher;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import org.junit.Before;
import org.junit.Test;

public class TargetParameterDeclarationEnricherTestCase {

  private ExtensionDeclaration declaration = null;

  @Before
  public void setUp() {
    ExtensionDeclarer declarer = declarerFor(HeisenbergExtension.class, getProductVersion());
    new TargetParameterDeclarationEnricher()
        .enrich(new DefaultExtensionLoadingContext(declarer, getClass().getClassLoader(), getDefault(emptySet())));
    declaration = declarer.getDeclaration();
  }

  @Test
  public void verifyTargetParameterOnOperation() {
    OperationDeclaration operationDeclaration =
        getNamedObject(declaration.getOperations(), "transform");
    ParameterDeclaration parameterDeclaration = getNamedObject(operationDeclaration.getAllParameters(), "target");

    assertThat(parameterDeclaration.getName(), is("target"));
    assertThat(parameterDeclaration.getExpressionSupport(), is(NOT_SUPPORTED));
    assertThat(parameterDeclaration.getType(), instanceOf(StringType.class));
    assertThat(parameterDeclaration.isRequired(), is(false));
  }

  @Test
  public void verifyTargetValueParameterOnOperation() {
    OperationDeclaration operationDeclaration =
        getNamedObject(declaration.getOperations(), "transform");
    ParameterDeclaration parameterDeclaration = getNamedObject(operationDeclaration.getAllParameters(), "targetValue");

    assertThat(parameterDeclaration.getName(), is("targetValue"));
    assertThat(parameterDeclaration.getExpressionSupport(), is(REQUIRED));
    assertThat(parameterDeclaration.getType(), instanceOf(StringType.class));
    assertThat(parameterDeclaration.isRequired(), is(false));
  }

}
