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
package org.mule.providers.soap;

import org.mule.impl.endpoint.UrlEndpointBuilder;
import org.mule.umo.endpoint.MalformedEndpointException;

import java.net.URI;
import java.util.Properties;

/**
 * <code>SoapEndpointBuilder</code> constructs an endpointUri using the full
 * url and sets the method property to the last path element.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SoapEndpointBuilder extends UrlEndpointBuilder
{
    protected void setEndpoint(URI uri, Properties props) throws MalformedEndpointException
    {
        address = uri.getScheme() + "://" + uri.getHost();
        if (uri.getPort() != -1)
        {
            address += ":" + uri.getPort();
        }
        address += uri.getPath();
    }
}
