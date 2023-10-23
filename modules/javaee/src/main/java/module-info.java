/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * This module provides Java EE extensions for backwards compatibility when running on Java 11+.
 *
 * @moduleGraph
 * @since 4.6
 * @deprecated to be removed when Java 8 reaches EoS.
 */
@Deprecated
module org.mule.runtime.javaee {

  // These `requires` must in NO WAY be transitive, we precisely want these to not be accessible form clients of the Mule
  // Container.
  requires com.sun.xml.bind;
  requires jakarta.activation;
  requires jakarta.resource.api;
  requires java.annotation;
  requires java.jws;
  requires java.transaction;
  requires java.xml.bind;
  requires java.xml.soap;
  requires java.xml.ws;

}
