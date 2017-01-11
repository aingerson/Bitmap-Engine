# Bitmap-Engine
Bitmap formatter (striped/unstriped), compressor (WAH/VAL), query (WAH/VAL)

This tool is intended to provide bitmap creation, formatting, compressing, and querying functionality for a variety of formatting and compression algorithms, compiling timed threaded results in csv form for manual comparison.

Some settings currently do not include a flag (yet) and must be manually changed in Control.h for alteration. These include:
IN_CORE / OUT_CORE (in core or out of core compression)
WAH / VAL (compression algorithm)
32 / 64 (bit)
SEG_LENGTH (VAL seg length)
PAR / SEQ (parallel or sequentially reading (for parallelized compression)

It is recommended not to alter these too much as some functionality is not yet fully supported.

Currently, command line arguments are as follow:

Modes
F(ormat)/C(ompression)/Q(Query)

./Run	F	<bitmap_file_extension> 	--formats bitmap_file into striped and unstriped formats
./Run	C	<bitmap_file_extension>		--compresses bitmap_file (both striped and unstriped formats mut already exist to run)
./Run	Q	<compressed_bitmap_file_extension>	<query_file>	--queries bitmap_file (in compressed format specified by extension) using list of queries in query_file



**Note: Command line querying currently disabled



 	


