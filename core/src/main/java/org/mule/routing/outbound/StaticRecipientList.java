/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
 *
 * Deprecated from 3.6.0.  This functionality is specific to Services.
 */

// TODO I think the ExpressionRecipientList router does everything this router does and more.
// Perhaps we should rename it to simply "RecipientList Router" and remove this one?
@Deprecated
public class StaticRecipientList extends AbstractRecipientList
{
    public static final String RECIPIENTS_PROPERTY = "recipients";
    public static final String RECIPIENT_DELIMITER = ",";

    private volatile List<Object> recipients = Collections.emptyList();

    @Override
    protected List<Object> getRecipients(MuleEvent event)
    {
        Object msgRecipients = event.getMessage().removeProperty(RECIPIENTS_PROPERTY);

        if (msgRecipients == null)
        {
            return recipients;
        }
        else if (msgRecipients instanceof String)
        {
            Object[] split = StringUtils.splitAndTrim(msgRecipients.toString(), getListDelimiter());
            return Arrays.<Object>asList(split);
        }
        else if (msgRecipients instanceof List)
        {
            return new ArrayList<Object>((List<?>) msgRecipients);
        }
        else
        {
            logger.warn("Recipients on message are neither String nor List but: " + msgRecipients.getClass());
            return Collections.emptyList();
        }
    }

    public List<Object> getRecipients()
    {
        return recipients;
    }

    public void setRecipients(List<?> recipients)
    {
        if (recipients != null)
        {
            this.recipients = new ArrayList<Object>(recipients);
        }
        else
        {
            this.recipients = Collections.emptyList();
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
