/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.el.validation.ScopePhaseValidationItemKind.DEPRECATED;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.el.validation.Location;
import org.mule.runtime.api.el.validation.ScopePhaseValidationItem;
import org.mule.runtime.api.el.validation.ScopePhaseValidationItemKind;
import org.mule.runtime.api.el.validation.ScopePhaseValidationMessages;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import io.qameta.allure.Features;
import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.hamcrest.CoreMatchers;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Features({@Feature(MULE_DSL), @Feature(REUSE)})
@Stories({@Story(DSL_VALIDATION_STORY), @Story(OPERATIONS)})
public class OperationDoesNotHaveDeprecatedExpressionTestCase extends AbstractCoreValidationTestCase {

  private static final String XML_NAMESPACE_DEF = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
      "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
      "      xmlns:operation=\"http://www.mulesoft.org/schema/mule/operation\"" +
      "      xsi:schemaLocation=\"\n" +
      "       http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd" +
      "       http://www.mulesoft.org/schema/mule/operation http://www.mulesoft.org/schema/mule/operation/current/mule-operation.xsd\">\n";
  private static final String XML_CLOSE = "</mule>";

  private ExpressionLanguage expressionLanguage = mock(ExpressionLanguage.class);

  @Override
  protected Validation getValidation() {
    return new OperationDoesNotHaveDeprecatedExpression(expressionLanguage);
  }

  @Test
  public void withoutDeprecation() {
    when(expressionLanguage.collectScopePhaseValidationMessages(contains("keySet("), anyString(), any()))
        .thenReturn(new TestScopePhaseValidationMessages(singletonList(new TestScopePhaseValidationItem("2.3",
                                                                                                        "dw::core::Objects::keySet"))));
    when(expressionLanguage.collectScopePhaseValidationMessages(not(contains("keySet(")), anyString(), any()))
        .thenReturn(new TestScopePhaseValidationMessages());
    final Optional<ValidationResultItem> msg = runValidation(XML_NAMESPACE_DEF +
        "<operation:def name=\"someOp\"><operation:body>" +
        "<set-payload value=\"10\"/>" +
        "<logger level=\"WARN\" message=\"#[payload ++ 10]\"/>" +
        "</operation:body></operation:def>" +
        XML_CLOSE).stream().findFirst();
    assertThat(msg, is(empty()));
  }

  @Test
  public void withDeprecation() {
    when(expressionLanguage.collectScopePhaseValidationMessages(contains("keySet("), anyString(), any()))
        .thenReturn(new TestScopePhaseValidationMessages(singletonList(new TestScopePhaseValidationItem("2.3.0",
                                                                                                        "dw::core::Objects::keySet"))));
    final Optional<ValidationResultItem> msg = runValidation(XML_NAMESPACE_DEF +
        "<operation:def name=\"someOp\"><operation:body>" +
        "<set-payload value=\"10\"/>" +
        "<logger level=\"WARN\" message=\"#[%dw 2.0 import * from dw::core::Objects --- { 'keySet' : keySet({ 'a' : true, 'b' : 1}) }]\"/>"
        +
        "</operation:body></operation:def>" +
        XML_CLOSE).stream().findFirst();
    assertThat(msg, is(CoreMatchers.not(empty())));
    assertThat(msg.get().getMessage(), containsString("Using an invalid function within an operation"));
  }

  @Test
  public void withErrors() {
    when(expressionLanguage.collectScopePhaseValidationMessages(contains("keySet("), anyString(), any()))
        .thenReturn(new TestScopePhaseValidationMessages(emptyList(),
                                                         singletonList(new TestScopePhaseValidationItem("2.3.0",
                                                                                                        "dw::core::Objects::keySet"))));
    final Optional<ValidationResultItem> msg = runValidation(XML_NAMESPACE_DEF +
        "<operation:def name=\"someOp\"><operation:body>" +
        "<set-payload value=\"10\"/>" +
        "<logger level=\"WARN\" message=\"#[%dw 2.0 import * from dw::core::Objects --- { 'keySet' : keySet({ 'a' : true, 'b' : 1}) }]\"/>"
        +
        "</operation:body></operation:def>" +
        XML_CLOSE).stream().findFirst();
    assertThat(msg, is(CoreMatchers.not(empty())));
    assertThat(msg.get().getMessage(), containsString("Using an invalid function within an operation"));
  }

  @Test
  public void withLookup() {
    when(expressionLanguage.collectScopePhaseValidationMessages(contains("Mule::lookup("), anyString(), any()))
        .thenReturn(new TestScopePhaseValidationMessages(emptyList(),
                                                         singletonList(new TestScopePhaseValidationItem("2.5.0",
                                                                                                        "Mule::lookup"))));
    final Optional<ValidationResultItem> msg = runValidation(XML_NAMESPACE_DEF +
        "<operation:def name=\"someOp\"><operation:body>" +
        "<set-payload value=\"10\"/>" +
        "<logger level=\"WARN\" message=\"#[%dw 2.0 --- { 'v' : Mule::lookup('someFlow') }]\"/>" +
        "</operation:body></operation:def>" +
        XML_CLOSE).stream().findFirst();
    assertThat(msg, is(CoreMatchers.not(empty())));
    assertThat(msg.get().getMessage(), containsString("Using an invalid function within an operation"));
  }


  @Test
  @Ignore
  public void withLookupWithoutMulePrefix() {
    when(expressionLanguage.collectScopePhaseValidationMessages(contains("lookup("), anyString(), any()))
        .thenReturn(new TestScopePhaseValidationMessages(emptyList(),
                                                         singletonList(new TestScopePhaseValidationItem("2.5.0", "lookup"))));
    final Optional<ValidationResultItem> msg = runValidation(XML_NAMESPACE_DEF +
        "<operation:def name=\"someOp\"><operation:body>" +
        "<set-payload value=\"10\"/>" +
        "<logger level=\"WARN\" message=\"#[%dw 2.0 --- { 'v' : lookup('someFlow') }]\"/>" +
        "</operation:body></operation:def>" +
        XML_CLOSE).stream().findFirst();
    assertThat(msg, is(CoreMatchers.not(empty())));
    assertThat(msg.get().getMessage(), containsString("Using an invalid function within an operation"));
  }

  @Test
  public void withoutExpression() {
    when(expressionLanguage.collectScopePhaseValidationMessages(contains("Mule::lookup("), anyString(), any()))
        .thenReturn(new TestScopePhaseValidationMessages(emptyList(),
                                                         singletonList(new TestScopePhaseValidationItem("2.5.0",
                                                                                                        "Mule::lookup"))));
    final Optional<ValidationResultItem> msg = runValidation(XML_NAMESPACE_DEF +
        "<operation:def name=\"someOp\"><operation:body>" +
        "<set-payload value=\"10\"/>" +
        "</operation:body></operation:def>" +
        XML_CLOSE).stream().findFirst();
    assertThat(msg, is(empty()));
  }

  @Test
  public void withMultipleCorrectExpressions() {
    when(expressionLanguage.collectScopePhaseValidationMessages(contains("keySet("), anyString(), any()))
        .thenReturn(new TestScopePhaseValidationMessages(singletonList(new TestScopePhaseValidationItem("2.3",
                                                                                                        "dw::core::Objects::keySet"))));
    when(expressionLanguage.collectScopePhaseValidationMessages(not(contains("keySet(")), anyString(), any()))
        .thenReturn(new TestScopePhaseValidationMessages());
    final Optional<ValidationResultItem> msg = runValidation(XML_NAMESPACE_DEF +
        "<operation:def name=\"someOp\"><operation:body>" +
        "<set-payload value=\"10\"/>" +
        "<logger level=\"WARN\" message=\"#[payload ++ 10]\"/>" +
        "<set-payload value=\"#[payload + 1]\"/>" +
        "<set-variable value=\"#[someFunction(10)]\"/>" +
        "</operation:body></operation:def>" +
        XML_CLOSE).stream().findFirst();
    assertThat(msg, is(empty()));
  }

  @Test
  public void withOneOverManyExpressionsDeprecated() {
    when(expressionLanguage.collectScopePhaseValidationMessages(anyString(), anyString(), any()))
        .thenReturn(new TestScopePhaseValidationMessages());
    when(expressionLanguage.collectScopePhaseValidationMessages(contains("Mule::lookup("), anyString(), any()))
        .thenReturn(new TestScopePhaseValidationMessages(emptyList(),
                                                         singletonList(new TestScopePhaseValidationItem("2.5.0",
                                                                                                        "Mule::lookup"))));
    final Optional<ValidationResultItem> msg = runValidation(XML_NAMESPACE_DEF +
        "<operation:def name=\"someOp\"><operation:body>" +
        "<set-payload value=\"10\"/>" +
        "<logger level=\"WARN\" message=\"#[payload ++ 10]\"/>" +
        "<set-payload value=\"#[payload + 1]\"/>" +
        "<logger level=\"WARN\" message=\"#[%dw 2.0 --- { 'v' : Mule::lookup('someFlow') }]\"/>" +
        "<set-variable value=\"#[someFunction(10)]\"/>" +
        "</operation:body></operation:def>" +
        XML_CLOSE).stream().findFirst();
    assertThat(msg, is(CoreMatchers.not(empty())));
    assertThat(msg.get().getMessage(), containsString("Using an invalid function within an operation"));
  }

  @Test
  public void withFutureDeprecatedExpression() {
    when(expressionLanguage.collectScopePhaseValidationMessages(contains("keySet("), anyString(), any()))
        .thenReturn(new TestScopePhaseValidationMessages(singletonList(new TestScopePhaseValidationItem("2.7.0",
                                                                                                        "dw::core::Objects::keySet"))));
    final Optional<ValidationResultItem> msg = runValidation(XML_NAMESPACE_DEF +
        "<operation:def name=\"someOp\"><operation:body>" +
        "<set-payload value=\"10\"/>" +
        "<logger level=\"WARN\" message=\"#[%dw 2.0 import * from dw::core::Objects --- { 'keySet' : keySet({ 'a' : true, 'b' : 1}) }]\"/>"
        +
        "</operation:body></operation:def>" +
        XML_CLOSE).stream().findFirst();
    assertThat(msg, is(empty()));
  }

  private class TestScopePhaseValidationMessages implements ScopePhaseValidationMessages {

    private final List<ScopePhaseValidationItem> errors;
    private final List<ScopePhaseValidationItem> warnings;

    public TestScopePhaseValidationMessages() {
      this(emptyList(), emptyList());
    }

    public TestScopePhaseValidationMessages(List<ScopePhaseValidationItem> warnings) {
      this(warnings, emptyList());
    }

    public TestScopePhaseValidationMessages(List<ScopePhaseValidationItem> warnings, List<ScopePhaseValidationItem> errors) {
      this.errors = errors;
      this.warnings = warnings;
    }

    @Override
    public List<ScopePhaseValidationItem> getErrors() {
      return errors;
    }

    @Override
    public List<ScopePhaseValidationItem> getWarnings() {
      return warnings;
    }
  }

  private class TestScopePhaseValidationItem implements ScopePhaseValidationItem {

    private final String since;
    private final String function;

    public TestScopePhaseValidationItem(String since, String function) {
      this.since = since;
      this.function = function;
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
      return null;
    }
  }

}
