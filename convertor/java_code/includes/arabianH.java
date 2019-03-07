/***************************************************************************

	Sun Electronics Arabian hardware

	driver by Dan Boris

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package includes;

public class arabianH
{
	
	
	/*----------- defined in vidhrdw/arabian.c -----------*/
	
	extern UINT8 arabian_video_control;
	extern UINT8 arabian_flip_screen;
	
	void arabian_vh_stop(void);
	void arabian_vh_screenrefresh(struct mame_bitmap *bitmap, int full_refresh);
	void arabian_vh_convert_color_prom(unsigned char *obsolete, unsigned short *colortable, const unsigned char *color_prom);
	
	}
