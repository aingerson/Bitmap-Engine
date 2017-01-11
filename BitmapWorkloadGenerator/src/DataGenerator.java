import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

/**
 * Objects of this class writes to an ASCII-based bitmap file with the given
 * attributes. For each attribute, a fixed number of bins is created. The 
 * Zipf-ranks for each attribute's bin are then randomly assigned (so that
 * the 1s aren't always in the same bin in a highly-skewed data set).
 * 
 * @author Alexia, David, et al.
 * @version 1/7/17
 */
public class DataGenerator extends WorkloadGenerator
{
	protected int num_rows; 		// number of rows
	protected Zipf zipf_rng;		// a random number generator based on Zipf distribution

	/**
	 * Constructs a data generator.
	 * 
	 * @param num_attributes
	 * @param cardinality
	 * @param num_rows
	 * @param skew
	 */
	public DataGenerator(int num_attributes, int cardinality, int skew, int num_rows)
	{
		super(num_attributes, cardinality);
		this.num_rows = num_rows;
		this.zipf_rng = new Zipf(cardinality, skew);
	}	


	/**
	 * Writes the bitmap a file with the given name. Each row contains 
	 * exactly <num_attributes> 1s. That is, no more than one bin per 
	 * attribute can be set.
	 * 
	 * @param file_out
	 * @author David
	 * @version 1/5/17
	 */
	@Override
	public void writeFile(String file_out)
	{
		BufferedWriter out;
		try {
			//open file for writing
			out = new BufferedWriter(new FileWriter(file_out));
			
			//building each row
			char[] row_str = new char[num_attributes * cardinality];
			for (int row = 0; row < this.num_rows; row++) {
				Arrays.fill(row_str, '0');	//a row string that contains sequence of only 0s
				for (int att_id = 0; att_id < num_attributes; att_id++) {
					//skip to the offset for a randomly chosen bin, set the bit to 1
					int set_bin = this.rank_bin_map[att_id][zipf_rng.next()];	//get a bin based on zipf distribution
					row_str[att_id * this.cardinality + set_bin] = '1';			//set the bit
				}
				//write row to file
				out.write(row_str);
				out.newLine();
				out.flush();
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	

}
