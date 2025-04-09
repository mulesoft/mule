/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;

import static java.util.Optional.empty;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.config.internal.validation.test.AbstractCoreValidationTestCase;

import java.util.Optional;

import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;

@Features({@Feature(MULE_DSL), @Feature(REUSE)})
@Stories({@Story(DSL_VALIDATION_STORY), @Story(OPERATIONS)})
public class OperationDoesNotHaveApikitConsoleTestCase extends AbstractCoreValidationTestCase {

  private static final String XML_NAMESPACE_DEF = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
      "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
      "      xmlns:operation=\"http://www.mulesoft.org/schema/mule/operation\"" +
      "      xmlns:apikit=\"http://www.mulesoft.org/schema/mule/mule-apikit\"" +
      "      xsi:schemaLocation=\"\n" +
      "       http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\n" +
      "       http://www.mulesoft.org/schema/mule/operation http://www.mulesoft.org/schema/mule/operation/current/mule-operation.xsd\n"
      +
      "       http://www.mulesoft.org/schema/mule/mule-apikit http://www.mulesoft.org/schema/mule/mule-apikit/current/mule-apikit.xsd\">\n";
  private static final String XML_CLOSE = "</mule>";

  @Override
  protected Validation getValidation() {
    return new OperationDoesNotHaveApikitConsole();
  }

  @Test
  @Description("Checks that no validation message is returned if there is no operation")
  public void withoutOperation() {
    final Optional<ValidationResultItem> msg = runValidation("OperationDoesNotHaveApikitConsoleTestCase#withoutOperation",
                                                             XML_NAMESPACE_DEF + XML_CLOSE)
        .stream().findFirst();
    assertThat(msg, is(empty()));
  }

  @Test
  @Description("Checks that no validation message is returned if there is no apikit:console inside operation")
  public void operationWithoutApikitConsole() {
    final Optional<ValidationResultItem> msg =
        runValidation("OperationDoesNotHaveApikitConsoleTestCase#operationWithoutApikitConsole",
                      XML_NAMESPACE_DEF +
                          "<operation:def name=\"someOp\">" +
                          "    <operation:body>" +
                          "        <logger />" +
                          "    </operation:body>" +
                          "</operation:def>" +
                          XML_CLOSE)
            .stream().findFirst();
    assertThat(msg, is(empty()));
  }

  @Test
  @Description("Checks that no validation message is returned if there is an apikit:console inside a flow (backwards)")
  public void flowWithApikitConsole() {
    final Optional<ValidationResultItem> msg = runValidation("OperationDoesNotHaveApikitConsoleTestCase#flowWithApikitConsole",
                                                             XML_NAMESPACE_DEF +
                                                                 "<flow name=\"someFlow\">" +
                                                                 "    <apikit:console config-ref=\"console-config\" />" +
                                                                 "</flow>" +
                                                                 XML_CLOSE)
        .stream().findFirst();
    assertThat(msg, is(empty()));
  }

  @Test
  @Description("Checks that a corresponding validation message is returned if there is an apikit:console inside an operation")
  public void operationWithApikitConsole() {
    final Optional<ValidationResultItem> msg =
        runValidation("OperationDoesNotHaveApikitConsoleTestCase#operationWithApikitConsole",
                      XML_NAMESPACE_DEF +
                          "<operation:def name=\"someOp\">" +
                          "    <operation:body>" +
                          "        <apikit:console config-ref=\"console-config\" />" +
                          "    </operation:body>" +
                          "</operation:def>" +
                          XML_CLOSE)
            .stream().findFirst();
    assertThat(msg, is(not(empty())));
    assertThat(msg.get().getMessage(),
               containsString("Usages of the component 'apikit:console' are not allowed inside a Mule SDK Operation Definition"));
  }
}
