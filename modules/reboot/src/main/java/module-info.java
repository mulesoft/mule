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
 * @since 4.5
 */
module org.mule.boot {

  // TODO W-13151134: export only to modules that require it once org.mule.runtime.launcher is modularized
  // exports org.mule.runtime.module.reboot.internal to org.mule.boot.tanuki,org.mule.runtime.launcher;
  exports org.mule.runtime.module.reboot.internal;

  requires org.mule.runtime.logging;
  requires org.mule.runtime.jpms.utils;

  requires commons.cli;
}
