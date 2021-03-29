/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.Optional;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;

@Features({@Feature(ERROR_HANDLING), @Feature(MULE_DSL)})
@Stories({@Story("Validations"), @Story(DSL_VALIDATION_STORY)})
public class SourceErrorMappingTypeNotRepeatedTestCase extends AbstractCoreValidationTestCase {

  @Override
  protected Validation getValidation() {
    return new SourceErrorMappingTypeNotRepeated();
  }

  @Test
  public void repeatedMappingsNotAllowed() {
    final Optional<ValidationResultItem> msg = runValidation("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
        "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
        "      xmlns:test=\"http://www.mulesoft.org/schema/mule/test\"\n" +
        "      xsi:schemaLocation=\"\n" +
        "       http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\n" +
        "       http://www.mulesoft.org/schema/mule/test http://www.mulesoft.org/schema/mule/test/current/mule-test.xsd\">\n" +
        "\n" +
        "    <flow name=\"onErrorPropagateTypeMatch\">\n" +
        "        <test:operation>\n" +
        "            <error-mapping sourceType=\"ROUTING\" targetType=\"TRANSFORMATION\"/>\n" +
        "            <error-mapping sourceType=\"ROUTING\" targetType=\"CONNECTIVITY\"/>\n" +
        "            <error-mapping sourceType=\"EXPRESSION\" targetType=\"CONNECTIVITY\"/>\n" +
        "            <error-mapping sourceType=\"EXPRESSION\" targetType=\"TRANSFORMATION\"/>\n" +
        "            <error-mapping targetType=\"EXPRESSION\"/>\n" +
        "        </test:operation>\n" +
        "        <error-handler>\n" +
        "            <on-error-propagate type=\"EXPRESSION\">\n" +
        "                <set-payload value=\"#[payload ++ ' nope']\"/>\n" +
        "            </on-error-propagate>\n" +
        "            <on-error-propagate type=\"ANY\">\n" +
        "                <logger level=\"ERROR\"/>\n" +
        "            </on-error-propagate>\n" +
        "        </error-handler>\n" +
        "    </flow>\n" +
        "\n" +
        "</mule>");

    assertThat(msg.get().getMessage(),
               containsString("Repeated source types are not allowed. Offending types are 'ROUTING', 'EXPRESSION'."));
  }

}
