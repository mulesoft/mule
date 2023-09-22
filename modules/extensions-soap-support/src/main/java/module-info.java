/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
 /**
  * Mule Extensions SOAP Support
  *
  * @moduleGraph
  * @since 4.6
  */
 module org.mule.runtime.extensions.soap.support {

   requires org.mule.runtime.core;
   requires org.mule.runtime.extensions.api;
   requires org.mule.runtime.extensions.soap.api;
   requires org.mule.runtime.extensions.support;
   requires org.mule.runtime.http.api;
   requires org.mule.runtime.metadata.model.api;
   requires org.mule.runtime.metadata.model.java;
   requires org.mule.runtime.metadata.support;
   requires org.mule.runtime.soap.api;
   requires org.mule.sdk.api;

   exports org.mule.runtime.module.extension.soap.api.loader;
   exports org.mule.runtime.module.extension.soap.api.runtime.connection.transport;

   exports org.mule.runtime.module.extension.soap.internal.loader to
       org.mule.runtime.extensions.spring.support;
   exports org.mule.runtime.module.extension.soap.internal.loader.property to
       org.mule.runtime.extensions.spring.support;
   exports org.mule.runtime.module.extension.soap.internal.runtime.connection to
       org.mule.runtime.extensions.spring.support;

   requires java.inject;

   opens org.mule.runtime.module.extension.soap.internal.runtime.connection to spring.core;
   opens org.mule.runtime.module.extension.soap.api.runtime.connection.transport to spring.core;
   opens org.mule.runtime.module.extension.soap.internal.runtime.operation to spring.core;

   provides org.mule.runtime.extension.api.loader.ExtensionModelLoaderProvider with
       org.mule.runtime.module.extension.soap.api.loader.SoapExtensionModelLoaderProvider;
 }
