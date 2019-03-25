/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

public class CxfConstants 
{
    public static final String JAX_WS_FRONTEND = "jaxws";
    public static final String SIMPLE_FRONTEND = "simple";
    public static final String OPERATION = "operation";
    public static final String MTOM_ENABLED = "mtomEnabled";
    
    public static final String CXF_OUTBOUND_MESSAGE_PROCESSOR = "cxf_outbound_message_processor";
    public static final String MULE_EVENT = "mule.event";
    public static final String NON_BLOCKING_RESPONSE = "mule.nonBlockingResponse";

    public static final String ATTACHMENTS = "cxf_attachments";
    public static final String INBOUND_SERVICE= "cxf_service";
    public static final String INBOUND_OPERATION= "cxf_operation";
    public static final String ENABLE_MULE_SOAP_HEADERS = "enableMuleSoapHeaders";

    public static final String PAYLOAD = "payload";
    public static final String PAYLOAD_BODY = "body";
    public static final String PAYLOAD_ENVELOPE = "envelope";

    public static final String PAYLOAD_TO_ARGUMENTS = "payloadToArguments";
    public static final String PAYLOAD_TO_ARGUMENTS_NULL_PAYLOAD_AS_PARAMETER = "nullPayloadAsParameter";
    public static final String PAYLOAD_TO_ARGUMENTS_NULL_PAYLOAD_AS_VOID = "nullPayloadAsVoid";
    public static final String PAYLOAD_TO_ARGUMENTS_BY_DEFAULT = PAYLOAD_TO_ARGUMENTS_NULL_PAYLOAD_AS_PARAMETER;
    public static final String DEFAULT_CXF_CONFIGURATION = "_cxfConfiguration";
    
    public static final String DATA_BINDING = "databinding";
    public static final String FEATURES = "features";
    public static final String IN_INTERCEPTORS = "inInterceptors";
    public static final String IN_FAULT_INTERCEPTORS = "inFaultInterceptors";
    public static final String OUT_INTERCEPTORS = "outInterceptors";
    public static final String OUT_FAULT_INTERCEPTORS = "outFaultInterceptors";
    
    public static final String UNWRAP_MULE_EXCEPTIONS = "unwrapMuleExceptions";

    public static final String WSDL_LOCATION = "WSDL_LOCATION";
    
    public static final String NON_BLOCKING_LATCH = "NON_BLOCKING_LATCH";

}
