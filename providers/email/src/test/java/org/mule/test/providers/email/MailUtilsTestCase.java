/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.providers.email;

import org.mule.providers.email.MailUtils;
import org.mule.tck.AbstractMuleTestCase;

import javax.mail.internet.InternetAddress;

/**
 * @author <a href="mailto:aperepel@itci.com">Andrew Perepelytsya</a>
 */
public class MailUtilsTestCase extends AbstractMuleTestCase
{
    private static final String EMAIL_1 = "vasya@pupkin.com";
    private static final String EMAIL_2 = "zhora@buryakov.com";
    private InternetAddress inetAddress1;
    private InternetAddress inetAddress2;
    private static final String MULTIPLE_EMAILS_WITH_WHITESPACE = EMAIL_1 + ", " + EMAIL_2;
    private static final String MULTIPLE_EMAILS_WITHOUT_WHITESPACE = EMAIL_1 + "," + EMAIL_2;


    protected void doSetUp() throws Exception
    {
        inetAddress1 = new InternetAddress(EMAIL_1);
        inetAddress2 = new InternetAddress(EMAIL_2);
    }

    public void testSingleInternetAddressToString() throws Exception
    {
        String result = MailUtils.internetAddressesToString(inetAddress1);
        assertEquals("Wrong internet address conversion.", EMAIL_1, result);
    }

    public void testMultipleInternetAddressesToString()
    {
        String result = MailUtils.internetAddressesToString(new InternetAddress[] {inetAddress1, inetAddress2});
        assertEquals("Wrong internet address conversion.", MULTIPLE_EMAILS_WITH_WHITESPACE , result);
    }

    public void testStringToSingleInternetAddresses() throws Exception
    {
        InternetAddress[] result = MailUtils.stringToInternetAddresses(EMAIL_1);
        assertNotNull(result);
        assertEquals("Wrong number of addresses parsed.", 1, result.length);
        assertEquals("Wrong internet address conversion.", inetAddress1, result[0]);
    }

    public void testStringWithWhitespaceToMultipleInternetAddresses() throws Exception
    {
        InternetAddress[] result = MailUtils.stringToInternetAddresses(MULTIPLE_EMAILS_WITH_WHITESPACE);
        assertNotNull(result);
        assertEquals("Wrong number of addresses parsed.", 2, result.length);
        assertEquals("Wrong internet address conversion.", inetAddress1, result[0]);
        assertEquals("Wrong internet address conversion.", inetAddress2, result[1]);
    }

    public void testStringWithoutWhitespaceToMultipleInternetAddresses() throws Exception
    {
        InternetAddress[] result = MailUtils.stringToInternetAddresses(MULTIPLE_EMAILS_WITHOUT_WHITESPACE);
        assertNotNull(result);
        assertEquals("Wrong number of addresses parsed.", 2, result.length);
        assertEquals("Wrong internet address conversion.", inetAddress1, result[0]);
        assertEquals("Wrong internet address conversion.", inetAddress2, result[1]);
    }

}
