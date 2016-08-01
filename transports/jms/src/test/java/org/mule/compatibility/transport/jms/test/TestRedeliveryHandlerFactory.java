/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms.test;

import org.mule.compatibility.transport.jms.redelivery.RedeliveryHandler;
import org.mule.compatibility.transport.jms.redelivery.RedeliveryHandlerFactory;

public class TestRedeliveryHandlerFactory implements RedeliveryHandlerFactory
{

    public RedeliveryHandler create()
    {
        return new TestRedeliveryHandler();
    }

}


