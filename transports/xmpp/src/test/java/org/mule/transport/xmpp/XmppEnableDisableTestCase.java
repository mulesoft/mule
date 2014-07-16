/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import org.mule.tck.AbstractServiceAndFlowTestCase;

/**
 * This should be the superclass for all unit tests in the XMPP transport that require an external
 * Jabber server to work with. It allows to enable or disable unit tests by setting the system
 * property <em>jabber.test.enabled</em>.
 */
public abstract class XmppEnableDisableTestCase extends AbstractServiceAndFlowTestCase
{
    public XmppEnableDisableTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    public static boolean isTestDisabled()
    {
        boolean testEnabled = Boolean.getBoolean("jabber.test.enabled");
        return (testEnabled == false);
    }
    
    @Override
    protected boolean isDisabledInThisEnvironment()
    {
        return XmppEnableDisableTestCase.isTestDisabled();
    }
}
