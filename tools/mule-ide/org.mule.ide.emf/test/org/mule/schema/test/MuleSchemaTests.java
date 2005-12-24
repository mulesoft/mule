package org.mule.schema.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import junit.framework.TestCase;

import org.eclipse.emf.ecore.resource.Resource;
import org.mule.schema.util.MuleResourceFactoryImpl;

/**
 * Test marshalling data to/from XML using Ecore.
 */
public class MuleSchemaTests extends TestCase {

	public void testLoadSave() {
		try {
			Resource.Factory factory = new MuleResourceFactoryImpl();
			Resource resource = factory.createResource(null);
			InputStream stream = new FileInputStream("xml/mule-config.xml");
			resource.load(stream, Collections.EMPTY_MAP);
			assertTrue("Could not find resource via URI.", resource.isLoaded());
			resource.save(System.out, Collections.EMPTY_MAP);
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}
}