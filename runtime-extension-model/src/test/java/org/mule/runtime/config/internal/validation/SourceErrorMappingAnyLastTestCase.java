/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.config.internal.validation.test.AbstractCoreValidationTestCase;

import java.util.Optional;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;

@Features({@Feature(ERROR_HANDLING), @Feature(MULE_DSL)})
@Story(DSL_VALIDATION_STORY)
public class SourceErrorMappingAnyLastTestCase extends AbstractCoreValidationTestCase {

  @Override
  protected Validation getValidation() {
    return new SourceErrorMappingAnyLast();
  }

  @Test
  public void middleAnyMappingsNotAllowed() {
    final Optional<ValidationResultItem> msg = runValidation("SourceErrorMappingAnyLastTestCase#middleAnyMappingsNotAllowed",
                                                             "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                                 "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
                                                                 "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                                                                 +
                                                                 "      xmlns:test=\"http://www.mulesoft.org/schema/mule/test\"\n"
                                                                 +
                                                                 "      xsi:schemaLocation=\"\n" +
                                                                 "       http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\n"
                                                                 +
                                                                 "       http://www.mulesoft.org/schema/mule/test http://www.mulesoft.org/schema/mule/test/current/mule-test.xsd\">\n"
                                                                 +
                                                                 "\n" +
                                                                 "    <flow name=\"onErrorPropagateTypeMatch\">\n" +
                                                                 "        <test:operation>\n" +
                                                                 "            <error-mapping sourceType=\"ROUTING\" targetType=\"TRANSFORMATION\"/>\n"
                                                                 +
                                                                 "            <error-mapping sourceType=\"ANY\" targetType=\"CONNECTIVITY\"/>\n"
                                                                 +
                                                                 "            <error-mapping sourceType=\"REDELIVERY_EXHAUSTED\" targetType=\"EXPRESSION\"/>\n"
                                                                 +
                                                                 "        </test:operation>\n" +
                                                                 "        <error-handler>\n" +
                                                                 "            <on-error-propagate type=\"EXPRESSION\">\n" +
                                                                 "                <set-payload value=\"#[payload ++ ' nope']\"/>\n"
                                                                 +
                                                                 "            </on-error-propagate>\n" +
                                                                 "            <on-error-propagate type=\"ANY\">\n" +
                                                                 "                <logger level=\"ERROR\"/>\n" +
                                                                 "            </on-error-propagate>\n" +
                                                                 "        </error-handler>\n" +
                                                                 "    </flow>\n" +
                                                                 "\n" +
                                                                 "</mule>")
        .stream().findFirst();

    assertThat(msg.get().getMessage(),
               containsString("Only the last error mapping can have 'ANY' or an empty source type."));
  }
}
