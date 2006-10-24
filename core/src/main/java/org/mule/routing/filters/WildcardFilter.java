/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.filters;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;
import org.mule.util.StringUtils;

/**
 * <code>WildcardFilter</code> is used to match wildcard string. It performs
 * matches with * i.e. jms.events.* would catch jms.events.customer
 * jms.events.receipts This filter accepts a comma separented list of patterns so
 * more than one filter pattenr can be matched for a given argument i.e.-
 * jms.events.*, jms.actions.* will match jms.events.system and jms.actions but not
 * jms.queue
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class WildcardFilter implements UMOFilter, ObjectFilter
{
    private static final Log LOGGER = LogFactory.getLog(WildcardFilter.class);

    protected String[] patterns;
    protected String pattern;
    private boolean caseSensitive = true;

    public WildcardFilter()
    {
        super();
    }

    public WildcardFilter(String pattern)
    {
        setPattern(pattern);
    }

    public boolean accept(UMOMessage message)
    {
        try
        {
            return accept(message.getPayloadAsString());
        }
        catch (Exception e)
        {
            LOGGER.warn("An exception occured while filtering", e);
            return false;
        }
    }

    public boolean accept(Object object)
    {
        if (object == null)
        {
            return false;
        }

        boolean match = false;
        for (int x = 0; x < patterns.length; x++)
        {
            String pattern = patterns[x];

            String string = object.toString();
            if ("*".equals(pattern) || "**".equals(pattern))
            {
                return true;
            }

            int i = pattern.indexOf('*');

            if (!isCaseSensitive())
            {
                pattern = pattern.toLowerCase();
                string = string.toLowerCase();
            }

            if (i == -1)
            {
                match = pattern.equals(string);
            }
            else
            {
                int i2 = pattern.indexOf('*', i + 1);
                if (i2 > 1)
                {
                    match = string.indexOf(pattern.substring(1, i2)) > -1;
                }
                else if (i == 0)
                {
                    match = string.endsWith(pattern.substring(1));
                }
                else
                {
                    match = string.startsWith(pattern.substring(0, i));
                }
            }
            if (match)
            {
                return true;
            }
        }
        return false;
    }

    public String getPattern()
    {
        return pattern;
    }

    public void setPattern(String pattern)
    {
        this.pattern = pattern;
        this.patterns = StringUtils.splitAndTrim(pattern, ",");
    }

    public boolean isCaseSensitive()
    {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive)
    {
        this.caseSensitive = caseSensitive;
    }
}
