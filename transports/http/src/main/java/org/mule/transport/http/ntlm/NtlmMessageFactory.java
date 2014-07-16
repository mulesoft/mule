/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.ntlm;

import java.io.IOException;

import jcifs.ntlmssp.Type1Message;
import jcifs.ntlmssp.Type2Message;
import jcifs.ntlmssp.Type3Message;
import jcifs.util.Base64;
import org.apache.commons.httpclient.NTCredentials;

public class NtlmMessageFactory
{

    // Defines the default flags value set in the Type3Message. These flags must be set:
    // NEGOTIATE_VERSION
    // NEGOTIATE_TARGET_INFO
    // NEGOTIATE_EXTENDED_SECURITY
    // TARGET_TYPE_SERVER
    // NEGOTIATE_ALWAYS_SIGN
    // NEGOTIATE_NTLM_KEY
    // REQUEST_TARGET
    // NEGOTIATE_UNICODE
    public static final int DEFAULT_TYPE_3_MESSAGE_FLAGS = 0X88205;

    // Defines flags value to use in the Type1Message. These flags must be set:
    // NEGOTIATE_EXTENDED_SECURITY
    // NEGOTIATE_ALWAYS_SIGN
    // NEGOTIATE_NTLM_KEY
    // REQUEST_TARGET
    // NEGOTIATE_OEM
    // NEGOTIATE_UNICODE
    public static final int DEFAULT_TYPE_1_MESSAGE_FLAGS = 0X88207;

    /**
     * Creates a {@link Type1Message} for NTLM authentication.
     *
     * @param host the client host
     * @param domain the client domain
     * @return a {@link Type1Message} to initiate the authentication process.
     */
    public Type1Message createType1Message(String host, String domain)
    {
        Type1Message message = new Type1Message(DEFAULT_TYPE_1_MESSAGE_FLAGS, domain, host);

        // Type1Message constructor sets a default workstation name when host == null, so it
        // requires an override of that value in order to make it work
        if (host == null)
        {
            message.setSuppliedWorkstation(null);
        }

        return message;
    }

    /**
     * Creates a {@link Type2Message} for NTLM authentication from a challenge
     * received from the NTLM server.
     *
     * @param challenge the challenge received from the server in response to a
     *        {@link Type1Message} message previously sent.
     * @return a {@link Type2Message} to continue the authentication process.
     */
    public Type2Message createType2Message(String challenge)
    {
        try
        {
            return new Type2Message(Base64.decode(challenge));
        }
        catch (IOException e)
        {
            throw new RuntimeException("Invalid Type2 message", e);
        }
    }

    /**
     * Creates a {@link Type3Message} for NTLM authentication.
     *
     * @param ntCredentials the credentials used for the authentication
     * @param type2Message the {@link Type2Message} received from the server
     *        in response to a {@link Type1Message} message previously sent.
     * @return a {@link Type3Message} to continue the authentication process.
     */
    public Type3Message createType3Message(NTCredentials ntCredentials, Type2Message type2Message)
    {
        return new Type3Message(type2Message, ntCredentials.getPassword(), type2Message.getTarget(),
                                ntCredentials.getUserName(), ntCredentials.getHost(), DEFAULT_TYPE_3_MESSAGE_FLAGS);
    }
}
