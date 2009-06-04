/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.email.adapters;

import org.mule.config.i18n.LocaleMessageHandler;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transport.email.MailProperties;
import org.mule.transport.email.SimpleMailMessageAdapter;
import org.mule.util.SystemUtils;

import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SimpleMailMessageAdapterTestCase extends AbstractMuleTestCase
{

    private static final String NAME_1 = "name1";
    private static final String NAME_2 = "name2";
    private static final String VALUE_1 = "value1";
    private static final String VALUE_2 = "value2";


    public void testHeaders() throws Exception
    {
        Message message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        message.addHeader(NAME_1, VALUE_1);
        message.addHeader(NAME_1, VALUE_2);
        message.addHeader(NAME_2, VALUE_2);
        SimpleMailMessageAdapter adapter = new SimpleMailMessageAdapter(message);
        assertEquals(adapter.getProperty(NAME_1), VALUE_1);
        assertTrue(adapter.getProperty(SimpleMailMessageAdapter.toListHeader(NAME_1)) instanceof List);
        List list1 = (List) adapter.getProperty(SimpleMailMessageAdapter.toListHeader(NAME_1));
        assertTrue(list1.contains(VALUE_1));
        assertTrue(list1.contains(VALUE_2));
        assertEquals(2, list1.size());
        assertEquals(adapter.getProperty(NAME_2), VALUE_2);
        assertTrue(adapter.getProperty(SimpleMailMessageAdapter.toListHeader(NAME_2)) instanceof List);
        List list2 = (List) adapter.getProperty(SimpleMailMessageAdapter.toListHeader(NAME_2));
        assertTrue(list2.contains(VALUE_2));
        assertEquals(1, list2.size());
    }


    public void testInvalidFrom() throws Exception
    {
        Message message = new javax.mail.internet.MimeMessage(Session.getDefaultInstance(new Properties()));

        // do not use the ctor taking a string here as it tries to parse the string and we're
        // trying to use an invalid address here.
        InternetAddress fromAddress = new InternetAddress();
        fromAddress.setAddress("foo@bar@baz");
        message.setFrom(fromAddress);

        InternetAddress replyToAddrress = new InternetAddress();
        replyToAddrress.setAddress("baz@bletch@buzz");
        message.setReplyTo(new Address[]{replyToAddrress});

        SimpleMailMessageAdapter adapter = new SimpleMailMessageAdapter(message);
        assertEquals(null, adapter.getProperty(MailProperties.INBOUND_FROM_ADDRESS_PROPERTY));
        assertEquals(null, adapter.getProperty(MailProperties.INBOUND_REPLY_TO_ADDRESSES_PROPERTY));
    }

    public void testGetPayloadAsString() throws Exception
    {
        testGetPayloadAsString(Locale.US, "US-ASCII");
        testGetPayloadAsString(Locale.US, "UTF-8");
        testGetPayloadAsString(Locale.US, "UTF-8");
        testGetPayloadAsString(new Locale("be", "", ""), "UTF-16be");

        testGetPayloadAsString(Locale.JAPAN, "UTF-8");
        testGetPayloadAsString(Locale.JAPAN, "Shift_JIS");
        testGetPayloadAsString(Locale.JAPAN, "Windows-31J");
        testGetPayloadAsString(Locale.JAPAN, "EUC-JP");
    }

    public void testGetPayloadAsString(Locale locale, String encoding) throws Exception
    {
        Message message = new MimeMessage(Session.getDefaultInstance(new Properties()));

        InternetAddress fromAddress = new InternetAddress();
        fromAddress.setAddress("foo@example.com");
        message.setFrom(fromAddress);

        InternetAddress toAddrress = new InternetAddress();
        toAddrress.setAddress("bar@example.com");
        message.setRecipient(RecipientType.TO, toAddrress);

        String text = LocaleMessageHandler.getString("test-data", locale, "SimpleMailMessageAdapterTestCase.testGetPayloadAsString", new Object[]{});
        message.setContent(text, "text/plain; charset="+ encoding);

        SimpleMailMessageAdapter adapter = new SimpleMailMessageAdapter(message);
        assertEquals(text + SystemUtils.LINE_SEPARATOR, adapter.getPayloadAsString(encoding));
    }

}
