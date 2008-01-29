/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.bpm.tests;

import org.mule.tck.FunctionalTestCase;
import org.mule.transport.bpm.BPMS;
import org.mule.transport.bpm.ProcessConnector;

public abstract class AbstractBpmTestCase extends FunctionalTestCase 
{
    protected ProcessConnector connector;
    protected BPMS bpms;

    protected void doSetUp() throws Exception {
        connector =
            (ProcessConnector) muleContext.getRegistry().lookupConnector("bpmConnector");
        bpms = connector.getBpms();
        super.doSetUp();
    }
}


