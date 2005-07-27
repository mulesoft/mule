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
package org.mule.jbi.components.mule;

import org.mule.jbi.components.AbstractComponent;
import org.mule.umo.endpoint.UMOEndpoint;

import javax.xml.namespace.QName;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractEndpointComponent extends AbstractComponent {

    protected UMOEndpoint endpoint;

    protected QName targetService;

    public QName getTargetService() {
        return targetService;
    }

    public void setTargetService(QName targetService) {
        this.targetService = targetService;
    }

    public UMOEndpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(UMOEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    protected void doInit() throws Exception {
        if (endpoint == null) {
            throw new NullPointerException("A Mule endpoint must be set on this component");
        }
        if (targetService == null) {
            throw new NullPointerException("A targetService must be set on this component");
        }
    }
}
