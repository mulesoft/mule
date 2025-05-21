/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.multi.implicit.config.extension.extension;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_21;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Import;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.sdk.api.annotation.JavaVersionSupport;
import org.mule.test.implicit.exclusive.config.extension.extension.BlaConfig;
import org.mule.test.implicit.exclusive.config.extension.extension.BleConfig;
import org.mule.test.implicit.exclusive.config.extension.extension.NonImplicitConfig;
import org.mule.test.implicit.exclusive.config.extension.extension.NullSafeInterface;

@Extension(name = "multiImplicitConfig")
@JavaVersionSupport({JAVA_21, JAVA_17})
@Xml(namespace = "http://www.mulesoft.org/schema/mule/multiimplicitconfig", prefix = "multiimplicitconfig")
@Configurations(value = {BlaConfig.class, BleConfig.class, NonImplicitConfig.class, AnotherConfigThatCanBeUsedImplicitly.class})
@Import(type = NullSafeInterface.class)
public class MultipleImplicitConfigExtension {
}
