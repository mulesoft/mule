/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import org.mule.DefaultMuleMessage;
import org.mule.NonBlockingVoidMuleEvent;
import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.NonBlockingSupported;
import org.mule.api.config.MuleProperties;
import org.mule.api.processor.CloneableMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.DispatchException;
import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.cxf.i18n.CxfMessages;
import org.mule.module.cxf.security.WebServiceSecurityException;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.http.HttpConnector;
import org.mule.util.TemplateParser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientCallback;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.MethodDispatcher;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInEndingInterceptor;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.ws.addressing.WSAContextUtils;

/**
 * The CxfOutboundMessageProcessor performs outbound CXF processing, sending an event
 * through the CXF client, then on to the next MessageProcessor.
 */
public class CxfOutboundMessageProcessor extends AbstractInterceptingMessageProcessor implements CloneableMessageProcessor, NonBlockingSupported
{

    private static final String URI_REGEX = "cxf:\\[(.+?)\\]:(.+?)/\\[(.+?)\\]:(.+?)";
    Pattern URI_PATTERN = Pattern.compile(URI_REGEX);

    private final TemplateParser soapActionTemplateParser = TemplateParser.createMuleStyleParser();
    private CxfPayloadToArguments payloadToArguments = CxfPayloadToArguments.NULL_PAYLOAD_AS_PARAMETER;
    private Client client;
    private boolean proxy;
    private String operation;
    private BindingProvider clientProxy;
    private String decoupledEndpoint;
    private String mimeType;

    public CxfOutboundMessageProcessor(Client client)
    {
        this.client = client;
    }

    protected void cleanup()
    {
        // MULE-4899: cleans up client's request and response context to avoid a
        // memory leak.
        Map<String, Object> requestContext = client.getRequestContext();
        requestContext.clear();
        Map<String, Object> responseContext = client.getResponseContext();
        responseContext.clear();
    }

    protected Object[] getArgs(MuleEvent event) throws TransformerException
    {
        Object payload;

        payload = event.getMessage().getPayload();

        if (proxy)
        {
            return new Object[]{ event.getMessage() };
        }

        Object[] args = payloadToArguments.payloadToArrayOfArguments(payload);

        MuleMessage message = event.getMessage();
        Set<?> attachmentNames = message.getInboundAttachmentNames();
        if (attachmentNames != null && !attachmentNames.isEmpty())
        {
            List<DataHandler> attachments = new ArrayList<DataHandler>();
            for (Object attachmentName : attachmentNames)
            {
                attachments.add(message.getInboundAttachment((String)attachmentName));
            }
            List<Object> temp = new ArrayList<Object>(Arrays.asList(args));
            temp.add(attachments.toArray(new DataHandler[attachments.size()]));
            args = temp.toArray();
        }

        if (args.length == 0)
        {
            return null;
        }
        return args;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        try
        {
            MuleEvent res;
            if (!isClientProxyAvailable())
            {
                res = doSendWithClient(event);
            }
            else
            {
                res = doSendWithProxy(event);
            }
            return res;
        }
        catch (Exception e)
        {
            throw wrapException(event, e, false);
        }
        finally
        {
            cleanup();
        }
    }

    private MuleException wrapException(MuleEvent event, Throwable ex, boolean alwaysReturnMessagingException)
    {
        if (ex instanceof MessagingException)
        {
            return (MessagingException) ex;
        }
        if (ex instanceof Fault)
        {
            // Because of CXF API, MuleExceptions can be wrapped in a Fault, in that case we should return the mule exception
            Fault fault = (Fault) ex;
            if(fault.getCause() instanceof MuleException)
            {
                MuleException muleException = (MuleException) fault.getCause();
                return alwaysReturnMessagingException ? new MessagingException(event, muleException, this) : muleException;
            }
            return new DispatchException(MessageFactory.createStaticMessage(fault.getMessage()), event, this, fault);
        }
        return new DispatchException(MessageFactory.createStaticMessage(ExceptionHelper.getRootException(ex).getMessage()), event, this, ex);
    }

    private MessagingException wrapException(MuleEvent event, Throwable ex)
    {
        return (MessagingException) wrapException(event, ex, true);
    }

    /**
     * This method is public so it can be invoked from the MuleUniversalConduit.
     */
    @Override
    public MuleEvent processNext(MuleEvent event) throws MuleException
    {
        return super.processNext(event);
    }

    protected MuleEvent doSendWithProxy(MuleEvent event) throws Exception
    {
        Method method = getMethod(event);

        Map<String, Object> props = getInovcationProperties(event);

        Holder<MuleEvent> responseHolder = new Holder<MuleEvent>();
        props.put("holder", responseHolder);

        // Set custom soap action if set on the event or endpoint
        String soapAction = event.getMessage().getOutboundProperty(SoapConstants.SOAP_ACTION_PROPERTY);
        if (soapAction != null)
        {
            props.put(org.apache.cxf.binding.soap.SoapBindingConstants.SOAP_ACTION, soapAction);
        }

        clientProxy.getRequestContext().putAll(props);

        Object response;
        Object[] args = getArgs(event);
        try
        {
            response = method.invoke(clientProxy, args);
        }
        catch (InvocationTargetException e)
        {
            Throwable ex = e.getTargetException();

            if (ex != null && ex.getMessage().contains("Security"))
            {
                throw new WebServiceSecurityException(event, e, this);
            }
            else
            {
                throw e;
            }
        }

        Object[] objResponse = addHoldersToResponse(response, args);
        MuleEvent muleRes = responseHolder.value;

        // CXF blocks until the response value is aviailable and then proceeds with response processing on the waiting
        // thread.  When non-blocking is enabled the message owner will be a different thread and so this needs
        // reseting here.
        if (event.isAllowNonBlocking())
        {
            ((DefaultMuleMessage) muleRes.getMessage()).resetAccessControl();
        }

        return buildResponseMessage(event, muleRes, objResponse);
    }

    protected MuleEvent doSendWithClient(final MuleEvent event) throws Exception
    {
        BindingOperationInfo bop = getOperation(event);

        Map<String, Object> props = getInovcationProperties(event);

        // Holds the response from the transport
        final Holder<MuleEvent> responseHolder = new Holder<MuleEvent>();
        props.put("holder", responseHolder);

        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put(Client.REQUEST_CONTEXT, props);
        ctx.put(Client.RESPONSE_CONTEXT, props);

        // Set Custom Headers on the client
        Object[] arr = event.getMessage().getPropertyNames().toArray();
        String head;

        for (int i = 0; i < arr.length; i++)
        {
            head = (String)arr[i];
            if ((head != null) && (!head.startsWith("MULE")))
            {
                props.put((String)arr[i], event.getMessage().getProperty((String)arr[i]));
            }
        }
        
        ExchangeImpl exchange = new ExchangeImpl();
        // mule will close the stream so don't let cxf, otherwise cxf will close it too early
        exchange.put(StaxInEndingInterceptor.STAX_IN_NOCLOSE, Boolean.TRUE);

        if (event.isAllowNonBlocking() && event.getReplyToHandler() != null)
        {
            client.invoke(new ClientCallback()
            {
                @Override
                public void handleResponse(Map<String, Object> ctx, Object[] res)
                {
                    try
                    {
                        event.getReplyToHandler().processReplyTo(buildResponseMessage(event, responseHolder.value, res), null, null);
                    }
                    catch (MuleException ex)
                    {
                        handleException(ctx, ex);
                    }
                }

                @Override
                public void handleException(Map<String, Object> ctx, Throwable ex)
                {
                    event.getReplyToHandler().processExceptionReplyTo(wrapException(responseHolder.value, ex), null);
                }
            }, bop, getArgs(event), ctx, exchange);
            return NonBlockingVoidMuleEvent.getInstance();
        }
        else
        {
            Object[] response = client.invoke(bop, getArgs(event), ctx, exchange);
            return buildResponseMessage(event, responseHolder.value, response);
        }
    }

    public Method getMethod(MuleEvent event) throws Exception
    {
        Method method = null;
        String opName = (String)event.getMessage().getProperty(CxfConstants.OPERATION);
        if (opName != null)
        {
            method = getMethodFromOperation(opName);
        }

        if (method == null)
        {
            opName = operation;
            if (opName != null)
            {
                method = getMethodFromOperation(opName);
            }
        }

        if (method == null)
        {
            throw new MessagingException(CxfMessages.noOperationWasFoundOrSpecified(), event, this);
        }
        return method;
    }

    protected BindingOperationInfo getOperation(final String opName) throws Exception
    {
        // Normally its not this hard to invoke the CXF Client, but we're
        // sending along some exchange properties, so we need to use a more advanced
        // method
        Endpoint ep = client.getEndpoint();
        BindingOperationInfo bop = getBindingOperationFromEndpoint(ep, opName);
        if (bop == null)
        {
            bop = tryToGetTheOperationInDotNetNamingConvention(ep, opName);
            if (bop == null)
            {
                throw new Exception("No such operation: " + opName);
            }
        }

        if (bop.isUnwrappedCapable())
        {
            bop = bop.getUnwrappedOperation();
        }
        return bop;
    }

    /**
     * <p>
     * This method tries to call
     * {@link #getBindingOperationFromEndpoint(Endpoint, String)} with the .NET
     * naming convention for .NET webservices (method names start with a capital
     * letter).
     * </p>
     * <p>
     * CXF generates method names compliant with Java naming so if the WSDL operation
     * names starts with uppercase letter, matching with method name does not work -
     * thus the work around.
     * </p>
     */
    protected BindingOperationInfo tryToGetTheOperationInDotNetNamingConvention(Endpoint ep,
                                                                                final String opName)
    {
        final String capitalizedOpName = opName.substring(0, 1).toUpperCase() + opName.substring(1);
        return getBindingOperationFromEndpoint(ep, capitalizedOpName);
    }

    protected BindingOperationInfo getBindingOperationFromEndpoint(Endpoint ep, final String operationName)
    {
        QName q = new QName(ep.getService().getName().getNamespaceURI(), operationName);
        BindingOperationInfo bop = ep.getBinding().getBindingInfo().getOperation(q);
        return bop;
    }

    private Method getMethodFromOperation(String op) throws Exception
    {
        BindingOperationInfo bop = getOperation(op);
        MethodDispatcher md = (MethodDispatcher)client.getEndpoint()
            .getService()
            .get(MethodDispatcher.class.getName());
        return md.getMethod(bop);
    }

    protected String getMethodOrOperationName(MuleEvent event) throws DispatchException
    {
        // People can specify a CXF operation, which may in fact be different
        // than the method name. If that's not found, we'll default back to the
        // mule method property.
        String method = event.getMessage().getInvocationProperty(CxfConstants.OPERATION);

        if (method == null)
        {
            Object muleMethodProperty = event.getMessage().getInvocationProperty(MuleProperties.MULE_METHOD_PROPERTY);

            if (muleMethodProperty != null)
            {
                if (muleMethodProperty instanceof Method)
                {
                    method = ((Method) muleMethodProperty).getName();
                }
                else
                {
                    method = muleMethodProperty.toString();
                }
            }
        }

        if (method == null)
        {
            method = operation;
        }

        if (method == null && proxy)
        {
            return "invoke";
        }

        return method;
    }

    public BindingOperationInfo getOperation(MuleEvent event) throws Exception
    {
        String opName = getMethodOrOperationName(event);

        if (opName == null)
        {
            opName = operation;
        }

        return getOperation(opName);
    }

    private Map<String, Object> getInovcationProperties(MuleEvent event)
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(CxfConstants.MULE_EVENT, event);
        props.put(CxfConstants.CXF_OUTBOUND_MESSAGE_PROCESSOR, this);
        //props.put(org.apache.cxf.message.Message.ENDPOINT_ADDRESS, endpoint.getEndpointURI().toString());

        if (decoupledEndpoint != null)
        {
            props.put(WSAContextUtils.REPLYTO_PROPERTY, decoupledEndpoint);
        }

        return props;
    }

    protected MuleEvent buildResponseMessage(MuleEvent request, MuleEvent transportResponse, Object[] response)
    {
        // One way dispatches over an async transport result in this
        if (transportResponse == null)
        {
            return null;
        }
        if (VoidMuleEvent.getInstance().equals(transportResponse))
        {
            return transportResponse;
        }

        // Otherwise we may have a response!
        Object payload;
        if (response == null || response.length == 0)
        {
            payload = null;
        }
        else if (response.length == 1)
        {
            payload = response[0];
        }
        else
        {
            payload = response;
        }

        MuleMessage message = transportResponse.getMessage();

        Object httpStatusCode = message.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY);
        if (isProxy() && httpStatusCode != null)
        {
            message.setOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, httpStatusCode);
        }

        Class payloadClass = payload != null ? payload.getClass() : Object.class;
        message.setPayload(payload, DataTypeFactory.create(payloadClass, getMimeType()));

        return transportResponse;
    }

    protected Object[] addHoldersToResponse(Object response, Object[] args)
    {
        List<Object> responseWithHolders = new ArrayList<Object>();
        responseWithHolders.add(response);

        if(args != null)
        {
            for(Object arg : args)
            {
                if(arg instanceof Holder)
                {
                    responseWithHolders.add(arg);
                }
            }
        }

        return responseWithHolders.toArray();
    }

    public void setPayloadToArguments(CxfPayloadToArguments payloadToArguments)
    {
        this.payloadToArguments = payloadToArguments;
    }

    protected boolean isClientProxyAvailable()
    {
        return clientProxy != null;
    }

    public boolean isProxy()
    {
        return proxy;
    }

    public void setProxy(boolean proxy)
    {
        this.proxy = proxy;
    }

    public String getOperation()
    {
        return operation;
    }

    public void setOperation(String operation)
    {
        this.operation = operation;
    }

    public void setClientProxy(BindingProvider clientProxy)
    {
        this.clientProxy = clientProxy;
    }

    public CxfPayloadToArguments getPayloadToArguments()
    {
        return payloadToArguments;
    }

    public Client getClient()
    {
        return client;
    }

    public void setDecoupledEndpoint(String decoupledEndpoint)
    {
        this.decoupledEndpoint = decoupledEndpoint;
    }

    @Override
    public MessageProcessor clone()
    {
        CxfOutboundMessageProcessor clone = new CxfOutboundMessageProcessor(client);
        clone.payloadToArguments = this.payloadToArguments;
        clone.proxy = this.proxy;
        clone.operation = this.operation;
        clone.clientProxy = clientProxy;
        clone.decoupledEndpoint = decoupledEndpoint;

        return clone;
    }

    public String getMimeType()
    {
        return mimeType;
    }

    public void setMimeType(String mimeType)
    {
        this.mimeType = mimeType;
    }
}
