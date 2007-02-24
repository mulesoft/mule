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

import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;
import org.mule.util.StringUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>WildcardFilter</code> is used to match Strings against wildcards. It
 * performs matches with "*", i.e. "jms.events.*" would catch "jms.events.customer"
 * and "jms.events.receipts". This filter accepts a comma-separated list of patterns,
 * so more than one filter pattern can be matched for a given argument:
 * "jms.events.*, jms.actions.*" will match "jms.events.system" and "jms.actions" but
 * not "jms.queue".
 */

public class WildcardFilter implements UMOFilter, ObjectFilter
{
    protected final Log logger = LogFactory.getLog(this.getClass());

    protected volatile String pattern;
    protected volatile String[] patterns;
    private volatile boolean caseSensitive = true;

    public WildcardFilter()
    {
        super();
    }

    public WildcardFilter(String pattern)
    {
        this.setPattern(pattern);
    }

    public boolean accept(UMOMessage message)
    {
        try
        {
            return accept(message.getPayloadAsString());
        }
        catch (Exception e)
        {
            logger.warn("An exception occured while filtering", e);
            return false;
        }
    }

    public boolean accept(Object object)
    {
        if (object == null)
        {
            return false;
        }

        String[] currentPatterns = this.patterns;
        if (currentPatterns != null)
        {
            for (int x = 0; x < currentPatterns.length; x++)
            {
                boolean foundMatch;
                String pattern = currentPatterns[x];

                if ("*".equals(pattern) || "**".equals(pattern))
                {
                    return true;
                }

                String candidate = object.toString();

                if (!isCaseSensitive())
                {
                    pattern = pattern.toLowerCase();
                    candidate = candidate.toLowerCase();
                }

                int i = pattern.indexOf('*');
                if (i == -1)
                {
                    foundMatch = pattern.equals(candidate);
                }
                else
                {
                    int i2 = pattern.indexOf('*', i + 1);
                    if (i2 > 1)
                    {
                        foundMatch = candidate.indexOf(pattern.substring(1, i2)) > -1;
                    }
                    else if (i == 0)
                    {
                        foundMatch = candidate.endsWith(pattern.substring(1));
                    }
                    else
                    {
                        foundMatch = candidate.startsWith(pattern.substring(0, i));
                    }
                }

                if (foundMatch)
                {
                    return true;
                }
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
