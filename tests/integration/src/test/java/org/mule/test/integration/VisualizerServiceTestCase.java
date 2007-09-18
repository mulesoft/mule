/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.tck.FunctionalTestCase;

import java.io.File;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

public class VisualizerServiceTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/integration/visualizer-service.xml";
    }

    public void testVisualizer() throws Exception
    {
        MuleMessage m = new MuleMessage("ooh, I love pictures!");
        FileDataSource ds = new FileDataSource(new File("tests/integration/src/test/resources/" + getConfigResources()).getAbsoluteFile());
        m.addAttachment("visualizer-service", new DataHandler(ds));
        MuleClient client = new MuleClient();
        client.send("email-out", m);
        Thread.sleep(300000);
    }
}
