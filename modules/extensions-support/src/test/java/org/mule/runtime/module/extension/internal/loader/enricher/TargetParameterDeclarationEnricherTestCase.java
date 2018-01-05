/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import org.junit.Before;
import org.junit.Test;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.enricher.TargetParameterDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaModelLoaderDelegate;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExpressionSupport.REQUIRED;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.module.extension.internal.loader.enricher.EnricherTestUtils.getNamedObject;

public class TargetParameterDeclarationEnricherTestCase {

  private ExtensionDeclaration declaration = null;

  @Before
  public void setUp() {
    ExtensionDeclarer declarer = new DefaultJavaModelLoaderDelegate(HeisenbergExtension.class, getProductVersion())
        .declare(new DefaultExtensionLoadingContext(getClass().getClassLoader(), getDefault(emptySet())));
    new TargetParameterDeclarationEnricher()
        .enrich(new DefaultExtensionLoadingContext(declarer, this.getClass().getClassLoader(), getDefault(emptySet())));
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
