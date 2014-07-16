/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.filters;

import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.api.transport.PropertyScope;
import org.mule.util.StringUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static org.mule.util.ClassUtils.equal;
import static org.mule.util.ClassUtils.hash;

/**
 * <code>MessagePropertyFilter</code> can be used to filter against properties on
 * an event. This can be very useful as the event properties represent all the meta
 * information about the event from the underlying transport, so for an event
 * received over HTTP you can check for HTTP headers etc. The pattern should be
 * expressed as a key/value pair, i.e. "propertyName=value". If you want to compare
 * more than one property you can use the logic filters for And, Or and Not
 * expressions. By default the comparison is case sensitive; you can set the
 * <i>caseSensitive</i> property to override this.
 * <p/>
 * Since 3.0.0 its possible to set the property value as a wildcard expression i.e.
 * <p/>
 * <pre>fooHeader = *foo*</pre>
 */
public class MessagePropertyFilter implements Filter
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(MessagePropertyFilter.class);
    private boolean caseSensitive = true;
    private boolean not = false;

    private String propertyName;
    private String propertyValue;
    private PropertyScope scope = PropertyScope.OUTBOUND;

    private WildcardFilter wildcardFilter;

    public MessagePropertyFilter()
    {
        super();
    }

    public MessagePropertyFilter(String expression)
    {
        setPattern(expression);
    }

    public boolean accept(MuleMessage message)
    {
        if (message == null)
        {
            return false;
        }
        Object value = message.getProperty(propertyName, scope);
        boolean match;
        if (value == null)
        {
            match = compare(null, propertyValue);
        }
        else
        {
            match = compare(value.toString(), propertyValue);
        }
        if (!match && logger.isDebugEnabled())
        {
            logger.debug(String.format("Property: '%s' not found in scope '%s'. Message %n%s", propertyName, scope, message));
        }
        return match;
    }

    protected boolean compare(String value1, String value2)
    {
        if (value1 == null && value2 != null && !"null".equals(value2) && not)
        {
            return true;
        }

        if (value1 == null)
        {
            value1 = "null";
        }


        boolean result;

        result = wildcardFilter.accept(value1);

        return (not ? !result : result);
    }

    public String getPattern()
    {
        return propertyName + '=' + propertyValue;
    }

    public void setPattern(String expression)
    {
        int x = expression.indexOf(":");
        int i = expression.indexOf('=');

        if (i == -1)
        {
            throw new IllegalArgumentException(
                    "Pattern is malformed - it should be a key value pair, i.e. property=value: " + expression);
        }

        if (x > -1 && x < i)
        {
            setScope(expression.substring(0, x));
            expression = expression.substring(x + 1);
            i = expression.indexOf('=');
        }


        if (expression.charAt(i - 1) == '!')
        {
            not = true;
            propertyName = expression.substring(0, i - 1).trim();
        }
        else
        {
            propertyName = expression.substring(0, i).trim();
        }
        propertyValue = expression.substring(i + 1).trim();

        wildcardFilter = new WildcardFilter(propertyValue);
        wildcardFilter.setCaseSensitive(isCaseSensitive());
    }

    public boolean isCaseSensitive()
    {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive)
    {
        this.caseSensitive = caseSensitive;
        if (wildcardFilter != null)
        {
            wildcardFilter.setCaseSensitive(caseSensitive);
        }
    }

    public String getScope()
    {
        return scope.getScopeName();
    }

    public void setScope(String scope)
    {
        if (StringUtils.isBlank(scope))
        {
            // ignore and use defaults
            return;
        }

        PropertyScope ps = PropertyScope.get(scope.toLowerCase().trim());
        if (ps == null)
        {
            throw new IllegalArgumentException(String.format("'%s' is not a valid property scope.", scope));
        }
        this.scope = ps;
    }

    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null || getClass() != obj.getClass())
        {
            return false;
        }

        final MessagePropertyFilter other = (MessagePropertyFilter) obj;
        return equal(propertyName, other.propertyName)
                && equal(propertyValue, other.propertyValue)
                && equal(scope, other.scope)
                && caseSensitive == other.caseSensitive;
    }

    public int hashCode()
    {
        return hash(new Object[]{this.getClass(), propertyName, propertyValue, scope, caseSensitive});
    }
}
