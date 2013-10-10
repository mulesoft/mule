/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
