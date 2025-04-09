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

import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.config.internal.validation.test.AbstractCoreValidationTestCase;

import java.util.Optional;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(MULE_DSL)
@Story(DSL_VALIDATION_STORY)
public class SingletonsAreNotRepeatedTestCase extends AbstractCoreValidationTestCase {

  private static final String REPEATED_ELEMENT_NAME = "configuration";

  @Override
  protected Validation getValidation() {
    return new SingletonsAreNotRepeated();
  }

  @Test
  public void conflictingNames() {
    final Optional<ValidationResultItem> msg = runValidation("SingletonsAreNotRepeatedTestCase#conflictingNames",
                                                             "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                                 "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
                                                                 "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                                                                 +
                                                                 "      xsi:schemaLocation=\"http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\">\n"
                                                                 +
                                                                 "\n" +
                                                                 "    <" + REPEATED_ELEMENT_NAME
                                                                 + " defaultResponseTimeout=\"10000\"/>\n" +
                                                                 "\n" +
                                                                 "    <flow name=\"firstFlow\">\n" +
                                                                 "        <logger/>\n" +
                                                                 "    </flow>\n" +
                                                                 "    \n" +
                                                                 "</mule>",
                                                             "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                                 "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
                                                                 "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                                                                 +
                                                                 "      xsi:schemaLocation=\"http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\">\n"
                                                                 +
                                                                 "\n" +
                                                                 "    <flow name=\"secondFlow\">\n" +
                                                                 "        <logger/>\n" +
                                                                 "    </flow>\n" +
                                                                 "\n" +
                                                                 "    <" + REPEATED_ELEMENT_NAME
                                                                 + " defaultTransactionTimeout=\"30000\"/>\n" +
                                                                 "\n" +
                                                                 "</mule>\n" +
                                                                 "")
        .stream().findFirst();

    assertThat(msg.get().getMessage(),
               containsString(format("The configuration element '%s' can only appear once",
                                     REPEATED_ELEMENT_NAME)));
  }

}
