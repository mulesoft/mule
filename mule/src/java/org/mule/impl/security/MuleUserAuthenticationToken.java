/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.impl.security;

import java.util.StringTokenizer;

/**
 * <code>MuleUserAuthenticationToken</code> can be used to read and
 * set Mule user information that can be stored in a message header
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class MuleUserAuthenticationToken
{
    public static final String TOKEN_DELIM = "::";

    private String username;
    private char[] password;
    private String roles;

    public MuleUserAuthenticationToken(String username, char[] password)
    {
        this.username = username;
        this.password = password;
    }

    public MuleUserAuthenticationToken(String header)
    {
        StringTokenizer st = new StringTokenizer(header, TOKEN_DELIM);
        username = st.nextToken();
        password = st.nextToken().toCharArray();
        if(st.hasMoreTokens()) {
            roles = st.nextToken();
        }
    }

    public String getToken()
    {
        StringBuffer buf = new StringBuffer();
        buf.append(username).append(TOKEN_DELIM);
        buf.append(password).append(TOKEN_DELIM);

        if(roles!=null) buf.append(roles);
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

    public String getRoles()
    {
        return roles;
    }
}
