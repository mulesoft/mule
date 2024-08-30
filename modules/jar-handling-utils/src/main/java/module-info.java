/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * This is a multi release module that allows handling of JDK internal JAR classes needed for Java 8 runtime and a cleaner way in 11+.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.jar.handling.utils {

  requires org.apache.commons.io;

  exports org.mule.runtime.core.internal.util.jar to
      org.mule.runtime.artifact.activation,
      org.mule.runtime.deployment.model.impl;
  exports org.mule.runtime.module.artifact.api.classloader.jar to
      org.mule.runtime.artifact;

}
