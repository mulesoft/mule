/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.mule;

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.transport.AbstractMessageRequester;

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
