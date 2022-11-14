/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.ast;

import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_MULE_SDK_PROPERTY;
import static org.mule.runtime.ast.api.validation.Validation.Level.WARN;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.APPLICATION_EXTENSION_MODEL;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.Validation.Level;
import org.mule.runtime.ast.api.validation.ValidationResult;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.tck.junit4.rule.SystemProperty;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(REUSE)
@Stories({@Story(APPLICATION_EXTENSION_MODEL), @Story(OPERATIONS)})
@Issue("W-11989569")
public class MuleSdkDisabledTestCase extends AbstractMuleSdkAstTestCase {

  @ClassRule
  public static SystemProperty disableMuleSdk = new SystemProperty(ENABLE_MULE_SDK_PROPERTY, "false");

  @Rule
  public ExpectedException expectedException = none();

  @Override
  protected String getConfigFile() {
    return null;
  }

  @Override
  protected boolean validateSchema() {
    return true;
  }

  @Test
  public void whenMuleSdkIsDisabledThenAnOperationCanNotBeDefined() {
    ValidationResult validationResult = parseAstExpectingValidationErrors("validation/app-with-simple-operation.xml");
    assertErrorMessages(validationResult,
                        "The component 'operation:def' doesn't belong to any extension model",
                        "The component 'operation:output' doesn't belong to any extension model",
                        "The component 'operation:payload-type' doesn't belong to any extension model",
                        "The component 'operation:body' doesn't belong to any extension model");

    // TODO (W-12050227): Change to ERROR when possible.
    assertAllValidationsLevel(validationResult, WARN);
  }

  @Test
  public void whenMuleSdkIsDisabledTheNamespaceThisIsNotResolved() {
    expectedException.expect(MuleRuntimeException.class);
    expectedException
        .expectMessage("Can't resolve http://www.mulesoft.org/schema/mule/this/current/mule-this.xsd, A dependency or plugin might be missing");
    getArtifactAst("validation/app-using-namespace-this.xml");
  }

  private void assertAllValidationsLevel(ValidationResult validationResult, Level expectedValidationLevel) {
    assertThat(validationResult.getItems().stream().map(ValidationResultItem::getValidation).map(Validation::getLevel)
        .collect(toList()),
               everyItem(is(expectedValidationLevel)));
  }

  private void assertErrorMessages(ValidationResult validationResult, String... messages) {
    assertThat(validationResult.getItems().stream().map(ValidationResultItem::getMessage).collect(toList()),
               containsInAnyOrder(messages));
  }
}
