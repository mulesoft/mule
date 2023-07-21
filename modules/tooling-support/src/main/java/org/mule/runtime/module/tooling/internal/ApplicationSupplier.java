/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.tooling.internal;

import org.mule.runtime.deployment.model.api.application.Application;

@FunctionalInterface
public interface ApplicationSupplier {

  Application get() throws Exception;

}
