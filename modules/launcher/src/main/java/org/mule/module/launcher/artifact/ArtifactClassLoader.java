/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.artifact;

import org.mule.module.launcher.DisposableClassLoader;
import org.mule.module.launcher.LocalResourceLocator;

import java.net.URL;

public interface ArtifactClassLoader extends DisposableClassLoader, LocalResourceLocator
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
     * ClassLoader is an abstract class. Not an interface.
     * There are parts of the code that requires a ClassLoader and others that requires an ArtifactClassLoader.
     * Ideally I would make ArtifactClassLoader implement ClassLoader interface but there's no such interface.
     *
     * So if I have a method that requires a ClassLoader instance and an ArtifactClassLoader I would have to down cast and assume that it can be down casted or send two parameters, one for the ClassLoader and one for the ArtifactClassLoader:
     *
     * public void doSomething(ArtifactClassLoader acl)
     * {
     *   doSomething2(acl); //this requires an ArtifactClassLoader
     *   doSomething3((ClassLoader)acl); //this requires a ClassLoader
     * }
     *
     * public void doSomething(ArtifactClassLoader acl, ClassLoader cl)
     * {
     *   doSomething2(acl); //this requires an ArtifactClassLoader
     *   doSomething3(cl); //this requires a ClassLoader
     * }
     *
     * To overcome that problem seems much better to have a method in ArtifactClassLoader that can actually return a ClassLoader instance:
     *
     * public void doSomething(ArtifactClassLoader acl)
     * {
     *   doSomething2(acl); //this requires an ArtifactClassLoader
     *   doSomething3(acl.getDomainClassLoader()); //this requires a ClassLoader
     * }
     * @return class loader to use for this artifact.
     */
    ClassLoader getClassLoader();

    /**
     * Adds a shutdown listener to the class loader. This listener will be invoked synchronously right
     * before the class loader is disposed and closed.
     */
    void addShutdownListener(ShutdownListener listener);

}
