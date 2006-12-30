/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extras.spring.config;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.transformer.UMOTransformer;

import java.beans.PropertyEditorSupport;
import java.util.StringTokenizer;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * TODO
 */
public class TransformerPropertyEditor extends PropertyEditorSupport
{
    private DefaultListableBeanFactory beanFactory;


    public TransformerPropertyEditor(DefaultListableBeanFactory beanFactory)
    {
        this.beanFactory = beanFactory;
    }

    public void setAsText(String text) {

        StringTokenizer st = new StringTokenizer(text, " ");
        UMOTransformer currentTrans = null;
        UMOTransformer returnTrans = null;

        while (st.hasMoreTokens())
        {
            //TODO RM* This should be using the registry
            String name = st.nextToken().trim();
            UMOTransformer tempTrans = (UMOTransformer)beanFactory.getBean(name);

            if (tempTrans == null)
            {
                throw new IllegalArgumentException(new Message(Messages.OBJECT_NOT_FOUND_X, name).toString());
            }

            if (currentTrans == null)
            {
                currentTrans = tempTrans;
                returnTrans = tempTrans;
            }
            else
            {
                currentTrans.setNextTransformer(tempTrans);
                currentTrans = tempTrans;
            }
        }
        setValue(returnTrans);
    }
}
