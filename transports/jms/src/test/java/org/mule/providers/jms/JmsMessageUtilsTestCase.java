/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms;

import com.mockobjects.dynamic.Mock;

import org.mule.tck.AbstractMuleTestCase;

import javax.jms.TextMessage;

/**
 * @author <a href="mailto:aperepel@gmail.com">Andrew Perepelytsya</a>
 */
public class JmsMessageUtilsTestCase extends AbstractMuleTestCase {

    public void testTextMessageNullContent() throws Exception {
        Mock mockMessage = new Mock(TextMessage.class);
        mockMessage.expectAndReturn("getText", null);

        TextMessage mockTextMessage = (TextMessage) mockMessage.proxy();

        byte[] result = JmsMessageUtils.getBytesFromMessage(mockTextMessage);
        assertNotNull(result);
        assertEquals("Should return an empty byte array.", 0, result.length);

        mockMessage.verify();
    }
}
