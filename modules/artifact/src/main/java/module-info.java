/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Defines the descriptors of the deployable artifacts.
 * 
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.artifact {

  requires org.mule.runtime.api;
  requires org.mule.runtime.artifact.declaration;
  requires org.mule.runtime.core;

  requires org.apache.commons.collections4;

  exports org.mule.runtime.module.artifact.api;
  exports org.mule.runtime.module.artifact.api.classloader;
  exports org.mule.runtime.module.artifact.api.classloader.net;
  exports org.mule.runtime.module.artifact.api.descriptor;
  exports org.mule.runtime.module.artifact.api.plugin;
  exports org.mule.runtime.module.artifact.api.serializer;

  exports org.mule.runtime.module.artifact.internal.classloader to
      org.mule.runtime.deployment.model,
      org.mule.runtime.deployment.model.impl,
      org.mule.runtime.artifact.activation,
      org.mule.runtime.launcher;
  exports org.mule.runtime.module.artifact.internal.util to
      org.mule.runtime.deployment.model,
      org.mule.runtime.deployment.model.impl,
      org.mule.runtime.artifact.activation;

  uses org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfigurationLoader;
  uses org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorLoader;

  // Required only be resource releaser
  requires mule.mvel2;
  requires java.desktop;
  requires java.management;
  requires java.sql;

}