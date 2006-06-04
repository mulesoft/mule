/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.jbi.components;

import org.mule.MuleManager;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;

import javax.jbi.JBIException;

import java.util.Map;

/**
 * A Jbi component that has a Mule muleEndpoint component configured on it.  Both the Dispatcher
 * and Receiver components extend this component.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractEndpointComponent extends AbstractJbiComponent {

    protected UMOEndpoint muleEndpoint;

    protected String endpoint;

    protected Map endpointProperties;

    protected AbstractEndpointComponent() {
        if(!MuleManager.isInstanciated()) {
            MuleManager.getConfiguration().setEmbedded(true);
            try {
                MuleManager.getInstance().start();
            } catch (UMOException e) {
                e.printStackTrace();
            }
        }
    }

    public UMOEndpoint getMuleEndpoint() {
        return muleEndpoint;
    }

    public void setMuleEndpoint(UMOEndpoint muleEndpoint) {
        this.muleEndpoint = muleEndpoint;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Map getEndpointProperties() {
        return endpointProperties;
    }

    public void setEndpointProperties(Map endpointProperties) {
        this.endpointProperties = endpointProperties;
    }

    protected void doInit() throws JBIException {
        try {
            if (muleEndpoint == null) {
                if(endpoint ==null) {
                    throw new NullPointerException("A Mule muleEndpoint must be set on this component");
                } else {
                    muleEndpoint = new MuleEndpoint(endpoint, true);
                }
            }

            if(endpointProperties!=null) {
                muleEndpoint.getProperties().putAll(endpointProperties);
            }
            muleEndpoint.initialise();

        } catch (Exception e) {
            throw new JBIException(e);
        }
    }
}
