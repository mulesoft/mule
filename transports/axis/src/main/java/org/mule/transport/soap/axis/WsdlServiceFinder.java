/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;


/**
 * <code>WsdlServiceFinder</code> finds a the connector service to use by checking
 * the classpath for jars required for each of the soap connector implementations
 */
public class WsdlServiceFinder extends SoapServiceFinder
{
    private static final String PROTOCOL_PREFIX = "wsdl-";

    protected String getProtocolFromKey(String key)
    {
        return PROTOCOL_PREFIX + super.getProtocolFromKey(key);
    }

}
