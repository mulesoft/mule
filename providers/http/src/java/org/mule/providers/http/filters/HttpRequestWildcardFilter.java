/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.http.filters;

import org.mule.providers.http.HttpConnector;
import org.mule.routing.filters.WildcardFilter;
import org.mule.umo.UMOMessage;

/**
 * <code>HttpRequestWildcardFilter</code> filters out wildcard Url expressions.
 * you can use a comma separated list of url patterns such as-
 *
 * *.gif, *blah*
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class HttpRequestWildcardFilter extends WildcardFilter
{
    public HttpRequestWildcardFilter()
    {
    }

    public HttpRequestWildcardFilter(String pattern)
    {
        super(pattern);
    }

    public boolean accept(Object object)
    {
        String request = (String)((UMOMessage)object).getProperty(HttpConnector.HTTP_REQUEST_PROPERTY);
        return super.accept(request);
    }
}
