/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 */


package org.mule.umo.security.provider;

/**
 * IBM JDK-specific security provider information.
 */
public class IBMSecurityProviderInfo implements SecurityProviderInfo {

    private static final String KEY_MANAGER_ALGORITHM = "IbmX509";

    private static final String PROTOCOL_HANDLER = "com.ibm.net.ssl.internal.www.protocol";

    private static final String PROVIDER_CLASS = "com.ibm.jsse.IBMJSSEProvider";

    public String getKeyManagerAlgorithm() {
        return KEY_MANAGER_ALGORITHM;
    }

    public String getProtocolHandler() {
        return PROTOCOL_HANDLER;
    }

    public String getProviderClass() {
        return PROVIDER_CLASS;
    }

}
