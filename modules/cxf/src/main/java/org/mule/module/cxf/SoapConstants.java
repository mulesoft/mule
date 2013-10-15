/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

/**
 * Common SOAP constants
 */
public interface SoapConstants
{

    public static final String WSDL_PROPERTY = "wsdl";
    public static final String LIST_PROPERTY = "list";
    public static final String SOAP_ENDPOINT_PREFIX = "soap:";
    public static final String METHOD_NAMESPACE_PROPERTY = "methodNamespace";
    // i don't udnerstand what is going on here, but these are two different properties and
    // axis fails (in partiuclar, look at AxisJmsEndpointFormat test) if they are unified.
    public static final String SOAP_ACTION_PROPERTY = "soapAction";
    public static final String SOAP_ACTION_PROPERTY_CAPS = "SOAPAction";
    public static final String WSDL_URL_PROPERTY = "WSDL_URL";
    public static final String SOAP_NAMESPACE_PROPERTY = "SOAP_NAMESPACE_PROPERTY";
    public static final String SERVICE_INTERFACES = "serviceInterfaces";
    
}
