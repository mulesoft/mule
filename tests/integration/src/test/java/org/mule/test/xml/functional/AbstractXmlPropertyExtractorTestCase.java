/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.xml.functional;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.message.InternalMessage;

import org.junit.Test;

public abstract class AbstractXmlPropertyExtractorTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/module/xml/property-extractor-test.xml";
  }

  protected abstract Object getMatchMessage() throws Exception;

  protected abstract Object getErrorMessage() throws Exception;

  @Test
  public void testMatch() throws Exception {
    InternalMessage message = flowRunner("test").withPayload(getMatchMessage()).run().getMessage();

    assertNotNull(message);
    assertThat(message.getPayload().getValue(), is("match"));
  }

  @Test
  public void testError() throws Exception {
    MessagingException e = flowRunner("test").withPayload(getErrorMessage()).runExpectingException();
    assertThat(e.getMessage(),
               containsString("evaluating expression: \"payload.childBean.value\". (org.mule.runtime.core.api.expression.ExpressionRuntimeException)."));
  }
}
