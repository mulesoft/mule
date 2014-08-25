/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

/**
 *  Class to check that system properties introduced to maintain backwards compatibility
 *  are enabled or not, the latter being the default. It can be used to track said properties.
 *
 *  @since 3.6.0
 */
public class BackwardsCompatibilityPropertyChecker extends PropertyChecker
{
    public BackwardsCompatibilityPropertyChecker(String propertyName)
    {
        super(propertyName);
    }
}
