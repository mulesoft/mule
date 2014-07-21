/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;

import org.junit.Test;

public class DynamicSubFlowTestCase extends FunctionalTestCase
{

    public static final String VM_IN = "vm://in";

    @Override
    protected String getConfigFile()
    {
        return "dynamic-subflow-test-config.xml";
    }

    @Test
    public void testCofiguration() throws Exception
    {
    	MuleClient client = muleContext.getClient();
        MuleMessage result = client.send(VM_IN, "", null);
        assertThat(result, is(notNullValue()));
        assertThat(result.getExceptionPayload(), is(nullValue()));
        assertThat(result.getPayload(), not(instanceOf(NullPayload.class)));
    }
}
