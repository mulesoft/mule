/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.atom.routing;

import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.transport.http.HttpConnector;
import org.mule.util.StringUtils;

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

        String[] split = verbString.split(",");
        verbs = new HashSet<String>();
        for (String s : split)
        {
            verbs.add(s.toUpperCase());
        }
    }

    public boolean accept(MuleMessage message)
    {
        String method = message.getInboundProperty(HttpConnector.HTTP_METHOD_PROPERTY, StringUtils.EMPTY);
        if (verbs != null && !verbs.contains(method.toUpperCase()))
        {
            return false;
        }

        String path = message.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY, StringUtils.EMPTY);

        return route.match(path);
    }

}
