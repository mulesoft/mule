/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo.withBrokenLifecycleListener;

import org.mule.sdk.api.annotation.Extension;
import org.mule.sdk.api.annotation.OnArtifactLifecycle;

/**
 * Extension with a LifecycleListener that fails, for testing purposes
 */
@Extension(name = "WithBrokenLifecycleListener")
@OnArtifactLifecycle(LifecycleListener.class)
public class WithBrokenLifecycleListenerExtension {
}
