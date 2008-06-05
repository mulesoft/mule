/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.test;

import org.mule.transport.jms.RedeliveryHandler;
import org.mule.transport.jms.RedeliveryHandlerFactory;

public class TestRedeliveryHandlerFactory implements RedeliveryHandlerFactory
{

    public RedeliveryHandler create()
    {
        return new TestRedeliveryHandler();
    }

}


