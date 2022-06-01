/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static java.util.Optional.empty;
import static java.util.stream.Stream.of;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.api.meta.model.operation.ExecutionType.CPU_LITE;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;

import org.junit.Before;
import org.junit.Test;
import org.mule.metadata.api.TypeLoader;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;

import java.util.Optional;

public class MuleSdkOperationModelParserSdkTestCase {

  private MuleSdkOperationModelParserSdk operationModelParser;
  private ComponentAst operationAst;

  @Before
  public void setup() {
    ComponentParameterAst operationName = stringParameterAst("mockName");

    operationAst = mock(ComponentAst.class);
    when(operationAst.getParameter(DEFAULT_GROUP_NAME, "name")).thenReturn(operationName);

    TypeLoader typeLoader = mock(TypeLoader.class);
    operationModelParser = new MuleSdkOperationModelParserSdk(operationAst, typeLoader);
  }

  // ------------------------------- //
  // Deprecation
  // ------------------------------- //

  @Test
  public void when_operationAstHasNotDeprecationParameter_then_parserHasNotDeprecationModel() {
    assertThat(operationModelParser.getDeprecationModel().isPresent(), is(false));
  }

  @Test
  public void when_operationAstHasDeprecationParameter_then_parserHasDeprecationModelWithCorrespondingValues() {
    ComponentAst deprecatedAst = mockDeprecatedAst("1.1.0", "Some Message", "2.0.0");
    when(operationAst.directChildrenStreamByIdentifier(null, "deprecated")).thenAnswer(invocation -> of(deprecatedAst));

    assertThat(operationModelParser.getDeprecationModel().isPresent(), is(true));

    DeprecationModel deprecationModel = operationModelParser.getDeprecationModel().get();
    assertThat(deprecationModel.getDeprecatedSince(), is("1.1.0"));
    assertThat(deprecationModel.getMessage(), is("Some Message"));
    assertThat(deprecationModel.getToRemoveIn(), is(Optional.of("2.0.0")));
  }

  @Test
  public void when_toRemoveInParameterIsNotConfigured_then_theDeprecationModelReturnsAnEmptyOptional() {
    ComponentAst deprecatedAst = mockDeprecatedAst("1.1.0", "Some Message", null);
    when(operationAst.directChildrenStreamByIdentifier(null, "deprecated")).thenAnswer(invocation -> of(deprecatedAst));

    assertThat(operationModelParser.getDeprecationModel().isPresent(), is(true));

    DeprecationModel deprecationModel = operationModelParser.getDeprecationModel().get();
    assertThat(deprecationModel.getToRemoveIn(), is(empty()));
  }

  // ------------------------------- //
  // Execution Type
  // ------------------------------- //

  @Test
  public void when_weHaveAnOperation_then_theExecutionTypeIsCpuLite() {
    assertThat(operationModelParser.getExecutionType(), is(Optional.of(CPU_LITE)));
  }

  private ComponentAst mockDeprecatedAst(String since, String message, String toRemoveIn) {
    ComponentParameterAst sinceAst = stringParameterAst(since);
    ComponentParameterAst messageAst = stringParameterAst(message);
    ComponentParameterAst toRemoveInAst = stringParameterAst(toRemoveIn);

    ComponentAst deprecatedAst = mock(ComponentAst.class);
    when(deprecatedAst.getParameter(DEFAULT_GROUP_NAME, "since")).thenReturn(sinceAst);
    when(deprecatedAst.getParameter(DEFAULT_GROUP_NAME, "message")).thenReturn(messageAst);
    when(deprecatedAst.getParameter(DEFAULT_GROUP_NAME, "toRemoveIn")).thenReturn(toRemoveInAst);

    return deprecatedAst;
  }

  private ComponentParameterAst stringParameterAst(String value) {
    ComponentParameterAst parameterAst = mock(ComponentParameterAst.class);
    when(parameterAst.getValue()).thenReturn(right(value));
    return parameterAst;
  }
}
