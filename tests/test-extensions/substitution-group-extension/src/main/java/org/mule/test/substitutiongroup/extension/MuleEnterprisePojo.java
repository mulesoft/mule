package org.mule.test.substitutiongroup.extension;

import org.mule.runtime.extension.api.annotation.dsl.xml.TypeXmlHints;
import org.mule.runtime.extension.api.annotation.param.Parameter;

@TypeXmlHints(substitutionGroup = "ee:abstract-event-copy-strategy" , baseType = "ee:abstractEventCopyStrategyType")
public class MuleEnterprisePojo
{

  @Parameter
  boolean innerParameter;

}
