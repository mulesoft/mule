/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Default implementation of the Mule Extension API.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.extensions.support {

  exports org.mule.runtime.module.extension.api.manager;
  exports org.mule.runtime.module.extension.api.loader;
  exports org.mule.runtime.module.extension.api.loader.java.property;
  exports org.mule.runtime.module.extension.api.util;
  exports org.mule.runtime.module.extension.api.metadata;
  exports org.mule.runtime.module.extension.api.tooling;
  exports org.mule.runtime.module.extension.api.runtime.connectivity.oauth;
  exports org.mule.runtime.module.extension.api.loader.java.type;

//  provides org.mule.runtime.api.connectivity.ConnectivityTestingStrategy with
//      org.mule.runtime.module.extension.api.tooling.ExtensionConnectivityTestingStrategy;
//  provides org.mule.runtime.extension.api.loader.ExtensionModelLoaderProvider with
//      org.mule.runtime.module.extension.api.loader.DefaultExtensionModelLoaderProvider;
    
}
