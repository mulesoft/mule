/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer;

public enum SoapVersion
{
    SOAP_11("1.1"), SOAP_12("1.2");

    private String version;

    SoapVersion(String version)
    {
        this.version = version;
    }

    public String getVersion()
    {
        return version;
    }
}
