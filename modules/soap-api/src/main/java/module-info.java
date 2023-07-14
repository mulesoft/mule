/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Mule service that allows to create SOAP servers and clients.
 *
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.soap.api {

  requires transitive org.mule.runtime.api;
  requires org.mule.runtime.metadata.model.api;
  requires org.mule.runtime.extensions.api;
  requires org.mule.runtime.extensions.soap.api;
  requires org.mule.runtime.http.api;

  requires java.xml.soap;

  requires org.apache.commons.io;

  exports org.mule.runtime.soap.api;
  exports org.mule.runtime.soap.api.client;
  exports org.mule.runtime.soap.api.client.metadata;
  exports org.mule.runtime.soap.api.exception;
  exports org.mule.runtime.soap.api.exception.error;
  exports org.mule.runtime.soap.api.message;
  exports org.mule.runtime.soap.api.message.dispatcher;
  exports org.mule.runtime.soap.api.transport;
  
}
