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
package org.mule.extras.client;

import edu.emory.mathcs.backport.java.util.concurrent.Callable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.internal.notifications.AdminNotification;
import org.mule.impl.security.MuleCredentials;
import org.mule.providers.AbstractConnector;
import org.mule.providers.service.ConnectorFactory;
import org.mule.transformers.xml.ObjectToXml;
import org.mule.transformers.xml.XmlToObject;
import org.mule.umo.FutureMessageResult;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.umo.security.UMOCredentials;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.MuleObjectHelper;

import java.util.Map;

/**
 * <code>RemoteDispatcher</code> is used to make and recieve requests to a
 * remote Mule instance. It is used to proxy requests to Mule using the Server
 * Url as the the transport channel.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class RemoteDispatcher implements Disposable {
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(RemoteDispatcher.class);

    private UMOEndpoint serverEndpoint;

    private UMOCredentials credentials = null;

    /**
     * Calls made to a remote server are serialised using xstream
     */
    private ObjectToXml objectToXml;
    private XmlToObject xmlToObject;

    RemoteDispatcher(String endpoint, UMOCredentials credentials)
            throws UMOException {
        this(endpoint);
        this.credentials = credentials;
    }

    RemoteDispatcher(String endpoint) throws UMOException {
        serverEndpoint = new MuleEndpoint(endpoint, true);
        objectToXml = new ObjectToXml();
        xmlToObject = new XmlToObject();
    }

    /**
     * Dispatcher an event asynchronously to a components on a remote Mule
     * instance. Users can endpoint a url to a remote Mule server in the
     * constructor of a Mule client, by default the default Mule server url
     * tcp://localhost:60504 is used.
     *
     * @param component         the name of the Mule components to dispatch to
     * @param payload           the object that is the payload of the event
     * @param messageProperties any properties to be associated with the
     *                          payload. as null
     * @throws org.mule.umo.UMOException if the dispatch fails or the components
     *                                   or transfromers cannot be found
     */
    public void dispatchToRemoteComponent(String component, Object payload, Map messageProperties) throws UMOException {
        doToRemoteComponent(component, payload, messageProperties, true);
    }

    /**
     * sends an event synchronously to a components on a remote Mule instance.
     * Users can endpoint a url to a remote Mule server in the constructor of a
     * Mule client, by default the default Mule server url tcp://localhost:60504
     * is used.
     *
     * @param component         the name of the Mule components to send to
     * @param payload           the object that is the payload of the event
     * @param messageProperties any properties to be associated with the
     *                          payload. as null
     * @return the result message if any of the invocation
     * @throws org.mule.umo.UMOException if the dispatch fails or the components
     *                                   or transfromers cannot be found
     */
    public UMOMessage sendToRemoteComponent(String component, Object payload, Map messageProperties)
            throws UMOException {
        return doToRemoteComponent(component, payload, messageProperties, true);
    }

    /**
     * sends an event to a components on a remote Mule instance, while making
     * the result of the event trigger available as a Future result that can be
     * accessed later by client code. Users can endpoint a url to a remote Mule
     * server in the constructor of a Mule client, by default the default Mule
     * server url tcp://localhost:60504 is used.
     *
     * @param component         the name of the Mule components to send to
     * @param transformers      a comma separated list of transformers to apply to
     *                          the result message
     * @param payload           the object that is the payload of the event
     * @param messageProperties any properties to be associated with the
     *                          payload. as null
     * @return the result message if any of the invocation
     * @throws org.mule.umo.UMOException if the dispatch fails or the components
     *                                   or transfromers cannot be found
     */
    public FutureMessageResult sendAsyncToRemoteComponent(final String component,
                                                          String transformers,
                                                          final Object payload,
                                                          final Map messageProperties) throws UMOException {
        FutureMessageResult result = null;
        UMOTransformer trans = null;

        Callable callable = new Callable() {
            public Object call() throws Exception {
                return doToRemoteComponent(component, payload, messageProperties, true);
            }
        };

        if (transformers != null) {
            trans = MuleObjectHelper.getTransformer(transformers, ",");
            result = new FutureMessageResult(callable, trans);
        } else {
            result = new FutureMessageResult(callable);
        }

        result.execute();
        return result;
    }

    public UMOMessage sendRemote(String endpoint, Object payload, Map messageProperties, int timeout) throws UMOException {
        return doToRemote(endpoint, payload, messageProperties, true, timeout);
    }

    public UMOMessage sendRemote(String endpoint, Object payload, Map messageProperties) throws UMOException {
        return doToRemote(endpoint, payload, messageProperties, true, MuleManager.getConfiguration().getSynchronousEventTimeout());
    }

    public void dispatchRemote(String endpoint, Object payload, Map messageProperties) throws UMOException {
        doToRemote(endpoint, payload, messageProperties, false, -1);
    }

    public FutureMessageResult sendAsyncRemote(final String endpoint, final Object payload, final Map messageProperties)
            throws UMOException {
        Callable callable = new Callable() {
            public Object call() throws Exception {
                return doToRemote(endpoint, payload, messageProperties, true, -1);
            }
        };

        FutureMessageResult result = new FutureMessageResult(callable);
        result.execute();
        return result;
    }

    public UMOMessage receiveRemote(String endpoint, int timeout) throws UMOException {
        AdminNotification action = new AdminNotification(null, AdminNotification.ACTION_RECEIVE, endpoint);
        action.setProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, "true");
        action.setProperty(MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY, new Long(timeout));
        UMOMessage result = dispatchAction(action, true, timeout);
        return result;
    }

    public FutureMessageResult asyncReceiveRemote(final String endpoint, final int timeout) throws UMOException {
        Callable callable = new Callable() {
            public Object call() throws Exception {
                return receiveRemote(endpoint, timeout);
            }
        };

        FutureMessageResult result = new FutureMessageResult(callable);
        result.execute();
        return result;
    }

    protected UMOMessage doToRemoteComponent(String component,
                                             Object payload,
                                             Map messageProperties,
                                             boolean synchronous) throws UMOException {
        UMOMessage message = new MuleMessage(payload, messageProperties);
        message.setBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, synchronous);
        setCredentials(message);
        AdminNotification action = new AdminNotification(message, AdminNotification.ACTION_INVOKE, "mule://" + component);
        UMOMessage result = dispatchAction(action, synchronous, MuleManager.getConfiguration()
                .getSynchronousEventTimeout());
        return result;
    }

    protected UMOMessage doToRemote(String endpoint, Object payload, Map messageProperties, boolean synchronous, int timeout)
            throws UMOException {
        UMOMessage message = new MuleMessage(payload, messageProperties);
        message.setProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, String.valueOf(synchronous));
        setCredentials(message);
        AdminNotification action = new AdminNotification(message,
                (synchronous ? AdminNotification.ACTION_SEND : AdminNotification.ACTION_DISPATCH),
                endpoint);

        UMOMessage result = dispatchAction(action, synchronous, timeout);
        return result;
    }

    protected UMOMessage dispatchAction(AdminNotification action, boolean synchronous, int timeout) throws UMOException {

        UMOEndpoint endpoint = ConnectorFactory.createEndpoint(serverEndpoint.getEndpointURI(), UMOEndpoint.ENDPOINT_TYPE_SENDER);
        endpoint.setRemoteSync(synchronous);

        String xml = (String) objectToXml.transform(action);

        UMOMessage message = null;
        if (action.getMessage() == null) {
            message = new MuleMessage(xml);
        } else {
            message = new MuleMessage(xml, action.getMessage());
        }

        message.addProperties(action.getProperties());
        MuleSession session = new MuleSession(message, ((AbstractConnector) endpoint.getConnector()).getSessionHandler());

        UMOEvent event = new MuleEvent(message, endpoint, session, true);
        event.setTimeout(timeout);
        if (logger.isDebugEnabled()) {
            logger.debug("MuleClient sending remote call to: " + action.getResourceIdentifier() + ". At "
                    + serverEndpoint.toString() + " .Event is: " + event);
        }

        UMOMessageDispatcher dispatcher = endpoint.getConnector().getDispatcher(serverEndpoint);

        UMOMessage result = null;

        try {
            if (synchronous) {
                result = dispatcher.send(event);
            } else {
                dispatcher.dispatch(event);
                return null;
            }
            if (result != null) {
                String resultXml = result.getPayloadAsString();
                if (resultXml != null && resultXml.length() > 0) {

                    Object obj = xmlToObject.transform(resultXml);
                    if (obj instanceof AdminNotification) {
                        result = ((AdminNotification) obj).getMessage();
                    } else {
                        result = (UMOMessage) obj;
                    }
                } else {
                    return result;
                }
            }


        } catch (Exception e) {
            throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Result of MuleClient remote call is: " + (result == null ? "null" : result.getPayload()));
        }

        return result;
    }

    public void dispose() {
        // template method
    }

    protected void setCredentials(UMOMessage message) {
        if (credentials != null) {
            message.setProperty(MuleProperties.MULE_USER_PROPERTY, MuleCredentials.createHeader(credentials.getUsername(), credentials.getPassword()));
        }
    }
}
