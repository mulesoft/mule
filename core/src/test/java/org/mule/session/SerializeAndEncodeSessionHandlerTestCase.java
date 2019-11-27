/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.session;

import static java.lang.System.setProperty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.api.config.MuleProperties.MULE_SESSION_PROPERTY;
import static org.mule.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.session.SerializeOnlySessionHandler.ALLOW_HTTP_FALLBACK_MAX_TIMESTAMP_PROPERTY;

import java.io.IOException;
import java.io.InvalidClassException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang.SerializationException;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleProperties;
import org.mule.tck.junit4.AbstractMuleTestCase;

@RunWith(MockitoJUnitRunner.class)
public class SerializeAndEncodeSessionHandlerTestCase extends AbstractMuleTestCase
{
    private static final String INVALID_CLASS_MESSAGE = "Invalid Class";

    private static final String SERIALIZATION_EXCEPTION_MESSAGE = "Serialization Exception";

    private static final String DUMMY_SESSION_STRING = "DUMMYSESSIONSTRING";

    private static final int SIGNATURE_LENGTH = 32;

    @Mock
    private MuleMessage message;

    @Mock
    private MuleContext context;

    @Before
    public void setup()
    {
        setProperty(SYSTEM_PROPERTY_PREFIX + "session.serialization.enforce.sinceTimestamp", "2999-01-01T12:00:00.000-0000");
        setProperty(ALLOW_HTTP_FALLBACK_MAX_TIMESTAMP_PROPERTY, "2999-01-01T12:00:00.000-0000");
    }

    @Test
    public void returnNullSessionWhenIncompatibleClass() throws Exception
    {
        when(message.getMuleContext()).thenReturn(context);
        when(message.getInboundProperty(MULE_SESSION_PROPERTY)).thenReturn(DUMMY_SESSION_STRING);
        SerializeAndEncodeSessionHandler handler = new SerializeAndEncodeSessionHandler();
        MuleSession session = handler.retrieveSessionInfoFromMessage(message);
        assertThat(session, nullValue());
    }

    @Test
    public void cycleWithSignedSession() throws MuleException
    {
        when(message.getMuleContext()).thenReturn(context);
        when(message.getInboundProperty("MULE_ENDPOINT")).thenReturn("http://myhost/lalala");
        when(context.getExecutionClassLoader()).thenReturn(this.getClass().getClassLoader());
        
        SerializeAndEncodeSessionHandler handler = new SerializeAndEncodeSessionHandler();
        
        ArgumentCaptor<String> serializedSessionCaptor = forClass(String.class);
        MuleSession session = new DefaultMuleSession();
        session.setProperty("hello", "world");
        handler.storeSessionInfoToMessage(session, message);
        verify(message).setOutboundProperty(eq(MULE_SESSION_PROPERTY), serializedSessionCaptor.capture());
        
        when(message.getInboundProperty(MULE_SESSION_PROPERTY)).thenReturn(serializedSessionCaptor.getValue());
        MuleSession newSession = handler.retrieveSessionInfoFromMessage(message);
        assertThat(newSession, not(nullValue()));
        assertThat((String)newSession.getProperty("hello"), Matchers.is("world"));
    }
    
    @Test
    public void returnNullSessionWhenUnsignedSession() throws MuleException
    {
        when(message.getMuleContext()).thenReturn(context);
        when(context.getExecutionClassLoader()).thenReturn(this.getClass().getClassLoader());

        SerializeAndEncodeSessionHandler handler = new SerializeAndEncodeSessionHandler();

        ArgumentCaptor<String> serializedSessionCaptor = forClass(String.class);
        handler.storeSessionInfoToMessage(new DefaultMuleSession(), message);
        verify(message).setOutboundProperty(Mockito.eq(MuleProperties.MULE_SESSION_PROPERTY), serializedSessionCaptor.capture());

        byte[] signedData = serializedSessionCaptor.getValue().getBytes(UTF_8);
        byte[] data = new byte[signedData.length - SIGNATURE_LENGTH];
        
        System.arraycopy(signedData, SIGNATURE_LENGTH, data, 0, signedData.length - SIGNATURE_LENGTH);

        when(message.getInboundProperty(MULE_SESSION_PROPERTY)).thenReturn(new String(data, UTF_8));
        MuleSession session = handler.retrieveSessionInfoFromMessage(message);
        assertThat(session, nullValue());
    }

    @Test
    public void returnNullSessionWhenInvalidSignatureSession() throws MuleException, IOException
    {
        when(message.getMuleContext()).thenReturn(context);
        when(context.getExecutionClassLoader()).thenReturn(this.getClass().getClassLoader());

        SerializeAndEncodeSessionHandler handler = new SerializeAndEncodeSessionHandler();

        ArgumentCaptor<String> serializedSessionCaptor = forClass(String.class);
        handler.storeSessionInfoToMessage(new DefaultMuleSession(), message);
        verify(message).setOutboundProperty(Mockito.eq(MuleProperties.MULE_SESSION_PROPERTY), serializedSessionCaptor.capture());

        String encodeBytes = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaAAAAAAAAAAAAAAAAAAAAAAAAaaaRRRRRRRRRRRGGGGGGGGGHHHHHHhhh==";
        String tamperedSignature = serializedSessionCaptor.getValue().substring(encodeBytes.length());
        
        when(message.getInboundProperty(MULE_SESSION_PROPERTY)).thenReturn(tamperedSignature);
        MuleSession session = handler.retrieveSessionInfoFromMessage(message);
        assertThat(session, nullValue());
    }
}
