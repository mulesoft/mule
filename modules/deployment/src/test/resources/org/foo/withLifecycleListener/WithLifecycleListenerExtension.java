/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.withLifecycleListener;

import org.mule.sdk.api.annotation.Extension;
import org.mule.sdk.api.annotation.OnArtifactLifecycle;
import org.mule.sdk.api.annotation.Operations;

/**
 * Extension with LifecycleListener, for testing purposes
 */
@Extension(name = "WithLifecycleListener")
@OnArtifactLifecycle(LifecycleListener.class)
@Operations({WithLifecycleListenerOperation.class})
public class WithLifecycleListenerExtension {
}
