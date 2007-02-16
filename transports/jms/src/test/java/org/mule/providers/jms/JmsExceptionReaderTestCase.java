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

import org.mule.MuleException;
import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.Message;
import org.mule.tck.AbstractMuleTestCase;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;

public class JmsExceptionReaderTestCase extends AbstractMuleTestCase
{

    protected void doSetUp() throws Exception
    {
        ExceptionHelper.registerExceptionReader(new JmsExceptionReader());
    }

    public void testNestedExceptionRetrieval() throws Exception
    {
        Exception testException = getException();
        Throwable t = ExceptionHelper.getRootException(testException);
        assertNotNull(t);
        assertEquals("blah", t.getMessage());
        assertNull(t.getCause());

        t = ExceptionHelper.getRootMuleException(testException);
        assertNotNull(t);
        assertEquals("bar", t.getMessage());
        assertNotNull(t.getCause());

        List l = ExceptionHelper.getExceptionsAsList(testException);
        assertEquals(4, l.size());

        Map info = ExceptionHelper.getExceptionInfo(testException);
        assertNotNull(info);
        assertEquals(2, info.size());
        assertNotNull(info.get("JavaDoc"));
        assertEquals("1234", info.get("JMS Code"));
    }

    private Exception getException()
    {

        JMSException e = new JMSException("Jms error", "1234");
        e.setLinkedException(new IOException("blah"));

        return new MuleException(Message.createStaticMessage("foo"), new MuleException(
            Message.createStaticMessage("bar"), e));
    }

}
