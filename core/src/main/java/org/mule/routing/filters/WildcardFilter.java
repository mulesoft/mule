/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.filters;

import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.api.routing.filter.ObjectFilter;
import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static org.mule.util.ClassUtils.equal;
import static org.mule.util.ClassUtils.hash;

/**
 * <code>WildcardFilter</code> is used to match Strings against wildcards. It
 * performs matches with "*", i.e. "jms.events.*" would catch "jms.events.customer"
 * and "jms.events.receipts". This filter accepts a comma-separated list of patterns,
 * so more than one filter pattern can be matched for a given argument:
 * "jms.events.*, jms.actions.*" will match "jms.events.system" and "jms.actions" but
 * not "jms.queue".
 */

public class WildcardFilter implements Filter, ObjectFilter
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

    public boolean accept(MuleMessage message)
    {
        try
        {
            return accept(message.getPayloadAsString());
        }
        catch (Exception e)
        {
            logger.warn("An exception occurred while filtering", e);
            return false;
        }
    }

    public boolean accept(Object object)
    {
        if (object == null || pattern ==null)
        {
            return false;
        }

        if (this.pattern.equals(object))
        {
            return true;
        }

        String[] currentPatterns = this.patterns;
        if (currentPatterns != null)
        {
            for (String pattern : currentPatterns)
            {
                boolean foundMatch;

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
                else if (pattern.endsWith("+") && pattern.length() > 1)
                {
                    logger.warn("wildcard-filter for payload based filtering is deprecated. Use expression" +
                            "-filter or payload-type-filter instead");
                    return filterByClassName(object, pattern);
                }
            }
        }

        return false;
    }

    @Deprecated
    private boolean filterByClassName(Object object, String pattern) 
    {
        String className = pattern.substring(0, pattern.length() - 1);
        try
        {
            Class<?> theClass = ClassUtils.loadClass(className, this.getClass());
            if (!(object instanceof String))
            {
                if (theClass.isInstance(object))
                {
                    return true;
                }
            }
            else if (theClass.isAssignableFrom(ClassUtils.loadClass(object.toString(),this.getClass())))
            {
                return true;
            }
        }
        catch (ClassNotFoundException e)
        {
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

    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        final WildcardFilter other = (WildcardFilter) obj;
        return equal(pattern, other.pattern)
                && equal(patterns, other.patterns)
                && caseSensitive == other.caseSensitive;
    }

    public int hashCode()
    {
        return hash(new Object[]{this.getClass(), pattern, patterns, caseSensitive});
    }
}
