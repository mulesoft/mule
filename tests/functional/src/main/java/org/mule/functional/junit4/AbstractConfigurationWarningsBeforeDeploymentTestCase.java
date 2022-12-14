/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static org.mule.runtime.ast.api.ArtifactType.APPLICATION;
import static org.mule.runtime.ast.api.util.MuleAstUtils.emptyArtifact;
import static org.mule.runtime.ast.api.util.MuleAstUtils.validatorBuilder;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.Validation.Level.WARN;

import static java.util.stream.Collectors.toList;

import static org.junit.Assert.fail;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.validation.ValidationResult;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.ast.api.xml.AstXmlParser.Builder;
import org.mule.runtime.dsl.api.ConfigResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;

public abstract class AbstractConfigurationWarningsBeforeDeploymentTestCase extends AbstractConfigurationFailuresTestCase {

  private List<String> warningMessages;

  @Before
  public void initWarnings() {
    warningMessages = new ArrayList<>();
  }

  @After
  public void clearWarnings() {
    warningMessages.clear();
  }

  private AstXmlParser getParser(Set<ExtensionModel> extensions) {
    Builder builder = AstXmlParser.builder()
        .withExtensionModels(extensions)
        .withArtifactType(APPLICATION)
        .withParentArtifact(emptyArtifact());

    return builder.build();
  }

  @Override
  protected void loadConfiguration(String configuration) throws MuleException, InterruptedException {
    ArtifactAst ast;
    try {
      ast = getParser(new HashSet<>(getRequiredExtensions())).parse(new ConfigResource(configuration));
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }

    ValidationResult validationResult = validatorBuilder()
        .ignoreParamsWithProperties(true)
        .build()
        .validate(ast);

    validationResult.getItems()
        .stream()
        .filter(vri -> vri.getValidation().getLevel().equals(ERROR))
        .forEach(vri -> fail(vri.getMessage()));

    this.warningMessages.addAll(validationResult.getItems()
        .stream()
        .filter(vri -> vri.getValidation().getLevel().equals(WARN))
        .map(v -> v.getMessage())
        .collect(toList()));
  }


  public List<String> getWarningMessages() {
    return warningMessages;
  }
}
