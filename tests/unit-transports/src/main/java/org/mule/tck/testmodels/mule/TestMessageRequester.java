/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.mule;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.transport.AbstractMessageRequester;

/**
 * TODO
 */
public class TestMessageRequester extends AbstractMessageRequester
{
    public TestMessageRequester(InboundEndpoint endpoint)
    {
        super(endpoint);
    }

    @Override
    protected MuleMessage doRequest(long timeout) throws Exception
    {
        return null;
    }
}
