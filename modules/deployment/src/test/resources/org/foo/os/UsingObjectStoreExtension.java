/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.os;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;

/**
 * Extension for testing purposes
 */
@Extension(name = "Using ObjectStore")
@Operations({UsingObjectStoreOperations.class})
public class UsingObjectStoreExtension {

}
