/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.artifact;

import org.mule.api.MuleContext;

import java.net.URL;

public interface ArtifactClassLoader
{

    /**
     * @return the artifact unique identifier
     */
    String getArtifactName();

    /**
     * @param resource name of the resource to find.
     * @return the resource URL, null if it doesn't exists.
     */
    URL findResource(String resource);

    /**
     * @return the mule context created for this artifact
     */
    MuleContext getMuleContext();

    /**
     * Unfortunately ClassLoader is an abstract class and not an interface so in
     * order to allow a class loader to extend any subclass of ClassLoader this method
     * is required. Most implementations will return this.
     *
     * @return the class loader represented by this class.
     */
    ClassLoader getClassLoader();
}
