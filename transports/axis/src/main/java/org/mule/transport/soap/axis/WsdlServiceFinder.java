/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
