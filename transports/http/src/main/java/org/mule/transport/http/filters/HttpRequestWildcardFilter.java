/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.filters;

import org.mule.api.MuleMessage;
import org.mule.routing.filters.WildcardFilter;
import org.mule.transport.http.HttpConnector;

/**
 * <code>HttpRequestWildcardFilter</code> filters out wildcard URL expressions. You
 * can use a comma-separated list of URL patterns such as "*.gif, *blah*".
 */
public class HttpRequestWildcardFilter extends WildcardFilter
{

    public HttpRequestWildcardFilter()
    {
        super();
    }

    public HttpRequestWildcardFilter(String pattern)
    {
        super(pattern);
    }

    @Override
    public boolean accept(MuleMessage message)
    {
        Object requestProperty = message.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY);
        return super.accept(requestProperty);
    }
    
}
