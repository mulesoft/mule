/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

public class CxfConstants 
{
    public static final String DATA_BINDING = "databinding";
    public static final String BINDING_ID = "bindingId";
    public static final String FRONTEND = "frontend";
    public static final String JAX_WS_FRONTEND = "jaxws";
    public static final String SIMPLE_FRONTEND = "simple";
    public static final String WSDL_LOCATION = "wsdlLocation";
    public static final String NAMESPACE = "namespace";
    public static final String SERVICE_NAME = "serviceName";
    public static final String CLIENT_CLASS = "clientClass";
    public static final String SERVICE_INTERFACE = "serviceInterface";
    public static final String CLIENT_PORT = "wsdlPort";
    public static final String OPERATION = "operation";
    public static final String PROXY = "proxy";
    public static final String SERVICE_CLASS = "serviceClass";
    public static final String FEATURES = "features";
    public static final String IN_INTERCEPTORS = "inInterceptors";
    public static final String IN_FAULT_INTERCEPTORS = "inFaultInterceptors";
    public static final String OUT_INTERCEPTORS = "outInterceptors";
    public static final String OUT_FAULT_INTERCEPTORS = "outFaultInterceptors";
    public static final String MTOM_ENABLED = "mtomEnabled";
    public static final String MULE_MESSAGE = "mule.message";
    
    public static final String APPLY_FILTERS_TO_PROTOCOL = "applyFiltersToProtocol";
    public static final String APPLY_TRANSFORMERS_TO_PROTOCOL = "applyTransformersToProtocol";
    public static final String APPLY_SECURITY_TO_PROTOCOL = "applySecurityToProtocol";
    public static final String PROTOCOL_CONNECTOR = "protocolConnector";
    public static final String ATTACHMENTS = "cxf_attachments";
    public static final String INBOUND_SERVICE= "cxf_service";
    public static final String INBOUND_OPERATION= "cxf_operation";
}
