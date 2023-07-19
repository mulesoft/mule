/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.internal.util.extension;

import org.mule.sdk.api.annotation.Export;
import org.mule.sdk.api.annotation.Extension;
import org.mule.sdk.api.annotation.Import;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;

@Extension(name = "SimpleExtension")
@Export(classes = {SimpleExportedType.class}, resources = "simpleResource.json")
@Import(type = KnockeableDoor.class)
public class SimpleExtensionUsingSdkApi {
}
