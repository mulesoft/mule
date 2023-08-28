/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.test.values.extension.config.ChatConfiguration;
import org.mule.test.values.extension.config.ConfigWithBoundValueParameter;
import org.mule.test.values.extension.config.ConfigWithFailureErrorProvider;
import org.mule.test.values.extension.config.ConfigWithParameterWithFieldValues;
import org.mule.test.values.extension.config.ConfigWithValueAndRequiredParam;
import org.mule.test.values.extension.config.ConfigWithValueParameter;
import org.mule.test.values.extension.config.ConfigWithValuesWithRequiredParamsFromParamGroup;
import org.mule.test.values.extension.config.ConfigWithValuesWithRequiredParamsFromShowInDslGroup;
import org.mule.test.values.extension.config.SimpleConfig;
import org.mule.test.values.extension.source.SimpleSource;
import org.mule.test.values.extension.source.SourceWithTwoBoundActingParameters;

@Extension(name = "Values")
@Configurations({SimpleConfig.class, ConfigWithValueParameter.class,
    ConfigWithValueAndRequiredParam.class, ConfigWithValuesWithRequiredParamsFromParamGroup.class,
    ConfigWithValuesWithRequiredParamsFromShowInDslGroup.class, ConfigWithFailureErrorProvider.class,
    ConfigWithBoundValueParameter.class, ConfigWithParameterWithFieldValues.class, ChatConfiguration.class, XmlBasedConfig.class})
@Sources({SimpleSource.class, SourceWithTwoBoundActingParameters.class})
@Export(classes = MyPojo.class)
@Xml(namespace = "http://www.mulesoft.org/schema/mule/values", prefix = "values")
public class ValuesExtension {

}
