/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

@Alias("pet-source")
public class PetStoreSource extends Source<String, Attributes> {

  @ParameterGroup(name = "Breeder")
  private ExclusivePetBreeder breeder;

  @Override
  public void onStart(SourceCallback<String, Attributes> sourceCallback) throws MuleException {}

  @Override
  public void onStop() {}
}
