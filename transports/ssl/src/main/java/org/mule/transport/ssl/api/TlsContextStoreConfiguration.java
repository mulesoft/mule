/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl.api;

/**
 * Provides methods to access the configuration of a store.
 */
public interface TlsContextStoreConfiguration
{

    /**
     * @return The location of the store.
     */
    public String getPath();

    /**
     * @return The password to access the store.
     */
    public String getPassword();

    /**
     * @return The type of store ("jks", "pkcs12", "jceks", or any other).
     */
    public String getType();

    /**
     * @return The algorithm used by the store.
     */
    public String getAlgorithm();

}
