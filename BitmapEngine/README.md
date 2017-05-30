# Bitmap-Engine
Bitmap formatter (striped/unstriped), compressor (WAH/VAL), query (WAH/VAL)

This tool is intended to provide bitmap creation, formatting, compressing, and querying functionality for a variety of formatting and compression algorithms, compiling timed threaded results in csv form for manual comparison.

Some settings currently do not include a flag (yet) and must be manually changed in Control.h for alteration. These include:
IN_CORE / OUT_CORE (in core or out of core compression)
WAH / VAL (compression algorithm)
32 / 64 (bit)
SEG_LENGTH (VAL seg length)
PAR / SEQ (parallel or sequentially reading - for parallelized compression)
BLOCK_SIZE (cache block size to use - for formatting and compression)

It is recommended not to alter these too much as some functionality is not yet fully supported.

Currently, command line arguments are as follow:

Modes
F(Format)/C(Compression)/Q(Query)

./Compressor_Run	F	<bitmap_file_extension> 	--formats bitmap_file into striped and unstriped formats
./Compressor_Run	C	<bitmap_file_extension>		--compresses bitmap_file (both striped and unstriped formats must already exist to run)
./Compressor_Run	Q	<compressed_bitmap_file_extension>	<query_file>	--queries bitmap_file (in compressed format specified by extension) using list of queries in query_file



**Note: Command line querying currently disabled



 	

FORMATTING:

The formatter can take in as input a text bitmap (either formatted with or without a starting column number and comma). It will take in that bitmap file and rewrite it in a format prepped for compression in the algorithm designated in the Control header. First, it will reformat it in an unstriped format. Essentially, it will save each character as a bit in a word with a leading zero. This allows the compressor the read in each word (word size can vary depending on algorithm and word length) and assume each uncompressed word is already in literal format.
When formatting a bitmap file in WAH32, it will read in 31 characters at a time and save them as a 32-bit word with one leading zero. When formatting in WAH64, it will read in 63 characters and save them as a 64-bit word with one leading zero.
For VAL formatting, the formatter will save in 8-bit segments (for 32-bit compression) or 16-bit segments (for 64-bit compression), each with a leading zero. This is allows for easy compression between different segment lengths. For example, VAL32 with SEG_LENGTH=1 would read in 4 8-bit words from the formatted file. Each 8-bit word represents 7 bits of the bitmap (exclusing the leading zero) and so 4 of them would be equivalent to the 28 bits necessary for VAL32 with one segment.
The formatter will automatically format the bitmap unstriped and then striped using 1..MAX_NUM_THREADS and block size defined in the Control header. For each thread count N = 1..MAX_NUM_THREADS, the striped formatter will open N unstriped column files and write BLOCK_SIZE from each column in order until all N column files are done. The name of the striped file will be the column number of the last column striped in that file. Each striped folder should contain num_cols / N files. Regardless of block size, each striped file should contain exactly N columns worth of data.



COMPRESSION:

Before compressing, the bitmap needs to have been previously formatted into unstriped and striped files using the appropriate block size and compression definitions.
The Raw Bitmap Reader will read in blocks of data (BLOCK_SIZE) and save them into blockSegs which contain various pieces of information about the segment such as where the block is being written to, if it is the first block of a file, the previous word to concatenate it to, segment length, etc. This blockSeg will be sent to the compressor which will compress the blockSeg accordingly and write to the file designated in the struct as needed.

Some notes about Control variables that can alter how the bitmap is processed during compression...

IN/OUT core:
In and out of core refers to how much data is taken in from the bitmap at once before sent to compression. In core suggests that all of the data is stored and processed in the core at once. This is not to be used with large data sets as it will run out of memory doing so. Instead, this is left as a performance enhancement if desired on smaller data sets or if the memory can be guaranteed to be available for compression. To be safe, it is recommended to remain in OUT_CORE mode which ensure that only BLOCK_SIZE of data is compressed at a time.

PAR/SEQ:
PAR and SEQ refer to how the uncompressed bitmaps are read in across the threads. In each mode, a thread is assigned one column at a time. In SEQ mode, the striped files are read sequentially. In other words, when a thread finishes compressing, it reads in the next block from the file and assigns each thread its relative chunk of data to compress next. If another thread finishes compressing while the first thread is reading, it will wait until the reading is done reading before continuing. In contrast, PAR mode will allow threads to continue reading further in the file while another is also reading.



QUERY:

**CURRENTLY DISABLED, COMING SOON**
