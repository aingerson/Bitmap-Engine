/*
 * Query.h
 *
 *  Created on: Jun 14, 2016
 *      Author: alexia
 */
#include "Control.h"
#include "QueryData.h"
#ifndef QUERY_H_
#define QUERY_H_

void runQueries(char *, char *, int);
int *parseRange(FILE *);
void loadCol(int);
void init();
void executeQuery();
void copyResult(queryData *);
void *startRangeOneThread(void *);
void *startRangeTwoThread(void *);

#endif /* QUERY_H_ */
