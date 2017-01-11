import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * Objects of this class writes to a file containing a query set.
 * 
 * Assumptions:
 * 1) Point (exact-match) queries select one bin from a random number of attributes.
 * 		an AND operator is used between all bins. Lines containing a point query 
 * 		begin with 'P:'
 * 
 * 2) Range queries select a random number of attributes, and apply an OR operator
 * 		from bin0 to the a random bin for each attribute. An AND is later applied to
 * 		to the resulting bin from each attribute. Lines containing a range query 
 * 		begin with 'R:'
 * 
 * 3) Lines beginning with '#' denote a comment
 * 
 * @author Alexia, David, et al.
 * @version 1/9/2017
 */

public class QueryGenerator extends WorkloadGenerator
{
	/** query types */
	public static final int POINT_ONLY = 0;		/** only point queries */
	public static final int RANGE_ONLY = 1;		/** only range queries */
	public static final int MIXED_MODE = 2;		/** mix of both types of queries */

	/** fields */
	protected long num_queries;		// number of queries to generate in the workload
	protected Zipf zipf_rng_att;	// a random number generator based on Zipf distribution (use for attr)
	protected Zipf zipf_rng_bin;	// a random number generator based on Zipf distribution (use for bin)
	protected Random uniform_rng;	// a random number generator based on uniform distribution
	private long num_pt_queries;	// number of point queries to generate in the workload
	private long num_range_queries;	// number of range queries to generate in the workload
	private long num_attributes;	// number of attributes queried
	private long num_bins;			// number of bins queried
	

	/**
	 * Constructs a query generator with the given attributes.
	 * 
	 * @param num_attributes
	 * @param cardinality
	 * @param num_queries
	 * @param skew
	 */
	public QueryGenerator(int num_attributes, int cardinality, int num_queries, int skew_att, int skew_bin)
	{
		super(num_attributes, cardinality);
		this.num_queries = num_queries;
		this.zipf_rng_att = new Zipf(num_attributes, skew_att);
		this.zipf_rng_bin = new Zipf(cardinality, skew_bin);
		this.uniform_rng = new Random(System.currentTimeMillis());
		this.num_pt_queries = 0;
		this.num_range_queries = 0;
		this.num_attributes = 0;
		this.num_bins = 0;
	}


	/**
	 * Writes the queries a file with the given name. Each line contains 
	 * a single point or range query in the following format.
	 * 
	 * @param query_type	The type of query. Must be one of {POINT_ONLY, RANGE_ONLY, MIXED_MODE}
	 * @param file_out 		Name of the output file containing the queries
	 */
	@Override
	public void writeFile(String file_out)
	{
		this.writeFile(POINT_ONLY, 0, file_out);
	}

	/**
	 * Writes the queries a file with the given name. Each line contains 
	 * a single point or range query in the following format:
	 * 
	 * @param query_type		The type of query. Must be one of {POINT_ONLY, RANGE_ONLY, MIXED_MODE}
	 * @param pt_load_factor	Fraction of queries that should be point queries if MIXED_MODE is selected
	 * 								(ignored unless MIXED_MODE is selected)
	 * @param file_out			Name of the output file containing the queries
	 */
	public void writeFile(int query_type, double pt_load_factor, String file_out)
	{
		if (query_type < QueryGenerator.POINT_ONLY || query_type > QueryGenerator.MIXED_MODE) {
			throw new IllegalArgumentException("Invalid query type. Must be one "
					+ "of {QueryGenerator.POINT_ONLY, QueryGenerator.RANGE_ONLY, QueryGenerator.MIXED_MODE}");
		}

		//initialize point-query load factor
		switch(query_type)
		{
		case POINT_ONLY:
			pt_load_factor = 1;
			break;
		case RANGE_ONLY:
			pt_load_factor = 0;
			break;
		}

		//build each query
		StringBuilder query_set = new StringBuilder();
		for (int query_id = 0; query_id < num_queries; query_id++) {
			String query = (uniform_rng.nextDouble() < pt_load_factor) ? point() : range();
			query_set.append(query + "\n");
		}
		
		//open file for writing
		BufferedWriter out;
		try {
			//write queries to file
			out = new BufferedWriter(new FileWriter(file_out));
			out.write(metadata());
			out.write(query_set.toString());
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	/**
	 * Produces a range query.  Format: 
	 * R: [att1_bin_start,att1_bin_end] <op> ... <op> [attK_bin_start,attK_bin_end]
	 * 	where <op> = & or |
	 * @return
	 */
	private String range()
	{
		StringBuilder s = new StringBuilder("R:");

		// choose number of attributes to involve in the query (use zipf(num_attribute))
		int n = zipf_rng_att.next() + 1;

		// choose bin_high from each of the K attributes (zipf'(cardinality))
		for (int att_id = 0; att_id < n; att_id++) {
			//a range query from [bin_0, ..., bin_high]
			int bin_upper = uniform_rng.nextInt(this.cardinality);
			int offset = att_id * this.cardinality;
			s.append("[" + offset + "," + (offset + bin_upper) + "]");
			
			//update stats
			this.num_bins += bin_upper + 1;

			//append an <op> between attribute ranges
			if (att_id < n-1) {
				s.append("&");
			}
		}

		//update stats
		this.num_range_queries++;
		this.num_attributes += n;
		return s.toString();
	}


	/**
	 * Produces a point (i.e., exact-match) query.
	 * Format: P: att1_bin <op> att2_bin2 <op> ...
	 *  <op> = & or |
	 * @return
	 */
	private String point()
	{
		StringBuilder s = new StringBuilder("P:");

		// choose number of attributes to involve in the query (use zipf(num_attribute))
		int n = zipf_rng_att.next() + 1;

		// choose one bin from each of the K attributes (use zipf(cardinality))
		for (int att_id = 0; att_id < n; att_id++) {
			//skip to the offset for a randomly chosen bin, set the bit to 1
			int bin = this.rank_bin_map[att_id][zipf_rng_bin.next()];	//get a bin based on zipf distribution
			s.append(att_id * this.cardinality + bin);

			//append an <op> between bins			
			if (att_id < n-1) {
				s.append("&");
			}
		}
		
		//update stats
		this.num_pt_queries++;
		this.num_attributes += n;
		this.num_bins += n;
		return s.toString();
	}

	/**
	 * Returns a metadata string containing the stats for this query set
	 * @return
	 */
	private String metadata()
	{
		StringBuilder s = new StringBuilder();
		s.append("#######################################");
		s.append("\n# Metadata of query set");
		s.append("\n#");
		s.append("\n# Queries");
		s.append("\n# \tTotal: " + this.num_queries);
		s.append("\n# \tPoint: " + this.num_pt_queries + " (" + 100 * ((double) this.num_pt_queries/this.num_queries) +"%)");
		s.append("\n# \tRange: " + this.num_range_queries + " (" + 100 * ((double) this.num_range_queries/this.num_queries) +"%)");
		s.append("\n#");
		s.append("\n# Attributes");
		s.append("\n# \tTotal: " + this.num_attributes);
		s.append("\n# \tPer query: " + ((double) this.num_attributes/this.num_queries));
		s.append("\n#");
		s.append("\n# Bins");
		s.append("\n# \tTotal: " + num_bins);
		s.append("\n# \tPer query: " + ((double) this.num_bins/this.num_queries));
		s.append("\n# \tPer attribute: " + ((double) this.num_bins/this.num_attributes));
		s.append("\n#######################################\n");
		return s.toString();
	}
}
