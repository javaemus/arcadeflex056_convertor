/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package vidhrdw;

public class avalnche
{
	
	
	/* The first entry defines the color with which the bitmap is filled initially */
	/* The array is terminated with an entry with negative coordinates. */
	/* At least two entries are needed. */
	static const struct artwork_element avalnche_ol[] =
	{
		{{  0, 255,  16,  25}, 0x20, 0xff, 0xff,   OVERLAY_DEFAULT_OPACITY},	/* cyan */
		{{  0, 255,  26,  35}, 0x20, 0x20, 0xff,   OVERLAY_DEFAULT_OPACITY},	/* blue */
		{{  0, 255,  36,  44}, 0xff, 0xff, 0x20,   OVERLAY_DEFAULT_OPACITY},	/* yellow */
		{{  0, 255,  45,  55}, 0xff, 0x80, 0x10,   OVERLAY_DEFAULT_OPACITY},	/* orange */
		{{  0, 255,  56, 255}, 0x20, 0xff, 0xff,   OVERLAY_DEFAULT_OPACITY},	/* cyan */
		{{-1,-1,-1,-1},0,0,0,0}
	};
	
	
	public static VhStartPtr avalnche_vh_start = new VhStartPtr() { public int handler() 
	{
		int start_pen = 2;	/* leave space for black and white */
	
		if (generic_vh_start()!=0)
			return 1;
	
		overlay_create(avalnche_ol, start_pen);
	
		return 0;
	} };
	
	public static WriteHandlerPtr avalnche_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		videoram[offset] = data;
	
		if (offset >= 0x200)
		{
			int x,y,i;
	
			x = 8 * (offset % 32);
			y = offset / 32;
	
			for (i = 0;i < 8;i++)
				plot_pixel(tmpbitmap,x+7-i,y,Machine->pens[(data >> i) & 1]);
		}
	} };
	
	public static VhUpdatePtr avalnche_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		if (full_refresh)
		{
			int offs;
	
	
			for (offs = 0;offs < videoram_size; offs++)
				avalnche_videoram_w(offs,videoram[offs]);
		}
	
		/* copy the character mapped graphics */
		copybitmap(bitmap,tmpbitmap,0,0,0,0,&Machine->visible_area,TRANSPARENCY_NONE,0);
	} };
}
