/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl.api;

/**
 * Provides methods to access the configuration of a key store.
 */
public interface TlsContextKeyStoreConfiguration extends TlsContextStoreConfiguration
{

    /**
     * @return The alias of the private key to use.
     */
    public String getAlias();

    /**
     * @return The password used to access the private key.
     */
    public String getKeyPassword();

}
