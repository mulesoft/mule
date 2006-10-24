/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;
import org.mule.umo.UMOMessage;
import org.mule.util.StringUtils;

import java.util.List;

/**
 * <code>StaticRecipientList</code> is used to dispatch a single event to multiple
 * recipients over the same transport. The recipient endpoints for this router can be
 * configured statically on the router itself.
 */

public class StaticRecipientList extends AbstractRecipientList
{
    public static final String RECIPIENTS_PROPERTY = "recipients";

    protected String delim = ",";

    private CopyOnWriteArrayList recipients = new CopyOnWriteArrayList();

    protected CopyOnWriteArrayList getRecipients(UMOMessage message)
    {
        Object msgRecipients = message.removeProperty(RECIPIENTS_PROPERTY);
        CopyOnWriteArrayList list;

        if (msgRecipients == null)
        {
            list = recipients;
        }
        else if (msgRecipients instanceof String)
        {
            list = new CopyOnWriteArrayList(StringUtils.splitAndTrim(msgRecipients.toString(),
                getListDelimiter()));
        }
        else
        {
            list = new CopyOnWriteArrayList((List)msgRecipients);
        }

        return list;
    }

    public List getRecipients()
    {
        return recipients;
    }

    public void setRecipients(List recipients)
    {
        if (recipients != null)
        {
            this.recipients = new CopyOnWriteArrayList(recipients);
        }
        else
        {
            this.recipients = null;
        }
    }

    /**
     * Overloading classes can change the delimiter used to separate entries in the
     * recipient list By default a ',' is used.
     * 
     * @return The list delimiter to use
     */
    protected String getListDelimiter()
    {
        return delim;
    }

}
