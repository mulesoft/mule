/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.IsNot;
import org.junit.Test;
import org.mockito.internal.matchers.Not;
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.DynamicPortTestCase;
import org.mule.transport.NullPayload;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class HttpExceptionStrategyTestCase extends DynamicPortTestCase
{

    public static final String MESSAGE = "some message";
    public static final int TIMEOUT = 3000;
    private MuleClient muleClient;


    @Override
    protected String getConfigResources()
    {
        return "http-exception-strategy-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        muleClient = new MuleClient(muleContext);
    }

    @Test
    public void testInExceptionDoRollbackHttpSync() throws Exception
    {
        muleClient = new MuleClient(muleContext);
        MuleMessage response = muleClient.send(String.format("http://localhost:%s/flowWithoutExceptionStrategySync", getPorts().get(0)), MESSAGE, null, TIMEOUT);
        assertThat(response, notNullValue());
        assertThat(response.getPayload(), IsNot.not(IsInstanceOf.instanceOf(NullPayload.class)));
        assertThat(response.getPayloadAsString(), IsNot.not(MESSAGE));
        assertThat(response.getExceptionPayload(), notNullValue()); //to be fixed
        assertThat(response.getExceptionPayload(), instanceOf(ExceptionPayload.class)); //to be review/fixed
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }
}

