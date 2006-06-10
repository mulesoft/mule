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

package org.mule.routing.outbound;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;
import org.mule.umo.UMOMessage;
import org.mule.util.StringUtils;

import java.util.List;

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
    public static final String RECIPIENTS_PROPERTY = "recipients";

    private CopyOnWriteArrayList recipients = new CopyOnWriteArrayList();

    protected CopyOnWriteArrayList getRecipients(UMOMessage message)
    {
        CopyOnWriteArrayList list = createList(message.removeProperty(RECIPIENTS_PROPERTY));
        if(list==null) {
            list = recipients;
        }
        return list;
    }

    private CopyOnWriteArrayList createList(Object list) {
        if(list==null) {
            return null;
        }
        if(list instanceof String) {
            String[] temp = StringUtils.split(list.toString(), ",");
             return new CopyOnWriteArrayList(temp);
        } else {
            return new CopyOnWriteArrayList((List)list);
        }
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
