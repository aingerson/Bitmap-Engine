import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Scanner;

/**
 * Objects of this class sorts a bitmap file given in ASCII format using
 * grey code ordering.
 * 
 * @author David
 * @version 1/9/17
 */
public class GreyCodeSorter
{
	private String file_in;

	/**
	 * Constructs a grey code sorter using the given bitmap file
	 * @param file_in
	 */
	public GreyCodeSorter(String file_in) {
		this.file_in = file_in;
	}
	
	/**
	 * Sorts the stored bitmap file, and write out to the file indicated by the given file name
	 * @param file_out
	 */
	public void writeFile(String file_out)
	{
		Scanner f_in;
		BufferedWriter f_out;
		try {
			//read each row into a a list of BitSets
			f_in = new Scanner(new File(file_in));
			ArrayList<BitSet> rows = new ArrayList<BitSet>();
			String bitstr = "";
			while (f_in.hasNext()) {
				bitstr =  f_in.nextLine();
				rows.add(strToBitSet(bitstr));
			}
			int row_len = bitstr.length();	//get the length of each row
			f_in.close();	//close file

			//convert into array of BitSets for processing
			BitSet[] rows_arr = new BitSet[rows.size()];
			rows_arr = (BitSet[]) rows.toArray(rows_arr);
			
			//reorder
			this.reorder(rows_arr, 0, 0, rows_arr.length-1, row_len);

			//open file for writing
			f_out = new BufferedWriter(new FileWriter(file_out));
			for (int i = 0; i < rows_arr.length; i++) {
				//write row to file
				f_out.write(bitSetToStr(rows_arr[i], row_len));
				f_out.newLine();
				f_out.flush();
			}
			
			f_out.close();	//close file
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	
	/**
	 * Helper method to sort using greycode ordering.
	 * 
	 * General idea is that we start with the MSBs and move toward the LSBs. If the preceding bit is a 0 or is null, then
	 * for each column position (col_idx), we partition the bitmap vertically such that the rows containing 0s at position 
	 * col_idx are moved to the top, and the rows containing 1s are moved to the bottom. If the preceding bit is 1, then
	 * do the opposite.
	 * 
	 * @param list			the bitmap
	 * @param col_idx		index of the current column
	 * @param row_start_idx	row id of the start of the current partition
	 * @param row_end_idx	row id of the end of the current partition
	 * @param bit_size		the size of each row
	 * @version 01/9/17
	 */
	private void reorder(BitSet[] rows, int col_idx, int row_start_idx, int row_end_idx, final int bit_size)
	{
		if (col_idx < bit_size) {
			int i = row_start_idx;
			int j = row_end_idx;

			//swap i_th row and the j_th row in place depending on bit value and the preceding bit value
			while (i < j) {
				boolean bit = (col_idx == 0 || !rows[i].get(col_idx-1)) ? false : true;

				//the current (i_th) row is out of place; look for a row with which to swap
				if (rows[i].get(col_idx) != bit) {
					if (rows[j].get(col_idx) == bit) {
						//swap i_th row and the j_th row
						BitSet tmp = rows[i];
						rows[i] = rows[j];
						rows[j] = tmp;
						i++;
						j--;
					}
					else {	//j_th row already in place; move up to next row
						j--;
					}
				}
				else {	//already in place; move down to the next row 
					i++;
				}
			}
			// reorder both "halves" for the next bit position in sequence
			if (row_start_idx < i-1) {
				reorder(rows, col_idx + 1, row_start_idx, i-1, bit_size);
			}
			if (j+1 < row_end_idx) {
				reorder(rows, col_idx + 1, j + 1, row_end_idx, bit_size);
			}
		}
	}
	
	/**
	 * Converts the given bit string to a BitSet
	 * @param bit_str
	 * @return
	 */
	private BitSet strToBitSet(String bit_str) {
		BitSet bs = new BitSet(bit_str.length());
		for (int i = 0; i < bit_str.length(); i++) {
			if (bit_str.charAt(i) == '1') {
				bs.set(i, true);
			}
		}
		return bs;
	}
	
	/**
	 * Converts the given bitset to a char array
	 * @param bs
	 * @param bit_len
	 * @return
	 */
	private char[] bitSetToStr(BitSet bs, final int bit_len) {
		char[] bitstring = new char[bit_len];
		Arrays.fill(bitstring, '0');	//a row string that contains sequence of only 0s
		for (int j = 0; j < bit_len; j++) {
			if (bs.get(j)) {
				bitstring[j] = '1';
			}
		}
		return bitstring;
	}
	
}
