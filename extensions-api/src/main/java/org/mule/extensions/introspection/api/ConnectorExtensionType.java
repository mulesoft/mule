/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.introspection.api;

/**
 * A {@link org.mule.extensions.introspection.api.ConnectorExtensionType} that defines a
 * connector. A connector is an extension which adds the capability to connect to an external system.
 * That means that the connector will hold some kind of connection to that system (either stateful or stateless)
 * and will include some kind of reconnection behavior.
 * <p/>
 * Additionally, a connector might define inbound endpoints to allow connections from extenral systems as well
 *
 * @since 1.0
 */
class ConnectorExtensionType extends MuleExtensionType
{

    protected static final ConnectorExtensionType INSTANCE = new ConnectorExtensionType();

    private ConnectorExtensionType()
    {
    }
}
