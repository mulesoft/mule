/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.callback;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;

/**
 * Extension for testing purposes
 */
@Extension(name = "Callback Extension")
@Operations({CallbackOperation.class})
public class CallbackExtension {
}
