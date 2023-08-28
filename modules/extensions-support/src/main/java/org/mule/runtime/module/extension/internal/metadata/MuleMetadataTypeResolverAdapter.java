/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.metadata;

/**
 * A generic contract for a metadata type resolver adapter that adapts a SDK metadata type resolver to Mule metadata type
 * resolver.
 *
 * @since 4.5.0
 */
public interface MuleMetadataTypeResolverAdapter {

  Class<?> getDelegateResolverClass();
}
