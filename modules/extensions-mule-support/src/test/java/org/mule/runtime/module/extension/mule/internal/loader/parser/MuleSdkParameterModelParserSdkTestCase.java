/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.Utils.stringParameterAst;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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

    TypeLoader typeLoader = mockTypeLoader();
    parameterModelParser = new MuleSdkParameterModelParserSdk(componentAst, typeLoader);
  }

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

  private TypeLoader mockTypeLoader() {
    TypeLoader typeLoader = mock(TypeLoader.class);
    // The type loader knows what "void" means, even if invalid for parameters.
    MetadataType voidMetadataType = mock(MetadataType.class);
    when(typeLoader.load("void")).thenReturn(of(voidMetadataType));
    // And also knows some valid type.
    someValidMetadataType = mock(MetadataType.class);
    when(typeLoader.load("somevalid")).thenReturn(of(someValidMetadataType));

    return typeLoader;
  }
}
