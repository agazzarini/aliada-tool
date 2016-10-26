package eu.aliada.rdfizer.pipeline.format.marc.frbr.cluster;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

/**
 * Integration service between the conversion pipeline and the database names / works cluster.
 * 
 * @author Andrea Gazzarini
 * @since 1.0
 */
@Component
public class ClusterService {
	@Autowired
	private DataSource clusterDataSource;
	
	private Map<String, Cluster> cachedNameClusters = new ConcurrentLinkedHashMap.Builder<String, Cluster>().initialCapacity(1000).maximumWeightedCapacity(50000).build();
	private Map<String, Cluster> cachedTitleClusters = new ConcurrentLinkedHashMap.Builder<String, Cluster>().initialCapacity(1000).maximumWeightedCapacity(50000).build();
	private Set<Integer> processedNameClusters = new HashSet<Integer>();
	private Set<Integer> processedTitleClusters = new HashSet<Integer>();
	
	/**
	 * Returns the name {@link Cluster} associated with the given heading.
	 * NOTE: although the heading is a string, the current implementation assumes that is actually 
	 * the cluster identifier (i.e. an integer).
	 * 
	 * @param heading the cluster search criterion.
	 * @return the name {@link Cluster} associated with the given heading.
	 * @param titlesClusters the list of title clusters associated with this heading.
	 * @throws SQLException in case of data access failure.
	 */
	public Cluster getNameCluster(final String heading, final Set<Cluster> titlesClusters) throws SQLException {
		Cluster cluster = cachedNameClusters.get(heading);
		if (cluster == null) {
			cluster = loadNameCluster(heading);
			if (cluster != null) {
				cachedNameClusters.put(heading, cluster);
			} else {
				// Do not cache fake clusters!
				return new FakeCluster(heading);
			}
		}
		return cluster;
	}
	
	/**
	 * Returns the title {@link Cluster} associated with the given heading.
	 * NOTE: although the heading is a string, the current implementation assumes that is actually 
	 * the cluster identifier (i.e. an integer).
	 * 
	 * @param heading the cluster search criterion.
	 * @return the title {@link Cluster} associated with the given heading.
	 * @throws SQLException in case of data access failure.
	 */
	public Cluster getTitleCluster(final String heading) throws SQLException {
		Cluster cluster = cachedTitleClusters.get(heading);
		if (cluster == null) {
			cluster = loadTitleCluster(heading);
			if (cluster != null) {
				cachedTitleClusters.put(heading, cluster);
			} else {
				// Do not cache fake clusters!
				return new FakeCluster(heading);
			}
		}
		return cluster;
	}
	
	/**
	 * Checks if the name cluster associated with the given identifier has been already processed.
	 * 
	 * @param id the cluster identifier.
	 * @return true if the name cluster associated with the given identifier has been already processed.
	 */
	public boolean nameClusterAlreadyProcessed(final int id) {
		return processedNameClusters.contains(id);
	}

	/**
	 * Marks the name cluster associated with the given identifier as processed.
	 * 
	 * @param id the cluster identifier.
	 */
	public void markNameClusterAsProcessed(int id) {
		processedNameClusters.add(id);
	}
	
	/**
	 * Checks if the title cluster associated with the given identifier has been already processed.
	 * 
	 * @param id the cluster identifier.
	 * @return true if the title cluster associated with the given identifier has been already processed.
	 */
	public boolean titleClusterAlreadyProcessed(final int id) {
		return processedTitleClusters.contains(id);
	}

	/**
	 * Marks the title cluster associated with the given identifier as processed.
	 * 
	 * @param id the cluster identifier.
	 */
	public void markTitleClusterAsProcessed(int id) {
		processedTitleClusters.add(id);
	}	
	
	/**
	 * Loads, from the database, the name {@link Cluster} associated with the given heading.
	 * 
	 * @param heading the cluster search criterion.
	 * @return the name {@link Cluster} associated with the given heading.
	 * @throws SQLException in case of data access failure.
	 */
	void loadTitlesBelongingToACluster(final Cluster nameCluster, final Connection connection) throws SQLException {
		if (nameCluster == null) { return; }
		try (final PreparedStatement statement = connection.prepareStatement("select clstr_ttl_id from clstr_nme_ttl where clstr_nme_id = ?")) {
			statement.setInt(1, nameCluster.getId());
			try( final ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					nameCluster.addParent(rs.getInt("clstr_ttl_id"));
				}
			} 
		}
	}
	
	/**
	 * Loads, from the database, the name {@link Cluster} associated with the given heading.
	 * 
	 * @param heading the cluster search criterion.
	 * @return the name {@link Cluster} associated with the given heading.
	 * @throws SQLException in case of data access failure.
	 */
	Cluster loadNameCluster(final String heading) throws SQLException {
		final List<String> externalUri = loadExternalUri(heading);
		try (final Connection connection = clusterDataSource.getConnection()) {
//			final String query = "select  distinct ON (name, pref_frm) a.clstr_id, a.hdg_id, a.name, a.pref_frm, b.ext_lnk_cde, b.url, b.ext_lnk_id " +
//					"from clstr_nme_grp a join nme_ext_lnk b on a.clstr_id = b.clstr_id " +
//					"where a.clstr_id = ?";
						
			final String query = "select a.clstr_id, a.name, a.pref_frm, a.hdg_id, b.typ_nme_id from clstr_nme_grp a join bib_nme b on a.clstr_id=b.clstr_id where a.clstr_id = ?";
			try (final PreparedStatement statement = connection.prepareStatement(query)) {				
				
				statement.setInt(1, Integer.parseInt(heading));
				try( final ResultSet rs = statement.executeQuery()) {
					Cluster cluster = null;					
					while (rs.next()) {
						if (cluster == null) {
							cluster = new Cluster(rs.getInt("clstr_id"));
						}	
						
						String name = rs.getString("name");
						Boolean pref_form = "t".equals(rs.getString("pref_frm"));					
						String hdg_id = rs.getString("hdg_id");
						String name_type = rs.getString("typ_nme_id");
						String viaf_id = null;
						if (hdg_id.contains("http://viaf.org/viaf")){
							viaf_id = hdg_id;
						}						
						
						cluster.addEntry(
								new ClusterEntry(name, pref_form,hdg_id, viaf_id, externalUri, name_type));
						//System.out.println("viaf id: " + viaf_id);
					}
					loadTitlesBelongingToACluster(cluster, connection);
					return cluster;
				}
			}
		}
	}
	List<String> loadExternalUri (final String heading) throws SQLException { 
		try (final Connection connection = clusterDataSource.getConnection()) {
			List<String> resultList = new ArrayList<String>();
			final String query = "select  distinct url from nme_ext_lnk  where clstr_id = ?";
			try (final PreparedStatement statement = connection.prepareStatement(query)) {				
				statement.setInt(1, Integer.parseInt(heading));
				try( final ResultSet rs = statement.executeQuery()) {				
					while (rs.next()) {						
						resultList.add(rs.getString("url"));						
					}
					return resultList;
				}
			}
		}
	}
	
	/**
	 * Loads, from the database, the title {@link Cluster} associated with the given heading.
	 * 
	 * @param heading the cluster search criterion.
	 * @return the title {@link Cluster} associated with the given heading.
	 * @throws SQLException in case of data access failure.
	 */
	//FIXME: Insert proper query
	Cluster loadTitleCluster(final String heading) throws SQLException {
		try (final Connection connection = clusterDataSource.getConnection()) {
			try (final PreparedStatement statement = connection.prepareStatement("select clstr_id,ttl_hdg_id,ttl_str_txt,viaf_id,typ_ttl from ttl_hdg where clstr_id = ? and (typ_ttl is null or typ_ttl in ('TU','TV'))")) {
				statement.setInt(1, Integer.parseInt(heading));
				try( final ResultSet rs = statement.executeQuery()) {
					Cluster cluster = null;
					while (rs.next()) {
						if (cluster == null) {
							cluster = new Cluster(rs.getInt("clstr_id"));
						}
//						String viaf_id = rs.getString("viaf_id");
//						if (viaf_id != null && !viaf_id.contains("http://viaf.org/viaf")){
//							System.out.println("+++++ " + heading + "---> to viaf: " + viaf_id + " +++++");
//						}
												
						cluster.addEntry(
								new ClusterEntry(
										rs.getString("ttl_str_txt"), 
										rs.getString("typ_ttl") == null,
										rs.getString("ttl_hdg_id"),
										rs.getString("viaf_id"), 
										null, 
										null));		
						
						
					}					
					return cluster;
				}
			}
		}
	}
}