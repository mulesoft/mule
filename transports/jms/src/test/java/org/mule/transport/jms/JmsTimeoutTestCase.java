/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import static org.hamcrest.core.IsInstanceOf.instanceOf;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.api.routing.ResponseTimeoutException;
import org.mule.api.transport.DispatchException;
import org.mule.tck.junit4.FunctionalTestCase;

public class JmsTimeoutTestCase extends FunctionalTestCase
{
    private static final String PAYLOAD = "test";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    protected String getConfigFile()
    {
        return "jms-timeout-config.xml";
    }

    @Test
    public void validateTimeout() throws Exception
    {
        expectedException.expect(DispatchException.class);
        expectedException.expectCause(instanceOf(ResponseTimeoutException.class));
        runFlow("timeoutFlow", PAYLOAD);
    }
}
