/*
 * QueryData.h
 *
 *  Created on: Jun 14, 2016
 *      Author: alexia
 */

#ifndef QUERYDATA_H_
#define QUERYDATA_H_

//struct to help in querying data
typedef struct queryData{
	word_32 *result;
	int resultSize;
	word_32 *resultCopy;
	int resultSizeCopy;
} queryData;



#endif /* QUERYDATA_H_ */
