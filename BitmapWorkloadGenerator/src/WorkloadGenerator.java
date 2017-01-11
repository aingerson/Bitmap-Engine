import java.util.Random;

/**
 * An abstract class that provides basic shared structure and methods
 * for experiment generators.
 * 
 * @author David
 * @version 1/5/17
 */
public abstract class WorkloadGenerator
{
	protected int[][] rank_bin_map;	// for each attribute, we have a rank_id to bin mapping
	protected int num_attributes; 	// number of attributes
	protected int cardinality; 		// cardinality (bins per attribute)
	
	/**
	 * Constructs a new generator
	 * @param num_attributes
	 * @param cardinality
	 */
	public WorkloadGenerator(int num_attributes, int cardinality)
	{
		this.num_attributes = num_attributes;
		this.cardinality = cardinality;

		//initialize each attribute's rank_id to a bin map
		this.rank_bin_map = new int[num_attributes][cardinality];
		for (int i = 0; i < num_attributes; i++) {
			for (int j = 0; j < cardinality; j++) {
				this.rank_bin_map[i][j] = j;
			}
			shuffle(this.rank_bin_map[i]);
		}
	}
	
	/**
	 * Subclasses must implement this method. Writes the 
	 * results to a file with the given name
	 * @param file_out
	 */
	abstract public void writeFile(String file_out);

	
	/**
	 * Helper method shuffles the elements in random order in the given array
	 * @param list A reference to an array to be randomly shuffled
	 */
	private void shuffle(int[] list)
	{
		Random rng = new Random(System.currentTimeMillis());
		for (int i = 0; i < list.length; i++) {
			int swap_idx = rng.nextInt(list.length - i) + i;
			int swap_element = list[swap_idx];
			list[swap_idx] = list[i];
			list[i] = swap_element;
		}
	}	
	
	
	/**
	 * Objects of this represent a number generator based on the Zipf distribution.
	 * A rank_id is returned with a probability of (1/rank_id^{skew}) / denom, where
	 * denom = \sum_{i=1}{N}{1/N^{skew}}
	 * 
	 * @author David
	 */

	protected class Zipf
	{
		private int size;			/** size of the distribution (i.e., N) */
		private double skew;		/** skew of the zipf distribution */
		private double denom;		/** denominator of the zipf distribution */
		private Random rnd;

		public Zipf(int size, double skew) {
			this.rnd = new Random(System.currentTimeMillis());
			this.size = size;
			this.skew = skew;
			this.denom = 0;
			for(int i = 1; i <= size; i++) {
				this.denom += (1 / Math.pow(i, this.skew));
			}
		}
		
		/**
		 * The frequency of returned rank ids are follows Zipf distribution 
		 * with the current skew.
		 * 
		 * @return a rank id (where rank > 0)
		 */
		public int next() {
			int rank_id;
			double frequency;
			do {
				rank_id = rnd.nextInt(size);	//choose a rank from the range [0,size-1]
				frequency = (1.0d / Math.pow(rank_id + 1, this.skew)) / this.denom;
			} while(rnd.nextDouble() >= frequency);
			return rank_id;
		}

		/**
		 * This method returns a probability that the given rank id occurs.
		 * 
		 * @param rank A zero-based rank
		 * @return the probability of occurrence of the given rank_id
		 */
		public double getProbability(int rank_id) {
			return (1.0d / Math.pow(rank_id + 1, this.skew)) / this.denom;
		}
	}	
}
