/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.session;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.mule.api.config.MuleProperties.MULE_SESSION_PROPERTY;

import java.io.InvalidClassException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.serialization.SerializationException;
import org.mule.serialization.internal.ClassSpecificObjectSerializer;
import org.mule.tck.junit4.AbstractMuleTestCase;

@RunWith(MockitoJUnitRunner.class)
public class SerializeAndEncodeSessionHandlerTestCase extends AbstractMuleTestCase
{
    private static final String INVALID_CLASS_MESSAGE = "Invalid Class";

    private static final String SERIALIZATION_EXCEPTION_MESSAGE = "Serialization Exception";

    private static final String DUMMY_SESSION_STRING = "DUMMYSESSIONSTRING";

    @Mock
    private MuleMessage message;

    @Mock
    private MuleContext context;

    @Mock
    private ClassSpecificObjectSerializer serializer;

    @Test
    public void returnNullSessionWhenIncompatibleClass() throws Exception
    {
        when(message.getMuleContext()).thenReturn(context);
        when(message.getInboundProperty(MULE_SESSION_PROPERTY)).thenReturn(DUMMY_SESSION_STRING);
        when(context.getObjectSerializer()).thenReturn(serializer);
        when(serializer.deserialize(Mockito.any(byte[].class), any(ClassLoader.class))).thenThrow(
                new SerializationException(SERIALIZATION_EXCEPTION_MESSAGE, new InvalidClassException(INVALID_CLASS_MESSAGE)));
        SerializeAndEncodeSessionHandler handler = new SerializeAndEncodeSessionHandler();
        MuleSession session = handler.retrieveSessionInfoFromMessage(message);
        assertThat(session, nullValue());
    }
}
