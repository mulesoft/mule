/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl.api;

/**
 * Provides methods to access the configuration of a trust store.
 */
public interface TlsContextTrustStoreConfiguration extends TlsContextStoreConfiguration
{
    /**
     * @return true if the trust store was configured and set as insecure, meaning no certificate validations will be performed.
     */
    boolean isInsecure();

}
