/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc;

import org.mule.api.DefaultMuleException;
import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.MessageFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SqlExceptionReaderTestCase extends AbstractMuleTestCase
{

    /**
     * Print the name of this test to standard output
     */
    @Before
    public void registerExceptionReader()
    {
        ExceptionHelper.registerExceptionReader(new SQLExceptionReader());
    }

    @Test
    public void testNestedExceptionRetreval() throws Exception
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
        assertEquals(3, info.size());
        assertNotNull(info.get("JavaDoc"));
        assertEquals("1234", info.get("SQL Code"));
        assertEquals("bad SQL state", info.get("SQL State"));
    }

    private Exception getException()
    {

        SQLException e = new SQLException("SQL error", "bad SQL state", 1234);
        e.setNextException(new SQLException("blah"));

        return new DefaultMuleException(MessageFactory.createStaticMessage("foo"), new DefaultMuleException(
            MessageFactory.createStaticMessage("bar"), e));
    }
}
