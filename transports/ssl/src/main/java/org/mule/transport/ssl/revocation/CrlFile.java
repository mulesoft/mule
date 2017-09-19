/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl.revocation;

import org.mule.api.security.tls.RevocationCheck;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.CertStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;

import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.ManagerFactoryParameters;

/**
 * Local file based certificate revocation checker, which requires a CRL file to be accessible and ignores
 * extension points (additional tags for CRLDP and OCSP) in the certificate.
 *
 * Note that the signer of the CRL must be the client certificate's CA, in case that's not possible consider
 * using {@link CustomOcspResponder}.
 *
 * @since 3.9
 */
public class CrlFile implements RevocationCheck
{
    private String path;

    public void setPath(String path)
    {
        this.path = path;
    }

    @Override
    public ManagerFactoryParameters configFor(KeyStore trustStore)
    {
        try
        {
            // When creating build parameters we must manually trust each certificate (which is automatic otherwise)
            Enumeration<String> aliases = trustStore.aliases();
            HashSet<TrustAnchor> trustAnchors = new HashSet<>();
            while (aliases.hasMoreElements())
            {
                String alias = aliases.nextElement();
                if (trustStore.isCertificateEntry(alias))
                {
                    trustAnchors.add(new TrustAnchor((X509Certificate) trustStore.getCertificate(alias), null));
                }
            }

            PKIXBuilderParameters pbParams = new PKIXBuilderParameters(trustAnchors, new X509CertSelector());

            // Make sure revocation checking is enabled (com.sun.net.ssl.checkRevocation)
            pbParams.setRevocationEnabled(true);

            Collection<? extends CRL> crls = loadCRL(path);
            if (crls != null && !crls.isEmpty())
            {
                pbParams.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(crls)));
            }

            return new CertPathTrustManagerParameters(pbParams);
        }
        catch (IOException | GeneralSecurityException e)
        {
            throw new RuntimeException(e);
        }
    }

    private Collection<? extends CRL> loadCRL(String crlPath) throws CertificateException, IOException, CRLException
    {
        Collection<? extends CRL> crlList = null;

        if (crlPath != null)
        {
            InputStream in = null;
            try
            {
                in = IOUtils.getResourceAsStream(crlPath, getClass());
                crlList = CertificateFactory.getInstance("X.509").generateCRLs(in);
            }
            finally
            {
                if (in != null)
                {
                    in.close();
                }
            }
        }

        return crlList;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        CrlFile crlFile = (CrlFile) o;

        return path != null ? path.equals(crlFile.path) : crlFile.path == null;
    }

    @Override
    public int hashCode()
    {
        return path != null ? path.hashCode() : 0;
    }
}
