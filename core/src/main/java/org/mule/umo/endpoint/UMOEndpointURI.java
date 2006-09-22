/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.umo.endpoint;

import java.io.Serializable;
import java.net.URI;
import java.util.Properties;

/**
 * <code>UMOEndpointURI</code> is used to determine how a message is sent or
 * received. The url defines the protocol, the endpointUri destination of the
 * message and optionally the endpoint to use when dispatching the event. Mule
 * urls take the form of -
 * 
 * protocol://[host]:[port]/[provider]/endpointUri or
 * protocol://[host]:[port]/endpointUri i.e.
 * 
 * vm://localhost/vmProvider/my.object or vm://my.object
 * 
 * The protocol can be any of any conector registered with Mule. The endpoint
 * name if specified must be the name of a register global endpoint
 * 
 * The endpointUri can be any endpointUri recognised by the endpoint type.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOEndpointURI extends Serializable
{
    public static final String PROPERTY_ENDPOINT_NAME = "endpointName";
    public static final String PROPERTY_ENDPOINT_URI = "address";
    public static final String PROPERTY_CREATE_CONNECTOR = "createConnector";
    public static final String PROPERTY_CONNECTOR_NAME = "connector";
    public static final String PROPERTY_TRANSFORMERS = "transformers";
    public static final String PROPERTY_RESPONSE_TRANSFORMERS = "responseTransformers";
    
    String getAddress();

    String getFilterAddress();

    String getEndpointName();

    void setEndpointName(String name);

    Properties getParams();

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

    int getCreateConnector();

    URI getUri();

    String getConnectorName();

    String getResourceInfo();

    String getUsername();

    String getPassword();
}
