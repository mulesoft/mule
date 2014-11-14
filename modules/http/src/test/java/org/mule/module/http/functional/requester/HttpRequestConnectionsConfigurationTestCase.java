/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.http.internal.request.DefaultHttpRequesterConfig;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class HttpRequestConnectionsConfigurationTestCase extends AbstractMuleTestCase
{

    @Test(expected=InitialisationException.class)
    public void invalidMaxConnections() throws InitialisationException
    {
        DefaultHttpRequesterConfig httpRequesterConfig = new DefaultHttpRequesterConfig();
        httpRequesterConfig.setMaxConnections(-2);
        httpRequesterConfig.initialise();
    }

    @Test(expected=InitialisationException.class)
    public void invalidMaxConnections0() throws InitialisationException
    {
        DefaultHttpRequesterConfig httpRequesterConfig = new DefaultHttpRequesterConfig();
        httpRequesterConfig.setMaxConnections(0);
        httpRequesterConfig.initialise();
    }

    @Test(expected=InitialisationException.class)
    public void nonPersistentConnectionsCanNotHaveIdleTimeout() throws InitialisationException
    {
        DefaultHttpRequesterConfig httpRequesterConfig = new DefaultHttpRequesterConfig();
        httpRequesterConfig.setUsePersistentConnections(false);
        httpRequesterConfig.setConnectionIdleTimeout(100);
        httpRequesterConfig.initialise();
    }

}
