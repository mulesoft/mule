/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
