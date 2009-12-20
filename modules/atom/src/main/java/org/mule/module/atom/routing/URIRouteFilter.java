/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom.routing;

import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.transport.http.HttpConnector;

import java.util.HashSet;
import java.util.Set;

import org.apache.abdera.i18n.templates.Route;

public class URIRouteFilter implements Filter
{

    private Set<String> verbs;
    private Route route;

    public URIRouteFilter()
    {
        super();
    }

    public void setRoute(String routePattern)
    {
        route = new Route("", routePattern);
    }

    public void setVerbs(String verbString)
    {
        if (verbString.equals("*"))
        {
            return;
        }

        String[] split = verbString.split(" ");
        verbs = new HashSet<String>();
        for (String s : split)
        {
            verbs.add(s.toUpperCase());
        }
    }

    public void setVerbs(Set<String> verbs)
    {
        this.verbs = verbs;
    }

    public boolean accept(MuleMessage message)
    {
        String method = message.getStringProperty(HttpConnector.HTTP_METHOD_PROPERTY, "");
        if (verbs != null && !verbs.contains(method))
        {
            return false;
        }

        String path = message.getStringProperty(HttpConnector.HTTP_REQUEST_PROPERTY, "");

        return route.match(path);
    }

}
