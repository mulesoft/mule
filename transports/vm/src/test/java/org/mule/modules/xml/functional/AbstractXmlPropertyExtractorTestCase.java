/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.xml.functional;

import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.extras.client.MuleClient;

public abstract class AbstractXmlPropertyExtractorTestCase extends FunctionalTestCase
{
    public static long WAIT_PERIOD = 3000L;

    protected abstract Object getMatchMessage();

    protected abstract Object getErrorMessage();

    public void testMatch() throws UMOException
    {
        MuleClient client = new MuleClient();
        client.dispatch("in", getMatchMessage(), null);
        UMOMessage message = client.receive("vm://match?connector=queue", WAIT_PERIOD);
        assertNotNull(message);
    }

    public void testError() throws UMOException
    {
        MuleClient client = new MuleClient();
        client.dispatch("in", getErrorMessage(), null);
        UMOMessage message = client.receive("vm://error?connector=queue", WAIT_PERIOD);
        assertNotNull(message);
    }
    
}
