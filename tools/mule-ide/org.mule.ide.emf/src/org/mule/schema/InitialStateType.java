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
 * A representation of the literals of the enumeration '<em><b>Initial State Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * @see org.mule.schema.MulePackage#getInitialStateType()
 * @model
 * @generated
 */
public final class InitialStateType extends AbstractEnumerator {
	/**
	 * The '<em><b>Started</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of '<em><b>Started</b></em>' literal object isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @see #STARTED_LITERAL
	 * @model name="started"
	 * @generated
	 * @ordered
	 */
	public static final int STARTED = 0;

	/**
	 * The '<em><b>Stopped</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of '<em><b>Stopped</b></em>' literal object isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @see #STOPPED_LITERAL
	 * @model name="stopped"
	 * @generated
	 * @ordered
	 */
	public static final int STOPPED = 1;

	/**
	 * The '<em><b>Started</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #STARTED
	 * @generated
	 * @ordered
	 */
	public static final InitialStateType STARTED_LITERAL = new InitialStateType(STARTED, "started");

	/**
	 * The '<em><b>Stopped</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #STOPPED
	 * @generated
	 * @ordered
	 */
	public static final InitialStateType STOPPED_LITERAL = new InitialStateType(STOPPED, "stopped");

	/**
	 * An array of all the '<em><b>Initial State Type</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final InitialStateType[] VALUES_ARRAY =
		new InitialStateType[] {
			STARTED_LITERAL,
			STOPPED_LITERAL,
		};

	/**
	 * A public read-only list of all the '<em><b>Initial State Type</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Initial State Type</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static InitialStateType get(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			InitialStateType result = VALUES_ARRAY[i];
			if (result.toString().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Initial State Type</b></em>' literal with the specified value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static InitialStateType get(int value) {
		switch (value) {
			case STARTED: return STARTED_LITERAL;
			case STOPPED: return STOPPED_LITERAL;
		}
		return null;	
	}

	/**
	 * Only this class can construct instances.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private InitialStateType(int value, String name) {
		super(value, name);
	}

} //InitialStateType
