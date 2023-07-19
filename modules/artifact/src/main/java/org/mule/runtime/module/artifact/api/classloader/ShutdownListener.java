/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.classloader;

import org.mule.api.annotation.NoImplement;

/**
 * Optional hook, invoked synchronously right before the class loader is disposed and closed.
 */
@NoImplement
public interface ShutdownListener {

  void execute();
}
