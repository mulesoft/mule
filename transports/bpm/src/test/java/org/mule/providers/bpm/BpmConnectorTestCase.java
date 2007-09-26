/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.bpm;

import org.mule.providers.bpm.test.TestBpms;
import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.provider.UMOConnector;

import com.mockobjects.dynamic.Mock;

/**
 * Generic connector tests.
 */
public class BpmConnectorTestCase extends AbstractConnectorTestCase
{

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractConnectorTestCase#getConnector()
     */
    public UMOConnector createConnector() throws Exception
    {
        ProcessConnector c = new ProcessConnector();
        c.setName("ProcessConnector");
        c.setBpms(new TestBpms());

        return c;
    }

    public void testMessageServiceSet() throws Exception
    {
        ProcessConnector c = (ProcessConnector) this.createConnector();
        // The BPMS must be set prior to initializing the connector.
        Mock bpms = new Mock(BPMS.class);
        bpms.expect("setMessageService", c);
        c.setBpms((BPMS) bpms.proxy());
        c.initialise();
        bpms.verify();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractConnectorTestCase#getValidMessage()
     */
    public Object getValidMessage() throws Exception
    {
        return "test";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractConnectorTestCase#getTestEndpointURI()
     */
    public String getTestEndpointURI()
    {
        return "bpm://dummyProcess?processId=1234";
    }

}
