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
package org.mule.providers.gs;

import net.jini.core.entry.Entry;

/**
 * The default wrapper Template for a GigiSpace entry
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JiniMessage implements Entry {
    public Object destination;
    public Object payload;

    public JiniMessage() {
    }

    /**
     * @param destination
     * @param payload
     */
    public JiniMessage(Object destination, Object payload) {
        super();
        this.destination = destination;
        this.payload = payload;
    }


    /**
     * @return Returns the destination.
     */
    public Object getDestination() {
        return destination;
    }

    /**
     * @param destination The destination to set.
     */
    public void setDestination(Object destination) {
        this.destination = destination;
    }

    /**
     * @return Returns the payload.
     */
    public Object getPayload() {
        return payload;
    }

    /**
     * @param payload The payload to set.
     */
    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public String toString() {
        return " GSMessageAdapter.Message> [destination=" + getDestination() + "]";
    }
}
