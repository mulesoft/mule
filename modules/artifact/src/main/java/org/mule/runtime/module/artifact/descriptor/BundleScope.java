/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.descriptor;

/**
 * Scope in which a bundle is required when used as a dependency.
 * <p/>
 * These scopes map to Maven defined scopes.
 */
public enum BundleScope {

  COMPILE,

  PROVIDED,

  RUNTIME,

  TEST,

  SYSTEM,

  IMPORT
}
