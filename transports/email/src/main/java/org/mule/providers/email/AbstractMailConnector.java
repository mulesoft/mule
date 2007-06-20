/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email;

import org.mule.util.StringUtils;
import org.mule.providers.AbstractConnector;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Session;
import javax.mail.URLName;

/**
 * Abstract superclass for mail connectors. Provides Mule with an Authenticator
 * object and other shared functionality like e.g. Session creation.
 */
public abstract class AbstractMailConnector extends AbstractConnector
{

    public static final String MAILBOX = "INBOX";

    private Map sessions = new HashMap();
    private String mailboxFolder;
    private int defaultPort;

    /**
     * A custom authenticator to be used on any mail sessions created with this
     * connector. This will only be used if user name credendials are set on the
     * endpoint.
     */
    private Authenticator authenticator = null;

    public AbstractMailConnector(int defaultPort, String mailboxFolder)
    {
        super();
        this.defaultPort = defaultPort;
        this.mailboxFolder = mailboxFolder;
    }

    public int getDefaultPort()
    {
        return defaultPort;
    }

    public Authenticator getAuthenticator()
    {
        return authenticator;
    }

    public void setAuthenticator(Authenticator authenticator)
    {
        this.authenticator = authenticator;
    }
    
    public String getMailboxFolder()
    {
        return mailboxFolder;
    }

    public void setMailboxFolder(String mailboxFolder)
    {
        this.mailboxFolder = mailboxFolder;
    }

    public synchronized SessionDetails getSessionDetails(UMOImmutableEndpoint endpoint)
    {
        SessionDetails sessionDetails = (SessionDetails) sessions.get(endpoint);
        if (null == sessionDetails)
        {
            sessionDetails = newSession(endpoint);
            sessions.put(endpoint, sessionDetails);
        }
        return sessionDetails;
    }
    
    public URLName urlFromEndpoint(UMOImmutableEndpoint endpoint)
    {
        String inbox = endpoint.getEndpointURI().getPath();
        if (inbox.length() == 0)
        {
            inbox = getMailboxFolder();
        }
        else
        {
            inbox = inbox.substring(1);
        }

        UMOEndpointURI uri = endpoint.getEndpointURI();
        return new URLName(uri.getScheme(), uri.getHost(), uri.getPort(), inbox,
                uri.getUsername(), uri.getPassword());
    }
    
    /**
     * Some protocols (eg secure extensions) extend a "base" protocol.
     * Subclasses for such protocols should override this method.
     * 
     * @return the underlying (eg non-secure) protocol
     */
    protected String getBaseProtocol()
    {
        return getProtocol();
    }
    
    /**
     * Subclasses should extend this to add further properties.  
     * Synchronization is managed outside this call (so no need to synchronize further on properties)
     * 
     * @param global system properties 
     * @param local local properties (specific to one session)
     * @param url the endpoint url
     */
    protected void extendPropertiesForSession(Properties global, Properties local, URLName url)
    {
        int port = url.getPort();
        if (port == -1)
        {
            port = this.getDefaultPort();
        }
        local.setProperty("mail." + getBaseProtocol() + ".socketFactory.port", Integer.toString(port));

        if (StringUtils.isNotBlank(url.getPassword()))
        {
            local.setProperty("mail." + getBaseProtocol() + ".auth", "true");
            if (getAuthenticator() == null)
            {
                setAuthenticator(new DefaultAuthenticator(url.getUsername(), url.getPassword()));
                if (logger.isDebugEnabled())
                {
                    logger.debug("No Authenticator set on connector: " + getName() + "; using default.");
                }
            }
        }
        else
        {
            local.setProperty("mail." + getBaseProtocol() + ".auth", "false");
        }
        
        // TODO - i'm not at all certain that these properties (especially the ones
        // using the base protocol) are needed.  they are inherited from old, gnarly
        // code.

        if (StringUtils.isNotBlank(url.getHost())) {
            local.setProperty("mail." + getBaseProtocol() + ".host", url.getHost());
        }
        local.setProperty("mail." + getBaseProtocol() + ".rsetbeforequit", "true");
    }

    protected SessionDetails newSession(UMOImmutableEndpoint endpoint)
    {
        URLName url = urlFromEndpoint(endpoint);

        Properties global = System.getProperties();
        Properties local = new Properties();
        Session session;

        // make sure we do not mess with authentication set via system properties
        synchronized (global)
        {
            extendPropertiesForSession(global, local, url);
            session = Session.getInstance(local, getAuthenticator());
        }

        if (logger.isDebugEnabled())
        {
            local.setProperty("mail.debug", "true");
            
            dumpProperties("Session local properties", local, true);
            dumpProperties("System global properties", global, true);
            logger.debug("Creating mail session: host = " + url.getHost() + ", port = " + url.getPort()
                + ", user = " + url.getUsername() + ", pass = " + url.getPassword());
        }

        return new SessionDetails(session, url);
    }
    
    protected void dumpProperties(String title, Properties properties, boolean filter)
    {
        int skipped = 0;
        logger.debug(title + " =============");
        Enumeration keys = properties.keys();
        while (keys.hasMoreElements())
        {
            String key = (String) keys.nextElement();
            if (!filter || key.startsWith("mule.") || key.startsWith("mail.") || key.startsWith("javax."))
            {
                String value = properties.getProperty(key);
                logger.debug(key + ": " + value);
            }
            else 
            {
                ++skipped;
            }
        }
        if (filter)
        {
            logger.debug("skipped " + skipped);
        }
    }
    
    // supply these here because sub-classes are very simple

    protected void doInitialise() throws InitialisationException
    {
        // template method, nothing to do
    }

    protected void doDispose()
    {
        // template method, nothing to do
    }

    protected void doConnect() throws Exception
    {
        // template method, nothing to do
    }

    protected void doDisconnect() throws Exception
    {
        // template method, nothing to do
    }

    protected void doStart() throws UMOException
    {
        // template method, nothing to do
    }

    protected void doStop() throws UMOException
    {
        // template method, nothing to do
    }

}
