/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
