/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.converters;

import org.mule.api.transformer.Transformer;

import java.util.ArrayList;

/**
 * Type definition for a transformer list. This is required so that the Transformer property converter
 * can define a typed List as the return Type. 
 */
public class TransformerList extends ArrayList<Transformer> {}
