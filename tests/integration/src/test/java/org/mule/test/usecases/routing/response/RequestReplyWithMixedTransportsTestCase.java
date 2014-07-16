/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.routing.response;

import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;

public class RequestReplyWithMixedTransportsTestCase extends RequestReplyInFlowTestCase
{

    @Rule
    public DynamicPort port = new DynamicPort("http.port");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/usecases/routing/response/request-reply-with-mixed-transports-flow.xml";
    }

}
