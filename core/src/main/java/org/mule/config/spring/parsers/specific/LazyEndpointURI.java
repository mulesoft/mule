/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.InitialisationException;

import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;

/**
 * This has the following logic:
 * - if address is specified, it is used verbatim.  this is consistent with the generic case
 * - otherwise, we construct from components, omitting things that aren't specified as much as possible
 * (use required attributes to guarantee entries)
 *
 * TODO - check that we have sufficient control via XML (what about empty strings?)
 */
public class LazyEndpointURI implements UMOEndpointURI
{

    private static final String DOTS = ":";
    private static final String DOTS_SLASHES = DOTS + "//";
    private static final String QUERY = "?";
    private static final String AND = "&";
    private static final String EQUALS = "=";

    public static final String META = "meta";
    public static final String PROTOCOL = "protocol";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String HOSTNAME = "hostname";
    public static final String ADDRESS = "address";
    public static final String PORT = "port";
    public static final String PATH = "path";

    public static final String[] ALL_ATTRIBUTES =
            new String[]{META, PROTOCOL, USERNAME, PASSWORD, HOSTNAME, ADDRESS, PORT, PATH};
    // combinations used in various endpoint parsers to validate required attributes
    public static final String[] PATH_ATTRIBUTES = new String[]{PATH};
    public static final String[] HOSTNAME_ATTRIBUTES = new String[]{HOSTNAME};
    public static final String[] SOCKET_ATTRIBUTES = new String[]{HOSTNAME, PORT};
    public static final String[] USERHOST_ATTRIBUTES = new String[]{USERNAME, HOSTNAME};

    private String address;
    private String meta;
    private String protocol;
    private String username;
    private String password;
    private String hostname;
    private Integer port;
    private String path;
    private Map queries;

    private AtomicReference delegate = new AtomicReference();

    public void setUsername(String username)
    {
        assertNotYetInjected();
        this.username = username;
    }

    public void setPassword(String password)
    {
        assertNotYetInjected();
        this.password = password;
    }

    public void setHostname(String hostname)
    {
        assertNotYetInjected();
        this.hostname = hostname;
    }

    public void setAddress(String address)
    {
        assertNotYetInjected();
        this.address = address;
        assertAddressConsistent();
    }

    public void setPort(int port)
    {
        assertNotYetInjected();
        this.port = new Integer(port);
    }

    public void setProtocol(String protocol)
    {
        assertNotYetInjected();
        this.protocol = protocol;
        assertAddressConsistent();
    }

    public void setMeta(String meta)
    {
        assertNotYetInjected();
        this.meta = meta;
    }

    public void setPath(String path)
    {
        assertNotYetInjected();
        if (null != path && path.indexOf(DOTS_SLASHES) > -1)
        {
            throw new IllegalArgumentException("Unusual syntax in path: '" + path + "' contains " + DOTS_SLASHES);
        }
        this.path = path;
    }

    public void setQueries(Map queries)
    {
        assertNotYetInjected();
        this.queries = queries;
    }

    /**
     * @return The String supplied to the delegate constructor
     */
    protected String toConstructor()
    {
        StringBuffer buffer = new StringBuffer();
        appendMeta(buffer);
        appendAddress(buffer);
        appendQueries(buffer);
        return buffer.toString();
    }

    private void appendMeta(StringBuffer buffer)
    {
        if (null != meta)
        {
            buffer.append(meta);
            buffer.append(DOTS);
        }
    }

    private void appendAddress(StringBuffer buffer)
    {
        if (null != address)
        {
            buffer.append(address);
        }
        else
        {
            constructAddress(buffer);
        }
    }

    private void constructAddress(StringBuffer buffer)
    {
        buffer.append(protocol);
        buffer.append(DOTS_SLASHES);
        boolean atStart = true;
        if (null != username)
        {
            buffer.append(username);
            if (null != password)
            {
                buffer.append(":");
                buffer.append(password);
            }
            buffer.append("@");
            atStart = false;
        }
        if (null != hostname)
        {
            buffer.append(hostname);
            if (null != port)
            {
                buffer.append(":");
                buffer.append(port);
            }
            atStart = false;
        }
        if (null != path)
        {
            if (! atStart)
            {
                buffer.append("/");
            }
            buffer.append(path);
        }
    }

    private void appendQueries(StringBuffer buffer)
    {
        if (null != queries)
        {
            // crude, but probably sufficient to allow literal values in path
            boolean first = buffer.indexOf(QUERY) > -1;
            // order so that testing is simpler
            Iterator keys = new TreeSet(queries.keySet()).iterator();
            while (keys.hasNext())
            {
                if (first)
                {
                    buffer.append(QUERY);
                    first = false;
                }
                else
                {
                    buffer.append(AND);
                }
                String key = (String)keys.next();
                buffer.append(key);
                buffer.append(EQUALS);
                buffer.append((String)queries.get(key));
            }
        }
    }

    protected void assertNotYetInjected()
    {
        if (null != delegate.get())
        {
            throw new IllegalStateException("Too late to set values now!");
        }
    }

    protected UMOEndpointURI lazyDelegate()
    {
        UMOEndpointURI exists = (UMOEndpointURI) delegate.get();
        if (null != exists)
        {
            return exists;
        }
        else
        {
            try
            {
                delegate.compareAndSet(null, new MuleEndpointURI(toConstructor()));
            }
            catch (EndpointException e)
            {
                throw (IllegalStateException) new IllegalStateException("Bad address").initCause(e);
            }
            return lazyDelegate();
        }
    }

    // these are called after injection

    public String getAddress()
    {
        return lazyDelegate().getAddress();
    }

    public String getFilterAddress()
    {
        return lazyDelegate().getFilterAddress();
    }

    public String getEndpointName()
    {
        return lazyDelegate().getEndpointName();
    }

    public void setEndpointName(String name)
    {
        throw new UnsupportedOperationException("LazyEndpointURI.setEdpointName");
    }

    public Properties getParams()
    {
        return lazyDelegate().getParams();
    }

    public Properties getUserParams()
    {
        return lazyDelegate().getUserParams();
    }

    public String getScheme()
    {
        return lazyDelegate().getScheme();
    }

    public String getSchemeMetaInfo()
    {
        return lazyDelegate().getSchemeMetaInfo();
    }

    public String getFullScheme()
    {
        return lazyDelegate().getFullScheme();
    }

    public String getAuthority()
    {
        return lazyDelegate().getAuthority();
    }

    public String getHost()
    {
        return lazyDelegate().getHost();
    }

    public int getPort()
    {
        return lazyDelegate().getPort();
    }

    public String getPath()
    {
        return lazyDelegate().getPath();
    }

    public String getQuery()
    {
        return lazyDelegate().getQuery();
    }

    public String getUserInfo()
    {
        return lazyDelegate().getUserInfo();
    }

    public String getTransformers()
    {
        return lazyDelegate().getTransformers();
    }

    public String getResponseTransformers()
    {
        return lazyDelegate().getResponseTransformers();
    }

    public int getCreateConnector()
    {
        return lazyDelegate().getCreateConnector();
    }

    public URI getUri()
    {
        return lazyDelegate().getUri();
    }

    public String getConnectorName()
    {
        return lazyDelegate().getConnectorName();
    }

    public String getResourceInfo()
    {
        return lazyDelegate().getResourceInfo();
    }

    public String getUsername()
    {
        return lazyDelegate().getUsername();
    }

    public String getPassword()
    {
        return lazyDelegate().getPassword();
    }

    public void initialise() throws InitialisationException
    {
        lazyDelegate().initialise();
    }

    protected void assertAddressConsistent()
    {
        if (null != meta)
        {
            if (null != address)
            {
                if (address.startsWith(meta + DOTS))
                {
                    throw new IllegalArgumentException("Meta-protocol '" + meta +
                            "' should not be specified in the address '" + address +
                            "' - it is implicit in the element namespace.");
                }
                if (null != protocol)
                {
                    assertProtocolConsistent();
                }
                else
                {
                    if (address.indexOf(DOTS_SLASHES) == -1)
                    {
                        throw new IllegalArgumentException("Address '" + address +
                                "' does not have a transport protocol prefix " +
                                "(omit the meta protocol prefix, '" + meta + DOTS + 
                                "' - it is implicit in the element namespace).");
                    }
                }
            }
        }
        else
        {
            assertProtocolConsistent();
        }
    }

    protected void assertProtocolConsistent()
    {
        if (null != protocol && null != address && !address.startsWith(protocol + DOTS_SLASHES))
        {
            throw new IllegalArgumentException("Address '" + address + "' for protocol '" + protocol +
                    "' should start with " + protocol + DOTS_SLASHES);
        }
    }

    public int hashCode()
    {
        return lazyDelegate().hashCode();
    }

    public boolean equals(Object obj)
    {
        return lazyDelegate().equals(obj);
    }
    
    /**
     * This doesn't perfectly mirror the delegate behaviour because toString() on the delegate does
     * not return the consructor argument when meta-info is present.  However, it is close enough
     * in practice, since this is only used for debugging before the delegate is created.
     */
    public String toString()
    {
        UMOEndpointURI exists = (UMOEndpointURI) delegate.get();
        if (null != exists)
        {
            return exists.toString();
        }
        else
        {
            return toConstructor();
        }
    }

}
