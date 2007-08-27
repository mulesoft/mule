/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jdbc;

import org.mule.MuleException;
import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.MessageFactory;
import org.mule.tck.AbstractMuleTestCase;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class SqlExceptionReaderTestCase extends AbstractMuleTestCase
{

    /**
     * Print the name of this test to standard output
     */
    protected void doSetUp() throws Exception
    {
        ExceptionHelper.registerExceptionReader(new SQLExceptionReader());
    }

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

        return new MuleException(MessageFactory.createStaticMessage("foo"), new MuleException(
            MessageFactory.createStaticMessage("bar"), e));
    }
}
