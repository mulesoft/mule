/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.routing.split;

public interface SplittingStrategy<Input, Output> {

  Output split(Input input);
}


