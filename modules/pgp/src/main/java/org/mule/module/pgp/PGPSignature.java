/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.util.encoders.Base64;
import org.mule.util.SecurityUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.Security;

import static org.mule.module.pgp.util.BouncyCastleUtil.PBE_SECRET_KEY_DECRYPTOR_BUILDER;

public class PGPSignature
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(PGPSignature.class);

    private PGPSignatureAlgorithm algorithm;
    private final PGPKeyRing keyRing;

    public PGPSignature(PGPKeyRing keyRing,
                        String algorithmName) {
        if (!SecurityUtils.isFipsSecurityModel()) {
            Security.addProvider(new BouncyCastleProvider());
        }
        try {
            algorithm = algorithmName != null ?
                    PGPSignatureAlgorithm.valueOf(algorithmName) : PGPSignatureAlgorithm.SHA256;
        }
        catch (final IllegalArgumentException e) {
            throw new RuntimeException("Could not initialise PGP Signature: invalid algorithm " + algorithm, e);
        }
        this.keyRing = keyRing;
    }

    public String sign(byte[] data) {

    final ByteArrayOutputStream buffer = new ByteArrayOutputStream(1024);
        final BCPGOutputStream outputStream = new BCPGOutputStream(new ArmoredOutputStream(new BufferedOutputStream(buffer)));
        try {
            final PGPPrivateKey privateKey = getPrivateKey();
            final JcaPGPContentSignerBuilder signerBuilder =
                    new JcaPGPContentSignerBuilder(privateKey.getPublicKeyPacket().getAlgorithm(),
                    algorithm.getNumericId()).setProvider("BC");
            final PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(signerBuilder);
            signatureGenerator.init(org.bouncycastle.openpgp.PGPSignature.BINARY_DOCUMENT, privateKey);
            signatureGenerator.update(data);
            signatureGenerator.generate().encode(outputStream);
        } catch (final Exception e) {
            throw new IllegalStateException("Could not sign the data.", e);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }

        final byte[] encode = Base64.encode(buffer.toByteArray());
        return new String(encode);

    }
    public String sign(InputStream stream) {
        try {
            final byte[] data = IOUtils.toByteArray(stream);
            return sign(data);
        } catch (final Exception e) {
            throw new IllegalStateException("Could not sign the data.", e);
        }
    }
    public boolean validateSignature(byte[] data,  String expectedSignature) {
        // TODO Implement
        return false;

    }
    public boolean validateSignature(InputStream stream, String expectedSignature) {
         // TODO Implement
        return false;
    }

    public void setAlgorithm(PGPSignatureAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    private PGPPrivateKey getPrivateKey() throws Exception {
        final PGPSecretKey secretKey = keyRing.getSecretKey();
        final String secretPassphrase = keyRing.getSecretPassphrase();
        return secretKey.extractPrivateKey(PBE_SECRET_KEY_DECRYPTOR_BUILDER.build(secretPassphrase.toCharArray()));
    }
}
