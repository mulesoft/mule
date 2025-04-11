/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.el.validation.ScopePhaseValidationItemKind.DEPRECATED;
import static org.mule.runtime.config.api.validation.ExpressionsSyntacticallyValidAdditionalDataKeys.LOCATION_END_POSITION_COLUMN;
import static org.mule.runtime.config.api.validation.ExpressionsSyntacticallyValidAdditionalDataKeys.LOCATION_END_POSITION_LINE;
import static org.mule.runtime.config.api.validation.ExpressionsSyntacticallyValidAdditionalDataKeys.LOCATION_END_POSITION_OFFSET;
import static org.mule.runtime.config.api.validation.ExpressionsSyntacticallyValidAdditionalDataKeys.LOCATION_START_POSITION_COLUMN;
import static org.mule.runtime.config.api.validation.ExpressionsSyntacticallyValidAdditionalDataKeys.LOCATION_START_POSITION_LINE;
import static org.mule.runtime.config.api.validation.ExpressionsSyntacticallyValidAdditionalDataKeys.LOCATION_START_POSITION_OFFSET;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.el.validation.Location;
import org.mule.runtime.api.el.validation.Position;
import org.mule.runtime.api.el.validation.ScopePhaseValidationItem;
import org.mule.runtime.api.el.validation.ScopePhaseValidationItemKind;
import org.mule.runtime.api.el.validation.ScopePhaseValidationMessages;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.config.internal.validation.test.AbstractCoreValidationTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import org.hamcrest.CoreMatchers;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;


@Features({@Feature(MULE_DSL), @Feature(REUSE)})
@Stories({@Story(DSL_VALIDATION_STORY), @Story(OPERATIONS)})
public class MuleSDKOperationDoesNotHaveDeprecatedExpressionTestCase extends AbstractCoreValidationTestCase {

  private static final String XML_NAMESPACE_DEF = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
      "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
      "      xmlns:operation=\"http://www.mulesoft.org/schema/mule/operation\"" +
      "      xsi:schemaLocation=\"\n" +
      "       http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd" +
      "       http://www.mulesoft.org/schema/mule/operation http://www.mulesoft.org/schema/mule/operation/current/mule-operation.xsd\">\n";
  private static final String XML_CLOSE = "</mule>";

  private final ExpressionLanguage expressionLanguage = mock(ExpressionLanguage.class);

  @Override
  protected Validation getValidation() {
    return new MuleSdkOperationDoesNotHaveForbiddenFunctionsInExpressions(expressionLanguage);
  }

  @Test
  public void withoutDeprecation() {
    when(expressionLanguage.collectScopePhaseValidationMessages(contains("keySet("), anyString(), any()))
        .thenReturn(new TestScopePhaseValidationMessages(singletonList(new TestScopePhaseValidationItem("2.3",
                                                                                                        "dw::core::Objects::keySet"))));
    when(expressionLanguage.collectScopePhaseValidationMessages(not(contains("keySet(")), anyString(), any()))
        .thenReturn(new TestScopePhaseValidationMessages());
    final Optional<ValidationResultItem> msg =
        runValidation("MuleSDKOperationDoesNotHaveDeprecatedExpressionTestCase#withoutDeprecation",
                      XML_NAMESPACE_DEF +
                          "<operation:def name=\"someOp\"><operation:body>" +
                          "<set-payload value=\"10\"/>" +
                          "<logger level=\"WARN\" message=\"#[payload ++ 10]\"/>" +
                          "</operation:body></operation:def>" +
                          XML_CLOSE)
            .stream().findFirst();
    assertThat(msg, is(empty()));
  }

  @Test
  public void withDeprecation() {
    when(expressionLanguage.collectScopePhaseValidationMessages(contains("keySet("), anyString(), any()))
        .thenReturn(new TestScopePhaseValidationMessages(singletonList(new TestScopePhaseValidationItem("2.3.0",
                                                                                                        "dw::core::Objects::keySet"))));
    final Optional<ValidationResultItem> msg =
        runValidation("MuleSDKOperationDoesNotHaveDeprecatedExpressionTestCase#withDeprecation",
                      XML_NAMESPACE_DEF +
                          "<operation:def name=\"someOp\"><operation:body>" +
                          "<set-payload value=\"10\"/>" +
                          "<logger level=\"WARN\" message=\"#[%dw 2.0 import * from dw::core::Objects --- { 'keySet' : keySet({ 'a' : true, 'b' : 1}) }]\"/>"
                          +
                          "</operation:body></operation:def>" +
                          XML_CLOSE)
            .stream().findFirst();
    assertThat(msg, is(CoreMatchers.not(empty())));
    assertThat(msg.get().getMessage(), containsString("Using an invalid function within a Mule SDK operation"));
  }

  @Test
  public void withLookup() {
    when(expressionLanguage.collectScopePhaseValidationMessages(contains("Mule::lookup("), anyString(), any()))
        .thenReturn(new TestScopePhaseValidationMessages(singletonList(new TestScopePhaseValidationItem("2.5.0",
                                                                                                        "Mule::lookup"))));
    final List<ValidationResultItem> msg = runValidation("MuleSDKOperationDoesNotHaveDeprecatedExpressionTestCase#withLookup",
                                                         XML_NAMESPACE_DEF +
                                                             "<operation:def name=\"someOp\"><operation:body>" +
                                                             "<set-payload value=\"10\"/>" +
                                                             "<logger level=\"WARN\" message=\"#[%dw 2.0 --- { 'v' : Mule::lookup('someFlow') }]\"/>"
                                                             +
                                                             "</operation:body></operation:def>" +
                                                             XML_CLOSE);
    assertThat(msg, hasSize(1));
    assertThat(msg.get(0).getMessage(), containsString("Using an invalid function within a Mule SDK operation"));
  }

  @Test
  public void withoutExpression() {
    when(expressionLanguage.collectScopePhaseValidationMessages(contains("Mule::lookup("), anyString(), any()))
        .thenReturn(new TestScopePhaseValidationMessages(singletonList(new TestScopePhaseValidationItem("2.5.0",
                                                                                                        "Mule::lookup"))));
    final Optional<ValidationResultItem> msg =
        runValidation("MuleSDKOperationDoesNotHaveDeprecatedExpressionTestCase#withoutExpression",
                      XML_NAMESPACE_DEF +
                          "<operation:def name=\"someOp\"><operation:body>" +
                          "<set-payload value=\"10\"/>" +
                          "</operation:body></operation:def>" +
                          XML_CLOSE)
            .stream().findFirst();
    assertThat(msg, is(empty()));
  }

  @Test
  public void withMultipleCorrectExpressions() {
    when(expressionLanguage.collectScopePhaseValidationMessages(contains("keySet("), anyString(), any()))
        .thenReturn(new TestScopePhaseValidationMessages(singletonList(new TestScopePhaseValidationItem("2.3",
                                                                                                        "dw::core::Objects::keySet"))));
    when(expressionLanguage.collectScopePhaseValidationMessages(not(contains("keySet(")), anyString(), any()))
        .thenReturn(new TestScopePhaseValidationMessages());
    final Optional<ValidationResultItem> msg =
        runValidation("MuleSDKOperationDoesNotHaveDeprecatedExpressionTestCase#withMultipleCorrectExpressions",
                      XML_NAMESPACE_DEF +
                          "<operation:def name=\"someOp\"><operation:body>" +
                          "<set-payload value=\"10\"/>" +
                          "<logger level=\"WARN\" message=\"#[payload ++ 10]\"/>" +
                          "<set-payload value=\"#[payload + 1]\"/>" +
                          "<set-variable value=\"#[someFunction(10)]\"/>" +
                          "</operation:body></operation:def>" +
                          XML_CLOSE)
            .stream().findFirst();
    assertThat(msg, is(empty()));
  }

  @Test
  public void withOneOverManyExpressionsDeprecated() {
    when(expressionLanguage.collectScopePhaseValidationMessages(anyString(), anyString(), any()))
        .thenReturn(new TestScopePhaseValidationMessages());
    when(expressionLanguage.collectScopePhaseValidationMessages(contains("Mule::lookup("), anyString(), any()))
        .thenReturn(new TestScopePhaseValidationMessages(singletonList(new TestScopePhaseValidationItem("2.5.0",
                                                                                                        "Mule::lookup"))));
    final List<ValidationResultItem> msgs =
        runValidation("MuleSDKOperationDoesNotHaveDeprecatedExpressionTestCase#withOneOverManyExpressionsDeprecated",
                      XML_NAMESPACE_DEF +
                          "<operation:def name=\"someOp\"><operation:body>" +
                          "<set-payload value=\"10\"/>" +
                          "<logger level=\"WARN\" message=\"#[payload ++ 10]\"/>" +
                          "<set-payload value=\"#[payload + 1]\"/>" +
                          "<logger level=\"WARN\" message=\"#[%dw 2.0 --- { 'v' : Mule::lookup('someFlow1') }]\"/>" +
                          "<logger level=\"WARN\" message=\"#[%dw 2.0 --- { 'v' : Mule::lookup('someFlow2') }]\"/>" +
                          "<set-variable value=\"#[someFunction(10)]\"/>" +
                          "</operation:body></operation:def>" +
                          XML_CLOSE);
    assertThat(msgs, hasSize(2));
    assertThat(msgs.get(0).getMessage(), containsString("Using an invalid function within a Mule SDK operation"));
    assertThat(msgs.get(0).getMessage(), containsString("Mule::lookup('someFlow1')"));
    assertThat(msgs.get(1).getMessage(), containsString("Using an invalid function within a Mule SDK operation"));
    assertThat(msgs.get(1).getMessage(), containsString("Mule::lookup('someFlow2')"));
  }

  @Test
  public void withFutureDeprecatedExpression() {
    when(expressionLanguage.collectScopePhaseValidationMessages(contains("keySet("), anyString(), any()))
        .thenReturn(new TestScopePhaseValidationMessages(singletonList(new TestScopePhaseValidationItem("2.7.0",
                                                                                                        "dw::core::Objects::keySet"))));
    final Optional<ValidationResultItem> msg =
        runValidation("MuleSDKOperationDoesNotHaveDeprecatedExpressionTestCase#withFutureDeprecatedExpression",
                      XML_NAMESPACE_DEF +
                          "<operation:def name=\"someOp\"><operation:body>" +
                          "<set-payload value=\"10\"/>" +
                          "<logger level=\"WARN\" message=\"#[%dw 2.0 import * from dw::core::Objects --- { 'keySet' : keySet({ 'a' : true, 'b' : 1}) }]\"/>"
                          +
                          "</operation:body></operation:def>" +
                          XML_CLOSE)
            .stream().findFirst();
    assertThat(msg, is(empty()));
  }

  @Test
  public void withoutOperation() {
    when(expressionLanguage.collectScopePhaseValidationMessages(contains("Mule::lookup("), anyString(), any()))
        .thenReturn(new TestScopePhaseValidationMessages(singletonList(new TestScopePhaseValidationItem("2.5.0",
                                                                                                        "Mule::lookup"))));
    final Optional<ValidationResultItem> msg =
        runValidation("MuleSDKOperationDoesNotHaveDeprecatedExpressionTestCase#withoutOperation",
                      XML_NAMESPACE_DEF +
                          "<flow name=\"someFlow\">" +
                          "<set-payload value=\"10\"/>" +
                          "<logger level=\"WARN\" message=\"#[%dw 2.0 --- { 'v' : Mule::lookup('someFlow') }]\"/>" +
                          "</flow>" +
                          XML_CLOSE)
            .stream().findFirst();
    assertThat(msg, is(empty()));
  }

  @Test
  public void correctLocation() {
    when(expressionLanguage.collectScopePhaseValidationMessages(contains("Mule::lookup("), anyString(), any()))
        .thenReturn(new TestScopePhaseValidationMessages(singletonList(new TestScopePhaseValidationItem("2.5.0",
                                                                                                        "Mule::lookup"))));
    final Optional<ValidationResultItem> msg =
        runValidation("MuleSDKOperationDoesNotHaveDeprecatedExpressionTestCase#correctLocation",
                      XML_NAMESPACE_DEF +
                          "<operation:def name=\"someOp\"><operation:body>" +
                          "<set-payload value=\"10\"/>" +
                          "<logger level=\"WARN\" message=\"#[%dw 2.0 --- { 'v' : Mule::lookup('someFlow') }]\"/>" +
                          "</operation:body></operation:def>" +
                          XML_CLOSE)
            .stream().findFirst();
    assertThat(msg, is(CoreMatchers.not(empty())));
    Map<String, String> info = msg.get().getAdditionalData();
    assertThat(info.keySet(), hasSize(6));
    assertThat(info.keySet(),
               hasItems(LOCATION_START_POSITION_LINE, LOCATION_START_POSITION_COLUMN, LOCATION_START_POSITION_OFFSET,
                        LOCATION_END_POSITION_LINE, LOCATION_END_POSITION_COLUMN, LOCATION_END_POSITION_OFFSET));
    assertThat(info.get(LOCATION_START_POSITION_LINE), is("1"));
    assertThat(info.get(LOCATION_START_POSITION_COLUMN), is("1"));
    assertThat(info.get(LOCATION_START_POSITION_OFFSET), is("0"));
    assertThat(info.get(LOCATION_END_POSITION_LINE), is("1"));
    assertThat(info.get(LOCATION_END_POSITION_COLUMN), is("20"));
    assertThat(info.get(LOCATION_END_POSITION_OFFSET), is("0"));
  }

  private class TestScopePhaseValidationMessages implements ScopePhaseValidationMessages {

    private final List<ScopePhaseValidationItem> warnings;

    public TestScopePhaseValidationMessages() {
      this(emptyList());
    }

    public TestScopePhaseValidationMessages(List<ScopePhaseValidationItem> warnings) {
      this.warnings = warnings;
    }

    @Override
    public List<ScopePhaseValidationItem> getErrors() {
      return emptyList();
    }

    @Override
    public List<ScopePhaseValidationItem> getWarnings() {
      return warnings;
    }
  }

  private class TestScopePhaseValidationItem implements ScopePhaseValidationItem {

    private final String since;
    private final String function;
    private final Location location;

    public TestScopePhaseValidationItem(String since, String function) {
      this.since = since;
      this.function = function;
      this.location = new Location(new Position(1, 1, 0), new Position(1, 20, 0));
    }

    @Override
    public ScopePhaseValidationItemKind getKind() {
      return DEPRECATED;
    }

    @Override
    public String getMessage() {
      return "";
    }

    @Override
    public Map<String, String> getParams() {
      Map<String, String> params = new HashMap<>();
      params.put("since", since);
      params.put("function", function);
      return params;
    }

    @Override
    public Location getLocation() {
      return location;
    }
  }

}
