/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.ast;

import static org.mule.runtime.ast.api.util.MuleAstUtils.validatorBuilder;
import static org.mule.runtime.ast.api.xml.AstXmlParser.builder;
import static org.mule.runtime.extension.api.provider.RuntimeExtensionModelProviderLoaderUtils.discoverRuntimeExtensionModels;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.ValidationResult;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.ast.api.xml.AstXmlParser.Builder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;

/**
 * Extend this class to make assertions on the AST generated from an application's XML config file.
 */
public abstract class AbstractMuleSdkAstTestCase extends AbstractMuleTestCase {

  private ClassLoader classLoader;
  private AstXmlParser parser;

  private final Map<String, String> properties = new HashMap<>();

  protected static Set<ExtensionModel> astParserExtensionModels;

  protected abstract String getConfigFile();

  @BeforeClass
  public static void beforeClass() throws Exception {
    astParserExtensionModels = new LinkedHashSet<>(discoverRuntimeExtensionModels());
  }

  protected static void addDependencyExtension(ExtensionModel extension) {
    astParserExtensionModels.add(extension);
  }

  @Before
  public void before() {
    properties.clear();
    classLoader = this.getClass().getClassLoader();

    Builder astParserBuilder = builder()
        .withExtensionModels(astParserExtensionModels)
        .withPropertyResolver(propertyKey -> properties.getOrDefault(propertyKey, propertyKey));

    if (!validateSchema()) {
      astParserBuilder.withSchemaValidationsDisabled();
    }

    customizeAstParserBuilder(astParserBuilder);
    parser = astParserBuilder.build();
  }

  protected void customizeAstParserBuilder(Builder astParserBuilder) {
    // nothing to do here, override this method if you want to customize the builder (for example, to add a custom extension
    // model)
  }

  protected boolean validateSchema() {
    return false;
  }

  protected ArtifactAst getArtifactAst() {
    return getArtifactAst(getConfigFile());
  }

  protected ArtifactAst getArtifactAst(String configFile) {
    ArtifactAst artifactAst = parser.parse(classLoader.getResource(configFile));
    assertThat(validatorBuilder().build().validate(artifactAst).getItems(), is(empty()));
    return artifactAst;
  }

  protected ValidationResult parseAstExpectingValidationErrors(String configFile) {
    ArtifactAst artifactAst = parser.parse(classLoader.getResource(configFile));
    ValidationResult validationResult = validatorBuilder().build().validate(artifactAst);
    assertThat(validationResult.getItems(), is(not(empty())));
    return validationResult;
  }

  protected ComponentAst getTopLevelComponent(ArtifactAst ast, String componentName) {
    return ast.topLevelComponentsStream().filter(componentAst -> componentAst.getComponentId().get().equals(componentName))
        .findFirst().get();
  }

  protected ComponentAst getChild(ComponentAst ast, String childName) {
    return ast.directChildrenStreamByIdentifier(null, childName).findFirst().get();
  }

  protected Optional<ComponentAst> getOptionalChild(ComponentAst ast, String childName) {
    return ast.directChildrenStreamByIdentifier(null, childName).findFirst();
  }
}
