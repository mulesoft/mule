/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.outbound;

import org.mule.api.MuleEvent;
import org.mule.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <code>StaticRecipientList</code> is used to dispatch a single event to multiple
 * recipients over the same transport. The recipient targets for this router can be
 * configured statically on the router itself.
 */

// TODO I think the ExpressionRecipientList router does everything this router does and more.
// Perhaps we should rename it to simply "RecipientList Router" and remove this one?
public class StaticRecipientList extends AbstractRecipientList
{
    public static final String RECIPIENTS_PROPERTY = "recipients";
    public static final String RECIPIENT_DELIMITER = ",";

    private volatile List recipients = Collections.EMPTY_LIST;

    protected List getRecipients(MuleEvent event)
    {
        Object msgRecipients = event.getMessage().removeProperty(RECIPIENTS_PROPERTY);

        if (msgRecipients == null)
        {
            return recipients;
        }
        else if (msgRecipients instanceof String)
        {
            return Arrays.asList(StringUtils.splitAndTrim(msgRecipients.toString(), this.getListDelimiter()));
        }
        else if (msgRecipients instanceof List)
        {
            return new ArrayList((List) msgRecipients);
        }
        else
        {
            logger.warn("Recipients on message are neither String nor List but: " + msgRecipients.getClass());
            return Collections.EMPTY_LIST;
        }
    }

    public List getRecipients()
    {
        return recipients;
    }

    public void setRecipients(List recipients)
    {
        if (recipients != null)
        {
            this.recipients = new ArrayList(recipients);
        }
        else
        {
            this.recipients = Collections.EMPTY_LIST;
        }
    }

    /**
     * Overloading classes can change the delimiter used to separate entries in the
     * recipient list. By default a ',' is used.
     * 
     * @return The list delimiter to use
     */
    protected String getListDelimiter()
    {
        return RECIPIENT_DELIMITER;
    }

}
