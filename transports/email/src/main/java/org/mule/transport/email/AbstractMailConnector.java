/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.transport.AbstractConnector;
import org.mule.util.PropertiesUtils;
import org.mule.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Session;
import javax.mail.URLName;

/**
 * Abstract superclass for mail connectors. Provides Mule with an Authenticator
 * object and other shared functionality like e.g. MuleSession creation.
 */
public abstract class AbstractMailConnector extends AbstractConnector
{
    public static final String ATTACHMENT_HEADERS_PROPERTY_POSTFIX = "Headers";
    public static final String MAILBOX = "INBOX";

    private Map<ImmutableEndpoint, SessionDetails> sessions = new HashMap<ImmutableEndpoint, SessionDetails>();
    private String mailboxFolder;
    private int defaultPort;

    /**
     * A custom authenticator to be used on any mail sessions created with this
     * connector. This will only be used if user name credendials are set on the
     * endpoint.
     */
    private Authenticator authenticator = null;

    public AbstractMailConnector(int defaultPort, String mailboxFolder, MuleContext context)
    {
        super(context);
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

    public SessionDetails getSessionDetails(ImmutableEndpoint endpoint) throws UnsupportedEncodingException
    {
        // do not use this connector's implicit mutex by making this method synchronized. This
        // may interfere with other methods using the same mutex for different purposes.
        synchronized (sessions)
        {
            SessionDetails sessionDetails = sessions.get(endpoint);
            if (null == sessionDetails)
            {
                sessionDetails = newSession(endpoint);
                sessions.put(endpoint, sessionDetails);
            }
            return sessionDetails;
        }
    }
    
    public URLName urlFromEndpoint(ImmutableEndpoint endpoint) throws UnsupportedEncodingException
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

        EndpointURI uri = endpoint.getEndpointURI();
        String user = uri.getUser();
        if (user != null)
        {
            user = URLDecoder.decode(user, endpoint.getEncoding());
        }
        String pass = uri.getPassword();
        if (pass != null)
        {
            pass = URLDecoder.decode(pass, endpoint.getEncoding());
        }

        return new URLName(uri.getScheme(), uri.getHost(), uri.getPort(), inbox, user, pass);
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

    protected SessionDetails newSession(ImmutableEndpoint endpoint) throws UnsupportedEncodingException
    {
        URLName url = urlFromEndpoint(endpoint);

        Properties global = System.getProperties();
        Properties local = new Properties();
        //Allow properties to be set on the endpoint
        PropertiesUtils.getPropertiesWithPrefix(endpoint.getProperties(), "mail.", local);
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
            
            dumpProperties("MuleSession local properties", local, true);
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

    @Override
    protected void doInitialise() throws InitialisationException
    {
        // template method, nothing to do
    }

    @Override
    protected void doDispose()
    {
        // template method, nothing to do
    }

    @Override
    protected void doConnect() throws Exception
    {
        // template method, nothing to do
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // template method, nothing to do
    }

    @Override
    protected void doStart() throws MuleException
    {
        // template method, nothing to do
    }

    @Override
    protected void doStop() throws MuleException
    {
        // template method, nothing to do
    }

}
