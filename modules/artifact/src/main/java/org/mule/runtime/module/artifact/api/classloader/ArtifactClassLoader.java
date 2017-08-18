/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader;

import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

public interface ArtifactClassLoader extends DisposableClassLoader, LocalResourceLocator, ClassLoaderLookupPolicyProvider {

  /**
   * @return the artifact unique identifier
   */
  String getArtifactId();

  /**
   * @param <T> the generic type of the artifact descriptor.
   * @return the artifact descriptor corresponding to this classloader instance. Non null.
   */
  <T extends ArtifactDescriptor> T getArtifactDescriptor();

  /**
   * @param resource name of the resource to find.
   * @return the resource URL, null if it doesn't exists.
   */
  URL findResource(String resource);

  /**
   * Returns an enumeration of {@link java.net.URL <tt>URL</tt>} objects representing all the resources with the given name which
   * are local to the classloader
   *
   * @param name The resource name
   * @return An enumeration of {@link java.net.URL <tt>URL</tt>} objects for the resources
   * @throws IOException If I/O errors occur
   */
  Enumeration<URL> findResources(final String name) throws IOException;

  /**
   * Loads the class with the specified <a href="#name">binary name</a> if defined on this class loader.
   * 
   * @param name The <a href="#name">binary name</a> of the class
   * @return The resulting <tt>Class</tt> object
   * @throws ClassNotFoundException If the class was not found
   */
  Class<?> findLocalClass(String name) throws ClassNotFoundException;

  /**
   * ClassLoader is an abstract class. Not an interface. There are parts of the code that requires a ClassLoader and others that
   * requires an ArtifactClassLoader. Ideally I would make ArtifactClassLoader implement ClassLoader interface but there's no such
   * interface.
   *
   * So if I have a method that requires a ClassLoader instance and an ArtifactClassLoader I would have to down cast and assume
   * that it can be down casted or send two parameters, one for the ClassLoader and one for the ArtifactClassLoader:
   *
   * public void doSomething(ArtifactClassLoader acl) { doSomething2(acl); //this requires an ArtifactClassLoader
   * doSomething3((ClassLoader)acl); //this requires a ClassLoader }
   *
   * public void doSomething(ArtifactClassLoader acl, ClassLoader cl) { doSomething2(acl); //this requires an ArtifactClassLoader
   * doSomething3(cl); //this requires a ClassLoader }
   *
   * To overcome that problem seems much better to have a method in ArtifactClassLoader that can actually return a ClassLoader
   * instance:
   *
   * public void doSomething(ArtifactClassLoader acl) { doSomething2(acl); //this requires an ArtifactClassLoader
   * doSomething3(acl.getDomainClassLoader()); //this requires a ClassLoader }
   * 
   * @return class loader to use for this artifact.
   */
  ClassLoader getClassLoader();

  /**
   * Adds a shutdown listener to the class loader. This listener will be invoked synchronously right before the class loader is
   * disposed and closed.
   */
  void addShutdownListener(ShutdownListener listener);

}
