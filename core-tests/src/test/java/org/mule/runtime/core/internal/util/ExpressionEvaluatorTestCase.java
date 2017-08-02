/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.weave.v2.el.ByteArrayBasedCursorStreamProvider;

import org.junit.Test;

public class ExpressionEvaluatorTestCase extends AbstractMuleContextTestCase {

  @Test
  public void handleNullEvent() throws MuleException {
    TypedValue evaluate = muleContext.getExpressionManager().evaluate("%dw 2.0\noutput application/json\n---\n{a: 1}");
    ByteArrayBasedCursorStreamProvider value = (ByteArrayBasedCursorStreamProvider) evaluate.getValue();
    String expected = "{\n" +
        "  \"a\": 1\n" +
        "}";
    assertThat(IOUtils.toString(value), is(expected));
  }
}
