/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Before;
import org.junit.Test;


public class DefaultHttpRequesterTestCase extends AbstractMuleContextTestCase
{
    private static final String TEST_HOST = "TEST_HOST";
    private static final String TEST_PORT = "TEST_PORT";

    private DefaultHttpRequester requester = new DefaultHttpRequester();
    private DefaultHttpRequesterConfig config = new DefaultHttpRequesterConfig();

    @Before
    public void setup()
    {
        requester.setMuleContext(muleContext);
        config.setMuleContext(muleContext);
        requester.setConfig(config);
        requester.setPath("/");
    }

    @Test
    public void initializesWithHostAndPortInRequesterConfig() throws InitialisationException
    {
        config.setHost(TEST_HOST);
        config.setPort(TEST_PORT);
        requester.initialise();
        assertThat(requester.getHost(), equalTo(TEST_HOST));
        assertThat(requester.getPort(), equalTo(TEST_PORT));
    }

    @Test
    public void initializesWithHostAndPortInRequester() throws InitialisationException
    {
        requester.setHost(TEST_HOST);
        requester.setPort(TEST_PORT);
        requester.initialise();
        assertThat(requester.getHost(), equalTo(TEST_HOST));
        assertThat(requester.getPort(), equalTo(TEST_PORT));
    }

    @Test(expected = InitialisationException.class)
    public void failsToInitialiseWithoutHost() throws InitialisationException
    {
        config.setHost(null);
        config.setPort(TEST_PORT);
        requester.initialise();
    }

    @Test(expected = InitialisationException.class)
    public void failsToInitializeWithoutPort() throws InitialisationException
    {
        config.setHost(TEST_HOST);
        config.setPort(null);
        requester.initialise();
    }
}
