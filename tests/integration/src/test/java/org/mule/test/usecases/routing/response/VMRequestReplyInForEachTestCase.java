/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.routing.response;

public class VMRequestReplyInForEachTestCase extends RequestReplyInForEachTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/usecases/routing/response/vm-request-reply-in-for-each.xml";
    }

}
