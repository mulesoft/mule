/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.classloading;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;

import javax.inject.Inject;

/**
 * Extension for testing purposes
 */
@Extension(name = "Load Class Extension")
@Operations({LoadClassOperation.class})
public class LoadClassExtension {
}
