/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.filters;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityContext;
import org.mule.api.security.SecurityException;
import org.mule.api.security.SecurityProviderNotFoundException;
import org.mule.api.security.UnauthorisedException;
import org.mule.api.security.UnknownAuthenticationTypeException;
import org.mule.api.security.UnsupportedAuthenticationSchemeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.security.AbstractEndpointSecurityFilter;
import org.mule.security.DefaultMuleAuthentication;
import org.mule.security.MuleCredentials;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.i18n.HttpMessages;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * <code>HttpBasicAuthenticationFilter</code> TODO
 */
public class HttpBasicAuthenticationFilter extends AbstractEndpointSecurityFilter
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(HttpBasicAuthenticationFilter.class);

    private String realm;

    private boolean realmRequired = true;

    public HttpBasicAuthenticationFilter()
    {
        super();
    }

    public HttpBasicAuthenticationFilter(String realm)
    {
        this.realm = realm;
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        if (realm == null)
        {
            if (isRealmRequired())
            {
                throw new InitialisationException(HttpMessages.authRealmMustBeSetOnFilter(), this);
            }
            else
            {
                logger.warn("There is no security realm set, using default: null");
            }
        }
    }

    public String getRealm()
    {
        return realm;
    }

    public void setRealm(String realm)
    {
        this.realm = realm;
    }

    public boolean isRealmRequired()
    {
        return realmRequired;
    }

    public void setRealmRequired(boolean realmRequired)
    {
        this.realmRequired = realmRequired;
    }

    /**
     * Authenticates the current message if authenticate is set to true. This method
     * will always populate the secure context in the session
     *
     * @param event the current message recieved
     * @throws org.mule.api.security.SecurityException if authentication fails
     */
    @Override
    public void authenticateInbound(MuleEvent event)
        throws SecurityException, SecurityProviderNotFoundException, UnknownAuthenticationTypeException
    {
        String header = event.getMessage().getInboundProperty(HttpConstants.HEADER_AUTHORIZATION);

        if (logger.isDebugEnabled())
        {
            logger.debug("Authorization header: " + header);
        }

        if ((header != null) && header.startsWith("Basic "))
        {
            String base64Token = header.substring(6);
            String token = new String(Base64.decodeBase64(base64Token.getBytes()));

            String username = "";
            String password = "";
            int delim = token.indexOf(":");

            if (delim != -1)
            {
                username = token.substring(0, delim);
                password = token.substring(delim + 1);
            }

            Authentication authResult;
            Authentication authentication = createAuthentication(username, password, event);

            try
            {
                authResult = getSecurityManager().authenticate(authentication);
            }
            catch (UnauthorisedException e)
            {
                // Authentication failed
                if (logger.isDebugEnabled())
                {
                    logger.debug(String.format("Authentication request for user: %s failed: %s\nCause: %s",
                            username, e.toString(), e.getCause().getMessage()));
                }
                setUnauthenticated(event);
                throw new UnauthorisedException(CoreMessages.authFailedForUser(username), event, e);
            }

            // Authentication success
            if (logger.isDebugEnabled())
            {
                logger.debug("Authentication success: " + authResult.toString());
            }

            SecurityContext context = getSecurityManager().createSecurityContext(authResult);
            context.setAuthentication(authResult);
            event.getSession().setSecurityContext(context);
        }
        else if (header == null)
        {
            setUnauthenticated(event);
            throw new UnauthorisedException(event, event.getSession().getSecurityContext(), this);
        }
        else
        {
            setUnauthenticated(event);
            throw new UnsupportedAuthenticationSchemeException(
                HttpMessages.basicFilterCannotHandleHeader(header), event);
        }
    }

    protected Authentication createAuthentication(String username, String password, MuleEvent event)
    {
        return new DefaultMuleAuthentication(new MuleCredentials(username, password.toCharArray()), event);
    }

    protected void setUnauthenticated(MuleEvent event)
    {
        String realmHeader = "Basic realm=";
        if (realm != null)
        {
            realmHeader += "\"" + realm + "\"";
        }
        MuleMessage msg = event.getMessage();
        msg.setOutboundProperty(HttpConstants.HEADER_WWW_AUTHENTICATE, realmHeader);
        msg.setOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, HttpConstants.SC_UNAUTHORIZED);
    }

    /**
     * Authenticates the current message if authenticate is set to true. This method
     * will always populate the secure context in the session
     *
     * @param event the current event being dispatched
     * @throws org.mule.api.security.SecurityException if authentication fails
     */
    @Override
    public void authenticateOutbound(MuleEvent event)
        throws SecurityException, SecurityProviderNotFoundException
    {
        SecurityContext securityContext = event.getSession().getSecurityContext();
        if (securityContext == null)
        {
            if (isAuthenticate())
            {
                throw new UnauthorisedException(event, securityContext, this);
            }
            else
            {
                return;
            }
        }

        Authentication auth = securityContext.getAuthentication();
        if (isAuthenticate())
        {
            auth = getSecurityManager().authenticate(auth);
            if (logger.isDebugEnabled())
            {
                logger.debug("Authentication success: " + auth.toString());
            }
        }

        StringBuilder header = new StringBuilder(128);
        header.append("Basic ");
        String token = auth.getCredentials().toString();
        header.append(new String(Base64.encodeBase64(token.getBytes())));

        event.getMessage().setOutboundProperty(HttpConstants.HEADER_AUTHORIZATION, header.toString());
    }
}
