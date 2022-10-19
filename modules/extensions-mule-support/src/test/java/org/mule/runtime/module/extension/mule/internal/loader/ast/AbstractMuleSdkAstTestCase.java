/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.ast;

import static org.mule.runtime.ast.api.util.MuleAstUtils.validatorBuilder;
import static org.mule.runtime.ast.api.xml.AstXmlParser.builder;

import static java.lang.Thread.currentThread;

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
import org.mule.runtime.core.api.extension.RuntimeExtensionModelProvider;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;

/**
 * Extend this class to make assertions on the AST generated from an application's XML config file.
 */
public abstract class AbstractMuleSdkAstTestCase extends AbstractMuleTestCase {

  private ClassLoader classLoader;
  private AstXmlParser parser;

  private final Map<String, String> properties = new HashMap<>();

  private static List<ExtensionModel> runtimeExtensionModels;

  protected abstract String getConfigFile();

  @BeforeClass
  public static void beforeClass() throws Exception {
    runtimeExtensionModels = new ArrayList<>();
    Collection<RuntimeExtensionModelProvider> runtimeExtensionModelProviders = new SpiServiceRegistry()
        .lookupProviders(RuntimeExtensionModelProvider.class, currentThread().getContextClassLoader());
    for (RuntimeExtensionModelProvider runtimeExtensionModelProvider : runtimeExtensionModelProviders) {
      runtimeExtensionModels.add(runtimeExtensionModelProvider.createExtensionModel());
    }
  }

  @Before
  public void before() {
    properties.clear();
    classLoader = this.getClass().getClassLoader();

    Builder astParserBuilder = builder()
        .withExtensionModels(runtimeExtensionModels)
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
}
