/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.some.extension;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.sdk.api.annotation.error.ErrorTypes;
import org.mule.test.heisenberg.extension.HeisenbergErrors;

@Extension(name = "SomeExtension")
@Configurations({ParameterGroupConfig.class, ParameterGroupDslConfig.class})
@ConnectionProviders(ExtConnProvider.class)
@ErrorTypes(HeisenbergErrors.class)
@Export(classes = CustomConnectionException.class)
@Sources({SomeEmittingSource.class, AnotherEmittingSource.class, YetAnotherEmittingSource.class,
    ParameterEmittingSource.class, ExclusiveOptionalsEmittingSource.class, AnotherExclusiveOptionalsEmittingSource.class})
@Xml(namespace = "http://www.mulesoft.org/schema/mule/some", prefix = "some")
public class SomeExtension {
}
