/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static org.mule.tck.junit4.rule.DataWeaveExpressionLanguage.dataWeaveRule;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.DataWeaveExpressionLanguage;
import org.mule.weave.v2.el.ByteArrayBasedCursorStreamProvider;

import org.junit.Rule;
import org.junit.Test;

public class ExpressionEvaluatorTestCase extends AbstractMuleTestCase {

  @Rule
  public DataWeaveExpressionLanguage dw = dataWeaveRule();

  @Test
  public void handleNullEvent() throws MuleException {
    TypedValue evaluate = dw.getExpressionManager().evaluate("%dw 2.0\noutput application/json\n---\n{a: 1}");
    ByteArrayBasedCursorStreamProvider value = (ByteArrayBasedCursorStreamProvider) evaluate.getValue();
    String expected = """
        {
          "a": 1
        }""";
    assertThat(IOUtils.toString(value), is(expected));
  }
}
