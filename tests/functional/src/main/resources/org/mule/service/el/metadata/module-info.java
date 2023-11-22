/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
import org.mule.api.annotation.jpms.RequiredOpens;
import org.mule.api.annotation.jpms.ServiceModule;

/**
 * Mock EL Metadata Service Implementation.
 */
@ServiceModule
module org.mule.service.el.metadata.mock {

  requires org.mule.runtime.api;
  requires org.mule.runtime.metadata.model.api;
  requires org.mule.runtime.metadata.model.message;

  exports org.mule.service.el.metadata;

}
