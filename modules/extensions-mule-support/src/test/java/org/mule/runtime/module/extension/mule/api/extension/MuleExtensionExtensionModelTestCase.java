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
import static org.mule.runtime.ast.api.ArtifactType.MULE_EXTENSION;
import static org.mule.runtime.extension.api.annotation.Extension.MULESOFT;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.EXTENSION_EXTENSION_MODEL;

import static java.util.stream.Collectors.toList;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.ValidationResult;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.ast.api.xml.AstXmlParser.Builder;
import org.mule.runtime.module.extension.mule.internal.loader.ast.AbstractMuleSdkAstTestCase;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
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
    ComponentAst extensionComponentAst = getDescriptionComponent(extensionAst);

    ExtensionModel extensionExtensionModel = extensionComponentAst.getExtensionModel();
    assertThat(extensionExtensionModel.getName(), is("Mule Extension DSL"));
  }

  @Test
  public void operationsCanBeRetrievedAsTopLevel() {
    ArtifactAst extensionAst = getArtifactAst();

    List<ComponentAst> operationsComponentAst = extensionAst.topLevelComponentsStream()
        .filter(c -> c.getComponentType() == OPERATION_DEF)
        .collect(Collectors.toList());

    // In this test config there is only one operation
    assertThat(operationsComponentAst, hasSize(1));

    ComponentAst operationComponentAst = operationsComponentAst.get(0);
    assertThat(operationComponentAst.getIdentifier(), is(buildFromStringRepresentation("operation:def")));
    assertThat(operationComponentAst.getComponentType(), is(OPERATION_DEF));
    assertThat(operationComponentAst.getExtensionModel().getName(), is("Mule Operations DSL"));
  }

  @Test
  public void parametersAreParsedAndHaveTheRightValueWhenFullyDefined() {
    ArtifactAst extensionAst = getArtifactAst();

    ComponentAst descriptionComponentAst = getDescriptionComponent(extensionAst);
    ComponentAst licensingComponentAst = getChild(descriptionComponentAst, "licensing");
    ComponentAst xmlDslAttributesComponentAst = getChild(descriptionComponentAst, "xml-dsl-attributes");

    assertThat(getParameterValue(descriptionComponentAst, "name"), is("Fully Parameterized Extension"));
    assertThat(getParameterValue(descriptionComponentAst, "category"), is("PREMIUM"));
    assertThat(getParameterValue(descriptionComponentAst, "vendor"), is("Extension Producers Inc."));
    assertThat(getParameterValue(licensingComponentAst, "requiredEntitlement"), is("Premium Extension"));
    assertThat(getParameterValue(licensingComponentAst, "requiresEnterpriseLicense"), is(true));
    assertThat(getParameterValue(licensingComponentAst, "allowsEvaluationLicense"), is(false));
    assertThat(getParameterValue(xmlDslAttributesComponentAst, "namespace"),
               is("http://www.mulesoft.org/schema/a/different/path/mule/fully-parameterized"));
    assertThat(getParameterValue(xmlDslAttributesComponentAst, "prefix"), is("fully-param"));
  }

  @Test
  public void parametersAreParsedAndHaveTheRightValueWhenMinimallyDefined() {
    ArtifactAst extensionAst = getArtifactAst("extensions/extension-minimally-parameterized.xml");

    ComponentAst descriptionComponentAst = getDescriptionComponent(extensionAst);
    Optional<ComponentAst> licensingComponentAst = getOptionalChild(descriptionComponentAst, "licensing");
    Optional<ComponentAst> xmlDslAttributesComponentAst = getOptionalChild(descriptionComponentAst, "xml-dsl-attributes");

    assertThat(getParameterValue(descriptionComponentAst, "name"), is("Minimally Parameterized Extension"));
    assertThat(getParameterValue(descriptionComponentAst, "category"), is(COMMUNITY.name()));
    assertThat(getParameterValue(descriptionComponentAst, "vendor"), is(MULESOFT));

    assertThat(licensingComponentAst.isPresent(), is(false));
    assertThat(xmlDslAttributesComponentAst.isPresent(), is(false));
  }

  @Test
  public void extensionWithoutDescriptionFailsWhenParsing() {
    expected.expect(MuleRuntimeException.class);
    expected.expectMessage(containsString("The content of element 'extension' is not complete"));
    getArtifactAst("extensions/extension-without-description.xml");
  }

  @Test
  public void extensionWithoutNameFailsWhenParsing() {
    expected.expect(MuleRuntimeException.class);
    expected.expectMessage(containsString("Attribute 'name' must appear on element 'description'"));
    getArtifactAst("extensions/extension-without-name.xml");
  }

  @Test
  public void notAnExtensionFailsWhenValidating() {
    // TODO: W-12020311 we need root element validation during parsing, everything else should come free from the schema
    // validations
  }

  @Override
  protected void customizeAstParserBuilder(Builder astParserBuilder) {
    astParserBuilder.withArtifactType(MULE_EXTENSION);
  }

  private ComponentAst getDescriptionComponent(ArtifactAst ast) {
    ComponentIdentifier descriptionIdentifier = ComponentIdentifier.builder()
        .namespaceUri("http://www.mulesoft.org/schema/mule/mule-extension")
        .namespace("extension")
        .name("description")
        .build();

    return ast.topLevelComponentsStream()
        .filter(c -> c.getIdentifier().equals(descriptionIdentifier))
        .findFirst()
        .get();
  }

  private <T> T getParameterValue(ComponentAst componentAst, String paramName) {
    return componentAst.getParameter(DEFAULT_GROUP_NAME, paramName).<T>getValue().getRight();
  }
}
