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
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class Order implements Entry {

    public String orderId;
    public Boolean processed = Boolean.FALSE;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Boolean getProcessed() {
        return processed;
    }

    public void setProcessed(Boolean processed) {
        this.processed = processed;
    }

    public String toString(){
        return "Order{id=" + orderId + ", processed=" + processed.booleanValue() + "}";
    }
}
