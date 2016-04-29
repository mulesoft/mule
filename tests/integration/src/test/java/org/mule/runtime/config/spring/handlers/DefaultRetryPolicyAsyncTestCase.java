/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.handlers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.retry.async.AsynchronousRetryTemplate;
import org.mule.runtime.core.retry.policies.SimpleRetryPolicyTemplate;

import org.junit.Test;

public class DefaultRetryPolicyAsyncTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/handlers/default-retry-policy-async.xml";
    }

    @Test
    public void testPolicyRegistration() throws Exception
    {
        Object obj = muleContext.getRegistry().lookupObject(OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE);
        assertThat(obj, not(nullValue()));
        assertThat(obj, instanceOf(AsynchronousRetryTemplate.class));
        assertThat(((SimpleRetryPolicyTemplate) ((AsynchronousRetryTemplate) obj).getDelegate()).getCount(), is(3));
    }

    @Test
    public void testConnectorPolicy() throws Exception
    {
        Connector c = muleContext.getRegistry().lookupConnector("testConnector");
        assertThat(c, not(nullValue()));

        RetryPolicyTemplate rpf = c.getRetryPolicyTemplate();
        assertThat(rpf, not(nullValue()));
        assertThat(rpf, instanceOf(AsynchronousRetryTemplate.class));
        assertThat(((SimpleRetryPolicyTemplate) ((AsynchronousRetryTemplate) rpf).getDelegate()).getCount(), is(3));
        
        assertThat(c.isConnected(), is(true));
        assertThat(c.isStarted(), is(true));
    }
}
