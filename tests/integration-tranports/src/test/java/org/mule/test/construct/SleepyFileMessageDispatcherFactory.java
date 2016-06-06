/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.api.transport.MessageDispatcher;
import org.mule.compatibility.transport.file.FileMessageDispatcherFactory;
import org.mule.runtime.core.api.MuleException;

public class SleepyFileMessageDispatcherFactory extends FileMessageDispatcherFactory
{    
    @Override
    public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException
    {
        return new SleepyFileMessageDispatcher(endpoint);
    }

}


