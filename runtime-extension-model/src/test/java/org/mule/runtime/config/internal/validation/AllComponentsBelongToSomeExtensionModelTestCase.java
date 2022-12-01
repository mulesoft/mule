/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.Optional;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(MULE_DSL)
@Story(DSL_VALIDATION_STORY)
public class AllComponentsBelongToSomeExtensionModelTestCase extends AbstractCoreValidationTestCase {

  @Override
  protected Validation getValidation() {
    return new AllComponentsBelongToSomeExtensionModel(false);
  }

  @Test
  public void topLevelComponentThatDoesNotBelongToAnyExtensionModel() {
    final Optional<ValidationResultItem> msg = runValidation("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
        "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
        "      xmlns:some=\"http://www.mulesoft.org/schema/mule/some\"\n" +
        "      xsi:schemaLocation=\"\n" +
        "       http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\">\n" +
        "       http://www.mulesoft.org/schema/mule/some http://www.mulesoft.org/schema/mule/some/current/mule-some.xsd\">\n" +
        "\n" +
        "    <some:thing name=\"theSomething\" />\n" +
        "\n" +
        "</mule>")
            .stream().findFirst();
    assertThat(msg.get().getMessage(), containsString("The component 'some:thing' doesn't belong to any extension model"));
  }
}
