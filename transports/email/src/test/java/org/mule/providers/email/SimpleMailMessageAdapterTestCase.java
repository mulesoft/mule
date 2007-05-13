/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email;

import org.mule.tck.AbstractMuleTestCase;

import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
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

}
