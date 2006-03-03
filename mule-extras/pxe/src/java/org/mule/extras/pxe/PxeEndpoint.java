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
package org.mule.extras.pxe;

import javax.xml.namespace.QName;

import java.net.URISyntaxException;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class PxeEndpoint {

    private String portName;
    private QName serviceName;

    public PxeEndpoint(String value) throws URISyntaxException {
        int i = value.indexOf(":");
        int x = value.indexOf("/");
        if(x==-1) {
            throw new URISyntaxException(value, "Missing portName: " + value);
        }
        portName = value.substring(x+1);
        String local = value.substring(i+1, x);
        if(i > -1) {
            String nsUri = value.substring(0, i);
            serviceName = new QName("uri:" + nsUri, local);
        } else {
            serviceName = new QName(local);            
        }

    }

    public String getPortName() {
        return portName;
    }

    public QName getServiceName() {
        return serviceName;
    }

}
