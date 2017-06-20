/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import static java.lang.String.format;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.ConfigOverride;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.Arrays;
import java.util.List;

@Alias("harvest-apples")
@MetadataScope(keysResolver = HarvestAppleKeyResolver.class,
    outputResolver = HarvestAppleKeyResolver.class)
public class HarvestApplesSource extends Source<Apple, HarvestApplesAttributes> {

  @Config
  private AppleConfig appleConfig;

  @MetadataKeyId
  @Parameter
  @Optional
  private String key;

  @Parameter
  @ConfigOverride
  private List<String> mainProducers;

  @ParameterGroup(name = "Sample Input Group")
  private HarvestInputGroup sampleInput;

  @Parameter
  @Optional
  private String shouldNotOverride;

  @Parameter
  @Optional
  private String flowName;

  @ParameterGroup(name = "As Group Inline", showInDsl = true)
  private GroupedFood inlineGroupedFood;

  @Parameter
  @Optional
  @NullSafe
  private GroupedFood pojoGroupedFood;

  @Override
  public void onStart(SourceCallback<Apple, HarvestApplesAttributes> sourceCallback) throws MuleException {
    if (inlineGroupedFood.getFood() == null || pojoGroupedFood.getFood() == null) {
      throw new IllegalArgumentException(
                                         format("Got a null in food groups: %s", inlineGroupedFood.getFood(),
                                                pojoGroupedFood.getFood()));
    }

    sourceCallback.handle(Result.<Apple, HarvestApplesAttributes>builder()
        .output(null).attributes(null).build());
  }

  @OnSuccess
  public void onSuccess(@ParameterGroup(name = "Response", showInDsl = true) SuccessResponse response) {
    appleConfig.getResults()
        .put(flowName, Arrays.asList(mainProducers, sampleInput.getSample(), shouldNotOverride, response.getTimeToPeel()));
  }

  @Override
  public void onStop() {}
}
