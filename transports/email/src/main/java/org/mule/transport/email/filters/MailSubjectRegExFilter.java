/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.filters;

import org.mule.routing.filters.RegExFilter;
import org.mule.util.ClassUtils;

import javax.mail.Message;
import javax.mail.MessagingException;

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

    public void setExpression(String pattern)
    {
        filter.setPattern(pattern);
    }

    public String getExpression()
    {
        return filter.getPattern();
    }

    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        final MailSubjectRegExFilter other = (MailSubjectRegExFilter) obj;
        return ClassUtils.equal(filter, other.filter);
    }

    public int hashCode()
    {
        return ClassUtils.hash(new Object[]{this.getClass(), filter});
    }
}
