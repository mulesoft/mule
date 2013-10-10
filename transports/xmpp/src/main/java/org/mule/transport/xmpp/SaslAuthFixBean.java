/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.xmpp;

import org.jivesoftware.smack.SASLAuthentication;

/**
 * Instantiate this bean in your spring config to work around a known issue with the SMACK
 * API: http://www.igniterealtime.org/issues/browse/SMACK-264
 */
public class SaslAuthFixBean
{
    public SaslAuthFixBean()
    {
        super();
        
        // fix SASL auth
        SASLAuthentication.supportSASLMechanism("PLAIN", 0);
    }
}


