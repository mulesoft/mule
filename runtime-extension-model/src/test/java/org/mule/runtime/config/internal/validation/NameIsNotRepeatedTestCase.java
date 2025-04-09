/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import static java.lang.String.format;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

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
public class NameIsNotRepeatedTestCase extends AbstractCoreValidationTestCase {

  private static final String REPEATED_GLOBAL_NAME = "LenderService";

  @Override
  protected Validation getValidation() {
    return new NameIsNotRepeated();
  }

  @Test
  public void repeatedGlobalNames() {
    final Optional<ValidationResultItem> msg = runValidation("NameIsNotRepeatedTestCase#repeatedGlobalNames",
                                                             getConfigDsl())
        .stream().findFirst();

    assertThat(msg.get().getMessage(),
               containsString(format("Two (or more) configuration elements have been defined with the same global name. Global name '%s' must be unique",
                                     REPEATED_GLOBAL_NAME)));
  }

  @Test
  @Issue("MULE-19959")
  public void repeatedGlobalNamesAllReported() {
    final Optional<ValidationResultItem> msg = runValidation("NameIsNotRepeatedTestCase#repeatedGlobalNamesAllReported",
                                                             getConfigDsl())
        .stream().findFirst();

    assertThat(msg.get().getComponents(), hasSize(2));
  }

  protected String getConfigDsl() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
        "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
        "      xmlns:test=\"http://www.mulesoft.org/schema/mule/test\"\n" +
        "      xsi:schemaLocation=\"http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\n"
        +
        "               http://www.mulesoft.org/schema/mule/test http://www.mulesoft.org/schema/mule/test/current/mule-test.xsd\">\n"
        +
        "\n" +
        "    <test:config name=\"" + REPEATED_GLOBAL_NAME + "\"/>\n" +
        "\n" +
        "    <flow name=\"" + REPEATED_GLOBAL_NAME + "\">\n" +
        "        <logger/>\n" +
        "    </flow>\n" +
        "    \n" +
        "</mule>";
  }
}
