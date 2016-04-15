/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.wssec;

import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.saml.ext.OpenSAMLUtil;
import org.apache.ws.security.validate.Credential;
import org.apache.ws.security.validate.SamlAssertionValidator;

public class SAMLCustomValidator extends SamlAssertionValidator
{

    private boolean requireSenderVouches = true;

    public void setRequireSenderVouches(boolean requireSenderVouches) {
        this.requireSenderVouches = requireSenderVouches;
    }

    @Override
    public Credential validate(Credential credential, RequestData data) throws WSSecurityException
    {
        Credential returnedCredential = super.validate(credential, data);

        //
        // Do some custom validation on the assertion
        //
        AssertionWrapper assertion = credential.getAssertion();
        if (!"www.example.com".equals(assertion.getIssuerString())) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity");
        }
        
        if (assertion.getSaml2() == null) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity");
        }
        
        String confirmationMethod = assertion.getConfirmationMethods().get(0);
        if (confirmationMethod == null) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity");
        }
        if (requireSenderVouches && !OpenSAMLUtil.isMethodSenderVouches(confirmationMethod)) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity");
        } else if (!requireSenderVouches
                   && !OpenSAMLUtil.isMethodHolderOfKey(confirmationMethod)) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity");
        }

        if(!"uid=joe,ou=people,ou=saml-demo,o=example.com".equals(assertion.getSaml2().getSubject().getNameID().getValue()))
        {
            throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity");
        }

        return returnedCredential;
    }


}
