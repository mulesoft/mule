/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.endpoint;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.Initialisable;

import java.io.Serializable;
import java.net.URI;
import java.util.Properties;

/**
 * <code>EndpointURI</code> is used to determine how a message is sent or
 * received. The url defines the protocol, the endpointUri destination of the message
 * and optionally the endpoint to use when dispatching the event. Mule urls take the
 * form of - protocol://[host]:[port]/[provider]/endpointUri or
 * protocol://[host]:[port]/endpointUri i.e. vm://my.object or
 * The protocol can be any of any conector registered with Mule. The
 * endpoint name if specified must be the name of a register global endpoint The
 * endpointUri can be any endpointUri recognised by the endpoint type.
 * 
 */
public interface EndpointURI extends Serializable, Initialisable
{

    String PROPERTY_ENDPOINT_NAME = "endpointName";
    String PROPERTY_ENDPOINT_URI = "address";
    String PROPERTY_CONNECTOR_NAME = "connector";
    String PROPERTY_TRANSFORMERS = "transformers";
    String PROPERTY_RESPONSE_TRANSFORMERS = "responseTransformers";

    String getAddress();

    String getFilterAddress();

    /** 
     * @deprecated This property is used by endpoint-identifiers, global-endpoints use ImmutableEndpoint.getName()
     */
    @Deprecated
    String getEndpointName();

    Properties getParams();

    /**
     * A reduced version of getParams() - drops some system parameters
     */
    Properties getUserParams();

    String getScheme();

    String getSchemeMetaInfo();

    String getFullScheme();

    String getAuthority();

    String getHost();

    int getPort();

    String getPath();

    String getQuery();

    String getUserInfo();

    String getTransformers();

    String getResponseTransformers();

    URI getUri();

    String getConnectorName();

    String getResourceInfo();

    String getUser();

    String getPassword();

    MuleContext getMuleContext();
}
