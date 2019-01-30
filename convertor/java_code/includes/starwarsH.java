/***************************************************************************

	Atari Star Wars hardware

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package includes;

public class starwarsH
{
	
	
	/*----------- defined in machine/starwars.c -----------*/
	
	WRITE_HANDLER( starwars_out_w );
	
	WRITE_HANDLER( starwars_adc_select_w );
	
	void swmathbox_init(void);
	void swmathbox_reset(void);
	
	
	WRITE_HANDLER( swmathbx_w );
	
	
	/*----------- defined in sndhrdw/starwars.c -----------*/
	
	WRITE_HANDLER( starwars_main_wr_w );
	WRITE_HANDLER( starwars_soundrst_w );
	
	
	WRITE_HANDLER( starwars_sout_w );
	WRITE_HANDLER( starwars_m6532_w );
}
