/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.security.provider;

import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.ClassUtils;
import org.mule.util.SystemUtils;

import java.security.Provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Automatically discovers the JDK we are running on and returns a corresponding
 * {@link SecurityProviderInfo}. <p/> Implementations of this class are thread-safe.
 */
public class AutoDiscoverySecurityProviderFactory implements SecurityProviderFactory
{

    /**
     * Default is Sun's JSSE.
     */
    public static final SecurityProviderInfo DEFAULT_SECURITY_PROVIDER = new SunSecurityProviderInfo();

    /**
     * Logger used by this class.
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    /**
     * Security provider properties for IBM JDK.
     */
    private static final SecurityProviderInfo IBM_SECURITY_PROVIDER = new IBMSecurityProviderInfo();

    /**
     * Security provider properties for IBM JDK 1.4.2 and higher.
     */
    // private static final SecurityProviderInfo IBM_SECURITY_PROVIDER_2 = new
    // IBMSecurityProvider2Info();

    public SecurityProviderInfo getSecurityProviderInfo()
    {
        SecurityProviderInfo info;

        if (SystemUtils.isIbmJDK())
        {
            // TODO test IBM JDK 1.4.2 more thoroughly and decide if
            // it's worth including this newer provider support.
            // switch to IBM's security provider
            // if (SystemUtils.isJavaVersionAtLeast(142)) {
            // IBM JSSE2
            // info = IBM_SECURITY_PROVIDER_2;
            // } else {
            // older IBM JSSE
            info = IBM_SECURITY_PROVIDER;
            // }
        }
        else
        {
            info = DEFAULT_SECURITY_PROVIDER;

        }

        // BEA's JRockit uses Sun's JSSE, so defaults are fine

        return info;
    }

    public Provider getProvider()
    {
        SecurityProviderInfo info = getSecurityProviderInfo();

        if (logger.isInfoEnabled())
        {
            logger.info("Using " + info.getClass().getName());
        }

        try
        {
            return (Provider) ClassUtils.instanciateClass(info.getProviderClass());
        }
        catch (Exception ex)
        {
            throw new MuleRuntimeException(
                CoreMessages.failedToInitSecurityProvider(info.getProviderClass()), ex);
        }
    }
}
