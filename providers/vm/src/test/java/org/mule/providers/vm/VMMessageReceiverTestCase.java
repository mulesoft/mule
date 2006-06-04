/*
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *
 */

package org.mule.providers.vm;

import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.tck.providers.AbstractMessageReceiverTestCase;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOMessageReceiver;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class VMMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{

    VMMessageReceiver receiver;

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        receiver = new VMMessageReceiver(endpoint.getConnector(), component, endpoint);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractMessageReceiverTestCase#getMessageReceiver()
     */
    public UMOMessageReceiver getMessageReceiver()
    {
        return receiver;
    }

    public UMOEndpoint getEndpoint() throws Exception
    {
        endpoint = new MuleEndpoint("vm://test", true);
        return endpoint;
    }
}
