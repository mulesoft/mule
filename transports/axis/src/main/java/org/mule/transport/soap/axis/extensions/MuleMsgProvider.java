/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis.extensions;

import org.mule.RequestContext;
import org.mule.api.service.Service;
import org.mule.transport.soap.axis.AxisConnector;
import org.mule.transport.soap.axis.AxisMessageReceiver;
import org.mule.transport.soap.axis.AxisServiceProxy;

import java.lang.reflect.Proxy;

import org.apache.axis.AxisFault;
import org.apache.axis.Constants;
import org.apache.axis.MessageContext;
import org.apache.axis.description.OperationDesc;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.providers.java.MsgProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleMsgProvider</code> is an Axis service endpoint that builds services
 * from Mule managed components.
 */
public class MuleMsgProvider extends MsgProvider
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -4399291846942449361L;

    private AxisConnector connector;

    private static Log logger = LogFactory.getLog(MuleMsgProvider.class);

    private String METHOD_BODYARRAY = "soapbodyelement";
    private String METHOD_ELEMENTARRAY = "element";
    private String METHOD_DOCUMENT = "document";

    public MuleMsgProvider(AxisConnector connector)
    {
        this.connector = connector;
    }

    protected Object makeNewServiceObject(MessageContext messageContext, String s) throws Exception
    {
        String transUrl = (String)messageContext.getProperty("transport.url");
        int i = transUrl.indexOf('?');
        if (i > -1)
        {
            transUrl = transUrl.substring(0, i);
        }
        AxisMessageReceiver receiver = (AxisMessageReceiver)connector.lookupReceiver(transUrl);
        if (receiver == null)
        {
            receiver = (AxisMessageReceiver)connector.lookupReceiver(messageContext.getTargetService());
        }
        if (receiver == null)
        {
            throw new AxisFault("Could not find Mule registered service: " + s);
        }
        
        if (!(receiver.getFlowConstruct() instanceof Service))
        {
            throw new IllegalArgumentException(
                "Only the Service flow constuct is supported by the axis transport");
        }
        Service service = (Service) receiver.getFlowConstruct();
        
        Class[] classes = AxisServiceProxy.getInterfacesForComponent(service);
        return AxisServiceProxy.createProxy(receiver, true, classes);
    }

    protected Class getServiceClass(String s, SOAPService soapService, MessageContext messageContext)
        throws AxisFault
    {
        Service component = connector.getMuleContext().getRegistry().lookupService(soapService.getName());
        try
        {
            Class[] classes = AxisServiceProxy.getInterfacesForComponent(component);
            return Proxy.getProxyClass(Thread.currentThread().getContextClassLoader(), classes);
        }
        catch (Exception e)
        {
            throw new AxisFault("Failed to implementation class for component: " + e.getMessage(), e);
        }
    }

    /**
     * @param msgContext
     * @deprecated I dont think this is necessary, but leaving it here for a while
     */
    protected void setOperationStyle(MessageContext msgContext)
    {
        /*
         * Axis requires that the OperationDesc.operationStyle be set to match the
         * method signature This does not appear to be an automated process so
         * determine from the 4 allowed forms public Element [] method(Element []
         * bodies); public SOAPBodyElement [] method (SOAPBodyElement [] bodies);
         * public Document method(Document body); public void method(SOAPEnvelope
         * req, SOAPEnvelope resp);
         */
        int methodType = msgContext.getOperation().getMessageOperationStyle();
        if (methodType > -1)
        {
            // Already set, nothing more to do
            return;
        }
        OperationDesc operation = msgContext.getOperation();
        String methodSignature = operation.getMethod().toString().toLowerCase();
        if (methodSignature.indexOf(METHOD_BODYARRAY) != -1)
        {
            methodType = OperationDesc.MSG_METHOD_BODYARRAY;
        }
        else if (methodSignature.indexOf(METHOD_ELEMENTARRAY) != -1)
        {
            methodType = OperationDesc.MSG_METHOD_ELEMENTARRAY;
        }
        else if (methodSignature.indexOf(METHOD_DOCUMENT) != -1)
        {
            methodType = OperationDesc.MSG_METHOD_DOCUMENT;
        }
        else
        {
            methodType = OperationDesc.MSG_METHOD_SOAPENVELOPE;
        }
        operation.setMessageOperationStyle(methodType);
        logger.debug("Now Invoking service (Method Format) " + operation.getMethod().toString());
        logger.debug("Now Invoking service (MethodType) "
                     + String.valueOf(operation.getMessageOperationStyle()));
    }

    public void invoke(MessageContext msgContext) throws AxisFault
    {
        // Make sure that the method style is correctly set (This does not appear to
        // be handled by default)
        // setOperationStyle(msgContext);
        super.invoke(msgContext);
        if (RequestContext.getExceptionPayload() != null)
        {
            Throwable t = RequestContext.getExceptionPayload().getException();
            if (t instanceof Exception)
            {
                AxisFault fault = AxisFault.makeFault((Exception)t);
                if (t instanceof RuntimeException)
                {
                    fault.addFaultDetail(Constants.QNAME_FAULTDETAIL_RUNTIMEEXCEPTION, "true");
                }
                throw fault;
            }
            else
            {
                throw (Error)t;
            }
        }
    }

}
