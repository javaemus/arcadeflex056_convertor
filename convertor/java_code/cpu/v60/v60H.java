#ifndef __V60_H
#define __V60_H

enum {
	V60_R0 = 1,
	V60_R1,
	V60_R2,
	V60_R3,
	V60_R4,
	V60_R5,
	V60_R6,
	V60_R7,
	V60_R8,
	V60_R9,
	V60_R10,
	V60_R11,
	V60_R12,
	V60_R13,
	V60_R14,
	V60_R15,
	V60_R16,
	V60_R17,
	V60_R18,
	V60_R19,
	V60_R20,
	V60_R21,
	V60_R22,
	V60_R23,
	V60_R24,
	V60_R25,
	V60_R26,
	V60_R27,
	V60_R28,
	V60_AP,
	V60_FP,
	V60_SP,
	V60_PC,
	V60_PSW,
	V60_TR,
	V60_PIR,
	V60_ISP,
	V60_L0SP,
	V60_L1SP,
	V60_L2SP,
	V60_L3SP,
	V60_SBR,
	V60_SYCW,
	V60_TKCW,
	V60_PSW2,
	V60_ATBR0,
	V60_ATLR0,
	V60_ATBR1,
	V60_ATLR1,
	V60_ATBR2,
	V60_ATLR2,
	V60_ATBR3,
	V60_ATLR3,
	V60_TRMODE,
	V60_ADTR0,
	V60_ADTR1,
	V60_ADTMR0,
	V60_ADTMR1,
	V60_REGMAX
};


extern const char *v60_reg_names[];

void v60_reset(void *param);
int v60_execute(int cycles);
unsigned v60_get_context(void *dst);
void v60_set_context(void *src);
unsigned v60_get_reg(int regnum);
void v60_set_reg(int regnum, unsigned val);
void v60_set_irq_line(int irqline, int state);
void v60_set_irq_callback(int (*callback)(int irqline));
const char *v60_info(void *context, int regnum);
unsigned v60_dasm(char *buffer, unsigned pc);

#endif
