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

import org.codehaus.xfire.DefaultXFire;
import org.codehaus.xfire.XFire;
import org.codehaus.xfire.service.binding.ObjectServiceFactory;
import org.mule.MuleManager;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.internal.notifications.ModelNotification;
import org.mule.impl.internal.notifications.ModelNotificationListener;
import org.mule.impl.internal.notifications.NotificationException;
import org.mule.providers.AbstractServiceEnabledConnector;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.HttpConstants;
import org.mule.umo.UMOException;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.provider.UMOMessageReceiver;

/**
 * todo document
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XFireConnector extends AbstractServiceEnabledConnector implements ModelNotificationListener
{
    public static final String XFIRE_SERVICE_COMPONENT_NAME = "_xfireServiceComponent";
    public static final String DEFAULT_MULE_NAMESPACE_URI = "http://www.muleumo.org";

    protected MuleDescriptor xfireDescriptor;

    private XFire xfire;

    private ObjectServiceFactory serviceFactory;

    public XFireConnector()
    {
        super();
        registerProtocols();
    }

    protected void registerProtocols()
    {
        registerSupportedProtocol("http");
        registerSupportedProtocol("https");
        registerSupportedProtocol("jms");
        registerSupportedProtocol("vm");
    }

    public String getProtocol()
    {
        return "xfire";
    }

    public void doInitialise() throws InitialisationException
    {
        super.doInitialise();
        try {
            MuleManager.getInstance().registerListener(this);
        }
        catch (NotificationException e) {
            throw new InitialisationException(e, this);
        }

        if (xfire == null) {
            xfire = new DefaultXFire();
        }
        if (serviceFactory == null) {
            serviceFactory = new ObjectServiceFactory(xfire.getTransportManager());
        }
        else if (serviceFactory.getTransportManager() == null) {
            serviceFactory.setTransportManager(xfire.getTransportManager());
        }
    }

    public XFire getXfire()
    {
        return xfire;
    }

    public void setXfire(XFire xfire)
    {
        this.xfire = xfire;
    }

    protected void registerReceiverWithMuleService(UMOMessageReceiver receiver, UMOEndpointURI ep)
            throws UMOException
    {
        // If this is the first receiver we need to create the Axis service
        // component
        // this will be registered with Mule when the Connector starts
        if (xfireDescriptor == null) {
            // See if the axis descriptor has already been added. This allows
            // developers to override the default configuration, say to increase
            // the threadpool
            xfireDescriptor = (MuleDescriptor)MuleManager.getInstance().getModel().getDescriptor(
                    XFIRE_SERVICE_COMPONENT_NAME);
            if (xfireDescriptor == null) {
                xfireDescriptor = createxfireDescriptor();
            }
            else {
                // Lets unregister the 'template' instance, configure it and
                // then register
                // again later
                MuleManager.getInstance().getModel().unregisterComponent(xfireDescriptor);
            }
            // if the axis server hasn't been set, set it now. The Axis server
            // may be set externally
            if (xfireDescriptor.getProperties().get("xfire") == null) {
                xfireDescriptor.getProperties().put("xfire", xfire);
            }
            xfireDescriptor.setContainerManaged(false);
        }
        String serviceName = receiver.getComponent().getDescriptor().getName();

        // No determine if the endpointUri requires a new connector to be
        // registed in the case of http we only need to register the new
        // endpointUri if
        // the port is different
        String endpoint = receiver.getEndpointURI().getAddress();

        String scheme = ep.getScheme().toLowerCase();
        boolean sync = receiver.getEndpoint().isSynchronous();
        if (scheme.equals("http") || scheme.equals("tcp")) {
            // if we are using a socket based endpointUri make sure it is
            // running synchronously by default
            sync = true;
            receiver.getEndpoint().getProperties().put(HttpConnector.HTTP_METHOD_PROPERTY, "POST");
            receiver.getEndpoint().getProperties().put(HttpConstants.HEADER_CONTENT_TYPE, "text/xml");
            endpoint += "/" + serviceName;
        }

        UMOEndpoint serviceEndpoint = new MuleEndpoint(endpoint, true);
        serviceEndpoint.setSynchronous(sync);
        serviceEndpoint.setName(ep.getScheme() + ":" + serviceName);

        // Set the transformers on the endpoint too
         serviceEndpoint.setTransformer(receiver.getEndpoint().getTransformer());
         receiver.getEndpoint().setTransformer(null);

         serviceEndpoint.setResponseTransformer(receiver.getEndpoint().getResponseTransformer());
         receiver.getEndpoint().setResponseTransformer(null);

        // set the filter on the axis endpoint on the real receiver endpoint
        serviceEndpoint.setFilter(receiver.getEndpoint().getFilter());
        // Remove the Axis filter now
        receiver.getEndpoint().setFilter(null);

        // set the Security filter on the axis endpoint on the real receiver
        // endpoint
        serviceEndpoint.setSecurityFilter(receiver.getEndpoint().getSecurityFilter());
        // Remove the Axis Receiver Security filter now
        receiver.getEndpoint().setSecurityFilter(null);
        xfireDescriptor.getInboundRouter().addEndpoint(serviceEndpoint);
    }

    protected MuleDescriptor createxfireDescriptor()
    {
        MuleDescriptor xfireDescriptor = (MuleDescriptor)MuleManager.getInstance().getModel()
                .getDescriptor(XFIRE_SERVICE_COMPONENT_NAME);
        if (xfireDescriptor == null) {
            xfireDescriptor = new MuleDescriptor(XFIRE_SERVICE_COMPONENT_NAME);
            xfireDescriptor.setImplementation(XFireServiceComponent.class.getName());
        }
        return xfireDescriptor;
    }

    public ObjectServiceFactory getServiceFactory()
    {
        return serviceFactory;
    }

    public void setServiceFactory(ObjectServiceFactory serviceFactory)
    {
        this.serviceFactory = serviceFactory;
    }

    /**
     * The method determines the key used to store the receiver against.
     *
     * @param component the component for which the endpoint is being registered
     * @param endpoint  the endpoint being registered for the component
     * @return the key to store the newly created receiver against. In this case
     *         it is the component name, which is equivilent to the Axis service
     *         name.
     */
    protected Object getReceiverKey(UMOComponent component, UMOEndpoint endpoint) {
        if(endpoint.getEndpointURI().getPort()==-1) {
            return component.getDescriptor().getName();
        } else {
            return endpoint.getEndpointURI().getAddress() + "/" + component.getDescriptor().getName();
        }
    }

    public void onNotification(UMOServerNotification event)
    {
        if (event.getAction() == ModelNotification.MODEL_STARTED) {
            // We need to register the xfire service component once the model
            // starts because
            // when the model starts listeners on components are started, thus
            // all listener
            // need to be registered for this connector before the xfire service
            // component is registered. The implication of this is that to add a
            // new service and a
            // different http port the model needs to be restarted before the
            // listener is available
            if (!MuleManager.getInstance().getModel()
                    .isComponentRegistered(XFIRE_SERVICE_COMPONENT_NAME)) {
                try {
                    // Descriptor might be null if no inbound endpoints have
                    // been register for the xfire connector
                    if (xfireDescriptor == null) {
                        xfireDescriptor = createxfireDescriptor();
                    }
                    MuleManager.getInstance().getModel().registerComponent(xfireDescriptor);
                }
                catch (UMOException e) {
                    handleException(e);
                }
            }
        }
    }

}
