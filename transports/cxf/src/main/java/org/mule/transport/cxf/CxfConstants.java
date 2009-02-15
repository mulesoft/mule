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

public interface CxfConstants 
{

    String DATA_BINDING = "databinding";
    String BINDING_ID = "bindingId";
    String FRONTEND = "frontend";
    String JAX_WS_FRONTEND = "jaxws";
    String SIMPLE_FRONTEND = "simple";
    String WSDL_LOCATION = "wsdlLocation";
    String NAMESPACE = "namespace";
    String SERVICE_NAME = "serviceName";
    String CLIENT_CLASS = "clientClass";
    String SERVICE_INTERFACE = "serviceInterface";
    String CLIENT_PORT = "wsdlPort";
    String OPERATION = "operation";
    String PROXY = "proxy";
    String SERVICE_CLASS = "serviceClass";
    String FEATURES = "features";
    String IN_INTERCEPTORS = "inInterceptors";
    String IN_FAULT_INTERCEPTORS = "inFaultInterceptors";
    String OUT_INTERCEPTORS = "outInterceptors";
    String OUT_FAULT_INTERCEPTORS = "outFaultInterceptors";
    String MTOM_ENABLED = "mtomEnabled";
    String MULE_MESSAGE = "mule.message";
    
    String APPLY_FILTERS_TO_PROTOCOL = "applyFiltersToProtocol";
    String APPLY_TRANSFORMERS_TO_PROTOCOL = "applyTransformersToProtocol";
    String APPLY_SECURITY_TO_PROTOCOL = "applySecurityToProtocol";
    String PROTOCOL_CONNECTOR = "protocolConnector";
    String ATTACHMENTS = "cxf_attachments";
    String INBOUND_SERVICE= "cxf_service";
    String INBOUND_OPERATION= "cxf_operation";
}
