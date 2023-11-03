/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
import org.mule.api.annotation.jpms.ServiceModule;

@ServiceModule
module org.mule.service.echo {

  requires org.mule.runtime.api;
  requires org.mule.test.services;

  exports org.mule.echo;
  
  opens org.mule.echo to
    org.mule.runtime.service;
  
}
