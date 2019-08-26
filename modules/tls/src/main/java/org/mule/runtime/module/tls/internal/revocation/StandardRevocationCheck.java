/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tls.internal.revocation;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.core.privileged.security.RevocationCheck;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertPathBuilder;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.ManagerFactoryParameters;

/**
 * Uses the standard JVM certificate revocation checks, which depend on the certificate having the
 * corresponding extension points (additional tags for CRLDP or OCSP), and the availability
 * of revocation servers.
 *
 * @since 4.1
 */
public class StandardRevocationCheck extends AbstractComponent implements RevocationCheck {

  private Boolean onlyEndEntities = false;
  private Boolean preferCrls = false;
  private Boolean noFallback = false;
  private Boolean softFail = false;

  public void setOnlyEndEntities(Boolean onlyEndEntities) {
    this.onlyEndEntities = onlyEndEntities;
  }

  public void setPreferCrls(Boolean preferCrls) {
    this.preferCrls = preferCrls;
  }

  public void setNoFallback(Boolean noFallback) {
    this.noFallback = noFallback;
  }

  public void setSoftFail(Boolean softFail) {
    this.softFail = softFail;
  }

  @Override
  public ManagerFactoryParameters configFor(KeyStore trustStore, Set<TrustAnchor> defaultTrustAnchors) {
    try {
      CertPathBuilder cpb = CertPathBuilder.getInstance("PKIX");
      PKIXRevocationChecker rc = (PKIXRevocationChecker) cpb.getRevocationChecker();

      Set<PKIXRevocationChecker.Option> options = new HashSet<>();
      if (onlyEndEntities) {
        options.add(PKIXRevocationChecker.Option.ONLY_END_ENTITY);
      }
      if (preferCrls) {
        options.add(PKIXRevocationChecker.Option.PREFER_CRLS);
      }
      if (noFallback) {
        options.add(PKIXRevocationChecker.Option.NO_FALLBACK);
      }
      if (softFail) {
        options.add(PKIXRevocationChecker.Option.SOFT_FAIL);
      }
      rc.setOptions(options);

      PKIXBuilderParameters pkixParams;
      if (trustStore != null) {
        pkixParams = new PKIXBuilderParameters(trustStore, new X509CertSelector());
      } else {
        pkixParams = new PKIXBuilderParameters(defaultTrustAnchors, new X509CertSelector());
      }

      pkixParams.addCertPathChecker(rc);

      return new CertPathTrustManagerParameters(pkixParams);
    } catch (GeneralSecurityException e) {
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

    StandardRevocationCheck that = (StandardRevocationCheck) o;

    if (onlyEndEntities != null ? !onlyEndEntities.equals(that.onlyEndEntities) : that.onlyEndEntities != null) {
      return false;
    }
    if (preferCrls != null ? !preferCrls.equals(that.preferCrls) : that.preferCrls != null) {
      return false;
    }
    if (noFallback != null ? !noFallback.equals(that.noFallback) : that.noFallback != null) {
      return false;
    }
    return softFail != null ? softFail.equals(that.softFail) : that.softFail == null;
  }

  @Override
  public int hashCode() {
    int result = onlyEndEntities != null ? onlyEndEntities.hashCode() : 0;
    result = 31 * result + (preferCrls != null ? preferCrls.hashCode() : 0);
    result = 31 * result + (noFallback != null ? noFallback.hashCode() : 0);
    result = 31 * result + (softFail != null ? softFail.hashCode() : 0);
    return result;
  }
}
