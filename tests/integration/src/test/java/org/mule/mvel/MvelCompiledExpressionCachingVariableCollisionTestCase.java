/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.mvel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;

public class MvelCompiledExpressionCachingVariableCollisionTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "mvel-compiled-expression-caching-variable-collision.xml";
    }

    @Test
    public void melCacheDoesMisunderstandsBeanPropertiesAsVariables() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://fooInput", NullPayload.getInstance(), null);
        
        assertThat(response.getPayload(), instanceOf(NullPayload.class));
        
        response = client.send("vm://fooInput", NullPayload.getInstance(), null);

        assertThat(response.getPayload(), instanceOf(NullPayload.class));
    }
}
