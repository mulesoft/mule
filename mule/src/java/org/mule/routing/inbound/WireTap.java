/*
 * $Id: $
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.routing.inbound;

import org.mule.MuleManager;
import org.mule.impl.ImmutableMuleEndpoint;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

/**
 * An inbound router that can forward every message to another destination as
 * defined in the "endpoint" property.  This can be a logical destination of a URI.
 * <p/>
 * A filter can be applied to this router so that only events matching a criteria will
 * be tapped.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class WireTap extends SelectiveConsumer
{
    private String endpoint;

    private UMOImmutableEndpoint tap;

    public boolean isMatch(UMOEvent event) throws MessagingException {
        if (endpoint != null) {
            return super.isMatch(event);
        } else {
            logger.warn("No endpoint identifier is set on this wire tap");
            return false;
        }
    }

    public UMOEvent[] process(UMOEvent event) throws MessagingException {

        try {
            event.getSession().dispatchEvent(event.getMessage(), tap);
        } catch (UMOException e) {
            logger.error(e.getMessage(), e);
        }
        return super.process(event);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) throws UMOException {
        this.endpoint = endpoint;
        if (this.endpoint != null) {
            tap = MuleManager.getInstance().lookupEndpoint(this.endpoint);
            if (tap == null) {
                tap = new ImmutableMuleEndpoint(this.endpoint, false);
            }
        }
    }
}
