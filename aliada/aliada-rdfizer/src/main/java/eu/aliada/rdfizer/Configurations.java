package eu.aliada.rdfizer;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class Configurations {
	@Autowired
	private Properties externalTargets;

	public List<String> getListProperty(final String key){		
		final String value = externalTargets.getProperty(key);
		return (isNotNullOrEmptyString(value))
				? Arrays.asList(value.split(","))
				: null;
	}
	
	public String getProperty(String key){
	    return externalTargets.getProperty(key);
	}	

	public Integer getIntProperty(String key){
	    Integer result = null;
	    if(key !=null && !key.trim().isEmpty()){
	        result = Integer.parseInt(this.externalTargets.getProperty(key));
	    }
	    return result;
	}	

	public boolean getBooleanProperty(String key) {
		return "true".equalsIgnoreCase(externalTargets.getProperty(key));
	}
	
	private boolean isNotNullOrEmptyString(final String value) {
		return value != null && !value.trim().isEmpty();
	}	
}
