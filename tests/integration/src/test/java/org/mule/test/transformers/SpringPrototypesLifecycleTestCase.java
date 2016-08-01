/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.transformers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.test.transformers.GraphTransformerResolutionTestCase.A;
import org.mule.test.transformers.GraphTransformerResolutionTestCase.B;

import org.junit.Test;

public class SpringPrototypesLifecycleTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "spring-prototypes-lifecycle-config.xml";
    }

    @Test
    public void registersTransformerOnce() throws Exception
    {
        final MuleEvent muleEvent = flowRunner("testFlow").withPayload(new A(TEST_MESSAGE)).run();
        final MuleMessage response = muleEvent.getMessage();

        assertThat(response.getPayload(), is(instanceOf(B.class)));
    }

    @Test
    public void exceptionHandlerWithTransformerInEndpoint() throws Exception
    {
        final MuleEvent muleEvent = flowRunner("testExceptionHandlerWithTransformerInEndpointFlow").withPayload(new A(TEST_MESSAGE)).run();
        final MuleMessage response = muleEvent.getMessage();

        assertThat(response.getPayload(), is(instanceOf(B.class)));
    }
}
