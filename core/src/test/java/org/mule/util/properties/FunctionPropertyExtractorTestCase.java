/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.properties;

import org.mule.impl.MuleMessage;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOMessage;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.Date;

public class FunctionPropertyExtractorTestCase extends AbstractMuleTestCase
{
    public void testFunctions() throws Exception
    {
        UMOMessage message = new MuleMessage("test");
        FunctionPropertyExtractor extractor = new FunctionPropertyExtractor();
        Object o = extractor.getProperty("uuid", message);
        assertNotNull(o);
        o = extractor.getProperty("now", message);
        assertNotNull(o);
        assertTrue(o instanceof Timestamp);

        o = extractor.getProperty("date", message);
        assertNotNull(o);
        assertTrue(o instanceof Date);

        o = extractor.getProperty("hostname", message);
        assertNotNull(o);
        assertEquals(InetAddress.getLocalHost().getHostName(), o);

        o = extractor.getProperty("ip", message);
        assertNotNull(o);
        assertEquals(InetAddress.getLocalHost().getHostAddress(), o);

        try
        {
            o = extractor.getProperty("bork", message);
            fail("bork is not a valid function");
        }
        catch (Exception e)
        {
            //expected
        }
    }

    public void testFunctionsFromExtractorManager() throws Exception
    {
        PropertyExtractorManager.setDefaultExtractor(FunctionPropertyExtractor.NAME);
        
        UMOMessage message = new MuleMessage("test");
        Object o = PropertyExtractorManager.processExpression("uuid", message);
        assertNotNull(o);
        o = PropertyExtractorManager.processExpression("now", message);
        assertNotNull(o);
        assertTrue(o instanceof Timestamp);

        o = PropertyExtractorManager.processExpression("date", message);
        assertNotNull(o);
        assertTrue(o instanceof Date);

        o = PropertyExtractorManager.processExpression("hostname", message);
        assertNotNull(o);
        assertEquals(InetAddress.getLocalHost().getHostName(), o);

        o = PropertyExtractorManager.processExpression("ip", message);
        assertNotNull(o);
        assertEquals(InetAddress.getLocalHost().getHostAddress(), o);

        try
        {
            o = PropertyExtractorManager.processExpression("bork", message);
            fail("bork is not a valid function");
        }
        catch (Exception e)
        {
            //expected
        }
    }
}
