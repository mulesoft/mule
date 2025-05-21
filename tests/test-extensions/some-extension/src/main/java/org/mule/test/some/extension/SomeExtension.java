/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.some.extension;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_21;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.sdk.api.annotation.JavaVersionSupport;
import org.mule.sdk.api.annotation.error.ErrorTypes;
import org.mule.test.heisenberg.extension.HeisenbergErrors;

@Extension(name = "SomeExtension")
@JavaVersionSupport({JAVA_21, JAVA_17})
@Configurations({ParameterGroupConfig.class, ParameterGroupDslConfig.class})
@ConnectionProviders(ExtConnProvider.class)
@ErrorTypes(HeisenbergErrors.class)
@Export(classes = CustomConnectionException.class)
@Sources({SomeEmittingSource.class, AnotherEmittingSource.class, YetAnotherEmittingSource.class,
    ParameterEmittingSource.class, ExclusiveOptionalsEmittingSource.class, AnotherExclusiveOptionalsEmittingSource.class})
@Xml(namespace = "http://www.mulesoft.org/schema/mule/some", prefix = "some")
public class SomeExtension {
}
