/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import org.mule.functional.exceptions.FunctionalTestException;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.ExceptionHelper;

import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

/**
 * see MULE-4512
 */
public class SynchronousResponseExceptionTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/messaging/meps/synchronous-response-exception-flow.xml";
  }

  @Test
  public void testComponentException() throws Exception {
    MessagingException e = flowRunner("ComponentException").withPayload("request").runExpectingException();
    assertThat(ExceptionHelper.getRootException(e), instanceOf(FunctionalTestException.class));
  }

  @Test
  public void testFlowRefInvalidException() throws Exception {
    MessagingException e = flowRunner("FlowRefInvalidException").withPayload("request").runExpectingException();
    assertThat(ExceptionHelper.getRootException(e), instanceOf(NoSuchBeanDefinitionException.class));
  }

  @Test
  public void testTransformerException() throws Exception {
    MessagingException e = flowRunner("TransformerException").withPayload("request").runExpectingException();
    assertThat(ExceptionHelper.getRootException(e), instanceOf(TransformerException.class));
  }
}
