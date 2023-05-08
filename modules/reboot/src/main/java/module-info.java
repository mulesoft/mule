/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Bootstrap for the Mule CE Container.
 * 
 * @moduleGraph
 * @since 4.6
 */
module org.mule.boot {
  
  requires org.mule.runtime.logging;

  requires commons.cli;
  // Tanuki wrapper
  requires wrapper;

}