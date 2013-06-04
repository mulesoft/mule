
package org.mule.config;

import org.mule.api.config.ConfigurationException;
import org.mule.api.config.MuleVersionChecker;
import org.mule.config.i18n.CoreMessages;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * <code>DefaultMuleVersionChecker</code> is the default implementation of
 * {@link org.mule.api.config.MuleVersionChecker}
 */
public class DefaultMuleVersionChecker implements MuleVersionChecker
{

    protected transient static final Log logger = LogFactory.getLog(DefaultMuleVersionChecker.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMuleVersion()
    {
        return MuleManifest.getProductVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void assertRuntimeGreaterOrEquals(String minVersion) throws ConfigurationException
    {

        if (StringUtils.isBlank(minVersion))
        {
            throw new IllegalArgumentException("Cannot validate a blank min mule version");
        }

        String runtimeVersion = this.getMuleVersion();

        if (runtimeVersion.equals("Unknown"))
        {
            logger.warn("Unknown Mule runtime version. This module may not work properly!");

        }
        else
        {

            String[] expectedMinVersion = minVersion.split("\\.");
            if (runtimeVersion.contains("-"))
            {
                runtimeVersion = runtimeVersion.split("-")[0];
            }

            String[] currentRuntimeVersion = runtimeVersion.split("\\.");
            int i = 0;

            for (; (i < currentRuntimeVersion.length); i++)
            {
                try
                {
                    if (Integer.parseInt(currentRuntimeVersion[i]) > Integer.parseInt(expectedMinVersion[i]))
                    {
                        break;
                    }
                    if (Integer.parseInt(currentRuntimeVersion[i]) < Integer.parseInt(expectedMinVersion[i]))
                    {
                        this.throwException(minVersion);
                    }
                }
                catch (NumberFormatException nfe)
                {
                    logger.warn(String.format(
                        "Error parsing Mule version '%s', cannot validate current Mule version",
                        runtimeVersion));
                }
                catch (ArrayIndexOutOfBoundsException iobe)
                {
                    logger.warn(String.format(
                        "Error parsing Mule version '%s', cannot validate current Mule version",
                        runtimeVersion));
                }
            }

            for (; i < expectedMinVersion.length; i++)
            {
                if (!"0".equals(expectedMinVersion[i]))
                {
                    this.throwException(minVersion);
                }
            }
        }
    }

    private void throwException(String minVersion) throws ConfigurationException
    {
        throw new ConfigurationException(CoreMessages.minMuleVersionNotMet(minVersion));
    }

}
