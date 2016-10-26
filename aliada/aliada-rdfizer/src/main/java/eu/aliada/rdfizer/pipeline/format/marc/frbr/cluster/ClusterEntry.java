package eu.aliada.rdfizer.pipeline.format.marc.frbr.cluster;

import java.util.List;

/**
 * A cluster entry.
 * 
 * @author Andrea Gazzarini
 * @since 1.0
 */
public final class ClusterEntry {
	public final String heading;
	public final boolean preferred;
	public final String id;
	public final String viafId;	
	public final List<String> externalLink;
	public final String nameType;
	
	/**
	 * Builds a new cluster entry.
	 * 
	 * @param heading the heading text.
	 * @param preferred indicate that is a preferred form.
	 * @param id the heading identifier.
	 * @param viafId (optional) the VIAF identifier.
	 */
	public ClusterEntry(final String heading, final boolean preferred, final String id, final String viafId, final List<String> externalLink, final String name_type) {
		this.heading = heading;
		this.preferred = preferred;
		this.id = id;
		this.viafId = viafId;
		this.externalLink = externalLink;
		this.nameType = name_type;
	}

	/**
	 * Returns the heading associated with this cluster entry.
	 * 
	 * @return the heading associated with this cluster entry.
	 */
	public String getHeading() {
		return heading;
	}

	/**
	 * Returns true if the heading associated with this entry is the preferred form.
	 * 
	 * @return true if the heading associated with this entry is the preferred form.
	 */
	public boolean isPreferred() {
		return preferred;
	}

	/**
	 * Returns the entry identifier.
	 * 
	 * @return the entry identifier.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns the VIAF identifier associated with this entry.
	 * 
	 * @return the VIAF identifier associated with this entry.
	 */
	public String getViafId() {		
		return viafId;
	}
	
	/**
	 * Returns the ISNI identifier associated with this entry.
	 * 
	 * @return the ISNI identifier associated with this entry.
	 */
	
	public List<String> getExternalLink() {
		return externalLink;
	}
	
	/**
	 * returns the nameType code (for person, meeting, corporate)
	 * 
	 * @return the nameType code (for person, meeting, corporate)
	 */
	public String getNameType() {
		return nameType;
	}
}