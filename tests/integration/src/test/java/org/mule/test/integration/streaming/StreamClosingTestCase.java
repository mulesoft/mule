/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.streaming;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.DefaultMessageAdapter;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamClosingTestCase extends FunctionalTestCase
{

    public void testCloseStreamOnException() throws MuleException, InterruptedException, IOException
    {
        MuleClient client = new MuleClient();

        String text = "\nblah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah "
                      + "\nblah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah "
                      + "\nblah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah "
                      + "\nblah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah\n\n";

        String basepath = muleContext.getConfiguration().getWorkingDirectory() + "/test-data";
        FileUtils.stringToFile(basepath + "/in/foo.txt", text);

        InputStream stream = new FileInputStream(new File(basepath + "/in/foo.txt"));
        DefaultMessageAdapter adapter = new DefaultMessageAdapter(stream);

        client.dispatch("tcpEndpoint", new DefaultMuleMessage(adapter));
        Thread.sleep(2000);
        try
        {
            stream.read();
            fail("STREAM SHOULD HAVE BEEN CLOSED");
        }
        catch (Exception e2)
        {
            // expected
        }

    }

    // @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/streaming/stream-closing.xml";
    }

}
