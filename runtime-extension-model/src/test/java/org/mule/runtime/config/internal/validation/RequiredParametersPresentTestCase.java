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
import static org.hamcrest.core.Is.is;

import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.config.internal.validation.test.AbstractCoreValidationTestCase;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(MULE_DSL)
@Story(DSL_VALIDATION_STORY)
public class RequiredParametersPresentTestCase extends AbstractCoreValidationTestCase {

  @Override
  protected Validation getValidation() {
    return new RequiredParametersPresent();
  }

  @Test
  public void flowRefWithNoName() {
    final Optional<ValidationResultItem> msg = runValidation("RequiredParametersPresentTestCase#flowRefWithNoName",
                                                             "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                                 "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
                                                                 "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                                                                 +
                                                                 "      xsi:schemaLocation=\"http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\">\n"
                                                                 +
                                                                 "\n" +
                                                                 "    <flow name=\"flow\">\n" +
                                                                 "        <flow-ref/>\n" +
                                                                 "    </flow>\n" +
                                                                 "    \n" +
                                                                 "</mule>")
        .stream().findFirst();

    assertThat(msg.get().getMessage(),
               containsString(format("Element <flow-ref> is missing required parameter 'name'.")));
  }

  @Test
  @Issue("MULE-19899")
  public void numberParamWithUnresolvedProperty() {
    final Optional<ValidationResultItem> msg =
        runValidation("RequiredParametersPresentTestCase#numberParamWithUnresolvedProperty",
                      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
                          "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                          "      xmlns:test=\"http://www.mulesoft.org/schema/mule/test\"\n" +
                          "      xsi:schemaLocation=\"http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\n"
                          +
                          "               http://www.mulesoft.org/schema/mule/test http://www.mulesoft.org/schema/mule/test/current/mule-test.xsd\">\n"
                          +
                          "\n" +
                          "    <test:other-config name=\"cfg\" count=\"${other.count}\"/>\n" +
                          "\n" +
                          "</mule>")
            .stream().findFirst();

    assertThat(msg.map(r -> r.getMessage()).orElse(null), msg.isPresent(), is(false));
  }

  @Test
  @Issue("MULE-19963")
  public void manyRequiredMissingAllReported() {
    final List<ValidationResultItem> msgs = runValidation("RequiredParametersPresentTestCase#manyRequiredMissingAllReported",
                                                          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                              "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
                                                              "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                                              "      xsi:schemaLocation=\"http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\">\n"
                                                              +
                                                              "\n" +
                                                              "    <global-property/>\n" +
                                                              "\n" +
                                                              "</mule>");

    assertThat(msgs, hasSize(2));
  }
}
