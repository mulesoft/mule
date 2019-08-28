/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import static org.apache.commons.io.IOUtils.copy;
import static org.mule.module.pgp.config.PGPOutputMode.ARMOR;
import org.mule.module.pgp.config.PGPOutputMode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Provider;
import java.util.Date;

import org.apache.commons.lang.Validate;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.operator.PGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyKeyEncryptionMethodGenerator;

public class EncryptStreamTransformer implements StreamTransformer
{

    private PGPPublicKey publicKey;
    private Provider provider;
    private final int algorithm;
    private OutputStream pgpOutputStream;
    private OutputStream compressedEncryptedOutputStream;
    private OutputStream encryptedOutputStream;
    private ByteArrayOutputStream outputStream;
    private PGPOutputMode pgpOutputMode;
    private OutputStream result;
    private String streamName = "stream";
    private boolean signatureEnabled = false;
    private PGPPrivateKey signerPrivateKey = null;
    private String signerId;
    private PGPSignatureGenerator signatureGenerator = null;

    public EncryptStreamTransformer(PGPPublicKey publicKey, Provider provider, int algorithm, PGPOutputMode pgpOutputMode, String streamName) throws IOException
    {
        this(publicKey, provider, algorithm, pgpOutputMode);
        if (streamName != null)
        {
            this.streamName = streamName;
        }
    }

    public EncryptStreamTransformer(PGPPublicKey publicKey, Provider provider, int algorithm, PGPOutputMode pgpOutputMode) throws IOException
    {
        Validate.notNull(publicKey, "The publicKey should not be null");
        this.publicKey = publicKey;
        this.provider = provider;
        this.algorithm = algorithm;
        this.pgpOutputMode = pgpOutputMode;
        outputStream = new ByteArrayOutputStream();
    }

    public EncryptStreamTransformer signContentsWith(PGPPrivateKey signerPrivateKey, String signerId)
    {
        this.signerPrivateKey = signerPrivateKey;
        this.signerId = signerId;
        signatureEnabled = true;
        return this;
    }

    @Override
    public InputStream process(InputStream data) throws Exception
    {
        if (pgpOutputMode == ARMOR)
        {
            result = new ArmoredOutputStream(outputStream);
        }
        else
        {
            result = outputStream;
        }


        BcPGPDataEncryptorBuilder encryptorBuilder = new BcPGPDataEncryptorBuilder(algorithm);

        if (signatureEnabled)
        {
            encryptorBuilder.setWithIntegrityPacket(true);
        }
        
        PGPEncryptedDataGenerator encrDataGen = new PGPEncryptedDataGenerator(encryptorBuilder, false);
        BcPublicKeyKeyEncryptionMethodGenerator methodGenerator = new BcPublicKeyKeyEncryptionMethodGenerator(this.publicKey);
        encrDataGen.addMethod(methodGenerator);
        encryptedOutputStream = encrDataGen.open(result, new byte[1 << 16]);

        PGPCompressedDataGenerator comprDataGen = new PGPCompressedDataGenerator(PGPCompressedData.ZIP);
        compressedEncryptedOutputStream = comprDataGen.open(encryptedOutputStream, new byte[1 << 16]);

        if (signatureEnabled)
        {
            PGPContentSignerBuilder signerBuilder = new BcPGPContentSignerBuilder(signerPrivateKey.getPublicKeyPacket().getAlgorithm(), HashAlgorithmTags.SHA1);
            signatureGenerator = new PGPSignatureGenerator(signerBuilder);
            signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, signerPrivateKey);

            PGPSignatureSubpacketGenerator signatureSubpacketGenerator = new PGPSignatureSubpacketGenerator();
            signatureSubpacketGenerator.setSignerUserID(false, signerId);
            signatureGenerator.setHashedSubpackets(signatureSubpacketGenerator.generate());

            signatureGenerator.generateOnePassVersion(false).encode(compressedEncryptedOutputStream);
        }

        PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator();
        pgpOutputStream = lData.open(compressedEncryptedOutputStream, PGPLiteralData.BINARY, streamName,
                                     new Date(), new byte[1 << 16]);

        write(data);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }


    private void write(InputStream toBeEncrypted) throws IOException, PGPException
    {
        try
        {
            if (!signatureEnabled)
            {
                copy(toBeEncrypted, pgpOutputStream);
            }
            else
            {
                // By hand literal data generation stream handling
                byte[] buf = new byte[1 << 16];
                int readBytesLength;
                while ((readBytesLength = toBeEncrypted.read(buf)) > 0)
                {
                    pgpOutputStream.write(buf, 0, readBytesLength);
                    signatureGenerator.update(buf, 0, readBytesLength);
                }
            }
        }
        finally
        {
            pgpOutputStream.close();
            if (signatureEnabled)
            {
                signatureGenerator.generate().encode(compressedEncryptedOutputStream);
            }
            compressedEncryptedOutputStream.close();
            encryptedOutputStream.close();
            result.close();
            toBeEncrypted.close();
            outputStream.close();
        }
    }

}
