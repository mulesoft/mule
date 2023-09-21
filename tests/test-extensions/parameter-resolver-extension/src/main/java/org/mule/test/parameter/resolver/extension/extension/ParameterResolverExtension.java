/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.parameter.resolver.extension.extension;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_11;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_8;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Import;
import org.mule.sdk.api.annotation.JavaVersionSupport;
import org.mule.test.heisenberg.extension.model.DifferedKnockableDoor;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;

@Configurations({ParameterResolverConfig.class, NestedWrapperTypesConfig.class})
@Extension(name = "ParameterResolver")
@JavaVersionSupport({JAVA_8, JAVA_11, JAVA_17})
@Import(type = KnockeableDoor.class)
@Import(type = DifferedKnockableDoor.class)
public class ParameterResolverExtension {


}
