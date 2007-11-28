/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis;

import org.mule.config.MuleProperties;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.NullPayload;
import org.mule.providers.soap.NamedParameter;
import org.mule.providers.soap.SoapConstants;
import org.mule.providers.soap.SoapMethod;
import org.mule.providers.soap.i18n.SoapMessages;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.BeanUtils;
import org.mule.util.StringUtils;
import org.mule.util.TemplateParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPEnvelope;

import org.apache.axis.AxisProperties;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.Message;
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
    private Map callParameters;

    public AxisMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (AxisConnector)endpoint.getConnector();
        AxisProperties.setProperty("axis.doAutoTypes", Boolean.toString(connector.isDoAutoTypes()));
    }

    protected void doConnect() throws Exception
    {
        if (service == null)
        {
            service = createService(endpoint);
        }
    }

    protected void doDisconnect() throws Exception
    {
        if (service != null)
        {
            service = null;
        }
    }

    protected void doDispose()
    {
        // template method
    }

    protected synchronized EngineConfiguration getClientConfig(UMOImmutableEndpoint endpoint)
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

    protected Service createService(UMOImmutableEndpoint endpoint) throws Exception
    {
        // Create a simple axis service without wsdl
        EngineConfiguration config = getClientConfig(endpoint);
        return new Service(config);
    }

    protected void doDispatch(UMOEvent event) throws Exception
    {
        Object[] args = getArgs(event);
        Call call = getCall(event, args);
        // dont use invokeOneWay here as we are already in a thread pool.
        // Axis creates a new thread for every invoke one way call. nasty!
        // Mule overides the default Axis HttpSender to return immediately if
        // the axis.one.way property is set
        call.setProperty("axis.one.way", Boolean.TRUE);
        call.setProperty(MuleProperties.MULE_EVENT_PROPERTY, event);
        call.invoke(args);
    }

    protected UMOMessage doSend(UMOEvent event) throws Exception
    {
        Call call;
        Object result;
        Object[] args = getArgs(event);
        call = getCall(event, args);
        result = call.invoke(args);
        if (result == null)
        {
            return null;
        }
        else
        {
            UMOMessage resultMessage = new MuleMessage(result, event.getMessage());
            setMessageContextProperties(resultMessage, call.getMessageContext());
            return resultMessage;
        }
    }

    protected Call getCall(UMOEvent event, Object[] args) throws Exception
    {
        UMOEndpointURI endpointUri = event.getEndpoint().getEndpointURI();
        Object method = getInitialMethod(event); // changes object state
        Call call = (Call) service.createCall();
        parseStyle(event, call);
        parseUse(event, call);

        // set properties on the call from the endpoint properties
        BeanUtils.populateWithoutFail(call, event.getEndpoint().getProperties(), false);
        call.setTargetEndpointAddress(endpointUri.getAddress());

        method = refineMethod(event, call, method);
        String methodNamespace = call.getOperationName().getNamespaceURI();

        // set Mule event here so that handlers can extract info
        call.setProperty(MuleProperties.MULE_EVENT_PROPERTY, event);
        call.setProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, event.getEndpoint());

        setCustomProperties(event, call);
        call.setTimeout(new Integer(event.getTimeout()));
        setUserCredentials(endpointUri, call);

        Map methodCalls = (Map)event.getMessage().getProperty("soapMethods");
        if (methodCalls == null && !(method instanceof SoapMethod))
        {
            buildSoapMethods(event, call, method, methodNamespace, args);
        }

        setCallParams(call, event, call.getOperationName());
        setSoapAction(event, endpointUri, call);
        addAttachments(event, call);
        return call;
    }

    protected void addAttachments(UMOEvent event, Call call)
    {
        // Add any attachments to the call
        for (Iterator iterator = event.getMessage().getAttachmentNames().iterator(); iterator.hasNext();)
        {
            String name = (String)iterator.next();
            DataHandler dh = event.getMessage().getAttachment(name);
            AttachmentPart part = new AttachmentPart(dh);
            call.addAttachmentPart(part);
        }
    }
    
    protected void setSoapAction(UMOEvent event, UMOEndpointURI endpointUri, Call call)
    {
        // Set custom soap action if set on the event or endpoint
        String soapAction = (String)event.getMessage().getProperty(SoapConstants.SOAP_ACTION_PROPERTY);
        if (soapAction != null)
        {
            soapAction = parseSoapAction(soapAction, call.getOperationName(), event);
            call.setSOAPActionURI(soapAction);
            call.setUseSOAPAction(Boolean.TRUE.booleanValue());
        }
        else
        {
            call.setSOAPActionURI(endpointUri.getAddress());
        }
    }

    protected void buildSoapMethods(UMOEvent event, Call call, Object method, String methodNamespace, Object[] args)
    {
        List params = new ArrayList();
        for (int i = 0; i < args.length; i++)
        {
            if (args[i] == null)
            {
                QName qname = call.getTypeMapping().getTypeQName(Object.class);
                params.add("value" + i + ";qname{" + qname.getPrefix() + ":" + qname.getLocalPart() + ":"
                        + qname.getNamespaceURI() + "};in");
            }
            else if (args[i] instanceof DataHandler[])
            {
                params.add("attachments;qname{DataHandler:http://xml.apache.org/xml-soap};in");
                // Convert key/value pairs into the parameters
            }
            else if (args[i] instanceof Map && connector.isTreatMapAsNamedParams())
            {
                for (Iterator iterator = ((Map)args[i]).entrySet().iterator(); iterator.hasNext();)
                {
                    Map.Entry entry = (Map.Entry)iterator.next();
                    if (call.getTypeMapping().getTypeQName(entry.getValue().getClass()) != null)
                    {
                        QName type = call.getTypeMapping().getTypeQName(entry.getValue().getClass());
                        params.add("qname{" + entry.getKey().toString()
                                + (methodNamespace == null ? "" : ":" + methodNamespace) + "};qname{"
                                + type.getPrefix() + ":" + type.getLocalPart() + ":"
                                + type.getNamespaceURI() + "};in");
                    }
                    else
                    {
                        params.add("value" + i + ";qname{"
                                + Types.getLocalNameFromFullName(args[i].getClass().getName()) + ":"
                                + Namespaces.makeNamespace(args[i].getClass().getName()) + "};in");
                        params.add("qname{" + entry.getKey().toString()
                                + (methodNamespace == null ? "" : ":" + methodNamespace) + "};qname{"
                                + Types.getLocalNameFromFullName(args[i].getClass().getName()) + ":"
                                + Namespaces.makeNamespace(args[i].getClass().getName()) + "};in");
                    }

                }
            }
            else if (call.getTypeMapping().getTypeQName(args[i].getClass()) != null)
            {
                QName qname = call.getTypeMapping().getTypeQName(args[i].getClass());
                params.add("value" + i + ";qname{" + qname.getPrefix() + ":" + qname.getLocalPart() + ":"
                        + qname.getNamespaceURI() + "};in");
            }
            else
            {
                params.add("value" + i + ";qname{"
                        + Types.getLocalNameFromFullName(args[i].getClass().getName()) + ":"
                        + Namespaces.makeNamespace(args[i].getClass().getName()) + "};in");
            }
        }

        HashMap map = new HashMap();
        map.put(method, params);
        event.getMessage().setProperty("soapMethods", map);
    }

    protected void setUserCredentials(UMOEndpointURI endpointUri, Call call)
    {
        if (endpointUri.getUserInfo() != null)
        {
            call.setUsername(endpointUri.getUser());
            call.setPassword(endpointUri.getPassword());
        }
    }

    protected void setCustomProperties(UMOEvent event, Call call)
    {
        for (Iterator iter = event.getMessage().getPropertyNames().iterator(); iter.hasNext();)
        {
            String key = (String)iter.next();
            if (!(key.startsWith(MuleProperties.PROPERTY_PREFIX)))
            {
                Object value = event.getMessage().getProperty(key);
                if (value != null)
                {
                    call.setProperty(key, value);
                }
            }
        }
    }

    protected Object refineMethod(UMOEvent event, Call call, Object method)
    {
        if (method instanceof String)
        {
            // Set a custome method namespace if one is set. This will be used forthe
            // parameters too
            String methodNamespace = (String)event.getMessage().getProperty(SoapConstants.METHOD_NAMESPACE_PROPERTY);
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

    protected void parseUse(UMOEvent event, Call call)
    {
        // Set use: Endcoded/Literal
        String use = event.getMessage().getStringProperty("use", null);
        if (use != null)
        {
            Use u = Use.getUse(use);
            if (u == null)
            {
                throw new IllegalArgumentException(
                        CoreMessages.valueIsInvalidFor(use, "use").toString());
            }
            else
            {
                call.setOperationUse(u);
            }
        }
    }

    protected void parseStyle(UMOEvent event, Call call)
    {
        // Note that Axis has specific rules to how these two variables are
        // combined. This is handled for us
        // Set style: RPC/wrapped/Doc/Message
        String style = event.getMessage().getStringProperty("style", null);
        if (style != null)
        {
            Style s = Style.getStyle(style);
            if (s == null)
            {
                throw new IllegalArgumentException(
                        CoreMessages.valueIsInvalidFor(style, "style").toString());
            }
            else
            {
                call.setOperationStyle(s);
            }
        }
    }

    protected Object getInitialMethod(UMOEvent event) throws DispatchException
    {
        Object method = event.getMessage().getProperty(MuleProperties.MULE_METHOD_PROPERTY);
        if (method == null)
        {
            method = event.getEndpoint().getEndpointURI().getParams().getProperty(MuleProperties.MULE_METHOD_PROPERTY);
        }
        if (method == null)
        {
            throw new DispatchException(
                    SoapMessages.cannotInvokeCallWithoutOperation(), event.getMessage(),
                    event.getEndpoint());
        }
        else if (method instanceof SoapMethod)
        {
            synchronized (this)
            {
                if (callParameters == null)
                {
                    callParameters = new HashMap();
                }
                callParameters.put(((SoapMethod)method).getName().getLocalPart(), method);
            }
        }
        return method;
    }

    private Object[] getArgs(UMOEvent event) throws TransformerException
    {
        Object payload = event.getTransformedMessage();
        Object[] args;
        if (payload instanceof Object[])
        {
            args = (Object[])payload;
        }
        else
        {
            args = new Object[]{payload};
        }
        if (event.getMessage().getAttachmentNames() != null
            && event.getMessage().getAttachmentNames().size() > 0)
        {
            ArrayList attachments = new ArrayList();
            Iterator i = event.getMessage().getAttachmentNames().iterator();
            while (i.hasNext())
            {
                attachments.add(event.getMessage().getAttachment((String)i.next()));
            }
            ArrayList temp = new ArrayList(Arrays.asList(args));
            temp.add(attachments.toArray(new DataHandler[0]));
            args = temp.toArray();
        }
        return args;
    }

    protected void setMessageContextProperties(UMOMessage message, MessageContext ctx)
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

    protected void setMessageContextAttachments(UMOMessage message, MessageContext ctx) throws Exception
    {
        int x = 0;
        for (Iterator iterator = ctx.getMessage().getAttachments(); iterator.hasNext(); x++)
        {
            message.addAttachment(String.valueOf(x),
                ((AttachmentPart)iterator.next()).getActivationDataHandler());
        }
    }

    protected static UMOMessage createMessage(Object result, Call call)
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
        return new MuleMessage(result, props);
    }

    public String parseSoapAction(String soapAction, QName method, UMOEvent event)
    {
        UMOEndpointURI endpointURI = event.getEndpoint().getEndpointURI();
        Map properties = new HashMap();
        UMOMessage msg = event.getMessage();
        for (Iterator iterator = msg.getPropertyNames().iterator(); iterator.hasNext();)
        {
            String propertyKey = (String)iterator.next();
            properties.put(propertyKey, msg.getProperty(propertyKey));
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
        if (event.getComponent() != null)
        {
            properties.put("serviceName", event.getComponent().getName());
        }

        TemplateParser tp = TemplateParser.createSquareBracesStyleParser();
        soapAction = tp.parse(properties, soapAction);

        if (logger.isDebugEnabled())
        {
            logger.debug("SoapAction for this call is: " + soapAction);
        }
        return soapAction;
    }

    private void setCallParams(Call call, UMOEvent event, QName method) throws ClassNotFoundException
    {
        synchronized (this)
        {
            if (callParameters == null)
            {
                loadCallParams(event, method.getNamespaceURI());
            }
        }

        SoapMethod soapMethod = (SoapMethod)event.getMessage()
            .removeProperty(MuleProperties.MULE_SOAP_METHOD);
        if (soapMethod == null)
        {
            soapMethod = (SoapMethod)callParameters.get(method.getLocalPart());
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

    private void loadCallParams(UMOEvent event, String namespace) throws ClassNotFoundException
    {
        Map methodCalls = (Map)event.getMessage().getProperty("soapMethods");
        if (methodCalls == null)
        {
            return;
        }

        Map.Entry entry;
        SoapMethod soapMethod;
        callParameters = new HashMap();

        for (Iterator iterator = methodCalls.entrySet().iterator(); iterator.hasNext();)
        {
            entry = (Map.Entry)iterator.next();
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
