/*
 * $Id
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
    private static final String PROTOCOL_HANDLER = "com.ibm.net.ssl.www2.protocol.Handler";

    private static final String PROVIDER_CLASS = "com.ibm.jsse2.IBMJSSEProvider2";

    public String getKeyManagerAlgorithm() {
        return IBMSecurityProvider2Info.KEY_MANAGER_ALGORITHM;
    }

    public String getProtocolHandler() {
        return IBMSecurityProvider2Info.PROTOCOL_HANDLER;
    }

    public String getProviderClass() {
        return IBMSecurityProvider2Info.PROVIDER_CLASS;
    }

}
