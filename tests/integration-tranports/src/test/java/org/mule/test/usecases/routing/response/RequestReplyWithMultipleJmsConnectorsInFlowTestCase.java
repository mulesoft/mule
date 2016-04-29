/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.routing.response;

public class RequestReplyWithMultipleJmsConnectorsInFlowTestCase extends RequestReplyInFlowTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/usecases/routing/response/request-reply-with-multiple-jms-connectors-flow.xml";
    }
}
