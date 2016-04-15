/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
