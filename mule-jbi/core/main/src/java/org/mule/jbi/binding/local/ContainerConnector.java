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
package org.mule.jbi.binding.local;

import org.mule.jbi.JbiContainer;
import org.mule.providers.AbstractServiceEnabledConnector;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOMessageReceiver;

/**
 * <code>TcpConnector</code> can bind or sent to a given tcp port on a given
 * host.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ContainerConnector extends AbstractServiceEnabledConnector  {

    private JbiContainer container;

    public String getProtocol() {
        return "container";
    }

    public JbiContainer getContainer() {
        return container;
    }

    public void setContainer(JbiContainer container) {
        this.container = container;
    }

    public UMOMessageReceiver registerListener(UMOComponent umoComponent, UMOEndpoint umoEndpoint) throws Exception {
        return null;
    }

}
