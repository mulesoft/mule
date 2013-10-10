/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    @Override
    public SecurityProviderInfo getSecurityProviderInfo()
    {
        SecurityProviderInfo info;

        if (SystemUtils.isIbmJDK())
        {
            info = IBM_SECURITY_PROVIDER;
        }
        else
        {
            info = DEFAULT_SECURITY_PROVIDER;

        }

        // BEA's JRockit uses Sun's JSSE, so defaults are fine

        return info;
    }

    @Override
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
