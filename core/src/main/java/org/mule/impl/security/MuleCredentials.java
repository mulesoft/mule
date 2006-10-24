/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.security;

import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMOEncryptionStrategy;
import org.mule.umo.security.CryptoFailureException;
import org.mule.umo.security.EncryptionStrategyNotFoundException;
import org.mule.umo.security.UMOCredentials;
import org.mule.umo.security.UMOSecurityManager;

import java.util.StringTokenizer;

/**
 * <code>MuleCredentials</code> can be used to read and set Mule user information
 * that can be stored in a message header
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class MuleCredentials implements UMOCredentials
{
    public static final String TOKEN_DELIM = "::";

    private String username;
    private char[] password;
    private Object roles;
    private String scheme;

    public MuleCredentials(String username, char[] password)
    {
        this.username = username;
        this.password = password;
    }

    public MuleCredentials(String username, char[] password, Object roles)
    {
        this.username = username;
        this.password = password;
        this.roles = roles;
    }

    public MuleCredentials(String header) throws EncryptionStrategyNotFoundException, CryptoFailureException
    {
        int i = header.indexOf(" ");
        if (i == -1)
        {
            throw new IllegalArgumentException(new Message(Messages.HEADER_X_MALFORMED_VALUE_IS_X,
                MuleProperties.MULE_USER_PROPERTY, header).toString());
        }
        else
        {
            scheme = header.substring(0, i);
        }
        String creds = header.substring(i + 1);
        if (!scheme.equals("Plain"))
        {
            UMOSecurityManager sm = MuleManager.getInstance().getSecurityManager();

            UMOEncryptionStrategy es = sm.getEncryptionStrategy(scheme);
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
        return password;
    }

    public Object getRoles()
    {
        return roles;
    }

    public static String createHeader(String username, char[] password)
    {
        StringBuffer buf = new StringBuffer();
        buf.append("Plain ");
        buf.append(username).append(TOKEN_DELIM);
        buf.append(password).append(TOKEN_DELIM);

        return buf.toString();
    }

    public static String createHeader(String username,
                                      String password,
                                      String encryptionName,
                                      UMOEncryptionStrategy es) throws CryptoFailureException
    {
        StringBuffer buf = new StringBuffer();
        buf.append(encryptionName).append(" ");
        String creds = username + TOKEN_DELIM + password;
        byte[] encrypted = es.encrypt(creds.getBytes(), null);
        buf.append(new String(encrypted));

        return buf.toString();
    }
}
