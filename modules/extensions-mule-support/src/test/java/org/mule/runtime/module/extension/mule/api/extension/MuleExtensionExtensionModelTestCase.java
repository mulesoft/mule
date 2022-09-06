/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.api.extension;

import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION_DEF;
import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.extension.api.annotation.Extension.MULESOFT;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.EXTENSION_EXTENSION_MODEL;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.module.extension.mule.internal.loader.ast.AbstractMuleSdkAstTestCase;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(REUSE)
@Story(EXTENSION_EXTENSION_MODEL)
public class MuleExtensionExtensionModelTestCase extends AbstractMuleSdkAstTestCase {

  @Rule
  public ExpectedException expected = none();

  @Override
  protected String getConfigFile() {
    return "extensions/extension-fully-parameterized.xml";
  }

  @Override
  protected boolean validateSchema() {
    return true;
  }

  @Test
  public void configFileCanBeParsedAndHasExpectedExtensionModel() {
    ArtifactAst extensionAst = getArtifactAst();
    ComponentAst extensionComponentAst = getRootComponent(extensionAst);

    ExtensionModel extensionExtensionModel = extensionComponentAst.getExtensionModel();
    assertThat(extensionExtensionModel.getName(), is("Mule Extension DSL"));
  }

  @Test
  public void operationsCanBeRetrievedAsChildrenOfTopLevel() {
    ArtifactAst extensionAst = getArtifactAst();
    ComponentAst extensionComponentAst = getRootComponent(extensionAst);

    // In this test config there is only one operation
    assertThat(extensionComponentAst.directChildren(), hasSize(1));

    ComponentAst operationComponentAst = extensionComponentAst.directChildren().get(0);
    assertThat(operationComponentAst.getIdentifier(), is(buildFromStringRepresentation("operation:def")));
    assertThat(operationComponentAst.getComponentType(), is(OPERATION_DEF));
    assertThat(operationComponentAst.getExtensionModel().getName(), is("Mule Operations DSL"));
  }

  @Test
  public void parametersAreParsedAndHaveTheRightValueWhenFullyDefined() {
    ArtifactAst extensionAst = getArtifactAst();

    ComponentAst extensionComponentAst = getRootComponent(extensionAst);

    Map<String, Matcher<Object>> expectedParameters = new LinkedHashMap<>();
    expectedParameters.put("name", is("Fully Parameterized Extension"));
    expectedParameters.put("category", is("PREMIUM"));
    expectedParameters.put("vendor", is("Extension Producers Inc."));
    expectedParameters.put("requiredEntitlement", is("Premium Extension"));
    expectedParameters.put("requiresEnterpriseLicense", is(true));
    expectedParameters.put("allowsEvaluationLicense", is(false));
    expectedParameters.put("namespace", is("http://www.mulesoft.org/schema/a/different/path/mule/fully-parameterized"));
    expectedParameters.put("prefix", is("fully-param"));

    assertThatParametersAreExactly(extensionComponentAst, expectedParameters);
  }

  @Test
  public void parametersAreParsedAndHaveTheRightValueWhenMinimallyDefined() {
    ArtifactAst extensionAst = getArtifactAst("extensions/extension-minimally-parameterized.xml");

    ComponentAst extensionComponentAst = getRootComponent(extensionAst);

    Map<String, Matcher<Object>> expectedParameters = new LinkedHashMap<>();
    expectedParameters.put("name", is("Minimally Parameterized Extension"));
    expectedParameters.put("category", is(COMMUNITY.name()));
    expectedParameters.put("vendor", is(MULESOFT));
    expectedParameters.put("requiredEntitlement", is(nullValue()));
    expectedParameters.put("requiresEnterpriseLicense", is(false));
    expectedParameters.put("allowsEvaluationLicense", is(true));
    expectedParameters.put("namespace", is(nullValue()));
    expectedParameters.put("prefix", is(nullValue()));

    assertThatParametersAreExactly(extensionComponentAst, expectedParameters);
  }

  @Test
  public void extensionWithoutNameFailsWhenParsing() {
    expected.expect(MuleRuntimeException.class);
    expected.expectMessage(containsString("Attribute 'name' must appear on element 'extension'"));
    getArtifactAst("extensions/extension-without-name.xml");
  }

  private ComponentAst getRootComponent(ArtifactAst ast) {
    // Checks there is only one top level component, which should be the root element
    assertThat(ast.topLevelComponents(), hasSize(1));

    ComponentAst onlyTopLevelComponent = ast.topLevelComponentsStream().findFirst().get();

    // Checks the only top level component was the one we expected
    ComponentIdentifier extensionIdentifier = onlyTopLevelComponent.getIdentifier();
    assertThat(extensionIdentifier.getName(), is("extension"));
    assertThat(extensionIdentifier.getNamespace(), is("extension"));
    assertThat(extensionIdentifier.getNamespaceUri(), is("http://www.mulesoft.org/schema/mule/mule-extension"));

    return onlyTopLevelComponent;
  }

  private void assertThatParametersAreExactly(ComponentAst componentAst, Map<String, Matcher<Object>> expectedParameters) {
    assertThat(componentAst.getParameters(), hasSize(expectedParameters.size()));
    for (Entry<String, Matcher<Object>> entry : expectedParameters.entrySet()) {
      assertThat(getParameterValue(componentAst, entry.getKey()), entry.getValue());
    }
  }

  private <T> T getParameterValue(ComponentAst componentAst, String paramName) {
    return componentAst.getParameter(DEFAULT_GROUP_NAME, paramName).<T>getValue().getRight();
  }
}
