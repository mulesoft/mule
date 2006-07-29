/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.acegi.filters.http;

import net.sf.acegisecurity.AuthenticationException;
import net.sf.acegisecurity.providers.UsernamePasswordAuthenticationToken;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.extras.acegi.AcegiAuthenticationAdapter;
import org.mule.impl.security.AbstractEndpointSecurityFilter;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.HttpConstants;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.security.SecurityException;
import org.mule.umo.security.SecurityProviderNotFoundException;
import org.mule.umo.security.UMOAuthentication;
import org.mule.umo.security.UMOSecurityContext;
import org.mule.umo.security.UnauthorisedException;
import org.mule.umo.security.UnknownAuthenticationTypeException;
import org.mule.umo.security.UnsupportedAuthenticationSchemeException;

/**
 * <code>HttpBasicAuthenticationFilter</code> TODO
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class HttpBasicAuthenticationFilter extends AbstractEndpointSecurityFilter
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(HttpBasicAuthenticationFilter.class);

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

    public void doInitialise() throws InitialisationException
    {
        if (realm == null) {
            if (isRealmRequired()) {
                throw new InitialisationException(new Message(Messages.AUTH_REALM_MUST_SET_ON_FILTER), this);
            } else {
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
     * Authenticates the current message if authenticate is set to true. This
     * method will always populate the secure context in the session
     * 
     * @param event the current message received
     * @throws org.mule.umo.security.SecurityException if authentication fails
     */
    public void authenticateInbound(UMOEvent event) throws SecurityException, SecurityProviderNotFoundException,
            UnknownAuthenticationTypeException
    {
        String header = event.getMessage().getStringProperty(HttpConstants.HEADER_AUTHORIZATION, null);

        if (logger.isDebugEnabled()) {
            logger.debug("Authorization header: " + header);
        }

        if ((header != null) && header.startsWith("Basic ")) {
            String base64Token = header.substring(6);
            String token = new String(Base64.decodeBase64(base64Token.getBytes()));

            String username = "";
            String password = "";
            int delim = token.indexOf(":");

            if (delim != -1) {
                username = token.substring(0, delim);
                password = token.substring(delim + 1);
            }

            UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(username,
                                                                                                      password);
            authRequest.setDetails(event.getMessage().getProperty(MuleProperties.MULE_ENDPOINT_PROPERTY));

            UMOAuthentication authResult;

            UMOAuthentication umoAuthentication = new AcegiAuthenticationAdapter(authRequest);

            try {
                authResult = getSecurityManager().authenticate(umoAuthentication);
            } catch (AuthenticationException e) {
                // Authentication failed
                if (logger.isDebugEnabled()) {
                    logger.debug("Authentication request for user: " + username + " failed: " + e.toString());
                }
                setUnauthenticated(event);
                throw new UnauthorisedException(new Message(Messages.AUTH_FAILED_FOR_USER_X, username), e);
            }

            // Authentication success
            if (logger.isDebugEnabled()) {
                logger.debug("Authentication success: " + authResult.toString());
            }

            UMOSecurityContext context = getSecurityManager().createSecurityContext(authResult);
            event.getSession().setSecurityContext(context);
        } else if (header == null) {
            setUnauthenticated(event);
            throw new UnauthorisedException(event.getMessage(),
                                            event.getSession().getSecurityContext(),
                                            getEndpoint(),
                                            this);
        } else {
            setUnauthenticated(event);
            throw new UnsupportedAuthenticationSchemeException(new Message("acegi", 1, header), event.getMessage());
        }
    }

    protected void setUnauthenticated(UMOEvent event)
    {
        String realmHeader = "Basic realm=";
        if (realm != null) {
            realmHeader += "\"" + realm + "\"";
        }
        UMOMessage msg = event.getMessage();
        msg.setProperty(HttpConstants.HEADER_WWW_AUTHENTICATE, realmHeader);
        msg.setIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, HttpConstants.SC_UNAUTHORIZED);
    }

    /**
     * Authenticates the current message if authenticate is set to true. This
     * method will always populate the secure context in the session
     * 
     * @param event the current event being dispatched
     * @throws org.mule.umo.security.SecurityException if authentication fails
     */
    public void authenticateOutbound(UMOEvent event) throws SecurityException, SecurityProviderNotFoundException
    {
        if (event.getSession().getSecurityContext() == null) {
            if (isAuthenticate()) {
                throw new UnauthorisedException(event.getMessage(),
                                                event.getSession().getSecurityContext(),
                                                event.getEndpoint(),
                                                this);
            } else {
                return;
            }
        }

        UMOAuthentication auth = event.getSession().getSecurityContext().getAuthentication();
        if (isAuthenticate()) {
            auth = getSecurityManager().authenticate(auth);
            if (logger.isDebugEnabled()) {
                logger.debug("Authentication success: " + auth.toString());
            }
        }

        StringBuffer header = new StringBuffer(128);
        header.append("Basic ");
        String token = auth.getCredentials().toString();
        header.append(Base64.encodeBase64(token.getBytes()));

        event.getMessage().setStringProperty(HttpConstants.HEADER_AUTHORIZATION, header.toString());
    }

}
