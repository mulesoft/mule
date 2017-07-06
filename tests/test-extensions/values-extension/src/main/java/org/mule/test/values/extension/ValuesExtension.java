/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.test.values.extension.config.ConfigWithValueAndRequiredParam;
import org.mule.test.values.extension.config.ConfigWithValueParameter;
import org.mule.test.values.extension.config.ConfigWithValuesWithRequiredParamsFromParamGroup;
import org.mule.test.values.extension.config.ConfigWithValuesWithRequiredParamsFromShowInDslGroup;
import org.mule.test.values.extension.config.SimpleConfig;
import org.mule.test.values.extension.source.SimpleSource;

@Extension(name = "Values")
@Configurations({SimpleConfig.class, ConfigWithValueParameter.class,
    ConfigWithValueAndRequiredParam.class, ConfigWithValuesWithRequiredParamsFromParamGroup.class,
    ConfigWithValuesWithRequiredParamsFromShowInDslGroup.class})
@Sources({SimpleSource.class})
@Xml(namespace = "http://www.mulesoft.org/schema/mule/values", prefix = "values")
public class ValuesExtension {

}
