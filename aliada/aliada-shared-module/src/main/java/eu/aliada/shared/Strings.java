// ALIADA - Automatic publication under Linked Data paradigm
//          of library and museum data
//
// Component: aliada-rdfizer
// Responsible: ALIADA Consortiums
package eu.aliada.shared;

import java.text.Normalizer;
import java.text.Normalizer.Form;

/**
 * Strings Booch utility.
 * 
 * @author Andrea Gazzarini
 * @since 1.0
 */
public abstract class Strings {

	/**
	 * Returns true if the given string is not null and not empty.
	 * 
	 * @param value the string to check.
	 * @return true if the given string is not null and not empty.
	 */
	public static boolean isNotNullAndNotEmpty(final String value) {
		return value != null && value.trim().length() != 0;
	}
	
	/**
	 * Returns true if the given string is null or empty.
	 * 
	 * @param value the string to check.
	 * @return true if the given string is null or empty.
	 */
	public static boolean isNullOrEmpty(final String value) {
		return value == null || value.trim().length() == 0;
	}	
	
	/**
	 * Converts the given value to a string that can be used as local name in URIs.
	 * Basically it will normalize diacritics and replace spaces with underscores.
	 * 
	 * @param value the source string.
	 * @return a string that can be used as local name in URIs.
	 */
	public static String toURILocalName(final String value) {
		   return value == null ? null
			        : Normalizer.normalize(value, Form.NFD)
			            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
			            .replaceAll(" ", "_");
	}
}