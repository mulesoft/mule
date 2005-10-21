/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.util;

import java.util.Iterator;
import java.util.Map;

/**
 * @author Holger Hoffstaette
 */

public class CollectionUtil
{

	/**
	 * Create & populate a Map of arbitrary class. Populating stops when either
	 * the keys or values iterator is null or exhausted.
	 * 
	 * @param mapClass the Class of the Map to instantiate
	 * @param keys iterator for Objects ued as keys
	 * @param values iterator for Objects used as values
	 * @return the instantiated Map
	 */
	public static Map createMapWithKeysAndValues(Class mapClass, Iterator keys, Iterator values)
	{
		Map m = null;

		if (mapClass == null)
		{
			throw new IllegalArgumentException("Map class must not be null!");
		}

		try
		{
			m = (Map)mapClass.newInstance();
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}

		if (keys != null && values != null)
		{
			while (keys.hasNext() && values.hasNext())
			{
				m.put(keys.next(), values.next());
			}
		}

		return m;
	}

}
