/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.marvel;

import static org.mule.test.marvel.MarvelExtension.MARVEL_EXTENSION;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.test.marvel.drstrange.DrStrange;
import org.mule.test.marvel.drstrange.DrStrangeErrorTypeDefinition;
import org.mule.test.marvel.drstrange.DrStrangeTypeWithCustomStereotype;
import org.mule.test.marvel.ironman.IronMan;
import org.mule.test.marvel.xmen.XMen;

@Extension(name = MARVEL_EXTENSION)
@Configurations({IronMan.class, DrStrange.class, XMen.class})
@ErrorTypes(DrStrangeErrorTypeDefinition.class)
@Export(classes = {IronMan.class, DrStrangeTypeWithCustomStereotype.class})
public class MarvelExtension {

  public static final String MARVEL_EXTENSION = "Marvel";
}
