/*
 * $Header:
/home/projects/mule/scm/mule/providers/soap/src/java/org/mule/providers/soap/axis/AxisMessageDispatcher.java,v
1.9 2005/06/09 21:15:40 gnt Exp $
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

import org.apache.axis.*;
import org.apache.axis.client.AxisClient;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.constants.Style;
import org.apache.axis.constants.Use;
import org.apache.axis.wsdl.gen.Parser;
import org.apache.axis.wsdl.symbolTable.ServiceEntry;
import org.apache.axis.wsdl.symbolTable.SymTabEntry;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.NullPayload;
import org.mule.providers.soap.NamedParameter;
import org.mule.providers.soap.SoapMethod;
import org.mule.providers.soap.axis.extensions.MuleSoapHeadersHandler;
import org.mule.providers.soap.axis.extensions.UniversalSender;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.BeanUtils;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPEnvelope;
import java.util.*;

/**
 * <code>AxisMessageDispatcher</code> is used to make soap requests via the
 * Axis soap client.
 * 
 *  <at> author <a href="mailto:ross.mason@...">Ross Mason</a>
 *  <at> version $Revision$
 */
public class AxisMessageDispatcher extends AbstractMessageDispatcher
{
    private Map services;

    private Map callParameters;

    protected SimpleProvider clientConfig;

    public AxisMessageDispatcher(AxisConnector connector) throws UMOException {
        super(connector);
        services = new HashMap();
        //Should be loading this from a WSDD but for some reason it is not working for me??
        createClientConfig();
    }

    public void doDispose()
    {
    }

    protected synchronized Service getService(UMOEvent event) throws Exception 
    {
        String wsdlUrl = getWsdlUrl(event);
        Service service = (Service) services.get(wsdlUrl);
        if (service == null) {
            service = createService(event);
            services.put(wsdlUrl, service);
        }
        return service;
    }

    protected void createClientConfig() {
        clientConfig = new SimpleProvider();
        Handler muleHandler = new MuleSoapHeadersHandler();
        SimpleChain reqHandler = new SimpleChain();
        SimpleChain respHandler = new SimpleChain();
        reqHandler.addHandler(muleHandler);
        respHandler.addHandler(muleHandler);
        Handler pivot = new UniversalSender();
        Handler transport = new SimpleTargetedChain(reqHandler, pivot, respHandler);
        clientConfig.deployTransport("MuleTransport", transport);
    }

    protected Service createService(UMOEvent event) throws Exception
    {
        String wsdlUrl = getWsdlUrl(event);
        // If an wsdl url is given use it 
        if (wsdlUrl.length() > 0) {
        	// Parse the wsdl
	        Parser parser = new Parser();
	        parser.run(wsdlUrl);
	        // Retrieves the defined services
	        Map map = parser.getSymbolTable().getHashMap();
	        List entries = new ArrayList();
	        for (Iterator it = map.entrySet().iterator(); it.hasNext();) {
	            Map.Entry entry = (Map.Entry) it.next();
	            Vector v = (Vector) entry.getValue();
	            for (Iterator it2 = v.iterator(); it2.hasNext();) {
	                SymTabEntry e = (SymTabEntry) it2.next();
	                if (ServiceEntry.class.isInstance(e)) {
	                    entries.add(entry.getKey());
	                }
	            }
	        }
	        // Currently, only one service should be defined
	        if (entries.size() != 1) {
	            throw new Exception("Need one and only one service entry, found " + entries.size());
	        }
	        // Create the axis service
	        Service service = new Service(parser, (QName) entries.get(0));
	        service.setEngineConfiguration(clientConfig);
	        service.setEngine(new AxisClient(clientConfig));
	        return service;
        } else {
        	// Create a simple axis service without wsdl
	        Service service = new Service();
	        service.setEngineConfiguration(clientConfig);
	        service.setEngine(new AxisClient(clientConfig));
	        return service;
        }
    }

    protected String getWsdlUrl(UMOEvent event)
    {
        Object wsdlUrlProp = event.getProperties().get(AxisConnector.WSDL_URL_PROPERTY);
        String wsdlUrl = "";
        if (wsdlUrlProp != null) {
            wsdlUrl = wsdlUrlProp.toString();
        }
        return wsdlUrl;
    }

    public void doDispatch(UMOEvent event) throws Exception
    {
        AxisProperties.setProperty("axis.doAutoTypes", "true");
        Call call = getCall(event);
        // dont use invokeOneWay here as we are already in a thread pool.
        // Axis creates a new thread for every invoke one way call. nasty!
        // Mule overides the default Axis HttpSender to return immediately if
        //the axis.one.way property is set
        Object[] args = getArgs(event);
        call.setProperty("axis.one.way", Boolean.TRUE);
        call.setProperty(MuleProperties.MULE_EVENT_PROPERTY, event);
        call.invoke(args);

    }

    public UMOMessage doSend(UMOEvent event) throws Exception
    {
        AxisProperties.setProperty("axis.doAutoTypes", "true");
        Call call = getCall(event);

        Object[] args = getArgs(event);
        Object result = call.invoke(args);
        if (result == null) {
            return null;
        } else {
            UMOMessage resultMessage = new MuleMessage(result, event.getProperties());
            setMessageContextProperties(resultMessage, call.getMessageContext());
            return resultMessage;
        }
    }

    private Call getCall(UMOEvent event) throws Exception
    {
        UMOEndpointURI endpointUri = event.getEndpoint().getEndpointURI();
        String method = (String) endpointUri.getParams().remove("method");

        if (method == null) {
            method = (String) event.getEndpoint().getProperties().get("method");
            if (method == null) {
                throw new DispatchException(new org.mule.config.i18n.Message("soap", 4),
                                            event.getMessage(),
                                            event.getEndpoint());
            }
        }

        Call call = (Call) getService(event).createCall();


        String style = (String) event.getProperties().get("style");
        String use = (String) event.getProperties().get("use");

        // Note that Axis has specific rules to how these two variables are
        // combined. This is handled for us
        // Set style: RPC/wrapped/Doc/Message
        if (style != null) {
            Style s = Style.getStyle(style);
            if(s==null) {
                throw new IllegalArgumentException(new org.mule.config.i18n.Message(Messages.X_IS_INVALID, "style=" + style).toString());
            } else {
                call.setOperationStyle(s);
            }
        }
        // Set use: Endcoded/Literal
        if (use != null) {
            Use u = Use.getUse(use);
            if(u==null) {
                throw new IllegalArgumentException(new org.mule.config.i18n.Message(Messages.X_IS_INVALID, "use=" + use).toString());
            } else {
                call.setOperationUse(u);
            }
        }

        // set properties on the call from the endpoint properties
        BeanUtils.populateWithoutFail(call, event.getEndpoint().getProperties(), false);
        call.setTargetEndpointAddress(endpointUri.toString());

        //Set custom soap action if set on the event or endpoint
        String soapAction = (String)event.getProperty("soapAction");
        if(soapAction != null) {
            soapAction = parseSoapAction(soapAction, method, event);
            call.setSOAPActionURI(soapAction);
            call.setUseSOAPAction(Boolean.TRUE.booleanValue());
        }

        //Set a custome method namespace if one is set.  This will be used forthe parameters too
        String methodNamespace = (String)event.getProperty(AxisConnector.METHOD_NAMESPACE_PROPERTY);
        if(methodNamespace!=null) {
            call.setOperationName(new QName(methodNamespace, method));
        } else {
            call.setOperationName(new QName(method));
        }

        // set Mule event here so that handlers can extract info
        call.setProperty(MuleProperties.MULE_EVENT_PROPERTY, event);
        // Set timeout
        int timeout = event.getIntProperty(MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY, -1);
        if (timeout >= 0) {
            call.setTimeout(new Integer(timeout));
        }
        // Add User Creds
        if (endpointUri.getUserInfo() != null) {
            call.setUsername(endpointUri.getUsername());
            call.setPassword(endpointUri.getPassword());
        }

        setCallParams(call, event, call.getOperationName());
        return call;
    }

    private Object[] getArgs(UMOEvent event) throws TransformerException
    {
        Object payload = event.getTransformedMessage();
        Object[] args;
        if (payload instanceof Object[]) {
            args = (Object[]) payload;
        } else {
            args = new Object[] { payload };
        }
        return args;
    }

    private void setMessageContextProperties(UMOMessage message, MessageContext ctx)
    {
        Object temp = ctx.getProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
        if (temp != null && !"".equals(temp.toString())) {
            message.setCorrelationId(temp.toString());
        }
        temp = ctx.getProperty(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY);
        if (temp != null && !"".equals(temp.toString())) {
            message.setCorrelationGroupSize(Integer.parseInt(temp.toString()));
        }
        temp = ctx.getProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY);
        if (temp != null && !"".equals(temp.toString())) {
            message.setCorrelationSequence(Integer.parseInt(temp.toString()));
        }
        temp = ctx.getProperty(MuleProperties.MULE_REPLY_TO_PROPERTY);
        if (temp != null && !"".equals(temp.toString())) {
            message.setReplyTo(temp.toString());
        }
    }

    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception
    {
        Service service = new Service();
        service.setEngineConfiguration(clientConfig);
        service.setEngine(new AxisClient(clientConfig));
        Call call = new Call(service);
        call.setSOAPActionURI(endpointUri.toString());
        call.setTargetEndpointAddress(endpointUri.toString());

        String method = (String) endpointUri.getParams().remove("method");
        call.setOperationName(method);
        Properties params = endpointUri.getUserParams();
        Object args[] = new Object[params.size()];
        int i = 0;
        for (Iterator iterator = params.values().iterator(); iterator.hasNext(); i++) {
            args[i] = iterator.next();
        }

        call.setOperationName(method);
        Object result = call.invoke(method, args);
        return createMessage(result, call);
    }

    public UMOMessage receive(String endpoint, Object[] args) throws Exception
    {
        Service service = new Service();
        service.setEngineConfiguration(clientConfig);
        service.setEngine(new AxisClient(clientConfig));
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

    public UMOMessage receive(String endpoint, SOAPEnvelope envelope) throws Exception
    {
        Service service = new Service();
        service.setEngineConfiguration(clientConfig);
        service.setEngine(new AxisClient(clientConfig));
        Call call = new Call(service);

        call.setSOAPActionURI(endpoint);
        call.setTargetEndpointAddress(endpoint);
        Object result = call.invoke(new Message(envelope));
        return createMessage(result, call);
    }

    protected UMOMessage createMessage(Object result, Call call)
    {
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
        return new MuleMessage(result, props);
    }

    public Object getDelegateSession() throws UMOException
    {
        return null;
    }

    public String parseSoapAction(String soapAction, String method, UMOEvent event) {
        if(soapAction.indexOf("${") > -1)
        {
            UMOEndpointURI endpointURI = event.getEndpoint().getEndpointURI();
            soapAction = soapAction.replaceFirst("\\$\\{method\\}", method);
            soapAction = soapAction.replaceFirst("\\$\\{address\\}", endpointURI.getAddress());
            soapAction = soapAction.replaceFirst("\\$\\{scheme\\}", endpointURI.getScheme());
            soapAction = soapAction.replaceFirst("\\$\\{host\\}", endpointURI.getHost());
            soapAction = soapAction.replaceFirst("\\$\\{port\\}", String.valueOf(endpointURI.getPort()));
            soapAction = soapAction.replaceFirst("\\$\\{path\\}", endpointURI.getPath());
            soapAction = soapAction.replaceFirst("\\$\\{hostInfo\\}", endpointURI.getScheme() + "://" + endpointURI.getHost() +
                    (endpointURI.getPort() > -1 ? ":" + String.valueOf(endpointURI.getPort()) : ""));
            soapAction = soapAction.replaceFirst("\\$\\{serviceName\\}", event.getComponent().getDescriptor().getName());
        }
        if(logger.isDebugEnabled()) {
            logger.debug("SoapAction for this call is: " + soapAction);
        }
        return soapAction;
    }

    private void setCallParams(Call call, UMOEvent event, QName method) throws ClassNotFoundException {
        if(callParameters==null) {
            loadCallParams(event, method.getNamespaceURI());
        }

        SoapMethod soapMethod;
        soapMethod = (SoapMethod)event.removeProperty(MuleProperties.MULE_SOAP_METHOD);
        if(soapMethod==null) {
            soapMethod = (SoapMethod)callParameters.get(method);
        }
        if(soapMethod!=null) {
            for (Iterator iterator = soapMethod.getNamedParameters().iterator(); iterator.hasNext();) {
                NamedParameter parameter = (NamedParameter) iterator.next();
                call.addParameter(parameter.getName(), parameter.getType(), parameter.getMode());
            }
            if(soapMethod.getReturnType()!=null) {
                call.setReturnType(soapMethod.getReturnType());
            }
            if(soapMethod.getReturnClass()!=null) {
                call.setReturnClass(soapMethod.getReturnClass());
            }
        }
    }

    private void loadCallParams(UMOEvent event, String namespace) throws ClassNotFoundException {
        callParameters = new HashMap();
        Map methodCalls = (Map)event.getProperty("soapMethods");
        if(methodCalls==null) return;

        Map.Entry entry;
        SoapMethod soapMethod;
        for (Iterator iterator = methodCalls.entrySet().iterator(); iterator.hasNext();) {
            entry = (Map.Entry)iterator.next();

            if("".equals(namespace) || namespace==null) {
                if(entry.getValue() instanceof List) {
                    soapMethod = new SoapMethod(entry.getKey().toString(), (List)entry.getValue());
                } else {
                    soapMethod = new SoapMethod(entry.getKey().toString(), entry.getValue().toString());
                }
            } else {
                if(entry.getValue() instanceof List) {
                    soapMethod = new SoapMethod(new QName(namespace, entry.getKey().toString()), (List)entry.getValue());
                } else {
                    soapMethod = new SoapMethod(new QName(namespace, entry.getKey().toString()), entry.getValue().toString());
                }
            }
            callParameters.put(soapMethod.getName(), soapMethod);
        }
    }
}