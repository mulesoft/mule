/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.file;

import org.mule.extras.client.MuleClient;
import org.mule.umo.UMOMessage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * We are careful here to access the file sstem in a generic way.  This means setting directories
 * dynamically.
 */
public class FileFunctionalTestCase extends AbstractFileFunctionalTestCase
{

    public void testSend() throws Exception
    {
        File target = File.createTempFile("mule-file-test-", ".txt");
        target.deleteOnExit();

        FileConnector connector =
                (FileConnector) managementContext.getRegistry().lookupConnector("sendConnector");
        connector.setWriteToDirectory(target.getParent());
        logger.debug("Directory is " + connector.getWriteToDirectory());
        Map props = new HashMap();
        props.put(TARGET_FILE, target.getName());
        logger.debug("File is " + props.get(TARGET_FILE));

        MuleClient client = new MuleClient();
        client.dispatch("send", TEST_MESSAGE, props);
        waitForFileSystem();

        String result = new BufferedReader(new FileReader(target)).readLine();
        assertEquals(TEST_MESSAGE, result);
    }

    public void testDirectRequest() throws Exception
    {
        File target = initForRequest();
        MuleClient client = new MuleClient();
        String url = fileToUrl(target) + "?connector=receiveConnector";
        logger.debug(url);
        UMOMessage message = client.request(url, 100000);
        checkReceivedMessage(message);
    }

}
