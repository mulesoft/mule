/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.wssec;

public class SpringSecurityProxyTestCase extends UsernameTokenProxyTestCase
{

    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {
                             "org/mule/runtime/module/cxf/wssec/cxf-secure-proxy-security-manager-flow.xml, org/mule/runtime/module/cxf/wssec/spring-security-ws-security-conf.xml",
                             "org/mule/runtime/module/cxf/wssec/spring-security-ws-security-conf.xml"
        };
    }
}
