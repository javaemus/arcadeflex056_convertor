/***************************************************************************

Tehkan World Cup - (c) Tehkan 1985


Ernesto Corvi
ernesto@imagina.com

Roberto Juan Fresca
robbiex@rocketmail.com

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package vidhrdw;

public class tehkanwc
{
	
	unsigned char *tehkanwc_videoram1;
	size_t tehkanwc_videoram1_size;
	static struct mame_bitmap *tmpbitmap1 = 0;
	static unsigned char *dirtybuffer1;
	static unsigned char scroll_x[2],scroll_y;
	static unsigned char led0,led1;
	
	
	int tehkanwc_vh_start(void)
	{
		if (generic_vh_start())
			return 1;
	
		if ((tmpbitmap1 = bitmap_alloc(2 * Machine->drv->screen_width, Machine->drv->screen_height)) == 0)
		{
			generic_vh_stop();
			return 1;
		}
	
		if ((dirtybuffer1 = malloc(tehkanwc_videoram1_size)) == 0)
		{
			bitmap_free(tmpbitmap1);
			generic_vh_stop();
			return 1;
		}
		memset(dirtybuffer1,1,tehkanwc_videoram1_size);
	
		return 0;
	}
	
	void tehkanwc_vh_stop(void)
	{
		free(dirtybuffer1);
		bitmap_free(tmpbitmap1);
		generic_vh_stop();
	}
	
	
	
	public static ReadHandlerPtr tehkanwc_videoram1_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return tehkanwc_videoram1[offset];
	} };
	
	WRITE_HANDLER( tehkanwc_videoram1_w )
	{
		tehkanwc_videoram1[offset] = data;
		dirtybuffer1[offset] = 1;
	}
	
	public static ReadHandlerPtr tehkanwc_scroll_x_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return scroll_x[offset];
	} };
	
	public static ReadHandlerPtr tehkanwc_scroll_y_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return scroll_y;
	} };
	
	WRITE_HANDLER( tehkanwc_scroll_x_w )
	{
		scroll_x[offset] = data;
	}
	
	WRITE_HANDLER( tehkanwc_scroll_y_w )
	{
		scroll_y = data;
	}
	
	
	
	WRITE_HANDLER( gridiron_led0_w )
	{
		led0 = data;
	}
	WRITE_HANDLER( gridiron_led1_w )
	{
		led1 = data;
	}
	
	/*
	   Gridiron Fight has a LED display on the control panel, to let each player
	   choose the formation without letting the other know.
	   We emulate it by showing a character on the corner of the screen; the
	   association between the bits of the port and the led segments is:
	
	    ---0---
	   |       |
	   5       1
	   |       |
	    ---6---
	   |       |
	   4       2
	   |       |
	    ---3---
	
	   bit 7 = enable (0 = display off)
	 */
	
	static void gridiron_drawled(struct mame_bitmap *bitmap,unsigned char led,int player)
	{
		int i;
	
	
		static unsigned char ledvalues[] =
				{ 0x86, 0xdb, 0xcf, 0xe6, 0xed, 0xfd, 0x87, 0xff, 0xf3, 0xf1 };
	
	
		if ((led & 0x80) == 0) return;
	
		for (i = 0;i < 10;i++)
		{
			if (led == ledvalues[i] ) break;
		}
	
		if (i < 10)
		{
			if (player == 0)
				drawgfx(bitmap,Machine->gfx[0],
						0xc0 + i,
						0x0a,
						0,0,
						0,232,
						&Machine->visible_area,TRANSPARENCY_NONE,0);
			else
				drawgfx(bitmap,Machine->gfx[0],
						0xc0 + i,
						0x03,
						1,1,
						0,16,
						&Machine->visible_area,TRANSPARENCY_NONE,0);
		}
	else logerror("unknown LED %02x for player %d\n",led,player);
	}
	
	
	
	void tehkanwc_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh)
	{
		int offs;
	
	
		/* draw the background */
		for (offs = tehkanwc_videoram1_size-2;offs >= 0;offs -= 2 )
		{
			if (dirtybuffer1[offs] || dirtybuffer1[offs + 1])
			{
				int sx,sy;
	
	
				dirtybuffer1[offs] = dirtybuffer1[offs + 1] = 0;
	
				sx = offs % 64;
				sy = offs / 64;
	
				drawgfx(tmpbitmap1,Machine->gfx[2],
						tehkanwc_videoram1[offs] + ((tehkanwc_videoram1[offs+1] & 0x30) << 4),
						tehkanwc_videoram1[offs+1] & 0x0f,
						tehkanwc_videoram1[offs+1] & 0x40, tehkanwc_videoram1[offs+1] & 0x80,
						sx*8,sy*8,
						0,TRANSPARENCY_NONE,0);
			}
		}
		{
			int scrolly = -scroll_y;
			int scrollx = -(scroll_x[0] + 256 * scroll_x[1]);
			copyscrollbitmap(bitmap,tmpbitmap1,1,&scrollx,1,&scrolly,&Machine->visible_area,TRANSPARENCY_NONE,0);
		}
	
	
		/* draw the foreground chars which don't have priority over sprites */
		for (offs = videoram_size - 1;offs >= 0;offs--)
		{
			int sx,sy;
	
	
			dirtybuffer[offs] = 0;
	
			sx = offs % 32;
			sy = offs / 32;
	
			if ((colorram[offs] & 0x20))
				drawgfx(bitmap,Machine->gfx[0],
						videoram[offs] + ((colorram[offs] & 0x10) << 4),
						colorram[offs] & 0x0f,
						colorram[offs] & 0x40, colorram[offs] & 0x80,
						sx*8,sy*8,
						&Machine->visible_area,TRANSPARENCY_PEN,0);
		}
	
	
		/* draw sprites */
		for (offs = 0;offs < spriteram_size;offs += 4)
		{
			drawgfx(bitmap,Machine->gfx[1],
					spriteram[offs+0] + ((spriteram[offs+1] & 0x08) << 5),
					spriteram[offs+1] & 0x07,
					spriteram[offs+1] & 0x40,spriteram[offs+1] & 0x80,
					spriteram[offs+2] + ((spriteram[offs+1] & 0x20) << 3) - 0x80,spriteram[offs+3],
					&Machine->visible_area,TRANSPARENCY_PEN,0);
		}
	
	
		/* draw the foreground chars which have priority over sprites */
		for (offs = videoram_size - 1;offs >= 0;offs--)
		{
			int sx,sy;
	
	
			dirtybuffer[offs] = 0;
	
			sx = offs % 32;
			sy = offs / 32;
	
			if (!(colorram[offs] & 0x20))
				drawgfx(bitmap,Machine->gfx[0],
						videoram[offs] + ((colorram[offs] & 0x10) << 4),
						colorram[offs] & 0x0f,
						colorram[offs] & 0x40, colorram[offs] & 0x80,
						sx*8,sy*8,
						&Machine->visible_area,TRANSPARENCY_PEN,0);
		}
	
		gridiron_drawled(bitmap,led0,0);
		gridiron_drawled(bitmap,led1,1);
	}
}
