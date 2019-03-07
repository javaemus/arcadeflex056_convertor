/***************************************************************************

	Taito Qix hardware

	driver by John Butler, Ed Mueller, Aaron Giles

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package includes;

public class qixH
{
	
	
	/*----------- defined in machine/qix.c -----------*/
	
	extern UINT8 *qix_sharedram;
	extern UINT8 *qix_68705_port_out;
	extern UINT8 *qix_68705_ddr;
	
	
	
	
	
	
	
	
	
	/*----------- defined in vidhrdw/qix.c -----------*/
	
	extern UINT8 *qix_palettebank;
	extern UINT8 *qix_videoaddress;
	extern UINT8 qix_cocktail_flip;
	
	void qix_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh);
	
	void qix_scanline_callback(int scanline);
	
	
	}
