/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.routing.response;

public class RequestReplyWithMultipleConnectorsInFlowTestCase extends RequestReplyInFlowTestCase
{

    private static final String CONNECTOR_REF_ATTRIBUTE = "?connector=default";

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/usecases/routing/response/request-reply-with-multiple-connectors-flow.xml";
    }

    @Override
    protected String getDestinationUrl()
    {
        return super.getDestinationUrl() + CONNECTOR_REF_ATTRIBUTE;
    }

    @Override
    protected String getDispatchUrl()
    {
        return super.getDispatchUrl() + CONNECTOR_REF_ATTRIBUTE;
    }

}
