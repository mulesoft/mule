/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.artifact;

import java.lang.String;
import java.net.URL;

/**
 * Typically a class loader first delegates resources look up to its parent, this may not be the required behaviour in
 * all cases.
 * This interface allows to define custom strategies to locate resources for artifacts, allowing to look for resources
 * in application local folders first and the to domain or container based directories.
 */
public interface ResourceLocator
{

    /**
     * @param name name of the resource to find.
     * @return the resource URL, null if it doesn't exists.
     */
    public URL locateResource(String name);

}
