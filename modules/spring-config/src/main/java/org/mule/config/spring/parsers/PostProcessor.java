/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers;

import org.mule.config.spring.parsers.assembly.BeanAssembler;

import org.w3c.dom.Element;

/**
 * This interface allows post-processing of the bean assmebler to be injected into
 * definition parsers
 */
public interface PostProcessor
{

    public void postProcess(BeanAssembler assembler, Element element);

}
