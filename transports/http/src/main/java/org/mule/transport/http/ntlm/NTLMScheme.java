/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.ntlm;

import jcifs.ntlmssp.NtlmMessage;
import jcifs.ntlmssp.Type2Message;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.auth.AuthChallengeParser;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.AuthenticationException;
import org.apache.commons.httpclient.auth.InvalidCredentialsException;
import org.apache.commons.httpclient.auth.MalformedChallengeException;

import static jcifs.util.Base64.encode;

/**
 * Reimplements {@link org.apache.commons.httpclient.auth.NTLMScheme} using JCIFS
 * org.apache.commons.httpclient.auth.NTLMScheme. <p>
 * This class has to be registered manually in order to be used:
 * <code>
 * AuthPolicy.registerAuthScheme(AuthPolicy.NTLM, NTLMScheme.class);
 * </code>
 */
public class NTLMScheme implements AuthScheme
{

    private static enum AUTHENTICATION_STATE
    {
        UNINITIATED, INITIATED, TYPE1_MSG_GENERATED, TYPE2_MSG_RECEIVED, TYPE3_MSG_GENERATED, FAILED
    }

    private static final String NOT_IMPLEMENTED_ERROR = "Not implemented as it is deprecated anyway in Httpclient 3.x";

    /**
     * Authentication process authenticationState
     */
    private AUTHENTICATION_STATE authenticationState = AUTHENTICATION_STATE.UNINITIATED;

    /**
     * NTLM challenge string received form the server.
     */
    private String receivedNtlmChallenge = null;

    /**
     * Creates NTLM messages used during the authentication process.
     */
    private final NtlmMessageFactory ntlmMessageFactory = new NtlmMessageFactory();

    public String authenticate(Credentials credentials, HttpMethod method) throws AuthenticationException
    {
        if (authenticationState == AUTHENTICATION_STATE.UNINITIATED)
        {
            throw new IllegalStateException("NTLM authentication process has not been initiated");
        }

        NtlmMessage response;

        NTCredentials ntcredentials = getNTCredentials(credentials);
        if (authenticationState == AUTHENTICATION_STATE.INITIATED || authenticationState == AUTHENTICATION_STATE.FAILED)
        {
            // Original implementation used ntCredential info instead of null values
            response = ntlmMessageFactory.createType1Message(null, null);
            authenticationState = AUTHENTICATION_STATE.TYPE1_MSG_GENERATED;
        }
        else
        {
            Type2Message type2MessageFromChallenge = ntlmMessageFactory.createType2Message(receivedNtlmChallenge);
            response = ntlmMessageFactory.createType3Message(ntcredentials, type2MessageFromChallenge);
            authenticationState = AUTHENTICATION_STATE.TYPE3_MSG_GENERATED;
        }

        return ntlmMessageToString(response);
    }

    private NTCredentials getNTCredentials(Credentials credentials) throws InvalidCredentialsException
    {
        try
        {
            return (NTCredentials) credentials;
        }
        catch (ClassCastException e)
        {
            throw new InvalidCredentialsException("Credentials cannot be used for NTLM authentication: "
                                                  + credentials.getClass().getName());
        }
    }

    public String authenticate(Credentials credentials, String method, String uri) throws AuthenticationException
    {
        throw new RuntimeException(NOT_IMPLEMENTED_ERROR);
    }

    public String getID()
    {
        throw new RuntimeException(NOT_IMPLEMENTED_ERROR);
    }

    /**
     * Returns the authentication parameter with the given name, if available.
     * <p/>
     * There are no valid parameters for NTLM authentication so this method
     * <p/>
     * always returns <tt>null</tt>.
     * <p/>
     *
     * @param name The name of the parameter to be returned
     * @return the parameter with the given name
     */
    public String getParameter(String name)
    {
        if (name == null)
        {
            throw new IllegalArgumentException("Parameter name may not be null");
        }

        return null;
    }

    /**
     * The concept of an authentication realm is not supported by the NTLM
     * authentication scheme. Always returns <code>null</code>.
     *
     * @return <code>null</code>
     */

    public String getRealm()
    {
        return null;
    }

    /**
     * Returns textual designation of the NTLM authentication scheme.
     *
     * @return <code>ntlm</code>
     */
    public String getSchemeName()
    {
        return "ntlm";
    }

    /**
     * Tests if the NTLM authentication process has been completed.
     *
     * @return <tt>true</tt> if Basic authorization has been processed,
     *         <tt>false</tt> otherwise.
     */
    public boolean isComplete()
    {
        return authenticationState == AUTHENTICATION_STATE.TYPE3_MSG_GENERATED || authenticationState == AUTHENTICATION_STATE.FAILED;
    }

    /**
     * Returns <tt>true</tt>. NTLM authentication scheme is connection based.
     *
     * @return <tt>true</tt>.
     */
    public boolean isConnectionBased()
    {
        return true;
    }

    /**
     * Processes the NTLM challenge.
     *
     * @param challenge the challenge string
     * @throws MalformedChallengeException is thrown if the authentication challenge is malformed
     */
    public void processChallenge(final String challenge) throws MalformedChallengeException
    {
        String s = AuthChallengeParser.extractScheme(challenge);

        if (!s.equalsIgnoreCase(getSchemeName()))
        {
            throw new MalformedChallengeException("Invalid NTLM challenge: " + challenge);
        }

        int i = challenge.indexOf(' ');

        if (i != -1)
        {
            s = challenge.substring(i, challenge.length());
            receivedNtlmChallenge = s.trim();
            authenticationState = AUTHENTICATION_STATE.TYPE2_MSG_RECEIVED;
        }
        else
        {
            receivedNtlmChallenge = null;
            authenticationState = authenticationState == AUTHENTICATION_STATE.UNINITIATED ? AUTHENTICATION_STATE.INITIATED : AUTHENTICATION_STATE.FAILED;
        }
    }

    private String ntlmMessageToString(NtlmMessage ntlmMessage)
    {
        return "NTLM " + encode(ntlmMessage.toByteArray());
    }
}