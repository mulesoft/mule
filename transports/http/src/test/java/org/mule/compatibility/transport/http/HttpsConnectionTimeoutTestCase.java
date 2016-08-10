/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.compatibility.transport.http;

import org.junit.Ignore;

@Ignore("MULE-6926: flaky test")
public class HttpsConnectionTimeoutTestCase extends HttpConnectionTimeoutTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "https-connection-timeout-config.xml";
    }
}
