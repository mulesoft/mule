/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection;

import org.mule.metadata.api.model.BooleanType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;

/**
 * Base implementation for a {@link MetadataTypeVisitor} which adds the new method {@link #visitBasicType(MetadataType)} which is
 * invoked by all the types which refer to a basic type. Those would be:
 * <p/>
 * <ul>
 * <li>{@link #visitBoolean(BooleanType)}</li>
 * <li>{@link #visitNumber(NumberType)}}</li>
 * <li>{@link #visitString(StringType)}</li>
 * </ul>
 * <p/>
 * All other qualifiers delegate into {@link #defaultVisit(MetadataType)} ()} by default, but they can be overridden at will
 *
 * @since 4.0
 */
public abstract class BasicTypeMetadataVisitor extends MetadataTypeVisitor {

  protected abstract void visitBasicType(MetadataType metadataType);

  @Override
  public void visitBoolean(BooleanType booleanType) {
    visitBasicType(booleanType);
  }

  @Override
  public void visitNumber(NumberType numberType) {
    visitBasicType(numberType);
  }

  @Override
  public void visitString(StringType stringType) {
    visitBasicType(stringType);
  }
}
