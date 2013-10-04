/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
