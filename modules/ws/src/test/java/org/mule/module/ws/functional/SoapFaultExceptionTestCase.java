/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.functional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.cxf.binding.soap.SoapFault;
import org.junit.Test;
import org.mockito.Mockito;
import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.ws.consumer.SoapFaultException;
import org.mule.tck.junit4.AbstractMuleTestCase;


public class SoapFaultExceptionTestCase extends AbstractMuleTestCase
{

    private static final String MESSAGE = "message";

    @Test
    public void testGeneratedMessage() throws Exception
    {
        MuleEvent event = mock(MuleEvent.class);
        SoapFault soapFault = Mockito.mock(SoapFault.class);
        when(soapFault.getMessage()).thenReturn(MESSAGE);
        MessageProcessor failingMessageProcessor = Mockito.mock(MessageProcessor.class);
        SoapFaultException exception = new SoapFaultException(event, soapFault, failingMessageProcessor);
        assertThat(exception.getMessage(), equalTo(MESSAGE));
    }
}
