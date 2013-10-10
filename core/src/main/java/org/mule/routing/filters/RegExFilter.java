/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.filters;

import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.api.routing.filter.ObjectFilter;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.simple.ByteArrayToObject;
import org.mule.util.ClassUtils;

import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static org.mule.util.ClassUtils.hash;

/**
 * <code>RegExFilter</code> is used to match a String argument against a regular expression.
 */
public class RegExFilter implements Filter, ObjectFilter
{
    private static final int NO_FLAGS = 0;
    protected transient Log logger = LogFactory.getLog(getClass());

    private Pattern pattern;

    private int flags = NO_FLAGS;

    public RegExFilter()
    {
        super();
    }

    public RegExFilter(String pattern)
    {
        this(pattern, NO_FLAGS);
    }

    public RegExFilter(String pattern, int flags)
    {
        this.pattern = Pattern.compile(pattern, flags);
        this.flags = flags;
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
        this.pattern = (pattern != null ? Pattern.compile(pattern, flags) : null);
    }

    public int getFlags()
    {
        return flags;
    }

    public void setFlags(int flags)
    {
        this.flags = flags;
        this.pattern = (this.pattern != null ? Pattern.compile(pattern.pattern(), flags) : null);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        final RegExFilter other = (RegExFilter) obj;
        boolean patternsAreEqual = ClassUtils.equal(pattern.pattern(), other.pattern.pattern());
        boolean flagsAreEqual = (flags == other.flags);
        return (patternsAreEqual && flagsAreEqual);
    }

    @Override
    public int hashCode()
    {
        return hash(new Object[]{this.getClass(), pattern});
    }
}
