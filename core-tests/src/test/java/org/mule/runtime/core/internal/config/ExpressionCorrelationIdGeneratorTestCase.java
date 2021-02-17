/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;
import org.mule.runtime.core.api.config.CorrelationIdGenerator;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import static java.lang.Integer.parseInt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.greaterThan;
import static org.mule.test.allure.AllureConstants.CorrelationIdFeature.CORRELATION_ID;
import static org.mule.test.allure.AllureConstants.CorrelationIdFeature.CorrelationIdOnSourcesStory.CORRELATION_ID_ON_SOURCES;

@Issue("MULE-18770")
@Feature(CORRELATION_ID)
@Story(CORRELATION_ID_ON_SOURCES)
public class ExpressionCorrelationIdGeneratorTestCase extends AbstractMuleContextTestCase {

  @Test
  public void staticEvaluation() {
    CorrelationIdGenerator generator = new ExpressionCorrelationIdGenerator(muleContext, "'test'");
    assertThat(generator.generateCorrelationId(), is("test"));
  }

  @Test
  public void fullExpressionEvaluation() {
    CorrelationIdGenerator generator =
        new ExpressionCorrelationIdGenerator(muleContext, "#[(random() * 42 as String splitBy('.'))[0]]");

    assertThat(parseInt(generator.generateCorrelationId()), is(both(greaterThan(0)).and(lessThan(42))));
  }

}
