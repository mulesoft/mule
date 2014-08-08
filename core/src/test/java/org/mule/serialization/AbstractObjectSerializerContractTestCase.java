/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.serialization;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.serialization.ObjectSerializer;
import org.mule.el.datetime.DateTime;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Locale;

import org.junit.Test;

public abstract class AbstractObjectSerializerContractTestCase extends AbstractMuleContextTestCase
{

    private static final String STRING_MESSAGE = "Hello World";

    protected ObjectSerializer serializer;

    @Test(expected = IllegalArgumentException.class)
    public final void nullBytes() throws Exception
    {
        serializer.deserialize((byte[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public final void nullStream() throws Exception
    {
        serializer.deserialize((InputStream) null);
    }

    @Test
    public final void nullObject() throws Exception
    {
        byte[] bytes = serializer.serialize(null);
        Object object = serializer.deserialize(bytes);
        assertNull(object);
    }

    @Test
    public final void inputStreamClosed() throws Exception
    {
        final byte[] bytes = serializer.serialize(STRING_MESSAGE);
        InputStream inputStream = spy(new ByteArrayInputStream(bytes));
        String output = serializer.deserialize(inputStream);

        verify(inputStream, atLeastOnce()).close();
        assertThat(output, equalTo(STRING_MESSAGE));
    }

    @Test
    public final void serializeWithoutDefaultConstructor() throws Exception
    {
        Calendar calendar = Calendar.getInstance();
        Locale locale = Locale.ITALIAN;

        DateTime dateTime = new DateTime(calendar, locale);
        dateTime.changeTimeZone("Pacific/Midway");

        MuleEvent event = getTestEvent(dateTime);
        byte[] bytes = serializer.serialize(event.getMessage());

        MuleMessage message = serializer.deserialize(bytes);
        DateTime deserealized = (DateTime) message.getPayload();

        assertEquals(calendar, deserealized.toCalendar());

        // test that the locale matches
        assertEquals(dateTime.format(), deserealized.format());
    }
}
