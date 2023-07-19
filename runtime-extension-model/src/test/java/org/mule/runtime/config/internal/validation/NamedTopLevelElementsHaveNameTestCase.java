/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.Optional;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(MULE_DSL)
@Story(DSL_VALIDATION_STORY)
public class NamedTopLevelElementsHaveNameTestCase extends AbstractCoreValidationTestCase {

  @Override
  protected Validation getValidation() {
    return new NamedTopLevelElementsHaveName();
  }

  @Test
  public void namelessTopLevelElement() {
    final Optional<ValidationResultItem> msg = runValidation("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<mule xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
        "      xmlns:heisenberg=\"http://www.mulesoft.org/schema/mule/heisenberg\"\n" +
        "      xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
        "      xsi:schemaLocation=\"http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\n"
        +
        "               http://www.mulesoft.org/schema/mule/heisenberg http://www.mulesoft.org/schema/mule/heisenberg/current/mule-heisenberg.xsd\">\n"
        +
        "\n" +
        "    <flow>\n" +
        "        <logger/>\n" +
        "    </flow>\n" +
        "</mule>")
            .stream().findFirst();

    assertThat(msg.get().getMessage(),
               containsString("Global element 'flow' does not provide a name attribute."));
  }

}
