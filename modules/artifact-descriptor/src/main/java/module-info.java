/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Mule Artifact Descriptor Module.
 *
 * @moduleGraph
 * @since 4.9
 */
module org.mule.runtime.artifact.descriptor {

  requires transitive org.mule.runtime.api;
  requires org.mule.runtime.artifact.declaration;

  requires org.apache.commons.io;

  exports org.mule.runtime.artifact.descriptor.api;

}
