package org.mule.schema.test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import junit.framework.TestCase;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.mule.schema.SchemaPackage;
import org.mule.schema.util.SchemaResourceFactoryImpl;

/**
 * Test marshalling data to/from XML using Ecore.
 */
public class MuleSchemaTests extends TestCase {

	public void testLoadSave() {
		URI fileURI = URI.createFileURI(new File("xml/mule-config.xml").getAbsolutePath());
		Resource res = (new SchemaResourceFactoryImpl()).createResource(fileURI);
		SchemaPackage schemaPkg = SchemaPackage.eINSTANCE;
		try {
			res.load(Collections.EMPTY_MAP);
			this.assertTrue("Could not find resource via URI.", res.isLoaded());
			res.save(System.out, Collections.EMPTY_MAP);
		} catch (IOException e) {
			this.fail(e.getMessage());
		}
	}
}