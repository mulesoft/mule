/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.components.script.refreshable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.util.IOUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

public abstract class AbstractRefreshableBeanTestCase extends AbstractServiceAndFlowTestCase
{
    protected static final int WAIT_TIME = 1000;

    public AbstractRefreshableBeanTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    protected void writeScript(String src, String path) throws IOException
    {
        FileWriter scriptFile = new FileWriter(path, false);
        scriptFile.write(src);
        scriptFile.flush();
        scriptFile.close();
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

        MuleClient client = muleContext.getClient();
        MuleMessage m = client.send(endpoint, payload, null);
        assertNotNull(m);
        assertEquals(payload + result, m.getPayloadAsString());
    }
}
