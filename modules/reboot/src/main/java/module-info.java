/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
/**
 * Bootstrap for the Mule CE Container.
 * 
 * @moduleGraph
 * @since 4.5
 */
module org.mule.boot {

  exports org.mule.runtime.module.reboot.api;

  // TODO W-13151134: export only to modules that require it once org.mule.runtime.launcher is modularized
  // exports org.mule.runtime.module.reboot.internal to org.mule.boot.tanuki,org.mule.runtime.launcher,com.mulesoft.mule.runtime.plugin;
  exports org.mule.runtime.module.reboot.internal;

  requires org.mule.runtime.logging;
  requires org.mule.runtime.jpms.utils;

  requires commons.cli;
}
