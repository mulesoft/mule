/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

}
