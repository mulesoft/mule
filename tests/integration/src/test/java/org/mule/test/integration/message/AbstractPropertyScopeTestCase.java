/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.message;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public abstract class AbstractPropertyScopeTestCase extends AbstractIntegrationTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Test
    public void testRequestResponse() throws Exception
    {
        MuleMessage result = flowRunner("foo").withPayload(TEST_PAYLOAD)
                                              .withInboundProperty("foo", "fooValue")
                                              .run()
                                              .getMessage();

        assertThat(result.getPayload(), is("test bar"));
        assertThat(result.getOutboundProperty("foo"), is("fooValue"));
    }

}
