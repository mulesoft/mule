/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http.filters;

import org.mule.providers.http.HttpConnector;
import org.mule.routing.filters.WildcardFilter;
import org.mule.umo.UMOMessage;

/**
 * <code>HttpRequestWildcardFilter</code> filters out wildcard Url
 * expressions. you can use a comma separated list of url patterns such as-
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
        super();
    }

    public HttpRequestWildcardFilter(String pattern)
    {
        super(pattern);
    }

    public boolean accept(Object object)
    {
        if (object instanceof UMOMessage) {
            object = ((UMOMessage)object).getProperty(HttpConnector.HTTP_REQUEST_PROPERTY);
        }
        return super.accept(object);
    }
}
