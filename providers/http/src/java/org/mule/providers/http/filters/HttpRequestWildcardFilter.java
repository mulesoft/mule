/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
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
 * <code>HttpRequestWildcardFilter</code> TODO
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
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
