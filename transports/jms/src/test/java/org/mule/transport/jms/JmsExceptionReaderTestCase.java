/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms;

import org.mule.api.DefaultMuleException;
import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.MessageFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SmallTest
public class JmsExceptionReaderTestCase extends AbstractMuleTestCase
{

    @Before
    public void registerExceptionReader()
    {
        ExceptionHelper.registerExceptionReader(new JmsExceptionReader());
    }

    @Test
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

        return new DefaultMuleException(MessageFactory.createStaticMessage("foo"), new DefaultMuleException(
            MessageFactory.createStaticMessage("bar"), e));
    }

}
