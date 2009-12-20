/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom.server;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

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
        return request.getStringProperty(HttpConnector.HTTP_METHOD_PROPERTY, "");
    }

    public Locale getPreferredLocale()
    {
        return null;
    }

    public Locale[] getPreferredLocales()
    {
        return null;
    }

    public String getTargetBasePath()
    {
        return event.getEndpointURI().getPath();
    }

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
                return request.getProperty(HttpConnector.HTTP_VERSION_PROPERTY);
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
                return request.getProperty(HttpConstants.HEADER_CONTENT_LENGTH);
            case CONTENTTYPE:
                return request.getProperty(HttpConstants.HEADER_CONTENT_TYPE);
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

    public RequestContext setAttribute(Scope scope, String name, Object value)
    {
        switch (scope)
        {
            case REQUEST:
                request.setProperty(name, value);
                break;
            case SESSION:
                event.getSession().setProperty(name, value);
                break;
        }
        return this;
    }

    public Object getAttribute(Scope scope, String name)
    {
        switch (scope)
        {
            case REQUEST:
                return request.getProperty(name);
            case SESSION:
                if (event.getSession() != null)
                {
                    return event.getSession().getProperty(name);
                }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public String[] getAttributeNames(Scope scope)
    {
        switch (scope)
        {
            case REQUEST:
                Set names = request.getPropertyNames();
                return (String[]) names.toArray(new String[names.size()]);
            case SESSION:
                return new String[0];
        }
        return null;
    }

    public String getParameter(String name)
    {
        return null;
    }

    @SuppressWarnings("unchecked")
    public String[] getParameterNames()
    {
        return new String[0];
    }

    public List<String> getParameters(String name)
    {
        return Collections.EMPTY_LIST;
    }

    public Date getDateHeader(String name)
    {
//        long value = request.getDateHeader(name);
//        return value != -1 ? new Date(value) : null;
        throw new UnsupportedOperationException();
    }

    public String getHeader(String name)
    {
        Object prop = request.getProperty(name);
        if (prop == null)
        {
            return null;
        }
        return prop.toString();
    }

    @SuppressWarnings("unchecked")
    public String[] getHeaderNames()
    {
        Set propNames = request.getPropertyNames();
        return (String[]) propNames.toArray(new String[propNames.size()]);
    }

    @SuppressWarnings("unchecked")
    public Object[] getHeaders(String name)
    {
        List<String> values = new ArrayList<String>();
        Set propNames = request.getPropertyNames();

        for (Object n : propNames)
        {
            Object prop = request.getProperty((String) n);
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

    public boolean isUserInRole(String role)
    {
        return false; // TODO
    }

    public String getContextPath()
    {
        return event.getEndpointURI().getPath();
    }
}
