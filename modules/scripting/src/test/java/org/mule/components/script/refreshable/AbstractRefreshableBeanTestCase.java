/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.components.script.refreshable;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;
import org.mule.util.IOUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

public abstract class AbstractRefreshableBeanTestCase extends FunctionalTestCase
{

    protected static final int WAIT_TIME = 1000;
    
    protected void writeScript(String src, String path) throws IOException, InterruptedException
    {
        for (int i=0; i<1; i++)
        {
            FileWriter scriptFile = new FileWriter(path, false);
            scriptFile.write(src);
            scriptFile.flush();
            scriptFile.close();
        }
    }

    protected String nameToPath(String name)
    {
        URL url = IOUtils.getResourceAsUrl(name, getClass());
        String path = url.getFile();
        logger.info(url + " -> " + path);
        return path;
    }

    // this is a bit of a messy hack.  if it fails check you don't have more than one copy
    // of the files on your classpath
    protected void runScriptTest(String script, String name, String endpoint, String payload, String result) throws Exception
    {
        // we overwrite the existing resource on the classpath...
        writeScript(script, nameToPath(name));
        Thread.sleep(WAIT_TIME); // wait for bean to refresh
        MuleClient client = new MuleClient();
        UMOMessage m = client.send(endpoint, payload, null);
        assertNotNull(m);
        assertEquals(payload + result, m.getPayloadAsString());
    }

}


