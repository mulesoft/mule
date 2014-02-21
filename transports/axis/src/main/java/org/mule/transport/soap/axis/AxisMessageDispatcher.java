/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.DispatchException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.cxf.SoapConstants;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.NullPayload;
import org.mule.transport.soap.axis.i18n.AxisMessages;
import org.mule.util.BeanUtils;
import org.mule.util.StringUtils;
import org.mule.util.TemplateParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;

import org.apache.axis.AxisProperties;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.MessageContext;
import org.apache.axis.attachments.AttachmentPart;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.configuration.FileProvider;
import org.apache.axis.constants.Style;
import org.apache.axis.constants.Use;
import org.apache.axis.wsdl.fromJava.Namespaces;
import org.apache.axis.wsdl.fromJava.Types;

/**
 * <code>AxisMessageDispatcher</code> is used to make soap requests via the Axis
 * soap client.
 */
public class AxisMessageDispatcher extends AbstractMessageDispatcher
{

    protected EngineConfiguration clientConfig;
    protected AxisConnector connector;
    protected Service service;
    private Map<String, SoapMethod> callParameters;

    public AxisMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (AxisConnector)endpoint.getConnector();
        AxisProperties.setProperty("axis.doAutoTypes", Boolean.toString(connector.isDoAutoTypes()));
    }

    @Override
    protected void doConnect() throws Exception
    {
        if (service == null)
        {
            service = createService(endpoint);
        }
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        if (service != null)
        {
            service = null;
        }
    }

    @Override
    protected void doDispose()
    {
        // template method
    }

    protected synchronized EngineConfiguration getClientConfig(ImmutableEndpoint endpoint)
    {
        if (clientConfig == null)
        {
            // Allow the client config to be set on the endpoint
            String config;
            config = (String)endpoint.getProperty(AxisConnector.AXIS_CLIENT_CONFIG_PROPERTY);

            if (config != null)
            {
                clientConfig = new FileProvider(config);
            }
            else
            {
                clientConfig = connector.getClientProvider();
            }
        }
        return clientConfig;
    }

    protected Service createService(ImmutableEndpoint endpoint) throws Exception
    {
        // Create a simple axis service without wsdl
        EngineConfiguration config = getClientConfig(endpoint);
        return new Service(config);
    }

    @Override
    protected void doDispatch(MuleEvent event) throws Exception
    {
        Object[] args = getArgs(event);
        Call call = getCall(event, args);
        // dont use invokeOneWay here as we are already in a thread pool.
        // Axis creates a new thread for every invoke one way call. nasty!
        // Mule overides the default Axis HttpSender to return immediately if
        // the axis.one.way property is set
        call.setProperty("axis.one.way", Boolean.TRUE);
        call.setProperty(MuleProperties.MULE_EVENT_PROPERTY, event);
        call.setProperty(MuleProperties.MULE_CONTEXT_PROPERTY, getEndpoint().getMuleContext());
        call.invoke(args);
    }

    @Override
    protected MuleMessage doSend(MuleEvent event) throws Exception
    {
        Call call;
        Object result;
        Object[] args = getArgs(event);
        call = getCall(event, args);
        result = call.invoke(args);
        if (result == null)
        {
            return new DefaultMuleMessage(NullPayload.getInstance(), getEndpoint().getMuleContext());
        }
        else
        {
            MuleMessage resultMessage = new DefaultMuleMessage(result, event.getMessage(), event.getMuleContext());
            setMessageContextProperties(resultMessage, call.getMessageContext());
            return resultMessage;
        }
    }

    protected Call getCall(MuleEvent event, Object[] args) throws Exception
    {
        EndpointURI endpointUri = endpoint.getEndpointURI();
        Object method = getInitialMethod(event); // changes object state
        Call call = (Call) service.createCall();
        parseStyle(event, call);
        parseUse(event, call);

        // set properties on the call from the endpoint properties
        BeanUtils.populateWithoutFail(call, endpoint.getProperties(), false);
        call.setTargetEndpointAddress(endpointUri.getAddress());

        method = refineMethod(event, call, method);
        String methodNamespace = call.getOperationName().getNamespaceURI();

        // set Mule event here so that handlers can extract info
        call.setProperty(MuleProperties.MULE_EVENT_PROPERTY, event);
        call.setProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, endpoint);
        call.setProperty(MuleProperties.MULE_CONTEXT_PROPERTY, getEndpoint().getMuleContext());

        setCustomProperties(event, call);
        call.setTimeout(new Integer(event.getTimeout()));
        setUserCredentials(endpointUri, call);

        Map<Object, List<String>> methodCalls = event.getMessage().getOutboundProperty(AxisConnector.SOAP_METHODS);
        if (methodCalls == null && !(method instanceof SoapMethod))
        {
            buildSoapMethods(event, call, method, methodNamespace, args);
        }

        setCallParams(call, event, call.getOperationName());
        setSoapAction(event, endpointUri, call);
        addAttachments(event, call);
        return call;
    }

    protected void addAttachments(MuleEvent event, Call call)
    {
        // Add any attachments to the call
        for (Iterator iterator = event.getMessage().getOutboundAttachmentNames().iterator(); iterator.hasNext();)
        {
            String name = (String)iterator.next();
            DataHandler dh = event.getMessage().getOutboundAttachment(name);
            AttachmentPart part = new AttachmentPart(dh);
            call.addAttachmentPart(part);
        }
    }

    protected void setSoapAction(MuleEvent event, EndpointURI endpointUri, Call call)
    {
        // Set custom soap action if set on the event or endpoint
        String soapAction = event.getMessage().getOutboundProperty(SoapConstants.SOAP_ACTION_PROPERTY);
        if (soapAction != null)
        {
            soapAction = parseSoapAction(soapAction, call.getOperationName(), event);
            call.setSOAPActionURI(soapAction);
            call.setUseSOAPAction(true);
        }
        else
        {
            call.setSOAPActionURI(endpointUri.getAddress());
        }
    }

    protected void buildSoapMethods(MuleEvent event, Call call, Object method, String methodNamespace, Object[] args)
    {
        List<String> params = new ArrayList<String>();
        for (int i = 0; i < args.length; i++)
        {
            if (args[i] == null)
            {
                QName qname = call.getTypeMapping().getTypeQName(Object.class);
                params.add(String.format("value%d;qname{%s:%s:%s};in",
                                         i, qname.getPrefix(), qname.getLocalPart(), qname.getNamespaceURI()));
            }
            else if (args[i] instanceof DataHandler[])
            {
                params.add("attachments;qname{DataHandler:http://xml.apache.org/xml-soap};in");
                // Convert key/value pairs into the parameters
            }
            else if (args[i] instanceof Map && connector.isTreatMapAsNamedParams())
            {
                for (Iterator<?> iterator = ((Map<?, ?>)args[i]).entrySet().iterator(); iterator.hasNext();)
                {
                    Map.Entry<?, ?> entry = (Map.Entry<?, ?>)iterator.next();
                    if (call.getTypeMapping().getTypeQName(entry.getValue().getClass()) != null)
                    {
                        QName type = call.getTypeMapping().getTypeQName(entry.getValue().getClass());
                        params.add(String.format("qname{%s%s};qname{%s:%s:%s};in",
                                                 entry.getKey().toString(),
                                                 (methodNamespace == null ? "" : ":" + methodNamespace),
                                                 type.getPrefix(), type.getLocalPart(), type.getNamespaceURI()));
                    }
                    else
                    {
                        params.add(String.format("value%d;qname{%s:%s};in",
                                                 i, Types.getLocalNameFromFullName(args[i].getClass().getName()),
                                                 Namespaces.makeNamespace(args[i].getClass().getName())));
                        params.add(String.format("qname{%s%s};qname{%s:%s};in",
                                                 entry.getKey().toString(),
                                                 (methodNamespace == null ? "" : ":" + methodNamespace),
                                                 Types.getLocalNameFromFullName(args[i].getClass().getName()),
                                                 Namespaces.makeNamespace(args[i].getClass().getName())));
                    }

                }
            }
            else if (call.getTypeMapping().getTypeQName(args[i].getClass()) != null)
            {
                QName qname = call.getTypeMapping().getTypeQName(args[i].getClass());
                params.add(String.format("value%d;qname{%s:%s:%s};in",
                                         i, qname.getPrefix(), qname.getLocalPart(), qname.getNamespaceURI()));
            }
            else
            {
                params.add(String.format("value%d;qname{%s:%s};in",
                                         i, Types.getLocalNameFromFullName(args[i].getClass().getName()),
                                         Namespaces.makeNamespace(args[i].getClass().getName())));
            }
        }

        Map<Object, List<String>> map = new HashMap<Object, List<String>>();
        map.put(method, params);
        event.getMessage().setOutboundProperty(AxisConnector.SOAP_METHODS, map);
    }

    protected void setUserCredentials(EndpointURI endpointUri, Call call)
    {
        if (endpointUri.getUserInfo() != null)
        {
            call.setUsername(endpointUri.getUser());
            call.setPassword(endpointUri.getPassword());
        }
    }

    protected void setCustomProperties(MuleEvent event, Call call)
    {
        for (String key : event.getMessage().getOutboundPropertyNames())
        {
            if (!(key.startsWith(MuleProperties.PROPERTY_PREFIX)))
            {
                Object value = event.getMessage().getOutboundProperty(key);
                if (value != null)
                {
                    call.setProperty(key, value);
                }
            }
        }
    }

    protected Object refineMethod(MuleEvent event, Call call, Object method)
    {
        if (method instanceof String)
        {
            // Set a custome method namespace if one is set. This will be used forthe
            // parameters too
            String methodNamespace = event.getMessage().getOutboundProperty(SoapConstants.METHOD_NAMESPACE_PROPERTY);
            if (methodNamespace != null)
            {
                call.setOperationName(new QName(methodNamespace, method.toString()));
            }
            else
            {
                call.setOperationName(new QName(method.toString()));
            }
        }
        else if (method instanceof QName)
        {
            call.setOperationName((QName)method);
            method = ((QName)method).getLocalPart();
        }
        else
        {
            call.setOperationName(((SoapMethod)method).getName());
        }
        return method;
    }

    protected void parseUse(MuleEvent event, Call call)
    {
        // Set use: Endcoded/Literal
        String use = event.getMessage().getOutboundProperty(AxisConnector.USE);
        if (use != null)
        {
            Use u = Use.getUse(use);
            if (u == null)
            {
                throw new IllegalArgumentException(
                        CoreMessages.valueIsInvalidFor(use, AxisConnector.USE).toString());
            }
            else
            {
                call.setOperationUse(u);
            }
        }
    }

    protected void parseStyle(MuleEvent event, Call call)
    {
        // Note that Axis has specific rules to how these two variables are
        // combined. This is handled for us
        // Set style: RPC/wrapped/Doc/Message
        String style = event.getMessage().getOutboundProperty(AxisConnector.STYLE);
        if (style != null)
        {
            Style s = Style.getStyle(style);
            if (s == null)
            {
                throw new IllegalArgumentException(
                        CoreMessages.valueIsInvalidFor(style, AxisConnector.STYLE).toString());
            }
            else
            {
                call.setOperationStyle(s);
            }
        }
    }

    protected Object getInitialMethod(MuleEvent event) throws DispatchException
    {
        Object method = event.getMessage().getOutboundProperty(MuleProperties.MULE_METHOD_PROPERTY);
        if (method == null)
        {
            method = endpoint.getEndpointURI().getParams().getProperty(MuleProperties.MULE_METHOD_PROPERTY);
        }
        if (method == null)
        {
            throw new DispatchException(AxisMessages.cannotInvokeCallWithoutOperation(),
                event, this);
        }
        else if (method instanceof SoapMethod)
        {
            synchronized (this)
            {
                if (callParameters == null)
                {
                    callParameters = new HashMap<String, SoapMethod>();
                }
                SoapMethod soapMethod = (SoapMethod) method;
                callParameters.put(((SoapMethod) method).getName().getLocalPart(), soapMethod);
            }
        }
        return method;
    }

    private Object[] getArgs(MuleEvent event) throws TransformerException
    {
        Object payload = event.getMessage().getPayload();
        Object[] args;
        if (payload instanceof Object[])
        {
            args = (Object[])payload;
        }
        else
        {
            args = new Object[]{payload};
        }
        if (event.getMessage().getOutboundAttachmentNames() != null
            && event.getMessage().getOutboundAttachmentNames().size() > 0)
        {
            List<DataHandler> attachments = new ArrayList<DataHandler>();
            for (String name : event.getMessage().getOutboundAttachmentNames())
            {
                attachments.add(event.getMessage().getOutboundAttachment(name));
            }
            ArrayList<Object> temp = new ArrayList<Object>(Arrays.asList(args));
            temp.add(attachments.toArray(new DataHandler[attachments.size()]));
            args = temp.toArray();
        }
        return args;
    }

    protected void setMessageContextProperties(MuleMessage message, MessageContext ctx)
    {
        String temp = ctx.getStrProp(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
        if (StringUtils.isNotBlank(temp))
        {
            message.setCorrelationId(temp);
        }
        temp = ctx.getStrProp(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY);
        if (StringUtils.isNotBlank(temp))
        {
            message.setCorrelationGroupSize(Integer.parseInt(temp));
        }
        temp = ctx.getStrProp(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY);
        if (StringUtils.isNotBlank(temp))
        {
            message.setCorrelationSequence(Integer.parseInt(temp));
        }
        temp = ctx.getStrProp(MuleProperties.MULE_REPLY_TO_PROPERTY);
        if (StringUtils.isNotBlank(temp))
        {
            message.setReplyTo(temp);
        }
    }

    protected void setMessageContextAttachments(MuleMessage message, MessageContext ctx) throws Exception
    {
        int x = 0;
        for (Iterator<?> iterator = ctx.getMessage().getAttachments(); iterator.hasNext(); x++)
        {
            message.addOutboundAttachment(String.valueOf(x),
                ((AttachmentPart)iterator.next()).getActivationDataHandler());
        }
    }

    protected static MuleMessage createMessage(Object result, Call call, MuleContext muleContext)
    {
        if (result == null)
        {
            result = NullPayload.getInstance();
        }
        Map props = new HashMap();
        Iterator iter = call.getMessageContext().getPropertyNames();
        Object key;
        while (iter.hasNext())
        {
            key = iter.next();
            props.put(key, call.getMessageContext().getProperty(key.toString()));
        }
        props.put("soap.message", call.getMessageContext().getMessage());
        call.clearHeaders();
        call.clearOperation();
        return new DefaultMuleMessage(result, props, muleContext);
    }

    public String parseSoapAction(String soapAction, QName method, MuleEvent event)
    {
        EndpointURI endpointURI = endpoint.getEndpointURI();
        Map properties = new HashMap();
        MuleMessage msg = event.getMessage();
        for (String propertyKey : msg.getOutboundPropertyNames())
        {
            Object value = msg.getOutboundProperty(propertyKey);
            properties.put(propertyKey, value);
        }
        properties.put(MuleProperties.MULE_METHOD_PROPERTY, method.getLocalPart());
        properties.put("methodNamespace", method.getNamespaceURI());
        properties.put("address", endpointURI.getAddress());
        properties.put("scheme", endpointURI.getScheme());
        properties.put("host", endpointURI.getHost());
        properties.put("port", String.valueOf(endpointURI.getPort()));
        properties.put("path", endpointURI.getPath());
        properties.put("hostInfo", endpointURI.getScheme()
                                   + "://"
                                   + endpointURI.getHost()
                                   + (endpointURI.getPort() > -1
                                                   ? ":" + String.valueOf(endpointURI.getPort()) : ""));
        if (event.getFlowConstruct() != null)
        {
            properties.put("serviceName", event.getFlowConstruct().getName());
        }

        TemplateParser tp = TemplateParser.createMuleStyleParser();
        soapAction = tp.parse(properties, soapAction);

        if (logger.isDebugEnabled())
        {
            logger.debug("SoapAction for this call is: " + soapAction);
        }
        return soapAction;
    }

    private void setCallParams(Call call, MuleEvent event, QName method) throws ClassNotFoundException
    {
        synchronized (this)
        {
            if (callParameters == null)
            {
                loadCallParams(event, method.getNamespaceURI());
            }
        }

        SoapMethod soapMethod = (SoapMethod)event.getMessage().removeProperty(MuleProperties.MULE_SOAP_METHOD);
        if (soapMethod == null)
        {
            soapMethod = callParameters.get(method.getLocalPart());
        }

        if (soapMethod != null)
        {
            for (Iterator iterator = soapMethod.getNamedParameters().iterator(); iterator.hasNext();)
            {
                NamedParameter parameter = (NamedParameter)iterator.next();
                call.addParameter(parameter.getName(), parameter.getType(), parameter.getMode());
            }

            if (soapMethod.getReturnType() != null)
            {
                call.setReturnType(soapMethod.getReturnType());
            }
            else if (soapMethod.getReturnClass() != null)
            {
                call.setReturnClass(soapMethod.getReturnClass());
            }

            call.setOperationName(soapMethod.getName());
        }
    }

    private void loadCallParams(MuleEvent event, String namespace) throws ClassNotFoundException
    {
        Map<Object, List<String>> methodCalls = event.getMessage().getOutboundProperty(AxisConnector.SOAP_METHODS);
        if (methodCalls == null)
        {
            return;
        }

        SoapMethod soapMethod;
        callParameters = new HashMap<String, SoapMethod>();

        for (Iterator iterator = methodCalls.entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry entry = (Map.Entry)iterator.next();
            if (StringUtils.isEmpty(namespace))
            {
                if (entry.getValue() instanceof List)
                {
                    soapMethod = new SoapMethod(entry.getKey().toString(), (List)entry.getValue());
                }
                else
                {
                    soapMethod = new SoapMethod(entry.getKey().toString(), entry.getValue().toString());
                }
            }
            else
            {
                if (entry.getValue() instanceof List)
                {
                    soapMethod = new SoapMethod(new QName(namespace, entry.getKey().toString()),
                        (List)entry.getValue());
                }
                else
                {
                    soapMethod = new SoapMethod(new QName(namespace, entry.getKey().toString()),
                        entry.getValue().toString());
                }
            }
            callParameters.put(soapMethod.getName().getLocalPart(), soapMethod);
        }
    }

}
