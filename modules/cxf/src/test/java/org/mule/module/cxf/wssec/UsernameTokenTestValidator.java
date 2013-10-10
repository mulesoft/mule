/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.wssec;

import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.validate.Credential;
import org.apache.ws.security.validate.Validator;
import org.apache.ws.security.message.token.UsernameToken;

public class UsernameTokenTestValidator implements Validator
{

    @Override
    public Credential validate(Credential credential, RequestData data) throws WSSecurityException
    {
        UsernameToken usernameToken = credential.getUsernametoken();

        if(!"secret".equals(usernameToken.getPassword()))
        {
            throw new WSSecurityException(WSSecurityException.FAILED_AUTHENTICATION);
        }

        return credential;
    }
}
