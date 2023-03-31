/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Provides container artifact related functionality.
 * 
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.container {
  
  requires org.mule.runtime.api;
  requires org.mule.runtime.core;

  requires org.mule.runtime.artifact;

  requires org.apache.commons.lang3;

  exports org.mule.runtime.container.api;

  exports org.mule.runtime.container.internal to
      org.mule.runtime.artifact.activation,
      org.mule.runtime.deployment.model,
      org.mule.runtime.deployment.model.impl;
  exports org.mule.runtime.container.internal.util to
      org.mule.runtime.artifact.activation,
      org.mule.runtime.deployment.model.impl;

}