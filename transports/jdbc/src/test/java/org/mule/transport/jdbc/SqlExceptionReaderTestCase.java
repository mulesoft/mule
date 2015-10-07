/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.api.DefaultMuleException;
import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.MessageFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

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

        List<Throwable> l = ExceptionHelper.getExceptionsAsList(testException);
        assertEquals(4, l.size());

        Map info = ExceptionHelper.getExceptionInfo(testException);
        assertNotNull(info);
        assertEquals(2, info.size());
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
