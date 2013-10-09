/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.Map;

/**
 * Sets system properties from the configured list.
 */
public class SystemPropertyInitializingBean implements InitializingBean {

    protected Log logger = LogFactory.getLog(getClass());

    private Map<String, String> systemProperties;

    /**
     * Sets the system properties
     */
    public void afterPropertiesSet() throws Exception {
        if (systemProperties == null || systemProperties.isEmpty()) {
            return;
        }

        for (Map.Entry<String, String> entry : systemProperties.entrySet()) {
            String key = entry.getKey();
            String value = systemProperties.get(key);

            if (logger.isInfoEnabled()) {
                logger.info(String.format("Setting system property: %s=%s", key, value));
            }

            System.setProperty(key, value);

        }
    }

    public void setSystemProperties(Map<String, String> systemProperties) {
        this.systemProperties = systemProperties;
    }
}
