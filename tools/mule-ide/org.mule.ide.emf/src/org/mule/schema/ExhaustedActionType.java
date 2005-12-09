/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.AbstractEnumerator;

/**
 * <!-- begin-user-doc -->
 * A representation of the literals of the enumeration '<em><b>Exhausted Action Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * @see org.mule.schema.MulePackage#getExhaustedActionType()
 * @model
 * @generated
 */
public final class ExhaustedActionType extends AbstractEnumerator {
	/**
	 * The '<em><b>GROW</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of '<em><b>GROW</b></em>' literal object isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @see #GROW_LITERAL
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int GROW = 0;

	/**
	 * The '<em><b>WAIT</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of '<em><b>WAIT</b></em>' literal object isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @see #WAIT_LITERAL
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int WAIT = 1;

	/**
	 * The '<em><b>FAIL</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of '<em><b>FAIL</b></em>' literal object isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @see #FAIL_LITERAL
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int FAIL = 2;

	/**
	 * The '<em><b>GROW</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #GROW
	 * @generated
	 * @ordered
	 */
	public static final ExhaustedActionType GROW_LITERAL = new ExhaustedActionType(GROW, "GROW");

	/**
	 * The '<em><b>WAIT</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #WAIT
	 * @generated
	 * @ordered
	 */
	public static final ExhaustedActionType WAIT_LITERAL = new ExhaustedActionType(WAIT, "WAIT");

	/**
	 * The '<em><b>FAIL</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #FAIL
	 * @generated
	 * @ordered
	 */
	public static final ExhaustedActionType FAIL_LITERAL = new ExhaustedActionType(FAIL, "FAIL");

	/**
	 * An array of all the '<em><b>Exhausted Action Type</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final ExhaustedActionType[] VALUES_ARRAY =
		new ExhaustedActionType[] {
			GROW_LITERAL,
			WAIT_LITERAL,
			FAIL_LITERAL,
		};

	/**
	 * A public read-only list of all the '<em><b>Exhausted Action Type</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Exhausted Action Type</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static ExhaustedActionType get(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			ExhaustedActionType result = VALUES_ARRAY[i];
			if (result.toString().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Exhausted Action Type</b></em>' literal with the specified value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static ExhaustedActionType get(int value) {
		switch (value) {
			case GROW: return GROW_LITERAL;
			case WAIT: return WAIT_LITERAL;
			case FAIL: return FAIL_LITERAL;
		}
		return null;	
	}

	/**
	 * Only this class can construct instances.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private ExhaustedActionType(int value, String name) {
		super(value, name);
	}

} //ExhaustedActionType
