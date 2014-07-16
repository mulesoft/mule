/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

/**
 * This interface is implemented for every {@link org.mule.api.annotations.Module}
 * and {@link org.mule.api.annotations.Connector} annotated class and its purpose is
 * to define a contract to query the annotated class about its metadata.
 */
public interface MetadataAware
{

    /**
     * Returns the user-friendly name of this module
     */
    public String getModuleName();

    /**
     * Returns the version of this module
     */
    public String getModuleVersion();

    /**
     * Returns the version of the DevKit used to create this module
     */
    public String getDevkitVersion();

    /**
     * Returns the build of the DevKit used to create this module
     */
    public String getDevkitBuild();

    /**
     * Returns the minimun compatible runtime version
     */
    public String getMinMuleVersion();

}
