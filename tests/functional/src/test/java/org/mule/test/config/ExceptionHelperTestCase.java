/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import java.util.List;
import java.util.Map;

import org.mule.MuleException;
import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.MessageFactory;
import org.mule.tck.AbstractMuleTestCase;

public class ExceptionHelperTestCase extends AbstractMuleTestCase
{

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
        assertEquals(3, l.size());

        Map info = ExceptionHelper.getExceptionInfo(testException);
        assertNotNull(info);
        assertEquals(1, info.size());
        assertNotNull(info.get("JavaDoc"));

    }

    private Exception getException()
    {

        return new MuleException(MessageFactory.createStaticMessage("foo"), new MuleException(
            MessageFactory.createStaticMessage("bar"), new Exception("blah")));
    }
}
