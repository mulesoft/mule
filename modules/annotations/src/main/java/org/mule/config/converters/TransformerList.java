/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.converters;

import org.mule.api.transformer.Transformer;

import java.util.ArrayList;

/**
 * Type definition for a transformer list. This is required so that the Transformer property converter
 * can define a typed List as the return Type. 
 */
public class TransformerList extends ArrayList<Transformer> {}
