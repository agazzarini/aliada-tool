package eu.aliada.rdfizer.delta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.util.MultiMap;

public class DeltaTest {

	public static void main (String [] args) {
		String id = "";
		String typeName = "NAME";
		String typeTitle = "TITLE";
		
		
		
		String [] records = {"UNIBAS000037578", "UNINA000683506"};
		//call to Aliada REST API
		//deleteRecords(records);	
		
		
		String [] clustersToDelete = {"17469;NAME"};
		//call to Aliada REST API
		deleteClusters(clustersToDelete);
		
		String [] clustersToUpdate = {"388751;NAME"};
		//call to Aliada REST API 
		//updateClusters(clustersToUpdate);
		
		
		
					
		
	}
	
	/**
	 * Call aliada REST API to delete clusters
	 * @param clusters list 
	 */
	private static void deleteClusters(String [] clusters) {
		//create json from list of record
		String jsonClusters = createClusterJson(clusters);
		try {
			sendPOST("http://localhost:8891/rdfizer/deleteCluster", jsonClusters);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Call aliada REST API to update clusters
	 * @param clusters list 
	 */
	private static void updateClusters(String [] clusters) {
		//create json from list of record
		String jsonClusters = createClusterJson(clusters);
		try {
			sendPOST("http://localhost:8891/rdfizer/updateCluster", jsonClusters);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Call aliada REST API to delete records 
	 * @param records list
	 */
	
	private static void deleteRecords(String [] records) {
		//create json from list of record
		String jsonRecords = createRecordJson(records);
		try {
			sendPOST("http://localhost:8891/rdfizer/deleteRecord", jsonRecords);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Transform array of records ID in json 
	 * 
	 * { "records": ["UNIBAS000037578", "UNINA000683506"] }	   
	 * @param records
	 * @return json String
	 */
	
	public static String createRecordJson (String [] records) {
		StringBuffer json = new StringBuffer();
		json.append("{");
		json.append(" \"records\": [ ");
		for (int i = 0; i < records.length; i ++) {
			json.append(" \"" + records[i] + "\"");
			if (i != records.length - 1) {
				json.append(", ");
			}
		}
		json.append("]");
		json.append("}");
		return json.toString();
	}
	
	
	/**
	 * Transform array of clusters ID_CLSTR;TYPE_CLSTR in json 
	 * 
	 * { "clusters": [
     * 		{"id": "3522", "type": "NAME"},
     * 		{"id": "3436", "type": "TITLE"} ]  }      
	 * @param clusters
	 * @return json String
	 */
	
	public static String createClusterJson (String [] clusters) {
		StringBuffer json = new StringBuffer();
		json.append("{");
		json.append(" \"clusters\": [ ");
		for (int i = 0; i < clusters.length; i ++) {
			json.append("{");
			String [] cluster = clusters[i].split(";");			
			json.append(" \"id\": \"" + cluster[0] + "\", \"type\": \"" + cluster[1] + "\"");	
			json.append("}");
			if (i != clusters.length - 1) {
				json.append(", ");
			}
		}
		json.append("]");
		json.append("}");
		return json.toString();
	}
	
	/**
	 * Perform a POST request with body datas
	 * @param url
	 * @param body
	 * @throws IOException
	 */
	
	private static void sendPOST(String url, String body) throws IOException {
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("POST");
		//con.setRequestProperty("User-Agent", USER_AGENT);

		// For POST only - START
		con.setDoOutput(true);
		OutputStream os = con.getOutputStream();
		os.write(body.getBytes());
		os.flush();
		os.close();
		// For POST only - END

		int responseCode = con.getResponseCode();
		System.out.println("POST Response Code :: " + responseCode);

		if (responseCode == HttpURLConnection.HTTP_OK) { //success
			BufferedReader in = new BufferedReader(new InputStreamReader(
					con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			// print result
			System.out.println(response.toString());
		} else {
			System.out.println("POST request not worked");
		}
	}

	
	
}
