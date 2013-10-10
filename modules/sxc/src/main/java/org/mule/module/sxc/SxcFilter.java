/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.sxc;

import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

import com.envoisolutions.sxc.xpath.XPathBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static org.mule.util.ClassUtils.equal;
import static org.mule.util.ClassUtils.hash;

public class SxcFilter implements Filter
{
    protected transient Log logger = LogFactory.getLog(getClass());

    private String pattern;
    
    public SxcFilter()
    {
        super();
    }

    public SxcFilter(String pattern)
    {
        this.pattern = pattern;
    }

    public boolean accept(MuleMessage msg)
    {
        Object accept = msg.getInvocationProperty(toString());
         
         if (accept == null && SxcFilteringOutboundRouter.getCurrentMessage() == null) 
         {
             return false;
         }
         else if (accept == null)
         {
             throw new UndefinedMatchException();
         }
         
         return (Boolean) accept;
    }

    /** @return XPath expression */
    public String getPattern()
    {
        return pattern;
    }

    /** @param pattern The XPath expression */
    public void setPattern(String pattern)
    {
        this.pattern = pattern;
    }

    public void addEventHandler(SxcFilteringOutboundRouter router, XPathBuilder builder)
    {    
        builder.listen(pattern, new FilterEventHandler(router, this));
    }
    
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        final SxcFilter other = (SxcFilter) obj;
        return equal(pattern, other.pattern);
    }

    public int hashCode()
    {
        return hash(new Object[]{this.getClass(), pattern});
    }
}


