/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.routing.RoutingException;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class ExceptionsTestCase extends AbstractMuleTestCase {

  @Test
  public void testExceptionChaining() {
    String rootMsg = "Root Test Exception Message";
    String msg = "Test Exception Message";

    Exception e = new MuleContextException(I18nMessageFactory.createStaticMessage(msg),
                                           new DefaultMuleException(I18nMessageFactory.createStaticMessage(rootMsg)));

    assertEquals(rootMsg, e.getCause().getMessage());
    assertEquals(msg, e.getMessage());
    assertEquals(e.getClass().getName() + ": " + msg, e.toString());
  }

  @Test
  public final void testRoutingExceptionNullMessageValidProcessor() throws MuleException {
    Processor processor = mock(Processor.class);
    RoutingException rex = new RoutingException(processor);
    assertSame(processor, rex.getRoute());
  }

}
