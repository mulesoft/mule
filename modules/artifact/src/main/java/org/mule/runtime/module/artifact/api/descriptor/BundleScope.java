/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.descriptor;

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
