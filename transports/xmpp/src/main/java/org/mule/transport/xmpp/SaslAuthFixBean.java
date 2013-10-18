/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


