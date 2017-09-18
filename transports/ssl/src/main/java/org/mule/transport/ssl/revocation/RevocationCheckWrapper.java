/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl.revocation;

import org.mule.api.security.tls.RevocationCheck;

/**
 * POJO for parsing the wrapper element for revocation checks in the XML.
 *
 * @since 3.9
 */
public class RevocationCheckWrapper
{
    private RevocationCheck config;

    public RevocationCheck getConfig()
    {
        return config;
    }

    public void setConfig(RevocationCheck config)
    {
        this.config = config;
    }
}
