/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;

import static java.util.Arrays.asList;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.List;
import java.util.Optional;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@Feature(MULE_DSL)
@Story(DSL_VALIDATION_STORY)
@RunWith(Parameterized.class)
public class NameHasValidCharactersTestCase extends AbstractCoreValidationTestCase {

  private final boolean ignoreParamsWithProperties;

  public NameHasValidCharactersTestCase(boolean ignoreParamsWithProperties) {
    this.ignoreParamsWithProperties = ignoreParamsWithProperties;
  }

  @Override
  protected Validation getValidation() {
    return new NameHasValidCharacters(ignoreParamsWithProperties);
  }

  @Parameterized.Parameters(name = "eventType: {0}")
  public static List<Boolean> parameters() {
    return asList(true, false);
  }

  @Test
  public void flowNameUsingInvalidCharacter() {
    final Optional<ValidationResultItem> msg = runValidation("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
        "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
        "      xsi:schemaLocation=\"\n" +
        "       http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\">\n" +
        "\n" +
        "    <flow name=\"flow/myFlow\">\n" +
        "        <logger/>\n" +
        "        <error-handler>\n" +
        "            <on-error-continue/>\n" +
        "            <on-error-continue/>\n" +
        "        </error-handler>\n" +
        "    </flow>\n" +
        "\n" +
        "</mule>")
            .stream().findFirst();

    assertThat(msg.get().getMessage(),
               containsString("Invalid global element name 'flow/myFlow'. Problem is: Invalid character used in location. Invalid characters are /,[,],{,},#"));
  }

  @Test
  @Issue("W-14009153")
  public void flowNameUsingProperty() {
    final Optional<ValidationResultItem> msg = runValidation("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
        "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
        "      xsi:schemaLocation=\"\n" +
        "       http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\">\n" +
        "\n" +
        "    <flow name=\"${someProperty}\">\n" +
        "        <logger/>\n" +
        "        <error-handler>\n" +
        "            <on-error-continue/>\n" +
        "        </error-handler>\n" +
        "    </flow>\n" +
        "\n" +
        "</mule>")
            .stream().findFirst();

    if (ignoreParamsWithProperties) {
      assertThat(msg.isPresent(), is(false));
    } else {
      assertThat(msg.get().getMessage(),
                 containsString("Invalid global element name '${someProperty}'. Problem is: Invalid character used in location. Invalid characters are /,[,],{,},#"));
    }
  }
}
