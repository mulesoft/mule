/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
