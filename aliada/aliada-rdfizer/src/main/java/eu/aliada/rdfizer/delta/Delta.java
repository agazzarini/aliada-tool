package eu.aliada.rdfizer.delta;

import static eu.aliada.shared.Strings.isNotNullAndNotEmpty;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ResponseCache;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Message;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONArray;
import org.json.JSONObject;
import org.marc4j.MarcXmlReader;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.sparql.engine.http.Service;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.sun.istack.FinalArrayList;

import eu.aliada.rdfizer.Configurations;
import eu.aliada.rdfizer.Constants;
import eu.aliada.rdfizer.Function;
import eu.aliada.rdfizer.datasource.Cache;
import eu.aliada.rdfizer.datasource.rdbms.JobInstance;
import eu.aliada.rdfizer.framework.UnableToProceedWithConversionException;
import eu.aliada.rdfizer.log.MessageCatalog;
import eu.aliada.rdfizer.pipeline.format.marc.frbr.cluster.Cluster;
import eu.aliada.rdfizer.pipeline.format.marc.frbr.model.FrbrDocument;
import eu.aliada.rdfizer.pipeline.processors.RESTSparqlInsertProcessor;
import eu.aliada.shared.log.Log;
import eu.aliada.shared.rdfstore.RDFStoreDAO;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;


@Component
public class Delta {
	
	@Autowired
	private DeltaDAO dao;
	
	@Autowired
	protected VelocityEngine velocityEngine;	

	@Autowired
	protected Function function;
	
	@Autowired
	protected Configurations configurations;

	private @Value("${record.delete.dir}") String deleteDir;
	private @Value("${record.delete.file}") String deleteFile;
	private @Value("${record.delete.update.file}") String deleteForUpdateFile;
	private @Value("${record.delete.update.dir}") String deleteForUpdateDir;
	
	
	private final Log logger = new Log(Delta.class);	
	final String triplesUrl;
	final RDFStoreDAO rdfStoreDAO;
	final String rdfStoreUrl;
	final JobInstance configuration;
	//constants
	final String NAME_TYPE = "NAME";
	final String TITLE_TYPE = "TITLE";
	final String WORK_ENTITY = "Work";
	final String PERSON_ENTITY = "Person";	
	final String AGENT_ENTITY = "Agent";
	final String ORGANIZATION_ENTITY = "Organization";
	final String MEETING_ENTITY = "Meeting";
	
	
	public Delta(String rdfStore, String triplesUrl) {		
		this.rdfStoreDAO = new RDFStoreDAO();		
		this.triplesUrl = triplesUrl;
		this.rdfStoreUrl = rdfStore;
		this.configuration =  new JobInstance();
		configuration.setNamespace(triplesUrl);
		configuration.setGraphName("");
		configuration.setSparqlUsername("");
		configuration.setSparqlPassword("");
		configuration.setSparqlEndpointUrl(rdfStore);	
	}
	
	public boolean deleteCluster(final String json){
		boolean result =  true;
		List<NameValuePair> clusterList = new ArrayList<>();
		//extract data from json
		try {
			clusterList = extractClusterFromJson(json);		
		}
		catch (Exception e){
			return false;
		}
		//delete records
		for (NameValuePair record : clusterList){
			result = result && deleteSingleCluster(record.getName(), record.getValue());
		}
		return result;
	}
	
	public boolean updateCluster(final String json) {
		boolean result = true;
		List<NameValuePair> clusterList = new ArrayList<>();				
		try {
			//extract data from json
			clusterList = extractClusterFromJson(json);
		}
		catch(Exception e) {
			return false;
		}
		//delete records
		for (NameValuePair record : clusterList){
			result = result && updateSingleCluster(record.getName(), record.getValue());
		}				
		return result;
	}
	
	/**
	 * extract json in a list of cluster
	 * 
	 * @param json
	 * @return list of pairs (clstr_id, clstr_type) 
	 */
	private List<NameValuePair> extractClusterFromJson(final String json) throws Exception {
		List<NameValuePair> list = new ArrayList<>();
		try {
			JSONObject root = new JSONObject(json);
			JSONArray jsonArray = root.getJSONArray("clusters");
			for(int i = 0; i < jsonArray.length(); i++){
				//System.out.println(jsonArray.getJSONObject(i).getString("id") + " " + jsonArray.getJSONObject(i).getString("type") );
			    list.add(new NameValuePair(jsonArray.getJSONObject(i).getString("id"), jsonArray.getJSONObject(i).getString("type")));
			}			
		} catch (Exception e){
			logger.error("extract from cluster: ", e);
			throw e ;
		}
		return list;
	}
	
		
	/**
	 * Extract list of records Id
	 */
	
	private List<String> extractRecordFromFile(String fileName) throws Exception{
		List<String> list = new ArrayList<String>();
		try {
			String inputPath = deleteDir + "/" + fileName;
			File inputFile = new File(inputPath);
			if(!inputFile.exists()) {
				logger.info("DELETE RECORD: There are no records to delete");
			}
			else {
				BufferedReader br = new BufferedReader(new FileReader(inputFile)); 
				String line;
				while ((line = br.readLine()) != null) {
					// process the line.
					list.add(line);
				}		
			}
		} catch (Exception e){
			logger.error("extractRecordFromFile Error in retrieving list", e);
			throw e ;
		}
		return list;		
	}
	
	/**
	 * Delete cluster
	 * @param idCluster
	 * @param type  NAME for cluster name, TITLE for cluster title
	 */
	
	public boolean deleteSingleCluster (final String idCluster, final String type) {
		logger.info("deleting cluster " + idCluster + " (" + type + ") ...");
		boolean result = true;
		String sparqlEndPointUrl = configuration.getSparqlEndpointUrl();
		
		String entity_type = "???";
		//if it's not person but meeting, corporate
		
		//mapping 
		if (NAME_TYPE.equals(type)){
			result = deleteOperations(idCluster, type, AGENT_ENTITY);
			//add person too because I have some old triplestore with Person triples
			result = result && deleteOperations(idCluster, type, PERSON_ENTITY);
		}
		else if(TITLE_TYPE.equals(type)) {
			result = deleteOperations(idCluster, type, WORK_ENTITY);
		}				
		return result;	
	}
	
	
	public boolean deleteOperations (final String idCluster, final String type, final String entity_type) {
		boolean result = true;
		String clusterUri = "<" + configuration.getNamespace() + entity_type + "/" + idCluster + ">";
		String sparqlEndPointUrl = configuration.getSparqlEndpointUrl();
		//delete triples where cluster is subject 
		final String whereClause1 = clusterUri + " ?p ?o ";
		result = result && callDelete(sparqlEndPointUrl, whereClause1);
				
		//delete triples where cluster is object
		final String whereClause2 = "?s ?p " + clusterUri;
		result = result && callDelete(sparqlEndPointUrl, whereClause2);
				
		if(!result){
			logger.error("Error deleting cluster " + idCluster + " (" + type + " " + entity_type + ")");
		}
		else {
			logger.info("cluster " + idCluster + " (" + type + " " + entity_type + ") deleted");
		}
		return result;
	}
	
	
	/**
	 * update cluster
	 * @param idCluster
	 * @param type  NAME for cluster name, TITLE for cluster title
	 */
	
	public boolean updateSingleCluster (final String idCluster, final String type) {
		logger.info("updating cluster " + idCluster + " (" + type + ") ...");
		boolean result = true;
		//delete cluster first
		result = result && deleteSingleCluster(idCluster, type);		
		//reload cluster then
		result = result && callReloadCluster(idCluster, type);
		
		if(!result) {
			logger.error("Error in uploading cluster " + idCluster  + " ("+ type + ")");
		}
		else {
			logger.info("cluster " + idCluster + " (" + type + ") updated");
		}
		return result;
	}
	
	/**
	 * reload cluster updated from database
	 * @param idCluster	 * 
	 */
	public boolean callReloadCluster (final String clusterId, final String type){	
		boolean result = true;		
		String sparqlEndPointUrl = configuration.getSparqlEndpointUrl();		
		String triplesToLoad = null;		
		Map<String, List<Cluster>> people = null;					
		Map<String, List<Cluster>> works = null;
		List<String> manifestations = null;
		List<String> authorWorks = null;		
		String mainAuthor = null;		
		if (NAME_TYPE.equals(type)){
			people = dao.getClusteredName(clusterId);
			authorWorks = dao.getAuthorWorks(clusterId);			
		}
		else if(TITLE_TYPE.equals(type)) {
			works = dao.getClusteredWork(clusterId);	
			manifestations = dao.getManifestationIDs(clusterId, configuration);
			//for authorized Access Point
			mainAuthor = dao.getMainAuthor(clusterId);
		}								
		final FrbrDocument document = new FrbrDocument(
				null,
				works,
				null,
				null,
				people,
				null,
				null,
				null,
				null,
				null,
				null);					
		try {
			VelocityContext velocityContext = populateVelocityContext(document, manifestations, configuration, authorWorks, mainAuthor);
			final Template template = velocityEngine.getTemplate("unimarcxml.frbr.4delta.vm", "UTF-8");
			/* now render the template into a StringWriter */
	        StringWriter writer = new StringWriter();
	        template.merge( velocityContext, writer );
	        triplesToLoad = writer.toString();
	        /* show triples generated */
	        if (logger.isDebugEnabled()){
	        	System.out.println( triplesToLoad ); 
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error("callReloadCluster", e);
			return false;
		}
		result = result && insert(configuration.getSparqlEndpointUrl(), triplesToLoad);
		return result;
		
	}
	
	protected VelocityContext populateVelocityContext(			
			final FrbrDocument frbrDocument,
			final List<String> manifestations,
			final JobInstance configuration,
			final List<String> authorWorks,
			final String mainAuthor) throws UnableToProceedWithConversionException {
		final VelocityContext velocityContext = new VelocityContext();
		if(frbrDocument !=null) {			
			velocityContext.put("manifestations", manifestations);
			velocityContext.put("authorWorks", authorWorks);
			velocityContext.put("mainAuthor", mainAuthor);
			velocityContext.put(Constants.JOB_CONFIGURATION_ATTRIBUTE_NAME, configuration);
			velocityContext.put(Constants.FRBR_DATA_ATTRIBUTE_NAME, frbrDocument);
			velocityContext.put(Constants.FUNCTION_ATTRIBUTE_NAME, function);			
		}
		return velocityContext;
	}
	/**
	 * Deletes record before the update
	 * @return
	 */
	public boolean deleteRecForUpdate() {
		boolean result = true;
			//write a file with record to delete for update
		try {
			//delete previous txt file
			Files.deleteIfExists(Paths.get(deleteDir + "/" + deleteForUpdateFile));
			File directory = new File(deleteForUpdateDir);
			for (final File fileEntry : directory.listFiles()) {	
				if(!fileEntry.isDirectory() && fileEntry.getName().endsWith(".xml")) {
					result = result && xmlExtract(fileEntry.getAbsolutePath(), deleteDir + "/" + deleteForUpdateFile);	
				}
			}	
		}catch (Exception e) {
			logger.error("delete record for update: ", e);
			
			return false;
		}
		//now call delete
		result = result && deleteRecord(deleteForUpdateFile);
		return result;
		
	}
	/**
	 * reads xml files and extract a txt with record's ids to delete them for update
	 * @param pathFileToRead
	 * @param pathFileToWrite
	 * @return
	 */
	private boolean xmlExtract(final String pathFileToRead, final String pathFileToWrite){
		boolean result = true;
		//new file	
		FileWriter writer = null;
		try {
			writer = new FileWriter(pathFileToWrite, true);
		}catch (Exception e){			
			logger.error("xml extract", e);
			return false;
		}
			
		try{  
			InputStream in = new FileInputStream(pathFileToRead);	
			MarcXmlReader reader = new MarcXmlReader(in);    
			
		    while(reader.hasNext()){
		    	Record record = reader.next();
		    	DataField _997 = (DataField) record.getVariableField("997");
		    	DataField _912 = (DataField) record.getVariableField("912");
		    	String newLine = _997.getSubfield('a').getData() + _912.getSubfield('a').getData();
		    	
		        writer.write(newLine + "\n"); 
		    }	    
		    //if I finished record and counter didn't reach its maximum
		    writer.close();
			return result;
		}
		catch(Exception e){
			logger.error("xml extract", e);	
			return false;
		}
	}

	public boolean deleteRecord(String fileName) {
		boolean result =  true;
		List<String> recordList = new ArrayList<>();
		try {
			//extract data from json		
			recordList = extractRecordFromFile(fileName);
		}
		catch (Exception e) {
			return false;
		}
		//delete records
		int count = 0;
		boolean singleResult = true;
		for (String record : recordList){
			singleResult = deleteSingleRecord(record);
			if(singleResult) count++;
			result = result && singleResult;			
		}
		logger.info("Deleted " + count + " records");
		return result;
	}
	
	/**
	 * Transform json in a list of records
	 * @param json
	 * @return List<String> list or records
	 */
	
	private List<String> extractRecordFromJson(final String json) {
		List<String> list = new ArrayList<String>();
		try{
			JSONObject root = new JSONObject(json);
			JSONArray jsonArray = root.getJSONArray("records");			
			for(int i = 0; i < jsonArray.length(); i++){
				//System.out.println(jsonArray.getString(i));
			    list.add(jsonArray.getString(i));
			}
		} catch (Exception e){
			logger.error("extractRecordFromJson", e);
		}
		return list;
	}
	
	
	/**
	 * Delete triples related to record 
	 * @param idRecord
	 */			
	public boolean deleteSingleRecord(final String idRecord) {		
		logger.debug("deleting record " + idRecord + "...");
		boolean result = true;
				
		final String sparqlEndPointUrl = configuration.getSparqlEndpointUrl();
		
		//create uri of istance associated with record		
		final String recordUri = "<" + configuration.getNamespace() + "Instance/" + idRecord + ">";		
		
		//delete triples where record is object		
		final String whereClause1 = "?s ?p " + recordUri;
		result = result && callDelete(sparqlEndPointUrl, whereClause1);
		
		//delete triples where record is subject and has property IstanceOf 
		//I make this query separated, becouse the next one will delete "children" triples, and I don't want to delete 
		//triple with Work subject
		final String whereClause2 = recordUri + "<http://bibframe.org/vocab/instanceOf>" + " ?o";
		result = result && callDelete(sparqlEndPointUrl, whereClause2);
		
		//delete triples that contains record's "children"				
		final String whereClause3 = recordUri + " ?p2 ?s . " +
							"?s ?p	?o . ";
		result = result && callDelete(sparqlEndPointUrl, whereClause3);
		
		//delete triples where record is subject
		final String whereClause4 = recordUri + " ?p ?o ";
		result = result && callDelete(sparqlEndPointUrl, whereClause4);
		
		if(!result){
			logger.error("Error deleting record " + idRecord);
		}
		else {			
		}		
		return result;
	}

	/**
	 * build query, encode query parameter, build url and call rest API
	 * @param sparqlEndPointUrl
	 * @param whereClause
	 */
	
	public boolean callDelete(final String sparqlEndPointUrl, final String whereClause) {		
		String query = buildDeleteQueryConstruct(whereClause);
		//logger.debug("url " + sparqlEndPointUrl + "?query=" + query , " (Before encoding");
		String encodedQuery = encodeUrl(query);
		String url = sparqlEndPointUrl + "?query=" + encodedQuery;
		try {
			delete(url);
		} catch (HttpException e) {			
			logger.error("call delete", e);
			return false;
		} catch (IOException e) {			
			logger.error("call delete", e);
			return false;
		}
		return true;
	}
		
	
	/**
	 * Builds a SPARQL DELETE with the given data with command DELETE (and not DELETE DATA)
	 * 
	 * @param graphName the graphName, null in case of default graph.
	 * @param triples the triples (in N3 format) that will be removed.
	 * @return a new SPARQL INSERT query.
	 * @since 2.0
	 */
	String buildDeleteQueryConstruct(final String triples) {
		final StringBuilder builder = new StringBuilder();
		builder.append("CONSTRUCT ");
		
		builder.append("WHERE { ").append(triples).append("}");
		String result = builder.toString();	
		return result;
	}
	

	
	/**
	 * Encode url parameter (i.e replace all blank space)
	 * @param input
	 * @return
	 */
	
	public String encodeUrl (String input) {
		try {
			return URLEncoder.encode(input, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage() + " for input " + input, input, e);
		}
		return input;
	}	
	
	/**
	 * Call httpClient to connect to blazegraph API REST and make delete
	 * @param url 
	 * @return response code
	 * @throws HttpException
	 * @throws IOException
	 */
	
	public long delete(String url) throws HttpException, IOException {		
		HttpClient httpclient = new HttpClient();
		//set method DELETE 
        HttpMethod method = new DeleteMethod(url);
        int responseCode = httpclient.executeMethod(method);
        logger.debug("responseCode for deleteRecord: " + responseCode);        
        return responseCode;
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
			logger.error("insert", e);
			return false;
		}
		return true;
	}
	
	
			

}
