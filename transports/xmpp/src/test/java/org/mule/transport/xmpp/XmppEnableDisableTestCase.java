/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
