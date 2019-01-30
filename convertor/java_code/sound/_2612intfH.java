#ifndef __2612INTF_H__
#define __2612INTF_H__

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package sound;

public class _2612intfH
{
	#ifdef BUILD_YM2612
	  void YM2612UpdateRequest(int chip);
	#endif
	
	#define   MAX_2612    (2)
	
	#define YM2612interface AY8910interface
	
	int  YM2612_sh_start(const struct MachineSound *msound);
	void YM2612_sh_stop(void);
	void YM2612_sh_reset(void);
	
	/************************************************/
	/* Chip 0 functions								*/
	/************************************************/
	WRITE_HANDLER( YM2612_control_port_0_A_w ); /* A=0:OPN  address */
	WRITE_HANDLER( YM2612_control_port_0_B_w ); /* A=2:OPN2 address */
	WRITE_HANDLER( YM2612_data_port_0_A_w );    /* A=1:OPN  data    */
	WRITE_HANDLER( YM2612_data_port_0_B_w );    /* A=3:OPN2 data    */
	
	/************************************************/
	/* Chip 1 functions								*/
	/************************************************/
	WRITE_HANDLER( YM2612_control_port_1_A_w );
	WRITE_HANDLER( YM2612_control_port_1_B_w );
	WRITE_HANDLER( YM2612_data_port_1_A_w );
	WRITE_HANDLER( YM2612_data_port_1_B_w );
	
	/**************************************************/
	/*   YM2612 left/right position change (TAITO)    */
	/**************************************************/
	
	#endif
	/**************** end of file ****************/
}
