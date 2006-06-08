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
package org.mule.providers.dq;

import java.net.URI;
import java.util.Properties;

import org.mule.impl.endpoint.ResourceNameEndpointBuilder;
import org.mule.umo.endpoint.MalformedEndpointException;

/**
 * <code>DQEndpointBuilder</code> constructs an endpoint used by AS400 data
 * queues
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class DQEndpointBuilder extends ResourceNameEndpointBuilder
{
    protected void setEndpoint(URI uri, Properties props) throws MalformedEndpointException
    {
        String lib = (String) props.get("lib");
        if (uri.getPath().length() > 0) {
            lib = uri.getHost();
            props.setProperty("lib", lib);
            address = "/" + lib + uri.getPath();
        } else if (lib == null) {
            throw new MalformedEndpointException("Could not extract Lib name: " + uri);
        } else {
            if (!lib.startsWith("/")) {
                lib = "/" + lib;
            }
            if (!lib.endsWith("/")) {
                lib += "/";
            }
            address = lib + uri.getHost();
        }
        // Resource info
        // int x = address.indexOf(":");
        // if(x > -1) {
        // String resourceInfo = address.substring(0, x);
        // props.setProperty("resourceInfo", resourceInfo);
        // address = address.substring(x+1);
        // }
    }
}
