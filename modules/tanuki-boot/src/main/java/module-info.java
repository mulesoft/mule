/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
/**
 * Container Wrapper for Tanuki integration
 * 
 * @moduleGraph
 * @since 4.5
 */
module org.mule.boot.tanuki {

  exports org.mule.runtime.module.boot.tanuki.internal to org.mule.boot, com.mulesoft.mule.boot;

  // This requirement is static because at runtime it may be changed to the EE version (which contains the same packages)
  requires static org.mule.boot;

  // Tanuki wrapper
  requires wrapper;

}