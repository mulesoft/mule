/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security;

import org.mule.api.EncryptionStrategy;
import org.mule.api.config.MuleProperties;
import org.mule.api.security.Credentials;
import org.mule.api.security.CryptoFailureException;
import org.mule.api.security.EncryptionStrategyNotFoundException;
import org.mule.api.security.SecurityManager;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.ArrayUtils;

import java.io.Serializable;
import java.util.StringTokenizer;

/**
 * <code>MuleCredentials</code> can be used to read and set Mule user information
 * that can be stored in a message header.
 */

public class MuleCredentials implements Credentials, Serializable
{
    public static final String TOKEN_DELIM = "::";

    private final String username;
    private final char[] password;
    private Object roles;

    public MuleCredentials(String username, char[] password)
    {
        this.username = username;
        this.password = ArrayUtils.clone(password);
    }

    public MuleCredentials(String username, char[] password, Object roles)
    {
        this.username = username;
        this.password = ArrayUtils.clone(password);
        this.roles = roles;
    }

    public MuleCredentials(String header, SecurityManager sm) throws EncryptionStrategyNotFoundException, CryptoFailureException
    {

        int i = header.indexOf(' ');
        if (i == -1)
        {
            throw new IllegalArgumentException(
                CoreMessages.headerMalformedValueIs(MuleProperties.MULE_USER_PROPERTY, header).toString());
        }

        String scheme = header.substring(0, i);
        String creds = header.substring(i + 1);

        if (!scheme.equalsIgnoreCase("plain"))
        {
            EncryptionStrategy es = sm.getEncryptionStrategy(scheme);
            if (es == null)
            {
                throw new EncryptionStrategyNotFoundException(scheme);
            }
            else
            {
                creds = new String(es.decrypt(creds.getBytes(), null));
            }
        }

        StringTokenizer st = new StringTokenizer(creds, TOKEN_DELIM);
        username = st.nextToken();
        password = st.nextToken().toCharArray();
        if (st.hasMoreTokens())
        {
            roles = st.nextToken();
        }
    }

    public String getToken()
    {
        StringBuffer buf = new StringBuffer();
        buf.append(username).append(TOKEN_DELIM);
        buf.append(password).append(TOKEN_DELIM);

        if (roles != null)
        {
            buf.append(roles);
        }

        return buf.toString();
    }

    public String getUsername()
    {
        return username;
    }

    public char[] getPassword()
    {
        return ArrayUtils.clone(password);
    }

    public Object getRoles()
    {
        return roles;
    }

    public static String createHeader(String username, char[] password)
    {
        StringBuffer buf = new StringBuffer(32);
        buf.append("Plain ");
        buf.append(username).append(TOKEN_DELIM);
        buf.append(password).append(TOKEN_DELIM);
        return buf.toString();
    }

    public static String createHeader(String username,
                                      String password,
                                      String encryptionName,
                                      EncryptionStrategy es) throws CryptoFailureException
    {
        StringBuffer buf = new StringBuffer();
        buf.append(encryptionName).append(" ");
        String creds = username + TOKEN_DELIM + password;
        byte[] encrypted = es.encrypt(creds.getBytes(), null);
        buf.append(new String(encrypted));
        return buf.toString();
    }
}
