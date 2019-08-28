/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tls.internal.revocation;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.core.privileged.security.RevocationCheck;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertPathBuilder;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.EnumSet;
import java.util.Set;

import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.ManagerFactoryParameters;

/**
 * Uses a custom OCSP responder for certificate revocation checks, with a specific trusted certificate for
 * revocating other keys. This ignores extension points (additional tags for CRLDP or OCSP) present in the
 * certificate, if any.
 *
 * @since 4.1
 */
public class CustomOcspResponder extends AbstractComponent implements RevocationCheck {

  private String url;
  private String certAlias;

  public void setUrl(String url) {
    this.url = url;
  }

  public void setCertAlias(String certAlias) {
    this.certAlias = certAlias;
  }

  @Override
  public ManagerFactoryParameters configFor(KeyStore trustStore, Set<TrustAnchor> defaultTrustAnchors) {
    checkArgument(url != null, "tls:custom-ocsp-responder requires the 'url' attribute");
    checkArgument(trustStore != null, "tls:custom-ocsp-responder requires a trust store");

    try {
      CertPathBuilder cpb = CertPathBuilder.getInstance("PKIX");
      PKIXRevocationChecker rc = (PKIXRevocationChecker) cpb.getRevocationChecker();
      rc.setOptions(EnumSet.of(PKIXRevocationChecker.Option.NO_FALLBACK));

      if (url != null) {
        rc.setOcspResponder(new URI(url));
      }
      if (certAlias != null) {
        if (trustStore.isCertificateEntry(certAlias)) {
          rc.setOcspResponderCert((X509Certificate) trustStore.getCertificate(certAlias));
        } else {
          throw new IllegalStateException("Key with alias \"" + certAlias + "\" was not found");
        }
      }

      PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(trustStore, new X509CertSelector());
      pkixParams.addCertPathChecker(rc);

      return new CertPathTrustManagerParameters(pkixParams);
    } catch (GeneralSecurityException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CustomOcspResponder that = (CustomOcspResponder) o;

    if (url != null ? !url.equals(that.url) : that.url != null) {
      return false;
    }
    return certAlias != null ? certAlias.equals(that.certAlias) : that.certAlias == null;
  }

  @Override
  public int hashCode() {
    int result = url != null ? url.hashCode() : 0;
    result = 31 * result + (certAlias != null ? certAlias.hashCode() : 0);
    return result;
  }
}
