/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp.util;

import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;

public class BouncyCastleUtil {

    public static final BcKeyFingerprintCalculator keyFingerprintCalculator = new BcKeyFingerprintCalculator();

    private static final BcPGPDigestCalculatorProvider pgpDigestCalculatorProvider = new BcPGPDigestCalculatorProvider();

    public static final BcPBESecretKeyDecryptorBuilder pbeSecretKeyDecryptorBuilder = new BcPBESecretKeyDecryptorBuilder(pgpDigestCalculatorProvider);

}
