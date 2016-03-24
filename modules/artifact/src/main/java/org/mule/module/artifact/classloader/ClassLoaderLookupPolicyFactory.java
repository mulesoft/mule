/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.artifact.classloader;

import org.mule.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 *  Creates {@link ClassLoaderLookupPolicy} instances
 */
public class ClassLoaderLookupPolicyFactory
{

    public static final String PACKAGE_SEPARATOR = ".";
    public static final String BLOCKED_PACKAGE_PREFIX = "-";
    private static final String LIST_SEPARATOR = ",";

    private static String[] systemPackages = {
            "java.",
            "javax.",
            "org.mule.",
            "com.mulesoft.",
    };

    /**
     * Creates a new lookup policy
     *
     * @param config comma separated list of overridden and blocked packages.
     * @return a new lookup policy matching the provided config
     * @throws IllegalArgumentException in case there is an attempt to override or block
     * a system package.
     */
    public ClassLoaderLookupPolicy create(String config)
    {
        Set<String> overridden = new HashSet<>();
        Set<String> blocked = new HashSet<>();

        if (!StringUtils.isEmpty(config))
        {
            final String[] overrides = config.split(LIST_SEPARATOR);

            if (overrides.length != 0)
            {
                for (String override : overrides)
                {
                    override = StringUtils.defaultString(override).trim();

                    // 'blocked' package definitions come with a '-' prefix
                    boolean isBlocked = override.startsWith(BLOCKED_PACKAGE_PREFIX);
                    if (isBlocked)
                    {
                        override = override.substring(1);
                    }

                    String dottedOverride;
                    if (override.endsWith(PACKAGE_SEPARATOR))
                    {
                        dottedOverride = override;
                        override = override.substring(0, override.length() - 1);
                    }
                    else
                    {
                        dottedOverride = override + PACKAGE_SEPARATOR;
                    }

                    if (isSystemPackage(dottedOverride))
                    {
                        throw new IllegalArgumentException("Can't override a system package. Offending value: " + override);
                    }

                    overridden.add(override);
                    if (isBlocked)
                    {
                        blocked.add(override);
                    }
                }
            }
        }

        return new ClassLoaderLookupPolicy(overridden, blocked);
    }

    private boolean isSystemPackage(String override)
    {
        for (String systemPackage : systemPackages)
        {
            if (override.startsWith(systemPackage))
            {
                return true;
            }
        }

        return false;
    }
}
