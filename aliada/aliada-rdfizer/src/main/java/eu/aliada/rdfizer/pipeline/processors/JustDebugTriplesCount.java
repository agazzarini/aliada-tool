package eu.aliada.rdfizer.pipeline.processors;

import java.util.Collection;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;

import eu.aliada.rdfizer.Constants;
import eu.aliada.rdfizer.datasource.Cache;
import eu.aliada.rdfizer.datasource.rdbms.JobInstance;
import eu.aliada.rdfizer.mx.InMemoryJobResourceRegistry;
import eu.aliada.rdfizer.rest.JobResource;

/**
 * A debug outbound processor that counts the outcoming triples.
 * 
 * @author Andrea Gazzarini
 * @since 1.0
 */
public class JustDebugTriplesCount implements Processor {

	@Autowired
	protected Cache cache;
	
	@Autowired
	protected InMemoryJobResourceRegistry jobRegistry;
	
	@Override
	public void process(final Exchange exchange) throws Exception {
		final Integer jobId = exchange.getIn().getHeader(Constants.JOB_ID_ATTRIBUTE_NAME, Integer.class);
		final JobResource job = jobRegistry.getJobResource(jobId);
		if (job != null) {
			if (job.getTotalRecordsCount() < 1000) {
				System.out.println("JOB #" + jobId + ":" + job.getTotalRecordsCount() + " / " + job.getProcessedRecordsCount());
			} else {
				if (job.getProcessedRecordsCount() % 1000 == 0) {
					System.out.println("JOB #" + jobId + ":" + job.getTotalRecordsCount() + " / " + job.getProcessedRecordsCount());
				}
			}
		}
	}
}
