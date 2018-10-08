/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import org.mule.transport.AbstractInvalidKeystoreTestCase;

public class HttpsInvalidKeystoreTestCase extends AbstractInvalidKeystoreTestCase
{

    public HttpsInvalidKeystoreTestCase()
    {
        super();
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "https-invalid-keystore-config.xml";
    }
}


