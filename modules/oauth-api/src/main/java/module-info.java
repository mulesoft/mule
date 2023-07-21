/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
/**
 * Mule service that provides OAuth authentication support.
 *
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.oauth.api {

  requires transitive org.mule.runtime.api;
  requires org.mule.oauth.client.api;
  requires org.mule.runtime.http.api;

  exports org.mule.runtime.oauth.api;
  exports org.mule.runtime.oauth.api.builder;
  exports org.mule.runtime.oauth.api.listener;
  exports org.mule.runtime.oauth.api.state;
  
}
