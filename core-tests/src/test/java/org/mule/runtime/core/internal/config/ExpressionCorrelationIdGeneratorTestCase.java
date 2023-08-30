/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config;

import static org.mule.test.allure.AllureConstants.CorrelationIdFeature.CORRELATION_ID;
import static org.mule.test.allure.AllureConstants.CorrelationIdFeature.CorrelationIdOnSourcesStory.CORRELATION_ID_ON_SOURCES;

import static java.lang.Integer.parseInt;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.api.el.ExpressionCompilationException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Issue("MULE-18770")
@Feature(CORRELATION_ID)
@Story(CORRELATION_ID_ON_SOURCES)
public class ExpressionCorrelationIdGeneratorTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException exception = none();

  @Test
  public void staticEvaluation() {
    ExpressionCorrelationIdGenerator generator = new ExpressionCorrelationIdGenerator(muleContext, "'test'");
    exception.expect(ExpressionCompilationException.class);
    generator.initializeGenerator();
  }

  @Test
  public void fullExpressionEvaluation() {
    ExpressionCorrelationIdGenerator generator =
        new ExpressionCorrelationIdGenerator(muleContext, "#[(random() * 42 as String splitBy('.'))[0]]");
    generator.initializeGenerator();

    assertThat(parseInt(generator.generateCorrelationId()), is(both(greaterThanOrEqualTo(0)).and(lessThan(42))));
  }

}
