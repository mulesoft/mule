/*
 * $Id: 
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.wssecurity.validationHandler;

import java.security.cert.X509Certificate;
import java.util.Vector;

import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSUsernameTokenPrincipal;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.codehaus.xfire.MessageContext;
import org.codehaus.xfire.handler.AbstractHandler;

import sun.security.x509.X500Name;

public class ValidateUserTokenHandler extends AbstractHandler
{

    public void invoke(MessageContext context)throws Exception
    {
        Vector result = (Vector) context.getProperty(WSHandlerConstants.RECV_RESULTS);
        for (int i = 0; i < result.size(); i++)
        {
            WSHandlerResult res = (WSHandlerResult) result.get(i);
            for (int j = 0; j < res.getResults().size(); j++)
            {
                WSSecurityEngineResult secRes = (WSSecurityEngineResult) res.getResults().get(j);
                int action  = secRes.getAction();
                // USER TOKEN
                if( (action &  WSConstants.UT )>0   ){
                WSUsernameTokenPrincipal principal = (WSUsernameTokenPrincipal) secRes
                        .getPrincipal();
                // Set user property to user from UT to allow response encryption
                context.setProperty(WSHandlerConstants.ENCRYPTION_USER,principal.getName());
                System.out.print("User : " + principal.getName() + " password : "
                        + principal.getPassword() + "\n");
                }
                // SIGNATURE
                if( ( action & WSConstants.SIGN ) > 0 ){
                    X509Certificate cert = secRes.getCertificate();
                    X500Name principal = (X500Name) secRes.getPrincipal();
                    // Do something with cert
                    System.out.print("Signature for : "  + principal.getCommonName());
                }
            }
        }
    }
}