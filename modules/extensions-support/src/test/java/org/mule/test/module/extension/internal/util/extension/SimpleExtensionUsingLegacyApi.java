/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.internal.util.extension;

import org.mule.sdk.api.annotation.Export;
import org.mule.sdk.api.annotation.Extension;

@Extension(name = "SimpleExtension")
@Export(classes = {SimpleExportedType.class}, resources = "simpleResource.json")
public class SimpleExtensionUsingLegacyApi {
}
