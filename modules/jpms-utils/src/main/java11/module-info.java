/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * This module provides JMPS utilities for use within the Mule Runtime.
 * 
 * @moduleGraph
 * @since 1.6
 */
module org.mule.runtime.jpms.utils {

  requires java.management;

  exports org.mule.runtime.jpms.api;

}
