package eu.aliada.rdfizer.delta;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.springframework.stereotype.Component;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;



@Component
public class ModsConv {
	
		
	public void convert(){		
		String xmlFilePath = "/home/alice/Documenti/atCult/marc_esperimenti/input.xml";
		String xsltPath = "/home/alice/Documenti/atCult/mods/modsrdf.xsl";
		String baseUrlConfig = "http://rdf.sdl.it/";
		File xmlFile = new File(xmlFilePath);
		String triples = "";
		try {
			triples =  conv(xsltPath, baseUrlConfig, xmlFile);	
			System.out.println(triples);
		}catch (Exception e) {
			e.printStackTrace();
		}
		/**
		 * url digilab
		 */
		//String url = "http://151.1.233.161:9999/blazegraph/namespace/aliada/sparql";
		String url = "http://localhost:9999/blazegraph/namespace/aliada/sparql";
		insert(url, triples);
	}






	public String conv(String xsltPath, String baseUrlConfig, File xmlFile)
			 {
		String triplesToLoad = null;
		try {			
			//transform record in modsRdfXml
			System.setProperty("javax.xml.transform.TransformerFactory",    
			        "net.sf.saxon.TransformerFactoryImpl");
			 TransformerFactory tFactory = TransformerFactory.newInstance();
		    
		     Transformer transformer =
		     tFactory.newTransformer(new StreamSource(new File(xsltPath)));
	    	 StringWriter outWriter = new StringWriter();
	    	 StreamResult streamResult = new StreamResult( outWriter );
	    	 transformer.transform(new StreamSource(xmlFile),
		                                  streamResult);
	    	 StringBuffer sb = outWriter.getBuffer(); 
	    	 String modsRdfXml = sb.toString();	
	    
	    	 InputStream rdfStream = new ByteArrayInputStream(modsRdfXml.getBytes(StandardCharsets.UTF_8));
			
	    	 //translate in N-TRIPLE
			 Model model = ModelFactory.createDefaultModel();		
			 model.read(rdfStream, baseUrlConfig, "RDF/XML");
			 StringWriter stringWriter = new StringWriter();
		     model.write(stringWriter, "N-TRIPLE"); 
		     
		     triplesToLoad = stringWriter.toString();	     
		    			
//			//read xml source
//			File fXmlFile = new File(xmlFilePath);
//			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
//			Document document = dBuilder.parse(fXmlFile);
//			document.getDocumentElement().normalize();
//			final Element root = document.getDocumentElement();
//			
//			//set root in velocity context
//			final VelocityContext velocityContext = new VelocityContext();
//			velocityContext.put(Constants.ROOT_ELEMENT_ATTRIBUTE_NAME, root);	
//			velocityContext.put(Constants.XPATH_ATTRIBUTE_NAME, oxpath);
//			velocityContext.put(Constants.FUNCTION_ATTRIBUTE_NAME, function);
//			final Template template = velocityEngine.getTemplate("mods.vm", "UTF-8");
//			
//			/* now render the template into a StringWriter */
//	        StringWriter writer = new StringWriter();
//	        template.merge( velocityContext, writer );
//	        triplesToLoad = writer.toString();
	       
	      
		} catch (Exception e) {
			// TODO Auto-generated catch block			
			e.printStackTrace();			
		}
		//result = result && insert(configuration.getSparqlEndpointUrl(), triplesToLoad);
//		return triplesToLoad;
		return triplesToLoad;
	}
	
	
	
	public boolean insert(String url, String triples){		
		OkHttpClient client = new OkHttpClient();

		MediaType mediaType = MediaType.parse("text/rdf+n3");
		RequestBody body = RequestBody.create(mediaType, triples);
		Request request = new Request.Builder()
		  .url(url)
		  .post(body)
		  .addHeader("content-type", "text/rdf+n3")	  	 
		  .build();
		try {
			Response response = client.newCall(request).execute();
			 final ResponseBody responseBody = response.body();
			 responseBody.close();
			//logger.debug("response code: " + response + ", inserting cluster");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

}
