/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.components.script.refreshable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.util.IOUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

public abstract class AbstractRefreshableBeanTestCase extends AbstractServiceAndFlowTestCase
{

    public AbstractRefreshableBeanTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    protected static final int WAIT_TIME = 1000;
    
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
        MuleClient client = new MuleClient(muleContext);
        MuleMessage m = client.send(endpoint, payload, null);
        assertNotNull(m);
        assertEquals(payload + result, m.getPayloadAsString());
    }

}


