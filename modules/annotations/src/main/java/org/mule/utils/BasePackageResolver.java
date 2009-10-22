/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.utils;

import org.springframework.core.io.Resource;

/**
 * Implementations of this interface are used to figure out the base path for classes that will be scanned for
 * annotations.
 */
public interface BasePackageResolver
{
    String getPackage(Resource resource);
}
