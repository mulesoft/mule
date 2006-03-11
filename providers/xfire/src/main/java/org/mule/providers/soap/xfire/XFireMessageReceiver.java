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

import org.codehaus.xfire.service.Service;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;

import java.util.HashMap;
import java.util.Map;

/**
 * todo document
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XFireMessageReceiver extends AbstractMessageReceiver
{

    protected XFireConnector connector;
    protected Service service;

    public XFireMessageReceiver(UMOConnector umoConnector,
                                UMOComponent component,
                                UMOEndpoint umoEndpoint) throws InitialisationException
    {
        super(umoConnector, component, umoEndpoint);
        connector = (XFireConnector)umoConnector;
        init();
    }

    protected void init() throws InitialisationException
    {
        try {
            // TODO MULE20 get namespace from service name
            String namespace = XFireConnector.DEFAULT_MULE_NAMESPACE_URI;

            Map props = new HashMap(component.getDescriptor().getProperties());
            props.putAll(endpoint.getProperties());

            // String soapVersionString =
            // PropertiesHelper.getStringProperty(props, "soapVersion", "1.1");
            //
            // SoapVersion version = null;
            // if(soapVersionString.equals("1.2")) {
            // version = new Soap12();
            // } else if(soapVersionString.equals("1.1")) {
            // version = new Soap11();
            // } else {
            // throw new InitialisationException(new Message("xfire", 1,
            // version), this);
            // }

            if (props.size() == 0) {
                // Xfire checks that properties are null rather than empty
                props = null;
            }
            else {
                rewriteProperty(props, "portType");
                rewriteProperty(props, "style");
                rewriteProperty(props, "use");
                rewriteProperty(props, "createDefaultBindings");
                rewriteProperty(props, "soap12Transports");
                rewriteProperty(props, "soap11Transports");
                rewriteProperty(props, "scope");
                rewriteProperty(props, "schemas");
            }

            service = connector.getServiceFactory().create(
                    component.getDescriptor().getImplementationClass(),
                    component.getDescriptor().getName(), namespace, props);

            boolean sync = endpoint.isSynchronous();
            // default to synchronous if using http
            if (endpoint.getEndpointURI().getScheme().startsWith("http")) {
                sync = true;
            }
            service.setInvoker(new MuleInvoker(this, sync));

        }
        catch (UMOException e) {
            throw new InitialisationException(e, this);
        }
    }

    protected void rewriteProperty(Map props, String name)
    {
        Object temp = null;
        if (props.containsKey(name)) {
            temp = props.remove(name);
            props.put("objectServiceFactory." + name, temp);
        }
    }

    public void doConnect() throws Exception
    {
        // Tell the Xfire registry about our new service.
        connector.getXfire().getServiceRegistry().register(service);
        connector.registerReceiverWithMuleService(this, endpoint.getEndpointURI());
    }

    public void doDisconnect() throws Exception
    {
        connector.getXfire().getServiceRegistry().unregister(service);
    }
}
