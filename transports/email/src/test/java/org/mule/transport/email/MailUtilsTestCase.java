/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email;

import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Map;

import javax.mail.Part;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;

import org.apache.commons.collections.map.HashedMap;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MailUtilsTestCase extends AbstractMuleTestCase
{

    private static final String EMAIL_1 = "vasya@pupkin.com";
    private static final String EMAIL_2 = "zhora@buryakov.com";
    private InternetAddress inetAddress1;
    private InternetAddress inetAddress2;
    private static final String MULTIPLE_EMAILS_WITH_WHITESPACE = EMAIL_1 + ", " + EMAIL_2;
    private static final String MULTIPLE_EMAILS_WITHOUT_WHITESPACE = EMAIL_1 + "," + EMAIL_2;

    @Before
    public void createInternetAddresses() throws AddressException
    {
        inetAddress1 = new InternetAddress(EMAIL_1);
        inetAddress2 = new InternetAddress(EMAIL_2);
    }

    @Test
    public void testSingleInternetAddressToString() throws Exception
    {
        String result = MailUtils.internetAddressesToString(inetAddress1);
        assertEquals("Wrong internet address conversion.", EMAIL_1, result);
    }

    @Test
    public void testMultipleInternetAddressesToString()
    {
        String result = MailUtils.internetAddressesToString(new InternetAddress[]{inetAddress1, inetAddress2});
        assertEquals("Wrong internet address conversion.", MULTIPLE_EMAILS_WITH_WHITESPACE, result);
    }

    @Test
    public void testStringToSingleInternetAddresses() throws Exception
    {
        InternetAddress[] result = MailUtils.stringToInternetAddresses(EMAIL_1);
        assertNotNull(result);
        assertEquals("Wrong number of addresses parsed.", 1, result.length);
        assertEquals("Wrong internet address conversion.", inetAddress1, result[0]);
    }

    @Test
    public void testStringWithWhitespaceToMultipleInternetAddresses() throws Exception
    {
        InternetAddress[] result = MailUtils.stringToInternetAddresses(MULTIPLE_EMAILS_WITH_WHITESPACE);
        assertNotNull(result);
        assertEquals("Wrong number of addresses parsed.", 2, result.length);
        assertEquals("Wrong internet address conversion.", inetAddress1, result[0]);
        assertEquals("Wrong internet address conversion.", inetAddress2, result[1]);
    }

    @Test
    public void testStringWithoutWhitespaceToMultipleInternetAddresses() throws Exception
    {
        InternetAddress[] result = MailUtils.stringToInternetAddresses(MULTIPLE_EMAILS_WITHOUT_WHITESPACE);
        assertNotNull(result);
        assertEquals("Wrong number of addresses parsed.", 2, result.length);
        assertEquals("Wrong internet address conversion.", inetAddress1, result[0]);
        assertEquals("Wrong internet address conversion.", inetAddress2, result[1]);
    }

    @Test
    public void testGetAttachmentName() throws Exception
    {
        @SuppressWarnings("unchecked")
        Map<String, Part> attachments = new HashedMap();

        String key = "test.txt";
        assertEquals(key, MailUtils.getAttachmentName(key, attachments));

        attachments.put(key, new MimeBodyPart());
        assertEquals("0_" + key, MailUtils.getAttachmentName(key, attachments));
    }
}
