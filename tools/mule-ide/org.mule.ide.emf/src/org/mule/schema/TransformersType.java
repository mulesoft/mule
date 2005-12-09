/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.eclipse.emf.ecore.util.FeatureMap;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Transformers Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.schema.TransformersType#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.TransformersType#getTransformer <em>Transformer</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.MulePackage#getTransformersType()
 * @model extendedMetaData="name='transformersType' kind='mixed'"
 * @generated
 */
public interface TransformersType extends EObject {
	/**
	 * Returns the value of the '<em><b>Mixed</b></em>' attribute list.
	 * The list contents are of type {@link org.eclipse.emf.ecore.util.FeatureMap.Entry}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Mixed</em>' attribute list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Mixed</em>' attribute list.
	 * @see org.mule.schema.MulePackage#getTransformersType_Mixed()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
	 *        extendedMetaData="kind='elementWildcard' name=':mixed'"
	 * @generated
	 */
	FeatureMap getMixed();

	/**
	 * Returns the value of the '<em><b>Transformer</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.TransformerType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Transformer</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Transformer</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getTransformersType_Transformer()
	 * @model type="org.mule.schema.TransformerType" containment="true" resolveProxies="false" required="true" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='transformer' namespace='##targetNamespace'"
	 * @generated
	 */
	EList getTransformer();

} // TransformersType
