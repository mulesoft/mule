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

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleRuntimeException;
import org.mule.config.i18n.CoreMessageConstants;
import org.mule.config.i18n.Message;
import org.mule.util.ClassUtils;

import java.security.Provider;

/**
 * Automatically discovers the JDK we are running on and returns a
 * corresponding {@link SecurityProviderInfo}.
 * <p/>
 * Implementations of this class are thread-safe.
 *
 * @author <a href="mailto:aperepel@gmail.com">Andrew Perepelytsya</a>
 */
public class AutoDiscoverySecurityProviderFactory implements SecurityProviderFactory {

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
//    private static final SecurityProviderInfo IBM_SECURITY_PROVIDER_2 = new IBMSecurityProvider2Info();


    public SecurityProviderInfo getSecurityProviderInfo() {
        SecurityProviderInfo info;

        if (SystemUtils.JAVA_VM_VENDOR.toUpperCase().indexOf("IBM") != -1) {
            // TODO test IBM JDK 1.4.2 more thoroughly and decide if
            // it's worth including this newer provider support.
            // switch to IBM's security provider
//            if (SystemUtils.isJavaVersionAtLeast(142)) {
                // IBM JSSE2
//                info = IBM_SECURITY_PROVIDER_2;
//            } else {
                // older IBM JSSE
                info = IBM_SECURITY_PROVIDER;
//            }
        } else {
            info = DEFAULT_SECURITY_PROVIDER;

        }

        // BEA's JRockit uses Sun's JSSE, so defaults are fine

        return info;
    }

    public Provider getProvider() {
        SecurityProviderInfo info = getSecurityProviderInfo();

        if (logger.isInfoEnabled()) {
            logger.info("Using " + info.getClass().getName());
        }

        try {
            return (Provider) ClassUtils.instanciateClass(info.getProviderClass(), null);
        } catch (Exception ex) {
            throw new MuleRuntimeException(
                    new Message("core", CoreMessageConstants.FAILED_TO_INITIALIZE_SECURITY_PROVIDER,
                                info.getProviderClass()),
                    ex);
        }
    }
}
