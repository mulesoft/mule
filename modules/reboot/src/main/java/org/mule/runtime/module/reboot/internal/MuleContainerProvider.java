/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.reboot.internal;

public interface MuleContainerProvider {

  MuleContainer provide(String[] args) throws Exception;

}
