/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.function.extension;

import org.mule.runtime.extension.api.annotation.ExpressionFunctions;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Import;
import org.mule.runtime.extension.api.annotation.ImportedTypes;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;



@Extension(name = "Test Functions")
@ExpressionFunctions(GlobalWeaveFunction.class)
@Operations(WeaveTestUtilsOperations.class)
@Xml(prefix = "fn")
@ImportedTypes(@Import(type = KnockeableDoor.class))
public class WeaveFunctionExtension {

}
