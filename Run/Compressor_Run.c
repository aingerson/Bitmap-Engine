/*
 ============================================================================
 Name        : Compressor_Run.c
 Author      : 
 Version     :
 Copyright   : Your copyright notice
 Description : Hello World in C, Ansi-style
 ============================================================================
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include "Vars.h"
#include <string.h>

/**
 * Runs formatter/compressor/query engines as set in Control.h
 */
int main(int argc, char*argv[]) {
	setbuf(stdout,NULL);

	if(argc>2 && (strcmp(argv[1],"F")==0 || strcmp(argv[1],"C")==0 || strcmp(argv[1],"Q")==0)){
		if(strcmp(argv[1],"F")==0){//FORMATTING
				char **newArgs = (char **) malloc(sizeof(char *) * 4);
				int h;
				for(h=0;h<3;h++){
					newArgs[h]=(char *) malloc(sizeof(char)*BUFF_SIZE);
				}
				strcpy(newArgs[0],PROGRAM);
				strcpy(newArgs[1],"F");
				strcpy(newArgs[2],argv[2]);
				newArgs[3]=NULL;

				if(!fork()){
					execv(PROGRAM,newArgs);//child executes
					perror("Failed to execute...");
					return 1;
				}
				else{
					wait(NULL);//parent waits for child
				}
		}
		else if(strcmp(argv[1],"C")==0){//COMPRESSION
			int h;

				printf("File: %s\n",argv[2]);

				char results_name[BUFF_SIZE];
				snprintf(results_name,BUFF_SIZE,"%s_RESULTS.csv",argv[2]);

				FILE *results_file = fopen(results_name,"w");
				if(results_file==NULL){
					printf("Failed to open results file %s\n",results_name);
					return 0;
				}
				fputs("STRIPED,NUM_THREADS,",results_file);
				int t;
				for(t=1;t<=NUM_TRIALS;t++){
					fprintf(results_file, "TRIAL_%d,", t);
				}

				fclose(results_file);

				int n,trial;


				printf("\tUNSTRIPED...\n");
				for(n=1;n<=MAX_NUM_THREADS;n++){

					results_file=fopen(results_name,"a");

					fprintf(results_file,"\nUNSTR,%d,",n);
					fclose(results_file);

					printf("\t\tTHREADS(%d/%d)\n",n,MAX_NUM_THREADS);
					for(trial=1;trial<=NUM_TRIALS;trial++){
						char **newArgs = (char **) malloc(sizeof(char *) * 6);
						for(h=0;h<5;h++){
							newArgs[h]=malloc(sizeof(char)*BUFF_SIZE);
						}
						strcpy(newArgs[0],PROGRAM);
						strcpy(newArgs[1],"C");
						strcpy(newArgs[2],argv[2]);
						snprintf(newArgs[3],BUFF_SIZE,"%d",n);//num threads
						strcpy(newArgs[4],"UNSTRIPED");
						newArgs[5]=NULL;
						printf("\t\t\tTRIAL(%d/%d) BEGIN...",trial,NUM_TRIALS);

						if(!fork()){
							execv(PROGRAM,newArgs);//child executes
							perror("ERROR...");
							return 1;
						}
						else{
							wait(NULL);//parent waits for child
						}

						printf("COMPLETE\n");
					}
				}

				printf("\tSTRIPED...\n");
				for(n=1;n<=MAX_NUM_THREADS;n++){
					results_file=fopen(results_name,"a");
					fprintf(results_file,"\nSTR,%d,",n);
					fclose(results_file);
					printf("\t\tTHREADS(%d/%d)\n",n,MAX_NUM_THREADS);

					for(trial=1;trial<=NUM_TRIALS;trial++){
						char **newArgs = (char **) malloc(sizeof(char *) * 6);
						for(h=0;h<5;h++){
							newArgs[h]=malloc(sizeof(char)*BUFF_SIZE);
						}
						strcpy(newArgs[0],PROGRAM);
						strcpy(newArgs[1],"C");
						strcpy(newArgs[2],argv[2]);
						snprintf(newArgs[3],BUFF_SIZE,"%d",n);//num threads
						strcpy(newArgs[4],"STRIPED");
						newArgs[5]=NULL;

						printf("\t\t\tTRIAL(%d/%d) BEGIN...",trial,NUM_TRIALS);
						if(!fork()){
							execv(PROGRAM,newArgs);
							perror("ERROR...");
							return 1;
						}
						else{
							wait(NULL);//parent waits for child
						}
						printf("COMPLETE\n");
					}
				}
				fclose(results_file);
		}
		else if(strcmp(argv[1],"Q")){
			printf("Query\n");
		}

	}
	else{
		printf("Intended use:\n\tArg1: F(Format)/C(Compress)\n\tArg2+: bitmapFiles\n");
	}

	return 0;
}
