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
*
*/
package org.mule.providers.soap.xfire;

import org.codehaus.xfire.MessageContext;
import org.codehaus.xfire.XFire;
import org.codehaus.xfire.soap.SoapTransport;
import org.codehaus.xfire.fault.XFireFault;
import org.codehaus.xfire.handler.Handler;
import org.codehaus.xfire.handler.AbstractHandler;
import org.codehaus.xfire.handler.OutMessageSender;
import org.codehaus.xfire.transport.http.HttpChannel;
import org.codehaus.xfire.transport.http.HttpTransport;
import org.codehaus.xfire.transport.http.SoapHttpTransport;
import org.codehaus.xfire.client.Client;
import org.codehaus.xfire.service.OperationInfo;
import org.codehaus.xfire.service.Service;
import org.mule.config.MuleProperties;
import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.providers.AbstractConnector;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.soap.xfire.transport.MuleLocalTransport;
import org.mule.providers.soap.xfire.transport.MuleUniversalTransport;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.transformer.TransformerException;

import javax.activation.DataHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XFireMessageDispatcher extends AbstractMessageDispatcher {

    protected XFireConnector connector;
    protected Client client;

    public XFireMessageDispatcher(AbstractConnector connector) {
        super(connector);
        this.connector = (XFireConnector) connector;

    }

    public void doDispose() {
        client = null;
    }

    protected String getMethod(UMOEvent event) throws DispatchException {
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
        return method;
    }

    protected Object[] getArgs(UMOEvent event) throws TransformerException {
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

    public UMOMessage doSend(UMOEvent event) throws Exception {
        Client client = getClient(event);

        String method = getMethod(event);
        Object[] response = client.invoke(method, getArgs(event));
        if (response != null && response.length <=1) {
            if(response.length == 1) {
                return new MuleMessage(response[0], event.getProperties());
            } else {
                return null;
            }
        } else {
            return new MuleMessage(response, event.getProperties());
        }
    }

    public void doDispatch(UMOEvent event) throws Exception {
        Client client = getClient(event);
        String method = getMethod(event);
        client.invoke(method, getArgs(event));
    }

    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception {

        String serviceName = getService(endpointUri);

        XFire xfire = connector.getXfire();
        Service service = xfire.getServiceRegistry().getService(serviceName);
        Client client = new Client(new MuleUniversalTransport(), service, endpointUri.toString());
        client.setXFire(xfire);
        client.setTimeout((int)timeout);
        client.setEndpointUri(endpointUri.toString());

        String method = (String) endpointUri.getParams().remove(MuleProperties.MULE_METHOD_PROPERTY);
        OperationInfo op = service.getServiceInfo().getOperation(method);

        Properties params = endpointUri.getUserParams();
        String args[] = new String[params.size()];
        int i = 0;
        for (Iterator iterator = params.values().iterator(); iterator.hasNext(); i++) {
            args[i] = iterator.next().toString();
        }

        UMOEndpoint ep = MuleEndpoint.getOrCreateEndpointForUri(endpointUri, UMOEndpoint.ENDPOINT_TYPE_SENDER);
        ep.initialise();

        Object[] response = client.invoke(op, args);

        if (response != null && response.length == 1) {
            return new MuleMessage(response[0], null);
        } else {
            return new MuleMessage(response, null);
        }
    }

    protected Client getClient(UMOEvent event) {
        if(client==null) {
            String serviceName = getService(event.getEndpoint().getEndpointURI());

            XFire xfire = connector.getXfire();
            Service service = xfire.getServiceRegistry().getService(serviceName);
            client = new Client(new MuleUniversalTransport(), service, event.getEndpoint().getEndpointURI().toString());

            client.setXFire(xfire);
        }
        if(client.getTimeout()!= event.getTimeout()) {
            client.setTimeout(event.getTimeout());
        }
        client.setEndpointUri(event.getEndpoint().getEndpointURI().toString());
        return client;
    }

    public Object getDelegateSession() throws UMOException {
        return null;
    }

    /**
     * Get the service that is mapped to the specified request.
     */
    protected String getService(UMOEndpointURI uri) {
        String pathInfo = uri.getPath();

        if (pathInfo == null || "".equals(pathInfo))
            return uri.getHost();

        String serviceName;

        int i = pathInfo.lastIndexOf("/");

        if (i > -1) {
            serviceName = pathInfo.substring(i+1);
        } else {
            serviceName = pathInfo;
        }

        return serviceName;
    }
}
