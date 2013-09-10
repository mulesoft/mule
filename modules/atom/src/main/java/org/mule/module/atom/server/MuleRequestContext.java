/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom.server;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.protocol.server.Provider;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.context.AbstractRequestContext;

public class MuleRequestContext extends AbstractRequestContext implements RequestContext
{

    private final MuleMessage request;
    private HttpSession session;
    private MuleEventContext event;
    private IRI baseIri;

    public MuleRequestContext(Provider context,
                              MuleEventContext event,
                              MuleMessage request,
                              String contextPath,
                              IRI baseIri)
    {
        super(context,
                getMethod(request),
                new IRI(contextPath),
                baseIri);

        this.baseIri = baseIri;
        this.request = request;
        this.event = event;
        // TODO: Sessions?
        this.session = null; // request.getSession(false);

        // TODO: Principals?
        principal = null; // request.getUserPrincipal();
        subject = context.resolveSubject(this);
        target = context.resolveTarget(this);
    }

    private static String getMethod(MuleMessage request)
    {
        return request.getInboundProperty(HttpConnector.HTTP_METHOD_PROPERTY, StringUtils.EMPTY);
    }

    @Override
    public Locale getPreferredLocale()
    {
        return null;
    }

    @Override
    public Locale[] getPreferredLocales()
    {
        return null;
    }

    @Override
    public String getTargetBasePath()
    {
        return event.getEndpointURI().getPath();
    }

    @Override
    public Object getProperty(Property property)
    {
        switch (property)
        {
            case SESSIONID:
                return (session != null) ? session.getId() : null;
            case SESSIONCREATED:
                return (session != null) ? new Date(session.getCreationTime()) : null;
            case SESSIONACCESSED:
                return (session != null) ? new Date(session.getLastAccessedTime()) : null;
            case SESSIONTIMEOUT:
                return (session != null) ? session.getMaxInactiveInterval() : -1;
            case CHARACTERENCODING:
                return request.getEncoding();
            case LOCALES:
                return null;
            case PROTOCOL:
                return request.getInboundProperty(HttpConnector.HTTP_VERSION_PROPERTY);
            case REMOTEADDRESS:
                return null;
            case REMOTEHOST:
                return baseIri.getHost();
            case REMOTEUSER:
                return baseIri.getUserInfo();
            case SCHEME:
                return baseIri.getScheme();
            case PRINCIPAL:
                return null; // TODO
            case AUTHTYPE:
                return null; // TODO
            case CONTENTLENGTH:
                return request.getOutboundProperty(HttpConstants.HEADER_CONTENT_LENGTH);
            case CONTENTTYPE:
                return request.getOutboundProperty(HttpConstants.HEADER_CONTENT_TYPE);
            case CONTEXTPATH:
                return ""; // TODO
            case LOCALADDR:
                return ""; // TODO
            case LOCALNAME:
                return ""; // TODO
            case SERVERNAME:
                return ""; // TODO
            case SERVERPORT:
                return ""; // TODO
            default:
                throw new UnsupportedOperationException("Property " + property.name() + " is not supported.");
        }
    }

    @Override
    public Reader getReader() throws IOException
    {
        Object payload = request.getPayload();
        if (payload instanceof Reader)
        {
            return (Reader) payload;
        }
        else if (payload instanceof InputStream)
        {
            return new InputStreamReader((InputStream) payload);
        }
        else if (payload instanceof byte[])
        {
            return new InputStreamReader(new ByteArrayInputStream((byte[]) payload));
        }
        else
        {
            try
            {
                return new StringReader(request.getPayloadAsString());
            }
            catch (Exception e)
            {
                IOException e2 = new IOException("Could not convert message to String.");
                e2.initCause(e);
                throw e2;
            }
        }
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
        Object payload = request.getPayload();
        if (payload instanceof InputStream)
        {
            return (InputStream) payload;
        }
        else if (payload instanceof byte[])
        {
            return new ByteArrayInputStream((byte[]) payload);
        }
        else
        {
            try
            {
                return new ByteArrayInputStream(request.getPayloadAsString().getBytes());
            }
            catch (Exception e)
            {
                IOException e2 = new IOException("Could not convert message to String.");
                e2.initCause(e);
                throw e2;
            }
        }
    }

    @Override
    public RequestContext setAttribute(Scope scope, String name, Object value)
    {
        switch (scope)
        {
            case REQUEST:
                // note it's not a usual Mule property scope, abdera puts and checks for things here
                request.setProperty(name, value, PropertyScope.INBOUND);
                break;
            case SESSION:
                event.getSession().setProperty(name, value);
                break;
        }
        return this;
    }

    @Override
    public Object getAttribute(Scope scope, String name)
    {
        switch (scope)
        {
            case REQUEST:
                return request.getInboundProperty(name);
            case SESSION:
                if (event.getSession() != null)
                {
                    return event.getSession().getProperty(name);
                }
        }
        return null;
    }

    @Override
    public String[] getAttributeNames(Scope scope)
    {
        switch (scope)
        {
            case REQUEST:
                Set<String> names = request.getPropertyNames();
                return names.toArray(new String[names.size()]);
            case SESSION:
                return new String[0];
        }
        return null;
    }

    @Override
    public String getParameter(String name)
    {
        return null;
    }

    @Override
    public String[] getParameterNames()
    {
        return new String[0];
    }

    @Override
    public List<String> getParameters(String name)
    {
        return Collections.emptyList();
    }

    @Override
    public Date getDateHeader(String name)
    {
//        long value = request.getDateHeader(name);
//        return value != -1 ? new Date(value) : null;
        throw new UnsupportedOperationException();
    }

    @Override
    public String getHeader(String name)
    {
        Object prop = request.getInboundProperty(name);
        if (prop == null)
        {
            return null;
        }
        return prop.toString();
    }

    @Override
    public String[] getHeaderNames()
    {
        Set<String> propNames = request.getPropertyNames();
        return propNames.toArray(new String[propNames.size()]);
    }

    @Override
    public Object[] getHeaders(String name)
    {
        List<String> values = new ArrayList<String>();
        Set<String> propNames = request.getPropertyNames();

        for (String n : propNames)
        {
            Object prop = request.getProperty(n);
            if (prop instanceof String)
            {
                values.add((String) prop);
            }
        }
        return values.toArray();
    }
//
//    private static String getHost(ServiceContext context, HttpServletRequest request) {
//        Abdera abdera = context.getAbdera();
//        String host = abdera.getConfiguration()
//            .getConfigurationOption("org.apache.abdera.protocol.server.Host");
//        return (host != null) ? host : request.getServerName();
//    }
//
//    private static int getPort(ServiceContext context, HttpServletRequest request) {
//        Abdera abdera = context.getAbdera();
//        String port = abdera.getConfiguration()
//            .getConfigurationOption("org.apache.abdera.protocol.server.Port");
//        return (port != null) ? Integer.parseInt(port) : request.getServerPort();
//    }

    @Override
    public boolean isUserInRole(String role)
    {
        return false; // TODO
    }

    @Override
    public String getContextPath()
    {
        return event.getEndpointURI().getPath();
    }
}
