/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension.model.attribute;

import java.io.Serializable;

public interface AbstractOutputAttributes extends Serializable {

  String getOutputId();
}
