/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.file;

import java.io.File;
import java.io.FileInputStream;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import org.apache.commons.io.IOUtils;

/**
 * 
 * @author <a href="mailto:stephen.fenech@symphonysoft.com">Stephen Fenech</a>
 *
 */
public class FileAppendConnectorTestCase extends FunctionalTestCase{
    
    public void testBasic() throws Exception
    {
        MuleClient client=new MuleClient();
        (new File("myout/out.txt")).delete();
        client.send("vm://fileappend", "Hello1", null);
        client.send("vm://fileappend", "Hello2", null);
        assertEquals("Hello1Hello2",IOUtils.toString(new FileInputStream("myout/out.txt")));
    }

    protected String getConfigResources() {
        return "org/mule/test/integration/providers/file/mule-fileappend-endpoint-config.xml";
    }
}
