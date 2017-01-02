// ALIADA - Automatic publication under Linked Data paradigm
//          of library and museum data
//
// Component: aliada-rdfizer
// Responsible: ALIADA Consortiums
package eu.aliada.rdfizer.pipeline.format.marc.frbr;

import javax.persistence.GeneratedValue;

import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.hp.hpl.jena.sparql.function.library.namespace;

import eu.aliada.rdfizer.log.MessageCatalog;
import eu.aliada.rdfizer.pipeline.format.xml.OXPath;

/**
 * Class containing "expression" objects which extracts identifiers related with
 * Manifestation entity.
 * 
 * @author Andrea Gazzarini.
 * @since 1.0
 */
public class UUIDManifestationDetector extends AbstractEntityDetector<String> {
	
	private final String prefixExp;
	private final String idExp;
	
	@Autowired
	private OXPath xpath;
	
	/**
	 * Builds a new manifestation detection rule with the following rule.
	 * 
	 * @param controlNumberDetectionRule the control number detection rule.
	 */
	public UUIDManifestationDetector(final String prefixExp, final String idExp) {
		this.prefixExp = prefixExp;
		this.idExp = idExp;
	}

	@Override
	public String detect(final Document target) {
		//there is an old approach, that not consider indicators.
		//and a new approach, that consider indicators (the same of AllMultipleMatches) that are expressend inside parenthesis
		//new approach
		try {
			  return new StringBuilder()
						.append(getValue(prefixExp, target))
						.append(getValue(idExp, target))
						.toString();			
			} catch (Exception exception) {
				LOGGER.error(MessageCatalog._00034_NWS_SYSTEM_INTERNAL_FAILURE, exception);
				return null;
			}			
		
//		//old approach
//			try {
//				return new StringBuilder()
//					.append(xpath.df(prefixExp.substring(0,3), prefixExp.substring(3,4), target).getTextContent())
//					.append(xpath.df(idExp.substring(0,3), idExp.substring(3,4), target).getTextContent())
//					.toString();
//			} catch (Exception exception) {
//				LOGGER.error(MessageCatalog._00034_NWS_SYSTEM_INTERNAL_FAILURE, exception);
//				return null;
//			}
//		}
	}
	
	/**
	 * Extract text value of an expression like 912(1-0)a 
	 * @param expression
	 * @param doc
	 * @return
	 */
	
	private String getValue(String expression, Document doc) throws Exception {
	    
		String result = null;
	
		//expression input is tag(ind1 - ind2)subfield    es. 912(1-0)a 
		String tag = expression.substring(0,3);
		String subfield;
		String [] indicators = null;
		int closedParenthesis = expression.indexOf(")");
		int openParenthesis = expression.indexOf("(");
		if(closedParenthesis > -1 ) {
			subfield = expression.substring(closedParenthesis + 1);
			indicators = expression.substring(openParenthesis + 1, closedParenthesis).split("-");
		}
		else {
			subfield = expression.substring(3);
		}
		 
		//the output expression is [datafield[@tag='912' and @ind1='1' and @ind2='0']
		StringBuilder expressionXpath = new StringBuilder();
		expressionXpath.append("datafield[@tag='")
				.append(tag)
				.append("'");
		if(indicators != null){
			expressionXpath.append(" and @ind1='").append(indicators[0]).append("'");
			expressionXpath.append(" and @ind2='").append(indicators[1]).append("'");
		}
		expressionXpath.append("]");
		
		//extract value of expression
		Element node = (Element) xpath.one(expressionXpath.toString(), doc);
		
		result = xpath.combine(node, subfield);
		return result;
	}

	@Override
	public String entityKind() {
		return "MANIFESTATION";
	}
}