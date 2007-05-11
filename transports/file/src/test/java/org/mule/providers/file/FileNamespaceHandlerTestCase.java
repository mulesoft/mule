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

import org.mule.providers.file.test.DummyFilenameParser;
import org.mule.tck.FunctionalTestCase;

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
        
        assertEquals(1234, c.getFileAge());
        assertEquals("abc", c.getMoveToDirectory());
        assertEquals("bcd", c.getMoveToPattern());
        assertEquals("cde", c.getOutputPattern());
        assertEquals(2345, c.getPollingFrequency());
        assertEquals("efg", c.getWriteToDirectory());
        assertEquals(false, c.isAutoDelete());
        assertEquals(true, c.isOutputAppend());
        assertEquals(true, c.isSerialiseObjects());
        FilenameParser parser = c.getFilenameParser();
        assertTrue(parser.getClass().getName(), c.getFilenameParser() instanceof DummyFilenameParser);

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    public void testSecondConnector() throws Exception
    {
        FileConnector c = (FileConnector)managementContext.getRegistry().lookupConnector("secondConnector");
        assertNotNull(c);

        FilenameParser parser = c.getFilenameParser();
        assertTrue(parser.getClass().getName(), c.getFilenameParser() instanceof SecondDummyFilenameParser);

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

}