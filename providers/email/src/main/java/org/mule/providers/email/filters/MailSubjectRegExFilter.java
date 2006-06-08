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
package org.mule.providers.email.filters;

import org.mule.routing.filters.RegExFilter;

import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * <code>MailSubjectRegExFilter</code> applies a regular expression to a Mail
 * Message subject.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MailSubjectRegExFilter extends AbstractMailFilter
{
    private RegExFilter filter = new RegExFilter();

    public boolean accept(Message message)
    {
        try {
            return filter.accept(message.getSubject());
        } catch (MessagingException e) {
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
