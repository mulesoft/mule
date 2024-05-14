/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.api.meta.model.operation.ExecutionType.CPU_LITE;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.MuleSdkApplicationExtensionModelParser.APP_LOCAL_EXTENSION_NAMESPACE;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.Utils.setMockAstChild;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.Utils.mockDeprecatedAst;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.Utils.mockOutputAst;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.Utils.singleParameterAst;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;

import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.internal.model.DefaultExtensionModelHelper;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Optional;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(REUSE)
@Story(OPERATIONS)
public class MuleSdkOperationModelParserTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expected = none();

  private MuleSdkOperationModelParser operationModelParser;
  private ComponentAst operationAst;
  private MetadataType someMetadataType;

  @Before
  public void setup() {
    ComponentParameterAst operationName = singleParameterAst("mockName");

    operationAst = mock(ComponentAst.class);
    when(operationAst.getParameter(DEFAULT_GROUP_NAME, "name")).thenReturn(operationName);

    TypeLoader typeLoader = mock(TypeLoader.class);
    someMetadataType = mock(MetadataType.class);
    when(typeLoader.load("some")).thenReturn(Optional.of(someMetadataType));

    operationModelParser = new MuleSdkOperationModelParser(operationAst, APP_LOCAL_EXTENSION_NAMESPACE, typeLoader,
                                                           new DefaultExtensionModelHelper(emptySet()));
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
    setMockAstChild(operationAst, "deprecated", deprecatedAst);

    assertThat(operationModelParser.getDeprecationModel().isPresent(), is(true));

    DeprecationModel deprecationModel = operationModelParser.getDeprecationModel().get();
    assertThat(deprecationModel.getDeprecatedSince(), is("1.1.0"));
    assertThat(deprecationModel.getMessage(), is("Some Message"));
    assertThat(deprecationModel.getToRemoveIn(), is(Optional.of("2.0.0")));
  }

  @Test
  public void when_toRemoveInParameterIsNotConfigured_then_theDeprecationModelReturnsAnEmptyOptional() {
    ComponentAst deprecatedAst = mockDeprecatedAst("1.1.0", "Some Message", null);
    setMockAstChild(operationAst, "deprecated", deprecatedAst);

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
    setMockAstChild(operationAst, "output", output);

    expected.expect(IllegalOperationModelDefinitionException.class);
    expected.expectMessage("Operation 'mockName' is missing its <payload-type> declaration");
    operationModelParser.getOutputType();
  }

  @Test
  public void when_anOperationOutputHasAPayloadTypeNotInTheTypeLoader_then_anExceptionIsRaised() {
    ComponentAst output = mockOutputAst("notDefined", null);
    setMockAstChild(operationAst, "output", output);

    expected.expect(IllegalModelDefinitionException.class);
    expected
        .expectMessage("Component <this:payload-type> defines type as 'notDefined' but such type is not defined in the application");
    operationModelParser.getOutputType();
  }

  @Test
  public void when_anOperationOutputHasAPayloadTypeInTheTypeLoader_then_theOutputTypeIsRetrievedFromThere() {
    ComponentAst output = mockOutputAst("some", null);
    setMockAstChild(operationAst, "output", output);

    assertThat(operationModelParser.getOutputType().getType(), is(someMetadataType));
  }
}
