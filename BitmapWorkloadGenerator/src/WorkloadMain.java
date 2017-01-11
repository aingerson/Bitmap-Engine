import java.io.IOException;


public class WorkloadMain
{
	public static void main(String[] args) throws IOException
	{
		//*************SET GENERAL VARIABLES
		int num_attributes = 10; 	// number of attributes
		int cardinality = 10; 		// cardinality (number of bins per attribute)
		//*************END
		
		//*************SET VARIABLES HERE FOR BITMAP DATA GENERATION
		int num_rows = 30; 		// number of rows
		int data_skew = 2; 		// skew for which bins are chosen to be favored with a 1
								// where 0 = uniform distribution and infinity = first rank always gets picked

		String bitmap_out = "bitmap_out.txt"; // name of the file containing the bitmap
		String bitmap_out_gc = "bitmap_out_gc.txt"; // name of the file containing the grey code-oredered bitmap
		//**************END

		//generate the bitmap file
		DataGenerator data = new DataGenerator(num_attributes, cardinality, data_skew, num_rows);
		data.writeFile(bitmap_out);

		//sort the file using Grey code ordering
		GreyCodeSorter sorter = new GreyCodeSorter(bitmap_out);
		sorter.writeFile(bitmap_out_gc);
		

		//*************SET VARIABLES HERE FOR QUERY SET GENERATION
		//int mode = QueryGenerator.POINT_ONLY;	// must be one of {POINT_ONLY, RANGE_ONLY, MIXED_MODE}
		//int mode = QueryGenerator.RANGE_ONLY;	// must be one of {POINT_ONLY, RANGE_ONLY, MIXED_MODE}
		int mode = QueryGenerator.MIXED_MODE;	// must be one of {POINT_ONLY, RANGE_ONLY, MIXED_MODE}

		int num_queries = 1000;					// number of queries to generate
		
		String query_out = "query_out.txt"; 	// name of the file containing the queries
		int qry_skew_att = 2;
		int qry_skew_bin = 2;
		//**************END

		//generate the query file
		QueryGenerator query = new QueryGenerator(num_attributes, cardinality, num_queries, qry_skew_att, qry_skew_bin);
 		query.writeFile(mode, 0.7, query_out);	//query set containing 70% point queries

	}
}
