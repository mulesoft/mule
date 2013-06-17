/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.config;

/**
 * This component performs validations to assure compatibility between a component's
 * minimun compatible mule version and the version of the current runtime.
 */
public interface MuleVersionChecker
{

    /**
     * @return the current runtime's version
     */
    public String getMuleVersion();

    /**
     * Validates that minVersion is greater or equals than the current runtime's
     * version
     * 
     * @param minVersion the minimum version you want to validate
     * @throws ConfigurationException if minVersion is greater than the runtime's
     *             version
     */
    public void assertRuntimeGreaterOrEquals(String minVersion) throws ConfigurationException;

}
