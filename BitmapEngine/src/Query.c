#include <stdio.h>
#include <stdlib.h>
#include "Query.h"
#include "Control.h"
#include "SegUtil.h"
#include "Vars.h"
#include <sys/stat.h>
#include <unistd.h>
#include <string.h>
#include "WAHQuery.h"
#include "VALQuery.h"
#include <pthread.h>
#include "RawBitmapReader.h"

int *range1;//list of all columns in first range
int *range2;//list of all columns in second range
pthread_t *threads;//threads
pthread_mutex_t mut;//mutex lock
int qid;//which query id we're currently executing
int started1;//marks which threads haven't started yet (first range)
int started2;//marks which threads haevn't started yet (second range)
int rangeIndex;//where in the range are we
int threadNum[MAX_NUM_THREADS];//thread ids

word_32 **cols;//loaded columns
int *sz;//size of each loaded column (init at -1 --> not loaded yet)
char *bitmapFile;//path for all compressed columns
char query_path[BUFF_SIZE];//path for results of queries

queryData **results1;//range1 results (OR)
queryData **results2;//range2 results (OR)
int num_threads;
char *query_file;

/**
 * Runs the designated file of queries on the bitmap file folder
 */
void runQueries(char *folder, char *query, int n){
	query_file=query;
	FILE *fp = fopen(query_file, "r");//open the query file
	num_threads=n;
	if(fp == NULL){
		fprintf(stderr,"Can't open query file\n");
	}
	else{
		bitmapFile=folder;
		init();

		qid = 0;//which query we're on
		char c = '\n';//new line char
		while(c!=EOF){//keep scanning each query
			fscanf(fp, "%d", &qid);//query id
			getc(fp);//comma
			range1 = parseRange(fp);//first range
			getc(fp);//comma
			range2 = parseRange(fp);//second range
			executeQuery();
			free(range1);
			free(range2);
			c=getc(fp);//get new line
		}
	}
}


/**
 *Executes the next query given the ranges already stored in globals
 */
void executeQuery(){
	started1 = num_threads;//marks first of threads that have not been started yet
	started2 = num_threads;

	//save global variables (both ranges being queried and path extension for compressed column files)
	unsigned int maxWords = 0;
	int i;
	//go through the first range and make sure every column that we're going to need is loaded
	for(i=1;i<=range1[0];i++){
		if(sz[range1[i]]==-1){//column hasn't been loaded
			loadCol(range1[i]);//load each file into memory first
		}
		if(sz[range1[i]]>maxWords) maxWords = sz[range1[i]];
	}
	//go through the second range and make sure every column that we're going to need is loaded
	for(i=1;i<=range2[0];i++){
		if(sz[range2[i]]==-1){//column hasn't been loaded yet
			loadCol(range2[i]);//load each file into memory first
		}
		if(sz[range2[i]]>maxWords) maxWords = sz[range2[i]];
	}
	rangeIndex = 1;//column tracker (to go through range), range[0] = numCols in range
	results1 = (queryData **) malloc(sizeof(queryData *) * num_threads);

	int h;
	for(h=0;h<num_threads;h++){
		pthread_mutex_lock(&mut);
		int ind = rangeIndex++;
		pthread_mutex_unlock(&mut);

		if(ind>range1[0]){//no more columns to OR
			started1=h;//mark this thread as not having started
			break;
		}
		results1[h] = (queryData *) malloc(sizeof(queryData));
		results1[h]->result = (word_32 *) malloc(sizeof(word_32) * maxWords);
		results1[h]->resultCopy = (word_32 *) malloc(sizeof(word_32) * maxWords);

		results1[h]->resultSize = sz[range1[ind]];
		int g;
		for(g=0;g<results1[h]->resultSize;g++){
			results1[h]->result[g]=cols[range1[ind]][g];
		}

		if(pthread_create(&threads[h],NULL,startRangeOneThread,(void *)(&threadNum[h]))){
			printf("Error creating thread\n");
			return;
		}
	}

	for(h=0;h<num_threads;h++){//wait for all the threads to finish
		if(h==started1) break;
		if(pthread_join(threads[h],NULL)){
			printf("Error joining thread\n");
			return;
		}
	}

	for(h=1;h<num_threads;h++){//finish ORing all thread results together
		if(h==started1) break;
		copyResult(results1[0]);
		if(COMPRESSION==WAH){
			results1[0]->resultSize = OR_WAH(results1[0]->result,results1[0]->resultCopy,results1[0]->resultSizeCopy,results1[h]->result,results1[h]->resultSize);
		}
		else if(COMPRESSION==VAL){
			results1[0]->resultSize = OR_VAL(results1[0]->result,results1[0]->resultCopy,results1[0]->resultSizeCopy,results1[h]->result,results1[h]->resultSize);
		}
	}

	//do it all again for range2
	rangeIndex = 1;//column tracker (to go through range)
	results2 = (queryData **) malloc(sizeof(queryData *) * num_threads);

	for(h=0;h<num_threads;h++){

		pthread_mutex_lock(&mut);
		int ind = rangeIndex++;
		pthread_mutex_unlock(&mut);

		if(ind>range2[0]){//no more columns to OR
			started2=h;//mark this thread as not having started
			break;
		}

		results2[h] = (queryData *) malloc(sizeof(queryData));
		results2[h]->result = (word_32 *) malloc(sizeof(word_32) * maxWords);
		results2[h]->resultCopy = (word_32 *) malloc(sizeof(word_32) *maxWords);

		results2[h]->resultSize = sz[range2[ind]];
		int g;
		for(g=0;g<results2[h]->resultSize;g++){
			results2[h]->result[g]=cols[range2[ind]][g];
		}

		if(pthread_create(&threads[h],NULL,startRangeTwoThread,(void *)(&threadNum[h]))){
			printf("Error creating thread\n");
			return;
		}
	}

	for(h=0;h<num_threads;h++){//wait for all the threads to finish
		if(h==started2) break;//we never started this thread
		if(pthread_join(threads[h],NULL)){
			printf("Error joining thread\n");
			return;
		}
	}

	for(h=1;h<num_threads;h++){//finish ORing all thread results together
		if(h==started2) break;
		copyResult(results2[0]);
		if(COMPRESSION==WAH){
			results2[0]->resultSize = OR_WAH(results2[0]->result,results2[0]->resultCopy,results2[0]->resultSizeCopy,results2[h]->result,results2[h]->resultSize);
		}
		else if(COMPRESSION==VAL){
			results2[0]->resultSize = OR_VAL(results2[0]->result,results2[0]->resultCopy,results2[0]->resultSizeCopy,results2[h]->result,results2[h]->resultSize);
		}
	}

	copyResult(results1[0]);
	if(COMPRESSION==WAH){
		results1[0]->resultSize = AND_WAH(results1[0]->result,results1[0]->resultCopy,results1[0]->resultSizeCopy,results2[0]->result,results2[0]->resultSize);
	}
	else if(COMPRESSION==VAL){
		results1[0]->resultSize = AND_VAL(results1[0]->result,results1[0]->resultCopy,results1[0]->resultSizeCopy,results2[0]->result,results2[0]->resultSize);
	}

	if(WRITE_TO_FILE){
		char buff[BUFF_SIZE];
		snprintf(buff,sizeof(buff),"%s%d%s",query_path,qid,".dat");//build the output file name based on query id
		FILE *dest = fopen(buff,"wb");//this is the destination file for the query result
		fwrite(results1[0]->result,sizeof(word_32),results1[0]->resultSize,dest);
	}
}


/**
 * Initializes environment (structs,paths,etc)
 */
void init(){
	int n=0;
	while(1){
		char name[BUFF_SIZE];
		snprintf(name,sizeof(name),"%s%d%s",bitmapFile,n,".dat");//build the file name based on colNum
		//counting the number of columns there are in that folder
		if(access(name,F_OK) != -1) n++;
		else break;
	}
	//building the folder for query results
	//the results will be saved in a folder where the query file was
	//Ex. if we ran "Queries/query1.txt"
	//the results will be "Queries/QueryResults_query1.txt/qID_0.dat" etc
	//char folder[] = "QueryResults_";

	char results_folder[BUFF_SIZE];
	snprintf(results_folder,BUFF_SIZE,"%s_RESULTS/",query_file);

	//char *dir = getDir(query_file,folder);

	mkdir(results_folder,S_IRWXU);
	int i=0;
	for(;i<BUFF_SIZE;query_path[i++]='\0');
	strcpy(query_path,results_folder);
	strcat(query_path,"/qID_");

	cols = (word_32 **) malloc(sizeof(word_32 *) * n);//the actual column
	sz = (int *) malloc(sizeof(int)*n);//how many words are in each column (empty --> -1)
	for(;n>=0;sz[--n]=-1);//no columns have been loaded
	threads = (pthread_t *) malloc(sizeof(pthread_t) * num_threads);//allocate each thread pointer
	if (pthread_mutex_init(&mut, NULL) != 0) printf("\n mutex init failed\n");
	for(n=0;n<num_threads;n++) threadNum[n]=n;


	initUtilSegs(WAH);
}

/**
 * Parses a range from a given file and returns array of the number of ints followed by the specified ints
 * Example: if query file says "9,11"
 * Returns [3,9,10,11] --> [numCols, col1, col2, col3]
 */
int *parseRange(FILE *qFile){
	int i =0;
	fscanf(qFile, "%d", &i);//first column of the range
	getc(qFile);//comma
	int j = 0;
	fscanf(qFile, "%d", &j);//second column of the range
	int sz = j-i+2;//size of the range array (number of columns + 1 for size)
	int *ret = (int *) malloc(sizeof(int) * (sz));
	int k;
	ret[0] = sz-1;//save number of columns
	for(k=1;k<sz;ret[k++] = i++);//fill array with the right range

	return ret;
}

/**
 * Copy contents of toCopy->result into toCopy->resultCopy
 */
void copyResult(queryData *toCopy){
	int u;
	for(u=0;u<toCopy->resultSize;u++){
		toCopy->resultCopy[u]=toCopy->result[u];
	}
	toCopy->resultSizeCopy=toCopy->resultSize;
}

/**
 * Loads column d into appropriate location in cols from global path
 */
void loadCol(int d){
	char file[BUFF_SIZE];
	snprintf(file,sizeof(file),"%s%d%s",bitmapFile,d,".dat");//build the file name based on colNum
	FILE *fp = fopen(file,"rb");
	if(fp==NULL){//out of range query probably
		printf("Can't open file");
		return;
	}
	//just trying to find the size of the file (to know how many words are in it)
	struct stat st;
	stat(file, &st);
	sz[d] = (st.st_size/sizeof(word_32));//save number of words in column into sz array

	cols[d] = (word_32 *) malloc(sizeof(word_32) *sz[d]);
	fread(&(cols[d][0]),sizeof(word_32),sz[d],fp);//read in the column
	fclose(fp);
}

/**
 * ORs all columns (assuming all are loaded) in range 1
 */
void *startRangeOneThread(void *args){

	int *h = (int *) (args);
	int mark = 0;//keeps track of column in range currently being processed
	if(range1[0]==1) return NULL;//point query

	while(mark<=range1[0]){//as long as there are more columns to process
		pthread_mutex_lock(&mut);
		mark = rangeIndex++;
		pthread_mutex_unlock(&mut);
		if(mark>range1[0]) return NULL;//no more columns to process
		copyResult(results1[*h]);
		if(COMPRESSION==WAH){
			results1[*h]->resultSize = OR_WAH(results1[*h]->result, results1[*h]->resultCopy, results1[*h]->resultSizeCopy, cols[range1[mark]],sz[range1[mark]]);
		}
		else if(COMPRESSION==VAL){
			results1[*h]->resultSize = OR_VAL(results1[*h]->result, results1[*h]->resultCopy, results1[*h]->resultSizeCopy, cols[range1[mark]],sz[range1[mark]]);
		}

	}
	return NULL;
}

/**
 * ORs all columns (assuming all are loaded) in range2
 */
void *startRangeTwoThread(void *args){
	int *h = (int *) (args);

	int mark = 0;//keeps track of column in range currently being processed
	if(range2[0]==1) return NULL;//point query

	while(mark<=range2[0]){//as long as there are more columns to process
		pthread_mutex_lock(&mut);
		mark = rangeIndex++;
		pthread_mutex_unlock(&mut);
		if(mark>range2[0]) return NULL;//no more columns to process
		copyResult(results2[*h]);
		if(COMPRESSION==WAH){
			results2[*h]->resultSize = OR_WAH(results2[*h]->result, results2[*h]->resultCopy, results2[*h]->resultSizeCopy, cols[range2[mark]],sz[range2[mark]]);
		}
		else if(COMPRESSION==VAL){
			results2[*h]->resultSize = OR_VAL(results2[*h]->result, results1[*h]->resultCopy, results2[*h]->resultSizeCopy, cols[range2[mark]],sz[range2[mark]]);
		}
	}
	return NULL;
}
