/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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