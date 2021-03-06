//============================================================
//
//	winddraw.h - Win32 DirectDraw code
//
//============================================================

#ifndef __WIN32_DDRAW__
#define __WIN32_DDRAW__

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package windows;

public class winddrawH
{
	
	
	//============================================================
	//	PROTOTYPES
	//============================================================
	
	int win_ddraw_init(int width, int height, int depth, int attributes, const struct win_effect_data *effect);
	int win_ddraw_draw(struct mame_bitmap *bitmap, int update);
	
	
	
	#endif
}
