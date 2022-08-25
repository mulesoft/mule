/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
