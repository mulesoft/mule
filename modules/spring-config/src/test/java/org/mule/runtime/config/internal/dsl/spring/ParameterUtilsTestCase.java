/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.nested.NestableElementModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentGenerationInformation;
import org.mule.runtime.ast.api.ComponentMetadataAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.internal.DefaultComponentAst;
import org.mule.runtime.ast.internal.builder.PropertiesResolver;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Issue;

public class ParameterUtilsTestCase extends AbstractMuleTestCase {

  private ComponentGenerationInformation generationInformation;
  private ComponentAst componentAst;
  private ParameterUtils parameterUtils;

  @Before
  public void setUp() {
    parameterUtils = new ParameterUtils();

    List<ComponentAst> builtChildren = null;
    Map<ParameterModel, ComponentParameterAst> parameterAsts = null;
    Map<String, String> extraParams = null;
    PropertiesResolver propertiesResolver = mock(PropertiesResolver.class);
    Supplier<Optional<String>> componentId = null;
    ExtensionModel extensionModel = null;
    ComponentModel componentModel = null;
    NestableElementModel nestableElementModel = null;
    ConfigurationModel configurationModel = null;
    ConnectionProviderModel connectionProviderModel = null;
    ParameterizedModel parameterizedModel = null;
    generationInformation = mock(ComponentGenerationInformation.class);
    ComponentLocation location = null;
    ComponentIdentifier identifier = null;
    TypedComponentIdentifier.ComponentType componentType = null;
    ComponentMetadataAst metadata = null;

    componentAst =
        new DefaultComponentAst(builtChildren, parameterAsts, extraParams, propertiesResolver, componentId, extensionModel,
                                componentModel, nestableElementModel, configurationModel, connectionProviderModel,
                                parameterizedModel, null, generationInformation, metadata, location, identifier, componentType);
  }

  @Test
  @Issue("MULE-19326")
  public void testGetParamNameReturnsNull_WhenSyntaxIsNotPresent() {
    // Given

    when(generationInformation.getSyntax()).thenReturn(Optional.empty());

    // When
    String paramName = parameterUtils.getParamName(componentAst, "some-name");

    // Then
    assertThat(paramName, nullValue());
  }

  @Test
  @Issue("MULE-19326")
  public void testGetParamNameReturnsHello_WhenGettingNameForHiAndChildrenWithElementNameHiHasParamNameHello() {
    // Given
    String elementName = "Hi";
    String expectedParamName = "Hello";

    DslElementSyntax dslElementSyntax = mock(DslElementSyntax.class);
    when(generationInformation.getSyntax()).thenReturn(Optional.of(dslElementSyntax));

    addParameterToFirstLevel(dslElementSyntax, elementName, expectedParamName);

    // When
    String paramName = parameterUtils.getParamName(componentAst, elementName);

    // Then
    assertThat(paramName, is(expectedParamName));
  }

  @Test
  @Issue("MULE-19326")
  public void testGetParamNameReturnsGoodBye_WhenGettingNameForByeAndChildrenWithElementNameByeHasParamNameGoodBye() {
    // Given
    String elementName = "Bye";
    String expectedParamName = "Good Bye";

    DslElementSyntax dslElementSyntax = mock(DslElementSyntax.class);
    when(generationInformation.getSyntax()).thenReturn(Optional.of(dslElementSyntax));

    addParameterToFirstLevel(dslElementSyntax, elementName, expectedParamName);

    // When
    String paramName = parameterUtils.getParamName(componentAst, elementName);

    // Then
    assertThat(paramName, is(expectedParamName));
  }

  @Test
  @Issue("MULE-19326")
  public void testGetParamNameReturnsGoodBye_WhenGettingNameForByeAndThereAreManyChildrenBesidesOneWithElementNameByeHasParamNameGoodBye() {
    // Given
    String elementName = "Bye";
    String expectedParamName = "Good Bye";

    DslElementSyntax dslElementSyntax = mock(DslElementSyntax.class);
    when(generationInformation.getSyntax()).thenReturn(Optional.of(dslElementSyntax));

    addParameterToFirstLevel(dslElementSyntax, "someOther", "some other param name");
    addParameterToFirstLevel(dslElementSyntax, elementName, expectedParamName);
    addParameterToFirstLevel(dslElementSyntax, "shrek", "FioNa");
    addParameterToFirstLevel(dslElementSyntax, "this-name-is-longer-for-some-ungodly-reason",
                             "So is this parameter name but it should not change anything");

    // When
    String paramName = parameterUtils.getParamName(componentAst, elementName);

    // Then
    assertThat(paramName, is(expectedParamName));
  }

  @Test
  @Issue("MULE-19326")
  public void testGetParamNameReturnsTony_WhenGettingNameForEzequielAndEzequielElementIsNestedInTheSecondLevel() {
    // Given
    String elementName = "Ezequiel";
    String expectedParamName = "Tony";

    DslElementSyntax dslElementSyntax = mock(DslElementSyntax.class);
    when(generationInformation.getSyntax()).thenReturn(Optional.of(dslElementSyntax));

    String intermediateChildrenParamName = "some other param name";
    addParameterToFirstLevel(dslElementSyntax, "dummy-element-name", "Dummy Parameter Name");
    addParameterToFirstLevel(dslElementSyntax, "someOther", intermediateChildrenParamName);
    addParameterToFirstLevel(dslElementSyntax, "shrek", "FioNa");

    addParameterUnderChildren(dslElementSyntax, intermediateChildrenParamName, elementName, expectedParamName);

    // When
    String paramName = parameterUtils.getParamName(componentAst, elementName);

    // Then
    assertThat(paramName, is(expectedParamName));
  }

  private void addParameterToFirstLevel(DslElementSyntax rootDslElementSyntax, String elementName, String expectedParamName) {
    Map<String, DslElementSyntax> containedElementsMap = new HashMap<>();

    if (rootDslElementSyntax.getContainedElementsByName() != null) {
      containedElementsMap = rootDslElementSyntax.getContainedElementsByName();
    }

    DslElementSyntax newDslElementSyntax = mock(DslElementSyntax.class);
    when(newDslElementSyntax.getElementName()).thenReturn(elementName);

    containedElementsMap.put(expectedParamName, newDslElementSyntax);

    when(rootDslElementSyntax.getContainedElementsByName()).thenReturn(containedElementsMap);

  }

  private void addParameterUnderChildren(DslElementSyntax dslElementSyntax, String intermediateChildrenParamName,
                                         String elementName, String expectedParamName) {
    DslElementSyntax intermediateDslElementSyntax =
        dslElementSyntax.getContainedElementsByName().get(intermediateChildrenParamName);

    Map<String, DslElementSyntax> intermediateContainedElementsByName = intermediateDslElementSyntax.getContainedElementsByName();

    if (intermediateContainedElementsByName == null) {
      intermediateContainedElementsByName = new HashMap<>();
    }

    when(intermediateDslElementSyntax.getContainedElementsByName()).thenReturn(intermediateContainedElementsByName);

    DslElementSyntax newDslElementSyntax = mock(DslElementSyntax.class);
    when(newDslElementSyntax.getElementName()).thenReturn(elementName);

    intermediateContainedElementsByName.put(expectedParamName, newDslElementSyntax);
  }
}
