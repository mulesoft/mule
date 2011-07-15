/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.resolvers;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public abstract class AbstractEntryPointResolverTestCase extends AbstractServiceAndFlowTestCase
{
    public AbstractEntryPointResolverTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    protected void doTest(String path, Object payload, String result) throws Exception
    {
        doTest(path, payload, result, null);
    }

    protected void doTest(String path, Object payload, String result, Map properties) throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("vm://" + path, payload, properties);
        assertEquals(result, response.getPayloadAsString());
    }
}
