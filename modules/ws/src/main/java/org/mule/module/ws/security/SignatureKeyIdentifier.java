/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.security;

/**
 * an Enum with all the possible Signature Key Identifier values.
 */
public enum SignatureKeyIdentifier {

    DirectReference,
    IssuerSerial,
    X509KeyIdentifier,
    SKIKeyIdentifier,
    EmbeddedKeyName,
    Thumbprint,
    EncryptedKeySHA1
}
