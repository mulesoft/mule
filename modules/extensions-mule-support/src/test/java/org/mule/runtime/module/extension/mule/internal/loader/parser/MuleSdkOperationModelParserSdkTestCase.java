/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.api.meta.model.operation.ExecutionType.CPU_LITE;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.Utils.mockDeprecatedAst;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.Utils.stringParameterAst;
import static java.util.Optional.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MuleSdkOperationModelParserSdkTestCase {

  @Rule
  public ExpectedException expected = none();

  private MuleSdkOperationModelParserSdk operationModelParser;
  private ComponentAst operationAst;
  private MetadataType someMetadataType;

  @Before
  public void setup() {
    ComponentParameterAst operationName = stringParameterAst("mockName");

    operationAst = mock(ComponentAst.class);
    when(operationAst.getParameter(DEFAULT_GROUP_NAME, "name")).thenReturn(operationName);

    TypeLoader typeLoader = mock(TypeLoader.class);
    someMetadataType = mock(MetadataType.class);
    when(typeLoader.load("some")).thenReturn(Optional.of(someMetadataType));

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
    when(operationAst.directChildrenStreamByIdentifier(null, "deprecated")).thenAnswer(invocation -> Stream.of(deprecatedAst));

    assertThat(operationModelParser.getDeprecationModel().isPresent(), is(true));

    DeprecationModel deprecationModel = operationModelParser.getDeprecationModel().get();
    assertThat(deprecationModel.getDeprecatedSince(), is("1.1.0"));
    assertThat(deprecationModel.getMessage(), is("Some Message"));
    assertThat(deprecationModel.getToRemoveIn(), is(Optional.of("2.0.0")));
  }

  @Test
  public void when_toRemoveInParameterIsNotConfigured_then_theDeprecationModelReturnsAnEmptyOptional() {
    ComponentAst deprecatedAst = mockDeprecatedAst("1.1.0", "Some Message", null);
    when(operationAst.directChildrenStreamByIdentifier(null, "deprecated")).thenAnswer(invocation -> Stream.of(deprecatedAst));

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

  // ------------------------------- //
  // Output Type
  // ------------------------------- //

  @Test
  public void when_anOperationHasNotOutput_then_anExceptionIsRaised() {
    expected.expect(IllegalOperationModelDefinitionException.class);
    expected.expectMessage("Operation 'mockName' is missing its <output> declaration");
    operationModelParser.getOutputType();
  }

  @Test
  public void when_anOperationOutputHasNotPayloadType_then_anExceptionIsRaised() {
    ComponentAst output = mockOutputAst(null, null);
    when(operationAst.directChildrenStreamByIdentifier(null, "output")).thenAnswer(invocation -> Stream.of(output));

    expected.expect(IllegalOperationModelDefinitionException.class);
    expected.expectMessage("Operation 'mockName' is missing its <payload-type> declaration");
    operationModelParser.getOutputType();
  }

  @Test
  public void when_anOperationOutputHasAPayloadTypeNotInTheTypeLoader_then_anExceptionIsRaised() {
    ComponentAst output = mockOutputAst("notDefined", null);
    when(operationAst.directChildrenStreamByIdentifier(null, "output")).thenAnswer(invocation -> Stream.of(output));

    expected.expect(IllegalModelDefinitionException.class);
    expected
        .expectMessage("Component <this:payload-type> defines type as 'notDefined' but such type is not defined in the application");
    operationModelParser.getOutputType();
  }

  @Test
  public void when_anOperationOutputHasAPayloadTypeInTheTypeLoader_then_theOutputTypeIsRetrievedFromThere() {
    ComponentAst output = mockOutputAst("some", null);
    when(operationAst.directChildrenStreamByIdentifier(null, "output")).thenAnswer(invocation -> Stream.of(output));

    assertThat(operationModelParser.getOutputType().getType(), is(someMetadataType));
  }

  private ComponentAst mockOutputAst(String payloadType, String attributesType) {
    ComponentAst outputAst = mock(ComponentAst.class);
    when(outputAst.directChildrenStreamByIdentifier(null, "payload-type")).thenAnswer(invocation -> {
      if (payloadType != null) {
        ComponentParameterAst typeParameterAst = stringParameterAst(payloadType);
        ComponentAst payloadTypeAst = mock(ComponentAst.class);
        when(payloadTypeAst.getIdentifier())
            .thenReturn(ComponentIdentifier.builder().namespace("this").name("payload-type").build());
        when(payloadTypeAst.getParameter(DEFAULT_GROUP_NAME, "type")).thenReturn(typeParameterAst);
        return Stream.of(payloadTypeAst);
      } else {
        return Stream.empty();
      }
    });
    return outputAst;
  }
}
