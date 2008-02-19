/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring;

import org.mule.module.client.MuleClient;
import org.mule.api.MuleException;

/**
 * MULE-2999 - This fails because we do not register components with the spring registry.
 */
public class ComponentInterceptorTestCase extends AbstractInterceptorTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/component-interceptor-test.xml";
    }

    public void testInterceptor() throws MuleException, InterruptedException
    {
        MuleClient client = new MuleClient();
        client.send("vm://in", MESSAGE, null);
        assertMessageIntercepted();
    }

}