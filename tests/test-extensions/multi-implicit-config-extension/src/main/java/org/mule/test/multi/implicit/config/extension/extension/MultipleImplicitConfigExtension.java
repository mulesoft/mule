/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.multi.implicit.config.extension.extension;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Import;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.test.implicit.exclusive.config.extension.extension.BlaConfig;
import org.mule.test.implicit.exclusive.config.extension.extension.BleConfig;
import org.mule.test.implicit.exclusive.config.extension.extension.NonImplicitConfig;
import org.mule.test.implicit.exclusive.config.extension.extension.NullSafeInterface;

@Extension(name = "multiImplicitConfig")
@Xml(namespace = "http://www.mulesoft.org/schema/mule/multiimplicitconfig", prefix = "multiimplicitconfig")
@Configurations(value = {BlaConfig.class, BleConfig.class, NonImplicitConfig.class, AnotherConfigThatCanBeUsedImplicitly.class})
@Import(type = NullSafeInterface.class)
public class MultipleImplicitConfigExtension {
}
