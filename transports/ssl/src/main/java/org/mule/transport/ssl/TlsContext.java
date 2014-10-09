/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl;

public interface TlsContext
{
    public String getKeyStorePath();

    public String getKeyStoreType();

    public String getKeyStorePassword();

    public String getKeyManagerPassword();

    public String getKeyManagerAlgorithm();

    public String getTrustManagerAlgorithm();

    public String getTrustStorePath();

    public String getTrustStoreType();

    public String getTrustStorePassword();

}
