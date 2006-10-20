/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email.filters;

import javax.mail.Message;
import javax.mail.MessagingException;

import org.mule.routing.filters.RegExFilter;

/**
 * <code>MailSubjectRegExFilter</code> applies a regular expression to a Mail
 * Message subject.
 */
public class MailSubjectRegExFilter extends AbstractMailFilter
{
    private RegExFilter filter = new RegExFilter();

    public boolean accept(Message message)
    {
        try
        {
            return filter.accept(message.getSubject());
        }
        catch (MessagingException e)
        {
            logger.warn("Failed to read message subject: " + e.getMessage(), e);
            return false;
        }
    }

    public void setPattern(String pattern)
    {
        filter.setPattern(pattern);
    }

    public String getPattern()
    {
        return filter.getPattern();
    }
}
