/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.activiti.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.module.activiti.action.model.ProcessDefinition;
import org.mule.module.activiti.i18n.ActivitiMessages;
import org.mule.module.json.transformers.AbstractJsonTransformer;
import org.mule.transformer.types.CollectionDataType;
import org.mule.util.BeanUtils;

public class JsonToProcessDefinitions extends AbstractJsonTransformer
{

    public JsonToProcessDefinitions()
    {
        this.setReturnDataType(new CollectionDataType<List>(List.class));
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        try
        {
            List<ProcessDefinition> processDefinitions = new ArrayList<ProcessDefinition>();

            Map<String, Object> rootAsMap = this.getMapper().readValue(message.getPayloadAsString(), Map.class);
            List processDefinitionsAsMaps = (List) rootAsMap.get("data");

            for (Object o : processDefinitionsAsMaps)
            {
                ProcessDefinition definition = new ProcessDefinition();
                Map<String, Object> map = (Map<String, Object>) o;

                BeanUtils.populateWithoutFail(definition, map, false);
                processDefinitions.add(definition);
            }

            return processDefinitions;
        }
        catch (Exception e)
        {
            throw new TransformerException(ActivitiMessages.failToProcessJson(), e);
        }
    }
}
