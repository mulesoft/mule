/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.launcher.coreextension;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.container.api.MuleCoreExtension;

import java.util.List;

/**
 * Defines a way to search for available {@link MuleCoreExtension}
 */
public interface MuleCoreExtensionDiscoverer {

  List<MuleCoreExtension> discover() throws MuleException;
}
