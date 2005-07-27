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
package org.mule.jbi.binding.local;

import org.mule.impl.endpoint.AbstractEndpointBuilder;
import org.mule.umo.endpoint.MalformedEndpointException;

import java.net.URI;
import java.util.Properties;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ServiceEndpointBuilder extends AbstractEndpointBuilder
{
    protected void setEndpoint(URI uri, Properties properties) throws MalformedEndpointException {
        String temp = uri.getSchemeSpecificPart().substring(2);
        int i = temp.indexOf("/");
        int x = temp.indexOf(":");
        if(i > -1) {
            address = temp.substring(i + 1, temp.length());
        } else if(x > -1) {
            address = temp.substring(x + 1, temp.length());
        } else {
            address = temp;
        }

        String localName = address;
        if(x > -1) {
            String prefix = temp.substring(0, x);
            //todo lookup Uri on prefix
            String nsUri = "http://www.mulejbi.org";
            if(i > -1) {
                localName = temp.substring(x+1, i);
            }
            properties.setProperty("namespaceUri", nsUri);
            properties.setProperty("namespacePrefix", prefix);
        }
        properties.setProperty("serviceName", localName);
    }
}
