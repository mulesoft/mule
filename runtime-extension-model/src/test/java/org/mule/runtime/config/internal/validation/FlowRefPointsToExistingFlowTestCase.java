/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.test.allure.AllureConstants.ComponentsFeature.CORE_COMPONENTS;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.FlowReferenceStory.FLOW_REFERENCE;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import static java.util.Optional.empty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.config.internal.validation.test.AbstractCoreValidationTestCase;

import java.util.Optional;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;

@Features({@Feature(CORE_COMPONENTS), @Feature(MULE_DSL)})
@Stories({@Story(FLOW_REFERENCE), @Story(DSL_VALIDATION_STORY)})
public class FlowRefPointsToExistingFlowTestCase extends AbstractCoreValidationTestCase {

  @Override
  protected Validation getValidation() {
    return new FlowRefPointsToExistingFlow(false);
  }

  @Test
  public void flowRefToNonExistentFlow() {
    final Optional<ValidationResultItem> msg = runValidation("FlowRefPointsToExistingFlowTestCase#flowRefToNonExistentFlow",
                                                             "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                                 "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                                                                 +
                                                                 "      xsi:schemaLocation=\"http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\">\n"
                                                                 +
                                                                 "\n" +
                                                                 "    <flow name=\"flow\">\n" +
                                                                 "        <flow-ref name=\"sub-flow-name\" />\n" +
                                                                 "    </flow>\n" +
                                                                 "</mule>")
        .stream().findFirst();

    assertThat(msg.get().getMessage(), containsString("'flow-ref' is pointing to 'sub-flow-name' which does not exist"));
  }

  @Test
  public void flowRefDynamic() {
    final Optional<ValidationResultItem> msg = runValidation("FlowRefPointsToExistingFlowTestCase#flowRefDynamic",
                                                             "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                                 "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                                                                 +
                                                                 "      xsi:schemaLocation=\"http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\">\n"
                                                                 +
                                                                 "\n" +
                                                                 "    <flow name=\"flow\">\n" +
                                                                 "        <flow-ref name=\"#['sub-flow-name']\" />\n" +
                                                                 "    </flow>\n" +
                                                                 "</mule>")
        .stream().findFirst();

    assertThat(msg, is(empty()));
  }
}
