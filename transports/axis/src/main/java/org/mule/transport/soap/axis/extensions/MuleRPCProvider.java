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
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.rpc.holders.Holder;

import org.apache.axis.AxisFault;
import org.apache.axis.Constants;
import org.apache.axis.MessageContext;
import org.apache.axis.constants.Style;
import org.apache.axis.description.OperationDesc;
import org.apache.axis.description.ParameterDesc;
import org.apache.axis.description.ServiceDesc;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.message.RPCElement;
import org.apache.axis.message.RPCHeaderParam;
import org.apache.axis.message.RPCParam;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.providers.java.RPCProvider;
import org.apache.axis.soap.SOAPConstants;
import org.apache.axis.utils.JavaUtils;

/**
 * <code>MuleProvider</code> is an Axis service endpoint that builds services from
 * Mule managed components.
 */
public class MuleRPCProvider extends RPCProvider
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -4987111047650933518L;

    private AxisConnector connector;

    public MuleRPCProvider(AxisConnector connector)
    {
        this.connector = connector;
    }

    protected Object makeNewServiceObject(MessageContext messageContext, String s) throws Exception
    {
        String transUrl = (String)messageContext.getProperty("transport.url");
        int i = transUrl.indexOf("?");
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

    public void invoke(MessageContext msgContext) throws AxisFault
    {
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

    protected RPCElement createResponseBody(RPCElement body,
                                            MessageContext msgContext,
                                            OperationDesc operation,
                                            ServiceDesc serviceDesc,
                                            Object objRes,
                                            SOAPEnvelope resEnv,
                                            ArrayList outs) throws Exception
    {
        String methodName = operation.getName();

        /* Now put the result in the result SOAPEnvelope */
        RPCElement resBody = new RPCElement(methodName + "Response");
        resBody.setPrefix(body.getPrefix());
        resBody.setNamespaceURI(body.getNamespaceURI());
        resBody.setEncodingStyle(msgContext.getEncodingStyle());
        try
        {
            // Return first
            if (operation.getMethod().getReturnType() != Void.TYPE)
            {
                QName returnQName = operation.getReturnQName();
                if (returnQName == null)
                {
                    String nsp = body.getNamespaceURI();
                    if (nsp == null || nsp.length() == 0)
                    {
                        nsp = serviceDesc.getDefaultNamespace();
                    }
                    returnQName = new QName(msgContext.isEncoded() ? "" : nsp, methodName + "Return");
                }

                RPCParam param = new RPCParam(returnQName, objRes);
                param.setParamDesc(operation.getReturnParamDesc());

                if (!operation.isReturnHeader())
                {
                    // For SOAP 1.2 rpc style, add a result
                    if (msgContext.getSOAPConstants() == SOAPConstants.SOAP12_CONSTANTS
                        && (serviceDesc.getStyle().equals(Style.RPC)))
                    {
                        RPCParam resultParam = new RPCParam(Constants.QNAME_RPC_RESULT, returnQName);
                        resultParam.setXSITypeGeneration(Boolean.FALSE);
                        resBody.addParam(resultParam);
                    }
                    resBody.addParam(param);
                }
                else
                {
                    resEnv.addHeader(new RPCHeaderParam(param));
                }

            }

            // Then any other out params
            if (!outs.isEmpty())
            {
                for (Iterator i = outs.iterator(); i.hasNext();)
                {
                    // We know this has a holder, so just unwrap the value
                    RPCParam param = (RPCParam)i.next();
                    Holder holder = (Holder)param.getObjectValue();
                    Object value = JavaUtils.getHolderValue(holder);
                    ParameterDesc paramDesc = param.getParamDesc();

                    param.setObjectValue(value);
                    if (paramDesc != null && paramDesc.isOutHeader())
                    {
                        resEnv.addHeader(new RPCHeaderParam(param));
                    }
                    else
                    {
                        resBody.addParam(param);
                    }
                }
            }
        }
        catch (Exception e)
        {
            throw e;
        }
        return resBody;
    }
}
