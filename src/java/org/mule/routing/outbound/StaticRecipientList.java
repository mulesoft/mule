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

package org.mule.routing.outbound;

import java.util.List;

import org.mule.impl.RequestContext;
import org.mule.umo.UMOMessage;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;

/**
 * <code>StaticRecipientList</code> is used to dispatch a single event to
 * multiple recipients over the same transport. The recipient endpoints for this
 * router can be configured statically on the router itself.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class StaticRecipientList extends AbstractRecipientList
{
    private CopyOnWriteArrayList recipients = new CopyOnWriteArrayList();

    protected CopyOnWriteArrayList getRecipients(UMOMessage message)
    {
        List list = (List) message.removeProperty("recipients");
        if (list != null) {
            return new CopyOnWriteArrayList(list);
        }
        list = (List) RequestContext.getProperty("recipients");

        if (list != null) {
            return new CopyOnWriteArrayList(list);
        }
        return recipients;
    }

    public List getRecipients()
    {
        return recipients;
    }

    public void setRecipients(List recipients)
    {
        if (recipients != null) {
            this.recipients = new CopyOnWriteArrayList(recipients);
        } else {
            this.recipients = null;
        }
    }

}
