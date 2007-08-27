/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.multicast;

import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.providers.AbstractConnector;
import org.mule.tck.providers.AbstractMessageReceiverTestCase;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOMessageReceiver;

import com.mockobjects.dynamic.Mock;

public class MulticastMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{

    public UMOMessageReceiver getMessageReceiver() throws Exception
    {
        Mock mockComponent = new Mock(UMOComponent.class);
        Mock mockDescriptor = new Mock(UMODescriptor.class);
        mockComponent.expectAndReturn("getDescriptor", mockDescriptor.proxy());
        mockDescriptor.expectAndReturn("getResponseTransformer", null);

        return new MulticastMessageReceiver((AbstractConnector)endpoint.getConnector(),
            (UMOComponent)mockComponent.proxy(), endpoint);
    }

    public UMOEndpoint getEndpoint() throws Exception
    {
        return new MuleEndpoint("multicast://228.2.3.4:10100", true);
    }

}
