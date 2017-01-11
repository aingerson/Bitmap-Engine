#ifndef WAHQUERY_H_
#define WAHQUERY_H_

#include "Control.h"

void initWAHQuery();

int AND_WAH(word_32 *, word_32 *,int, word_32 *,int);
word_32 fillANDfillWAH(word_32, int, word_32 *, int *);
word_32 litANDlitWAH(word_32,word_32);
word_32 fillANDlitWAH(word_32 *, int *, word_32);

int OR_WAH(word_32 *,word_32 *,int,word_32 * ,int);
word_32 fillORfillWAH(word_32, int, word_32 *, int *);
word_32 litORlitWAH(word_32,word_32);
word_32 fillORlitWAH(word_32 *, int *, word_32);

void appendWAH(word_32 *,word_32,int *);
#endif /* WAHQUERY_H_ */
