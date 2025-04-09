/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.config.internal.validation.test.AbstractCoreValidationTestCase;

import java.util.Optional;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(MULE_DSL)
@Story(DSL_VALIDATION_STORY)
public class NameHasValidCharactersTestCase extends AbstractCoreValidationTestCase {

  @Override
  protected Validation getValidation() {
    return new NameHasValidCharacters();
  }

  @Test
  public void flowNameUsingInvalidCharacter() {
    final Optional<ValidationResultItem> msg = runValidation("NameHasValidCharactersTestCase#flowNameUsingInvalidCharacter",
                                                             "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                                 "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
                                                                 "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                                                                 +
                                                                 "      xsi:schemaLocation=\"\n" +
                                                                 "       http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\">\n"
                                                                 +
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
    assertThat(msg.get().causedByDynamicArtifact(), is(false));
  }

  @Test
  @Issue("W-14009153")
  public void flowNameUsingProperty() {
    final Optional<ValidationResultItem> msg = runValidation("NameHasValidCharactersTestCase#flowNameUsingProperty",
                                                             "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                                 "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
                                                                 "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                                                                 +
                                                                 "      xsi:schemaLocation=\"\n" +
                                                                 "       http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\">\n"
                                                                 +
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

    assertThat(msg.get().getMessage(),
               containsString("Invalid global element name '${someProperty}'. Problem is: Invalid character used in location. Invalid characters are /,[,],{,},#"));
    assertThat(msg.get().causedByDynamicArtifact(), is(true));
  }
}
