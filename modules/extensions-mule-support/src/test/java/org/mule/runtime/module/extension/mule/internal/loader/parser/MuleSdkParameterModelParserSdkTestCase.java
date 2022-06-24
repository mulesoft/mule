/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.Utils.mockDeprecatedAst;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.Utils.mockTypeLoader;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.Utils.setMockAstChild;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.Utils.stringParameterAst;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.PARAMETERS;
import static java.util.Collections.singletonMap;
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
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Optional;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(REUSE)
@Story(PARAMETERS)
public class MuleSdkParameterModelParserSdkTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expected = none();

  private MuleSdkParameterModelParserSdk parameterModelParser;
  private ComponentAst componentAst;
  private MetadataType someValidMetadataType;

  @Before
  public void setUp() {
    componentAst = mock(ComponentAst.class);
    ComponentParameterAst parameterNameAst = stringParameterAst("someparam");
    when(componentAst.getParameter(DEFAULT_GROUP_NAME, "name")).thenReturn(parameterNameAst);

    someValidMetadataType = mock(MetadataType.class);
    TypeLoader typeLoader = mockTypeLoader(singletonMap("somevalid", someValidMetadataType));
    parameterModelParser = new MuleSdkParameterModelParserSdk(componentAst, typeLoader);
  }

  // ------------------------------- //
  // Parameter Type
  // ------------------------------- //

  @Test
  public void invalidParameterTypeRaisesException() {
    ComponentParameterAst typeParameterAst = stringParameterAst("invalid");
    when(componentAst.getParameter(DEFAULT_GROUP_NAME, "type")).thenReturn(typeParameterAst);

    expected.expect(IllegalModelDefinitionException.class);
    expected.expectMessage("Parameter 'someparam' references unknown type 'invalid'");
    parameterModelParser.getType();
  }

  @Test
  public void parameterTypeCanNotBeVoid() {
    ComponentParameterAst typeParameterAst = stringParameterAst("void");
    when(componentAst.getParameter(DEFAULT_GROUP_NAME, "type")).thenReturn(typeParameterAst);

    expected.expect(IllegalModelDefinitionException.class);
    expected.expectMessage("Parameter 'someparam' references type 'void', which is forbidden for parameters");
    parameterModelParser.getType();
  }

  @Test
  public void parameterTypeCanBeSomeValidParameterInTheApplicationTypeLoader() {
    ComponentParameterAst typeParameterAst = stringParameterAst("somevalid");
    when(componentAst.getParameter(DEFAULT_GROUP_NAME, "type")).thenReturn(typeParameterAst);

    assertThat(parameterModelParser.getType(), is(someValidMetadataType));
  }

  // ------------------------------- //
  // Deprecation
  // ------------------------------- //

  @Test
  public void when_parameterAstHasNotDeprecationParameter_then_parserHasNotDeprecationModel() {
    assertThat(parameterModelParser.getDeprecationModel().isPresent(), is(false));
  }

  @Test
  public void when_parameterAstHasDeprecationParameter_then_parserHasDeprecationModelWithCorrespondingValues() {
    ComponentAst deprecatedAst = mockDeprecatedAst("1.1.0", "Some Message", "2.0.0");
    setMockAstChild(componentAst, "deprecated", deprecatedAst);

    assertThat(parameterModelParser.getDeprecationModel().isPresent(), is(true));

    DeprecationModel deprecationModel = parameterModelParser.getDeprecationModel().get();
    assertThat(deprecationModel.getDeprecatedSince(), is("1.1.0"));
    assertThat(deprecationModel.getMessage(), is("Some Message"));
    assertThat(deprecationModel.getToRemoveIn(), is(Optional.of("2.0.0")));
  }

  @Test
  public void when_toRemoveInParameterIsNotConfigured_then_theDeprecationModelReturnsAnEmptyOptional() {
    ComponentAst deprecatedAst = mockDeprecatedAst("1.1.0", "Some Message", null);
    setMockAstChild(componentAst, "deprecated", deprecatedAst);

    assertThat(parameterModelParser.getDeprecationModel().isPresent(), is(true));

    DeprecationModel deprecationModel = parameterModelParser.getDeprecationModel().get();
    assertThat(deprecationModel.getToRemoveIn(), is(empty()));
  }
}
