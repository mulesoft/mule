/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf.wssec;

import static org.apache.ws.security.saml.ext.builder.SAML2Constants.CONF_SENDER_VOUCHES;
import static org.opensaml.common.SAMLVersion.VERSION_20;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Collections;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.components.crypto.CryptoType;
import org.apache.ws.security.saml.ext.SAMLCallback;
import org.apache.ws.security.saml.ext.bean.AuthenticationStatementBean;
import org.apache.ws.security.saml.ext.bean.KeyInfoBean;
import org.apache.ws.security.saml.ext.bean.SubjectBean;

/**
 * Callback handler that populates a SAML 2 assertion based on the SAML properties file
 */
public class SAML2CallbackHandler implements CallbackHandler {

  private X509Certificate[] certs;
  private String subjectName;
  private String subjectQualifier;
  private String confirmationMethod;
  private KeyInfoBean.CERT_IDENTIFIER certIdentifier = KeyInfoBean.CERT_IDENTIFIER.X509_CERT;

  public SAML2CallbackHandler() throws WSSecurityException {
    if (certs == null) {
      Crypto crypto = CryptoFactory.getInstance("org/mule/compatibility/module/cxf/wssec/wssecurity.properties");
      CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
      cryptoType.setAlias("joe");
      certs = crypto.getX509Certificates(cryptoType);
    }


    subjectName = "uid=joe,ou=people,ou=saml-demo,o=example.com";
    subjectQualifier = "www.example.com";
    confirmationMethod = CONF_SENDER_VOUCHES;
  }

  @Override
  public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
    for (Callback callback : callbacks) {
      if (callback instanceof SAMLCallback) {
        SAMLCallback samlCallback = (SAMLCallback) callback;
        samlCallback.setSamlVersion(VERSION_20);
        SubjectBean subjectBean = new SubjectBean(subjectName, subjectQualifier, confirmationMethod);
        samlCallback.setSubject(subjectBean);
        createAndSetStatement(null, samlCallback);
      } else {
        throw new UnsupportedCallbackException(callback, "Unrecognized Callback");
      }
    }
  }


  private void createAndSetStatement(SubjectBean subjectBean, SAMLCallback callback) {
    AuthenticationStatementBean authBean = new AuthenticationStatementBean();
    if (subjectBean != null) {
      authBean.setSubject(subjectBean);
    }
    authBean.setAuthenticationMethod("Password");
    callback.setAuthenticationStatementData(Collections.singletonList(authBean));
  }

}
