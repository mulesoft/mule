/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.filters;

import static org.mule.util.ClassUtils.equal;
import static org.mule.util.ClassUtils.hash;

import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.api.routing.filter.ObjectFilter;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.simple.ByteArrayToObject;

import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>RegExFilter</code> is used to match a String argument against a regular
 * pattern.
 */

public class RegExFilter implements Filter, ObjectFilter
{
    protected transient Log logger = LogFactory.getLog(getClass());

    private Pattern pattern;

    public RegExFilter()
    {
        super();
    }

    public RegExFilter(String pattern)
    {
        this.pattern = Pattern.compile(pattern);
    }

    public boolean accept(MuleMessage message)
    {
        try
        {
            return accept(message.getPayloadAsString());
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    public boolean accept(Object object)
    {
        if (object == null)
        {
            return false;
        }

        Object tempObject = object;

        // check whether the payload is a byte[] or a char[]. If it is, then it has
        // to be transformed otherwise the toString will not represent the true
        // contents
        // of the payload for the RegEx filter to use.
        if (object instanceof byte[])
        {
            ByteArrayToObject transformer = new ByteArrayToObject();
            try
            {
                object = transformer.transform(object);
            }
            catch (TransformerException e)
            {
                logger.warn(CoreMessages.transformFailedBeforeFilter(), e);
                // revert transformation
                object = tempObject;
            }
        }
        else if (object instanceof char[])
        {
            object = new String((char[]) object);
        }

        return (pattern != null && pattern.matcher(object.toString()).find());
    }

    public String getPattern()
    {
        return (pattern == null ? null : pattern.pattern());
    }

    public void setPattern(String pattern)
    {
        this.pattern = (pattern != null ? Pattern.compile(pattern) : null);
    }

    /**
     * @return
     * @deprecated Use {@link #getPattern()} This method name was changed to be
     *             consistent with other filters
     */
    public String getExpression()
    {
        return getPattern();
    }

    /**
     * @param
     * @deprecated Use {@link #getPattern()} This method name was changed to be
     *             consistent with other filters
     */
    public void setExpression(String expression)
    {
        setPattern(expression);
    }
    
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        final RegExFilter other = (RegExFilter) obj;
        return equal(pattern, other.pattern);
    }

    public int hashCode()
    {
        return hash(new Object[]{this.getClass(), pattern});
    }
}
