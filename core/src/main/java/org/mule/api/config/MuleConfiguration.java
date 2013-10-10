/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.config;

/**
 * Configuration info. which can be set when creating the MuleContext but becomes
 * immutable after startup.
 */
public interface MuleConfiguration
{
    int getDefaultResponseTimeout();

    String getWorkingDirectory();

    String getMuleHomeDirectory();

    int getDefaultTransactionTimeout();

    boolean isClientMode();

    String getDefaultEncoding();

    String getId();

    String getDomainId();

    String getSystemModelType();

    String getSystemName();

    boolean isAutoWrapMessageAwareTransform();

    boolean isCacheMessageAsBytes();

    boolean isCacheMessageOriginalPayload();

    boolean isEnableStreaming();

    boolean isValidateExpressions();

    int getDefaultQueueTimeout();
    
    int getShutdownTimeout();

    /**
     * A container mode implies multiple Mule apps running. When true, Mule changes behavior in some areas, e.g.:
     * <ul>
     *     <li>Splash screens</li>
     *     <li>Thread names have app name in the prefix to guarantee uniqueness</li>
     * </ul>
     * etc.
     *
     * Note that e.g. a WAR-embedded Mule will run in container mode, but will still be considerd embedded
     * for management purposes.
     *
     * @see #isStandalone()
     */
    boolean isContainerMode();

    /**
     * Try to guess if we're embedded. If "mule.home" JVM property has been set, then we've been
     * started via Mule script and can assume we're running standalone. Otherwise (no property set), Mule
     * has been started via a different mechanism.
     * <p/>
     * A standalone Mule is always considered running in 'container' mode.
     *
     * @see #isContainerMode()
     */
    boolean isStandalone();
}
