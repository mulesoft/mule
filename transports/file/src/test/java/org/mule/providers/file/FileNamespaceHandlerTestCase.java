/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.file;

import org.mule.tck.FunctionalTestCase;

import junit.framework.Assert;

public class FileNamespaceHandlerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "file-namespace-config.xml";
    }

    public void testConfig() throws Exception
    {
        FileConnector c = (FileConnector)managementContext.getRegistry().lookupConnector("fileConnector");
        assertNotNull(c);
        
        Assert.assertEquals(1234, c.getFileAge());
        Assert.assertEquals("abc", c.getMoveToDirectory());
        Assert.assertEquals("bcd", c.getMoveToPattern());
        Assert.assertEquals("cde", c.getOutputPattern());
        Assert.assertEquals(2345, c.getPollingFrequency());
        Assert.assertEquals("efg", c.getWriteToDirectory());
        Assert.assertEquals(false, c.isAutoDelete());
        Assert.assertEquals(true, c.isOutputAppend());
        Assert.assertEquals(true, c.isSerialiseObjects());

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

}