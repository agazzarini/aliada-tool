package eu.aliada.rdfizer.delta;

import java.sql.Connection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.aliada.rdfizer.Function;
import eu.aliada.rdfizer.datasource.rdbms.JobInstance;
import eu.aliada.rdfizer.pipeline.format.marc.frbr.cluster.Cluster;
import eu.aliada.rdfizer.pipeline.format.marc.frbr.cluster.ClusterEntry;


@Component
public class DeltaDAO {
	@Autowired
	private DataSource clusterDataSource;
	
	@Autowired
	private Function function;
	
	
	public Map<String, List<Cluster>> getClusteredWork (final String clstrId) {
		Map<String, List<Cluster>> map = new HashMap<String, List<Cluster>>();
		List<Cluster> clusters = new ArrayList<Cluster>();
		clusters.add(function.getTitleCluster(clstrId));
		String dummyString = "996";
		map.put(dummyString, clusters);
		return map;
	}
	
	public Map<String, List<Cluster>> getClusteredName (final String clstrId) {
		Map<String, List<Cluster>> map = new HashMap<String, List<Cluster>>();
		List<Cluster> clusters = new ArrayList<Cluster>();
		clusters.add(function.getNameCluster(clstrId, null));
		String dummyString = "700";
		map.put(dummyString, clusters);
		return map;
	}
	

	//called to fill authorized access point
	public String getMainAuthor (String clstr_id){
		String mainAuthor = null;
		Connection conn = null;
		String sql = "select norm_str_txt " +
				"from clstr_nme_ttl a join aut_nme b on a.clstr_nme_id = b.clstr_id " +
				"where clstr_ttl_id = ? " +
				"limit 1";
		try {
			conn = clusterDataSource.getConnection();
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setInt(1, Integer.parseInt(clstr_id));
			final ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				mainAuthor = rs.getString("norm_str_txt");
			}
			ps.close();
			return mainAuthor;
		} catch (SQLException e) {
			throw new RuntimeException(e);

		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {}
			}
		}
	}
	
	
	
	public List<String> getAuthorWorks (String clstr_id){
		List<String> authorWorks = new ArrayList<String>();
		Connection conn = null;
		String sql = "select distinct a.clstr_id clusterId " +
				"from ttl_hdg a join clstr_nme_ttl b on a.clstr_id = b.clstr_ttl_id and b.clstr_nme_id = ? and (a.typ_ttl <> 'TU' or a.typ_ttl is null)";
		try {
			conn = clusterDataSource.getConnection();
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setInt(1, Integer.parseInt(clstr_id));
			final ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {				
				authorWorks.add(rs.getString("clusterId"));
			}
			ps.close();
			return authorWorks;
		} catch (SQLException e) {
			throw new RuntimeException(e);

		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {}
			}
		}
	}
	
	public List<String> getManifestationIDs (final String clstr_id, final JobInstance configuration){
		List<String> manifestationsId = new ArrayList<String>();
		Connection conn = null;
		String sql = "select distinct a.orgnl_res_id, a.src_shrt_frm from ttl_hdg a where a.clstr_id = ? AND a.orgnl_res_id != ''";
		try {
			conn = clusterDataSource.getConnection();
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setInt(1, Integer.parseInt(clstr_id));
			final ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				manifestationsId.add( "<" + configuration.getNamespace() + "Instance/" + 
						rs.getString("src_shrt_frm") + rs.getString("orgnl_res_id") +
						">");
			}
			ps.close();
			return manifestationsId;
		} catch (SQLException e) {
			throw new RuntimeException(e);

		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {}
			}
		}
	}
	
		
	public Cluster connection (String clstr_id){
		Connection conn = null;
		Cluster cluster = null;
		String sql = "select clstr_id,ttl_hdg_id,ttl_str_txt,viaf_id,typ_ttl from ttl_hdg where clstr_id = ? and (typ_ttl is null or typ_ttl in ('TU','TV'))";
		try {
			conn = clusterDataSource.getConnection();
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setInt(1, Integer.parseInt(clstr_id));
			final ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
					if (cluster == null) {
						cluster = new Cluster(rs.getInt("clstr_id"));
					}
											
					cluster.addEntry(
							new ClusterEntry(
									rs.getString("ttl_str_txt"), 
									rs.getString("typ_ttl") == null,
									rs.getString("ttl_hdg_id"),
									rs.getString("viaf_id"), 
									null,
									null));	
			}	
			ps.close();
			return cluster;
			

		} catch (SQLException e) {
			throw new RuntimeException(e);

		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {}
			}
		}
	}
	
}
