/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.soap.axis;

import org.apache.axis.AxisProperties;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.configuration.FileProvider;
import org.apache.axis.constants.Style;
import org.apache.axis.constants.Use;
import org.apache.axis.wsdl.fromJava.Namespaces;
import org.apache.axis.wsdl.fromJava.Types;
import org.apache.commons.lang.StringUtils;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.NullPayload;
import org.mule.providers.soap.NamedParameter;
import org.mule.providers.soap.SoapMethod;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.BeanUtils;
import org.mule.util.TemplateParser;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPEnvelope;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * <code>AxisMessageDispatcher</code> is used to make soap requests via the
 * Axis soap client.
 * <p/>
 * <at> author <a href="mailto:ross.mason@...">Ross Mason</a>
 * <at> version $Revision$
 */
public class AxisMessageDispatcher extends AbstractMessageDispatcher {
    private Map callParameters;

    protected EngineConfiguration clientConfig;

    protected AxisConnector connector;

    protected Service service;

    public AxisMessageDispatcher(AxisConnector connector) {
        super(connector);
        this.connector = connector;
        AxisProperties.setProperty("axis.doAutoTypes", Boolean.toString(connector.isDoAutoTypes()));
    }

    public void doDispose() {
        if(service!=null) {
            service = null;
        }
    }

    protected Service getService(UMOEvent event) throws Exception {
        if (service == null) {
            service = createService(event);
        }
        return service;
    }

    protected EngineConfiguration getClientConfig(UMOEvent event) {
        if(clientConfig==null) {
            //Allow the client config to be set on the endpoint
            String config = null;
            if(event!=null) {
                config = event.getMessage().getStringProperty(AxisConnector.AXIS_CLIENT_CONFIG_PROPERTY, null);
            }
            if(config!=null) {
                clientConfig = new FileProvider(config);
            } else {
                clientConfig = connector.getClientProvider();
            }
        }
        return clientConfig;
    }

    protected Service createService(UMOEvent event) throws Exception {
        // Create a simple axis service without wsdl
        EngineConfiguration config = getClientConfig(event);
        Service service = new Service(config);
        return service;
    }

    protected String getWsdlUrl(UMOEvent event) {
        return event.getMessage().getStringProperty(AxisConnector.WSDL_URL_PROPERTY, StringUtils.EMPTY);
    }

    public void doDispatch(UMOEvent event) throws Exception {
        Object[] args = getArgs(event);
        Call call = getCall(event, args);
        // dont use invokeOneWay here as we are already in a thread pool.
        // Axis creates a new thread for every invoke one way call. nasty!
        // Mule overides the default Axis HttpSender to return immediately if
        //the axis.one.way property is set
        call.setProperty("axis.one.way", Boolean.TRUE);
        call.setProperty(MuleProperties.MULE_EVENT_PROPERTY, event);
        call.invoke(args);

    }

    public UMOMessage doSend(UMOEvent event) throws Exception {
        Call call;
        Object result;
        Object[] args = getArgs(event);
        call = getCall(event, args);
        result = call.invoke(args);
        if (result == null) {
            return null;
        } else {
            UMOMessage resultMessage = new MuleMessage(result, event.getMessage());
            setMessageContextProperties(resultMessage, call.getMessageContext());
            return resultMessage;
        }
    }

    protected Call getCall(UMOEvent event, Object[] args) throws Exception {
        UMOEndpointURI endpointUri = event.getEndpoint().getEndpointURI();
        String method = (String) endpointUri.getParams().remove("method");
        if (method == null) {
            throw new DispatchException(new org.mule.config.i18n.Message("soap", 4),
                    event.getMessage(),
                    event.getEndpoint());
        }

        Service service = getService(event);
        Call call = (Call) service.createCall();

        String style = event.getMessage().getStringProperty("style", null);
        String use = event.getMessage().getStringProperty("use", null);

        // Note that Axis has specific rules to how these two variables are
        // combined. This is handled for us
        // Set style: RPC/wrapped/Doc/Message
        if (style != null) {
            Style s = Style.getStyle(style);
            if (s == null) {
                throw new IllegalArgumentException(new org.mule.config.i18n.Message(Messages.VALUE_X_IS_INVALID_FOR_X, style, "style").toString());
            } else {
                call.setOperationStyle(s);
            }
        }
        // Set use: Endcoded/Literal
        if (use != null) {
            Use u = Use.getUse(use);
            if (u == null) {
                throw new IllegalArgumentException(new org.mule.config.i18n.Message(Messages.VALUE_X_IS_INVALID_FOR_X, use, "use").toString());
            } else {
                call.setOperationUse(u);
            }
        }

        // set properties on the call from the endpoint properties
        BeanUtils.populateWithoutFail(call, event.getEndpoint().getProperties(), false);
        call.setTargetEndpointAddress(endpointUri.getAddress());

        //Set a custome method namespace if one is set.  This will be used forthe parameters too
        String methodNamespace = (String) event.getMessage().getProperty(AxisConnector.METHOD_NAMESPACE_PROPERTY);
        if (methodNamespace != null) {
            call.setOperationName(new QName(methodNamespace, method));
        } else {
            call.setOperationName(new QName(method));
        }

        // set Mule event here so that handlers can extract info
        call.setProperty(MuleProperties.MULE_EVENT_PROPERTY, event);
        call.setProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, event.getEndpoint());
        // Set timeout
        call.setTimeout(new Integer(event.getTimeout()));

        // Add User Creds
        if (endpointUri.getUserInfo() != null) {
            call.setUsername(endpointUri.getUsername());
            call.setPassword(endpointUri.getPassword());
        }

        Map methodCalls = (Map) event.getMessage().getProperty("soapMethods");
        if (methodCalls == null) {
            ArrayList params = new ArrayList();
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof DataHandler[]) {
                    params.add("attachments;qname{DataHandler:http://xml.apache.org/xml-soap};in");
                    //Convert key/value pairs into the parameters
                } else if (args[i] instanceof Map && connector.isTreatMapAsNamedParams()) {
                    for (Iterator iterator = ((Map) args[i]).entrySet().iterator(); iterator.hasNext();) {
                        Map.Entry entry = (Map.Entry)iterator.next();
                        if (call.getTypeMapping().getTypeQName(entry.getValue().getClass()) != null) {
                            QName type = call.getTypeMapping().getTypeQName(entry.getValue().getClass());
                        params.add("qname{" + entry.getKey().toString() + (methodNamespace==null ? "" : ":" + methodNamespace) +
                                "};qname{" + type.getPrefix() + ":" + type.getLocalPart() + ":" + type.getNamespaceURI() + "};in");
                        } else {
                             params.add("value" + i + ";qname{" + Types.getLocalNameFromFullName(args[i].getClass().getName()) + ":" + Namespaces.makeNamespace(args[i].getClass().getName()) + "};in");
                             params.add("qname{" + entry.getKey().toString() + (methodNamespace==null ? "" : ":" + methodNamespace) +
                                "};qname{" + Types.getLocalNameFromFullName(args[i].getClass().getName()) + ":" + Namespaces.makeNamespace(args[i].getClass().getName()) + "};in");
                        }

                    }
                } else if (call.getTypeMapping().getTypeQName(args[i].getClass()) != null) {
                        QName qname = call.getTypeMapping().getTypeQName(args[i].getClass());
                        params.add("value" + i + ";qname{" + qname.getPrefix() + ":" + qname.getLocalPart() + ":" + qname.getNamespaceURI() + "};in");
                } else {
                        params.add("value" + i + ";qname{" + Types.getLocalNameFromFullName(args[i].getClass().getName()) + ":" + Namespaces.makeNamespace(args[i].getClass().getName()) + "};in");
                }
            }

            HashMap map = new HashMap();
            map.put(method, params);
            event.getMessage().setProperty("soapMethods", map);
        }

        setCallParams(call, event, call.getOperationName());

        //Set custom soap action if set on the event or endpoint
        String soapAction = (String) event.getMessage().getProperty(AxisConnector.SOAP_ACTION_PROPERTY);
        if (soapAction != null) {
            soapAction = parseSoapAction(soapAction, call.getOperationName(), event);
            call.setSOAPActionURI(soapAction);
            call.setUseSOAPAction(Boolean.TRUE.booleanValue());
        } else {
            call.setSOAPActionURI(endpointUri.getAddress());
        }
        return call;
    }

    private Object[] getArgs(UMOEvent event) throws TransformerException {
        Object payload = event.getTransformedMessage();
        Object[] args = new Object[0];
        if (payload instanceof Object[]) {
            args = (Object[]) payload;
        } else {
            args = new Object[]{payload};
        }
        if (event.getMessage().getAttachmentNames() != null && event.getMessage().getAttachmentNames().size() > 0) {
            ArrayList attachments = new ArrayList();
            Iterator i = event.getMessage().getAttachmentNames().iterator();
            while (i.hasNext()) {
                attachments.add(event.getMessage().getAttachment((String) i.next()));
            }
            ArrayList temp = new ArrayList(Arrays.asList(args));
            temp.add(attachments.toArray(new DataHandler[0]));
            args = temp.toArray();
        }
        return args;
    }

    private void setMessageContextProperties(UMOMessage message, MessageContext ctx) {
        String temp = ctx.getStrProp(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
        if (StringUtils.isNotBlank(temp)) {
            message.setCorrelationId(temp);
        }
        temp = ctx.getStrProp(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY);
        if (StringUtils.isNotBlank(temp)) {
            message.setCorrelationGroupSize(Integer.parseInt(temp));
        }
        temp = ctx.getStrProp(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY);
        if (StringUtils.isNotBlank(temp)) {
            message.setCorrelationSequence(Integer.parseInt(temp));
        }
        temp = ctx.getStrProp(MuleProperties.MULE_REPLY_TO_PROPERTY);
        if (StringUtils.isNotBlank(temp)) {
            message.setReplyTo(temp);
        }
    }

    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception {
        Service service = getService(null);
        Call call = new Call(service);
        call.setSOAPActionURI(endpointUri.toString());
        call.setTargetEndpointAddress(endpointUri.toString());

        String method = (String) endpointUri.getParams().remove(MuleProperties.MULE_METHOD_PROPERTY);
        call.setOperationName(method);
        Properties params = endpointUri.getUserParams();
        String args[] = new String[params.size()];
        int i = 0;
        for (Iterator iterator = params.values().iterator(); iterator.hasNext(); i++) {
            args[i] = iterator.next().toString();
        }

        call.setOperationName(method);
        UMOEndpoint ep = MuleEndpoint.getOrCreateEndpointForUri(endpointUri, UMOEndpoint.ENDPOINT_TYPE_SENDER);
        ep.initialise();
        call.setProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, ep);
        Object result = call.invoke(method, args);
        return createMessage(result, call);
    }

    public UMOMessage receive(String endpoint, Object[] args) throws Exception {
        Service service = getService(null);
        Call call = new Call(service);

        call.setSOAPActionURI(endpoint);
        call.setTargetEndpointAddress(endpoint);

        if (!endpoint.startsWith("axis:")) {
            endpoint = "axis:" + endpoint;
        }
        UMOEndpointURI ep = new MuleEndpointURI(endpoint);
        String method = (String) ep.getParams().remove("method");
        call.setOperationName(method);

        call.setOperationName(method);
        Object result = call.invoke(method, args);
        return createMessage(result, call);
    }

    public UMOMessage receive(String endpoint, SOAPEnvelope envelope) throws Exception {
        Service service = getService(null);
        Call call = new Call(service);

        call.setSOAPActionURI(endpoint);
        call.setTargetEndpointAddress(endpoint);
        Object result = call.invoke(new Message(envelope));
        return createMessage(result, call);
    }

    protected UMOMessage createMessage(Object result, Call call) {
        if (result == null) {
            result = new NullPayload();
        }
        Map props = new HashMap();
        Iterator iter = call.getMessageContext().getPropertyNames();
        Object key;
        while (iter.hasNext()) {
            key = iter.next();
            props.put(key, call.getMessageContext().getProperty(key.toString()));
        }
        props.put("soap.message", call.getMessageContext().getMessage());
        call.clearHeaders();
        call.clearOperation();
        return new MuleMessage(result, props);
    }

    public Object getDelegateSession() throws UMOException {
        return null;
    }

    public String parseSoapAction(String soapAction, QName method, UMOEvent event) {

        UMOEndpointURI endpointURI = event.getEndpoint().getEndpointURI();
        Map properties = new HashMap();
        for (Iterator iterator = event.getMessage().getPropertyNames(); iterator.hasNext();) {
            Object o =  iterator.next();
            properties.put(o, event.getMessage().getProperty(o));
        }
        properties.put("method", method.getLocalPart());
        properties.put("methodNamespace", method.getNamespaceURI());
        properties.put("address", endpointURI.getAddress());
        properties.put("scheme", endpointURI.getScheme());
        properties.put("host", endpointURI.getHost());
        properties.put("port", String.valueOf(endpointURI.getPort()));
        properties.put("path", endpointURI.getPath());
        properties.put("hostInfo", endpointURI.getScheme() + "://" + endpointURI.getHost() +
                (endpointURI.getPort() > -1 ? ":" + String.valueOf(endpointURI.getPort()) : ""));
        if (event.getComponent() != null) {
            properties.put("serviceName", event.getComponent().getDescriptor().getName());
        }

        TemplateParser tp = TemplateParser.createAntStyleParser();
        soapAction = tp.parse(properties, soapAction);

        if (logger.isDebugEnabled()) {
            logger.debug("SoapAction for this call is: " + soapAction);
        }
        return soapAction;
    }

    private void setCallParams(Call call, UMOEvent event, QName method) throws ClassNotFoundException {
        if (callParameters == null) {
            loadCallParams(event, method.getNamespaceURI());
        }

        SoapMethod soapMethod;
        soapMethod = (SoapMethod) event.getMessage().removeProperty(MuleProperties.MULE_SOAP_METHOD);
        if (soapMethod == null) {
            soapMethod = (SoapMethod) callParameters.get(method.getLocalPart());
        }
        if (soapMethod != null) {
            for (Iterator iterator = soapMethod.getNamedParameters().iterator(); iterator.hasNext();) {
                NamedParameter parameter = (NamedParameter) iterator.next();
                call.addParameter(parameter.getName(), parameter.getType(), parameter.getMode());
            }

            if (soapMethod.getReturnType() != null) {
                call.setReturnType(soapMethod.getReturnType());
            } else if (soapMethod.getReturnClass() != null) {
                call.setReturnClass(soapMethod.getReturnClass());
            }
            call.setOperationName(soapMethod.getName());
        }
    }

    private void loadCallParams(UMOEvent event, String namespace) throws ClassNotFoundException {
        Map methodCalls = (Map) event.getMessage().getProperty("soapMethods");
        if (methodCalls == null) {
            return;
        }

        Map.Entry entry;
        SoapMethod soapMethod;
        callParameters = new HashMap();

        for (Iterator iterator = methodCalls.entrySet().iterator(); iterator.hasNext();) {
            entry = (Map.Entry) iterator.next();
            if (StringUtils.isEmpty(namespace)) {
                if (entry.getValue() instanceof List) {
                    soapMethod = new SoapMethod(entry.getKey().toString(), (List) entry.getValue());
                } else {
                    soapMethod = new SoapMethod(entry.getKey().toString(), entry.getValue().toString());
                }
            } else {
                if (entry.getValue() instanceof List) {
                    soapMethod = new SoapMethod(new QName(namespace, entry.getKey().toString()), (List) entry.getValue());
                } else {
                    soapMethod = new SoapMethod(new QName(namespace, entry.getKey().toString()), entry.getValue().toString());
                }
            }
            callParameters.put(soapMethod.getName().getLocalPart(), soapMethod);
        }
    }
}