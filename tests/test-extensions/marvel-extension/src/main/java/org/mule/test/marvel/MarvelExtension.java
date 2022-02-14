/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel;

import static org.mule.test.marvel.MarvelExtension.MARVEL_EXTENSION;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.test.marvel.drstrange.DrStrange;
import org.mule.test.marvel.drstrange.DrStrangeErrorTypeDefinition;
import org.mule.test.marvel.ironman.IronMan;
import org.mule.test.marvel.drstrange.DrStrangeTypeWithCustomStereotype;

@Extension(name = MARVEL_EXTENSION)
@Configurations({IronMan.class, DrStrange.class})
@ErrorTypes(DrStrangeErrorTypeDefinition.class)
@Export(classes = {IronMan.class, DrStrangeTypeWithCustomStereotype.class})
public class MarvelExtension {

  public static final String MARVEL_EXTENSION = "Marvel";
}
