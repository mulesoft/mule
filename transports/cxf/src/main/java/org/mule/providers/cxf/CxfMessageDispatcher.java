/*
 * $Id: CxfMessageDispatcher.java 6446 2007-05-10 09:33:21Z dirk.olmes $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.cxf;

import org.mule.config.MuleProperties;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.cxf.support.MuleHeadersInInterceptor;
import org.mule.providers.cxf.support.MuleHeadersOutInterceptor;
import org.mule.providers.soap.i18n.SoapMessages;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.transformer.TransformerException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.ChainInitiationObserver;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.MessageObserver;

/**
 * The CxfMessageDispatcher is used for making Soap client requests to remote
 * services.
 */
public class CxfMessageDispatcher extends AbstractMessageDispatcher
{

    private static final String URI_REGEX = "cxf:\\[(.+?)\\]:(.+?)/\\[(.+?)\\]:(.+?)";
    Pattern URI_PATTERN = Pattern.compile(URI_REGEX);

    // Since the MessageDispatcher is guaranteed to serve a single thread,
    // the Dispatcher can own the xfire Client as an instance variable
    protected Client client = null;
    protected final CxfConnector connector;

    public CxfMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (CxfConnector) endpoint.getConnector();
    }

    /*
     * We need a way to associate an endpoint with a specific CXF service and
     * operation, and the most sensible way to accomplish that is to overload URI
     * syntax: cxf:[service_URI]:service_localname/[ep_URI]:ep_localname And the map
     * method to operation
     */
    protected void doConnect() throws Exception
    {
        if (client == null)
        {
            final Bus bus = connector.getCxfBus();

            String uri = getEndpoint().getEndpointURI().toString();
            int idx = uri.indexOf('?');
            if (idx != -1)
            {
                uri = uri.substring(0, idx);
            }

            EndpointInfo ei = new EndpointInfo();
            ei.setAddress(uri);

            DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
            DestinationFactory df = dfm.getDestinationFactoryForUri(uri);
            if (df == null)
            {
                throw new Exception("Could not find a destination factory for uri " + uri);
            }

            Destination dest = df.getDestination(ei);
            try
            {
                MessageObserver mo = dest.getMessageObserver();
                if (mo instanceof ChainInitiationObserver)
                {
                    ChainInitiationObserver cMo = (ChainInitiationObserver) mo;
                    Endpoint cxfEP = cMo.getEndpoint();

                    client = new ClientImpl(bus, cxfEP);
                    client.getInInterceptors().add(new MuleHeadersInInterceptor());
                    client.getInFaultInterceptors().add(new MuleHeadersInInterceptor());
                    client.getOutInterceptors().add(new MuleHeadersOutInterceptor());
                    client.getOutFaultInterceptors().add(new MuleHeadersOutInterceptor());
                }
                else
                {
                    throw new Exception(
                        "Could not create client! No Server was found directly on the endpoint: " + uri);
                }
            }
            catch (Exception ex)
            {
                disconnect();
                throw ex;
            }
        }
    }

    protected void doDisconnect() throws Exception
    {
        client = null;
    }

    protected void doDispose()
    {
        // nothing to do
    }

    protected String getMethod(UMOEvent event) throws DispatchException
    {
        // @TODO: Which of these *really* matter?
        String method = (String) event.getMessage().getProperty(MuleProperties.MULE_METHOD_PROPERTY);

        if (method == null)
        {
            UMOEndpointURI endpointUri = event.getEndpoint().getEndpointURI();
            method = (String) endpointUri.getParams().get(MuleProperties.MULE_METHOD_PROPERTY);
        }

        if (method == null)
        {
            method = (String) event.getEndpoint().getProperties().get(MuleProperties.MULE_METHOD_PROPERTY);
        }

        if (method == null)
        {
            throw new DispatchException(SoapMessages.cannotInvokeCallWithoutOperation(), event.getMessage(),
                event.getEndpoint());
        }

        return method;
    }

    @SuppressWarnings("unchecked")
    protected Object[] getArgs(UMOEvent event) throws TransformerException
    {
        Object payload = event.getTransformedMessage();
        Object[] args;

        if (payload instanceof Object[])
        {
            args = (Object[]) payload;
        }
        else
        {
            args = new Object[]{payload};
        }

        UMOMessage message = event.getMessage();
        Set attachmentNames = message.getAttachmentNames();
        if (attachmentNames != null && !attachmentNames.isEmpty())
        {
            List<DataHandler> attachments = new ArrayList<DataHandler>();
            for (Iterator i = attachmentNames.iterator(); i.hasNext();)
            {
                attachments.add(message.getAttachment((String) i.next()));
            }
            List<Object> temp = new ArrayList<Object>(Arrays.asList(args));
            temp.add(attachments.toArray(new DataHandler[0]));
            args = temp.toArray();
        }

        return args;
    }

    protected UMOMessage doSend(UMOEvent event) throws Exception
    {
        ((ClientImpl) client).setSynchronousTimeout(event.getTimeout());
        String method = getMethod(event);

        // Set custom soap action if set on the event or endpoint
        // String soapAction =
        // (String)event.getMessage().getProperty(SoapConstants.SOAP_ACTION_PROPERTY);
        // if (soapAction != null)
        // {
        // soapAction = parseSoapAction(soapAction, new QName(method), event);
        // this.client.setProperty(org.codehaus.xfire.soap.SoapConstants.SOAP_ACTION,
        // soapAction);
        // }

        Map<String, Object> exProps = new HashMap<String, Object>();
        exProps.put(MuleProperties.MULE_EVENT_PROPERTY, event);

        // Set Custom Headers on the client
        Object[] arr = event.getMessage().getPropertyNames().toArray();
        String head;

        for (int i = 0; i < arr.length; i++)
        {
            head = (String) arr[i];
            if ((head != null) && (!head.startsWith("MULE")))
            {
                exProps.put((String) arr[i], event.getMessage().getProperty((String) arr[i]));
            }
        }

        // Normally its not this hard to invoke the CXF Client, but we're
        // sending along some exchange properties, so we need to use a more advanced
        // method
        Endpoint ep = client.getEndpoint();
        QName q = new QName(ep.getService().getName().getNamespaceURI(), method);
        BindingOperationInfo bop = ep.getBinding().getBindingInfo().getOperation(q);
        if (bop == null)
        {
            throw new Exception("No such operation: " + method);
        }

        if (bop.isUnwrappedCapable())
        {
            bop = bop.getUnwrappedOperation();
        }

        Object[] response = client.invoke(bop, getArgs(event), exProps);

        UMOMessage result = null;
        if (response != null && response.length <= 1)
        {
            if (response.length == 1)
            {
                result = new MuleMessage(response[0], event.getMessage());
            }
        }
        else
        {
            result = new MuleMessage(response, event.getMessage());
        }

        return result;
    }

    protected void doDispatch(UMOEvent event) throws Exception
    {
        ((ClientImpl) this.client).setSynchronousTimeout(event.getTimeout());
        this.client.invoke(getMethod(event), getArgs(event));
    }

    /**
     * Make a specific request to the underlying transport
     * 
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    @SuppressWarnings("unchecked")
    protected UMOMessage doReceive(long timeout) throws Exception
    {
        ((ClientImpl) client).setSynchronousTimeout((int) timeout);

        String method = (String) endpoint.getProperty(MuleProperties.MULE_METHOD_PROPERTY);

        Properties params = endpoint.getEndpointURI().getUserParams();
        Object args[] = new Object[params.size()];
        int i = 0;
        for (Iterator iterator = params.values().iterator(); iterator.hasNext(); i++)
        {
            args[i] = iterator.next().toString();
        }

        Object[] response = client.invoke(method, args);

        if (response != null && response.length == 1)
        {
            return new MuleMessage(response[0]);
        }
        else
        {
            return new MuleMessage(response);
        }
    }

}
