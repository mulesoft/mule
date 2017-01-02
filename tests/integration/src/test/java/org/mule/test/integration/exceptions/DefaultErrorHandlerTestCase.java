/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.junit.Test;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.retry.RetryPolicyExhaustedException;
import org.mule.test.AbstractIntegrationTestCase;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Error Handling")
@Stories("Default Error Handler")
public class DefaultErrorHandlerTestCase extends AbstractIntegrationTestCase {

  private static Exception exception;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/default-error-handler-config.xml";
  }

  @Test
  public void defaultCustomIsUsedWhenCatchAllMissingAndNoMatch() throws Exception {
    verifyWithException(new ExpressionRuntimeException(createStaticMessage("Error")), "defaultEH-custom");
  }

  @Test
  public void defaultAllIsUsedWhenCatchAllMissingAndNoMatch() throws Exception {
    verifyWithException(new RuntimeException("Error"), "defaultEH-all");
  }

  @Test
  public void flowIsUsedWhenCatchAllIsMissingButMatchFound() throws Exception {
    verifyWithException(new RetryPolicyExhaustedException(createStaticMessage("Error"), mock(Component.class)), "innerEH");
  }

  private void verifyWithException(Exception exceptionToThrow, String expectedPayload) throws Exception {
    exception = exceptionToThrow;
    assertThat(flowRunner("test").withPayload("").run().getMessage(), hasPayload(is(expectedPayload)));
  }

  protected static class ThrowExceptionProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      if (exception instanceof RuntimeException) {
        throw (RuntimeException) exception;
      } else {
        throw (MuleException) exception;
      }
    }
  }

}
