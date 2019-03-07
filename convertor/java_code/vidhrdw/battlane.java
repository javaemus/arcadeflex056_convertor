/***************************************************************************

	Battlelane

    TODO: Properly support flip screen
          Tidy / Optimize and add dirty layer support

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package vidhrdw;

public class battlane
{
	
	static struct mame_bitmap *screen_bitmap;
	
	size_t battlane_bitmap_size;
	unsigned char *battlane_bitmap;
	static int battlane_video_ctrl;
	
	const int battlane_spriteram_size=0x100;
	static unsigned char battlane_spriteram[0x100];
	
	const int battlane_tileram_size=0x800;
	static unsigned char battlane_tileram[0x800];
	
	
	static int flipscreen;
	static int battlane_scrolly;
	static int battlane_scrollx;
	
	extern int battlane_cpu_control;
	
	static struct mame_bitmap *bkgnd_bitmap;  /* scroll bitmap */
	
	
	public static WriteHandlerPtr battlane_video_ctrl_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/*
	    Video control register
	
	        0x80    = low bit of blue component (taken when writing to palette)
	        0x0e    = Bitmap plane (bank?) select  (0-7)
	        0x01    = Scroll MSB
		*/
	
		battlane_video_ctrl=data;
	} };
	
	public static ReadHandlerPtr battlane_video_ctrl_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return battlane_video_ctrl;
	} };
	
	public static WriteHandlerPtr battlane_palette_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int r,g,b;
		int bit0,bit1,bit2;
	
	
		/* red component */
		bit0 = (~data >> 0) & 0x01;
		bit1 = (~data >> 1) & 0x01;
		bit2 = (~data >> 2) & 0x01;
		r = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
		/* green component */
		bit0 = (~data >> 3) & 0x01;
		bit1 = (~data >> 4) & 0x01;
		bit2 = (~data >> 5) & 0x01;
		g = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
		/* blue component */
		bit0 = (~battlane_video_ctrl >> 7) & 0x01;
		bit1 = (~data >> 6) & 0x01;
		bit2 = (~data >> 7) & 0x01;
		b = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
	
		palette_set_color(offset,r,g,b);
	} };
	
	
	void battlane_set_video_flip(int flip)
	{
	
	    if (flip != flipscreen)
	    {
	        // Invalidate any cached data
	    }
	
	    flipscreen=flip;
	
	    /*
	    Don't flip the screen. The render function doesn't support
	    it properly yet.
	    */
	    flipscreen=0;
	
	}
	
	public static WriteHandlerPtr battlane_scrollx_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	    battlane_scrollx=data;
	} };
	
	public static WriteHandlerPtr battlane_scrolly_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	    battlane_scrolly=data;
	} };
	
	public static WriteHandlerPtr battlane_tileram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	    battlane_tileram[offset]=data;
	} };
	
	public static ReadHandlerPtr battlane_tileram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	    return battlane_tileram[offset];
	} };
	
	public static WriteHandlerPtr battlane_spriteram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	    battlane_spriteram[offset]=data;
	} };
	
	public static ReadHandlerPtr battlane_spriteram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	    return battlane_spriteram[offset];
	} };
	
	
	public static WriteHandlerPtr battlane_bitmap_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int i, orval;
	
	    orval=(~battlane_video_ctrl>>1)&0x07;
	
		if (orval==0)
			orval=7;
	
		for (i=0; i<8; i++)
		{
			if (data & 1<<i)
			{
				((UINT8 *)screen_bitmap->line[offset % 0x100])[(offset / 0x100) * 8+i] |= orval;
			}
			else
			{
				((UINT8 *)screen_bitmap->line[offset % 0x100])[(offset / 0x100) * 8+i] &= ~orval;
			}
		}
		battlane_bitmap[offset]=data;
	} };
	
	public static ReadHandlerPtr battlane_bitmap_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return battlane_bitmap[offset];
	} };
	
	
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	public static VhStartPtr battlane_vh_start = new VhStartPtr() { public int handler() 
	{
		screen_bitmap = bitmap_alloc(0x20*8, 0x20*8);
		if (screen_bitmap == 0)
		{
			return 1;
		}
	
		battlane_bitmap=malloc(battlane_bitmap_size);
		if (battlane_bitmap == 0)
		{
			return 1;
		}
	
		memset(battlane_spriteram, 0, battlane_spriteram_size);
	    memset(battlane_tileram,255, battlane_tileram_size);
	
	    bkgnd_bitmap = bitmap_alloc(0x0200, 0x0200);
	    if (bkgnd_bitmap == 0)
		{
			return 1;
		}
	
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
	  Stop the video hardware emulation.
	
	***************************************************************************/
	void battlane_vh_stop(void)
	{
		if (screen_bitmap)
		{
			bitmap_free(screen_bitmap);
		}
		if (battlane_bitmap)
		{
			free(battlane_bitmap);
		}
	    if (bkgnd_bitmap)
	    {
	        free(bkgnd_bitmap);
	    }
	}
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	void battlane_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh)
	{
	    int scrollx,scrolly;
		int x,y, offs;
	
	    /* Scroll registers */
	    scrolly=256*(battlane_video_ctrl&0x01)+battlane_scrolly;
	    scrollx=256*(battlane_cpu_control&0x01)+battlane_scrollx;
	
	
	    /* Draw tile map. TODO: Cache it */
	    for (offs=0; offs <0x400;  offs++)
	    {
	        int sx,sy;
	        int code=battlane_tileram[offs];
	        int attr=battlane_tileram[0x400+offs];
	
	        sx=(offs&0x0f)+(offs&0x100)/16;
	        sy=((offs&0x200)/2+(offs&0x0f0))/16;
	        drawgfx(bkgnd_bitmap,Machine->gfx[1+(attr&0x01)],
	               code,
	               (attr>>1)&0x03,
	               !flipscreen,flipscreen,
	               sx*16,sy*16,
	               NULL,
	               TRANSPARENCY_NONE, 0);
	
	    }
	    /* copy the background graphics */
	    {
			int scrlx, scrly;
	        scrlx=-scrollx;
	        scrly=-scrolly;
	        copyscrollbitmap(bitmap,bkgnd_bitmap,1,&scrly,1,&scrlx,&Machine->visible_area,TRANSPARENCY_NONE,0);
	    }
	
	    /* Draw sprites */
	    for (offs=0; offs<0x0100; offs+=4)
		{
	           /*
	           0x80=bank 2
	           0x40=
	           0x20=bank 1
	           0x10=y double
	           0x08=color
	           0x04=x flip
	           0x02=y flip
	           0x01=Sprite enable
	           */
	          int attr=battlane_spriteram[offs+1];
	          int code=battlane_spriteram[offs+3];
	          code += 256*((attr>>6) & 0x02);
	          code += 256*((attr>>5) & 0x01);
	
	          if (attr & 0x01)
		      {
				  int color = (attr>>3)&1;
	               int sx=battlane_spriteram[offs+2];
	               int sy=battlane_spriteram[offs];
	               int flipx=attr&0x04;
	               int flipy=attr&0x02;
	               if (flipscreen == 0)
	               {
	                    sx=240-sx;
	                    sy=240-sy;
	                    flipy=!flipy;
	                    flipx=!flipx;
	               }
	               if ( attr & 0x10)  /* Double Y direction */
	               {
	                   int dy=16;
	                   if (flipy)
	                   {
	                        dy=-16;
	                   }
	                   drawgfx(bitmap,Machine->gfx[0],
	                     code,
	                     color,
	                     flipx,flipy,
	                     sx, sy,
						 &Machine->visible_area,
	                     TRANSPARENCY_PEN, 0);
	
	                    drawgfx(bitmap,Machine->gfx[0],
	                     code+1,
	                     color,
	                     flipx,flipy,
	                     sx, sy-dy,
						 &Machine->visible_area,
	                     TRANSPARENCY_PEN, 0);
	                }
	                else
	                {
	                   drawgfx(bitmap,Machine->gfx[0],
						 code,
	                     color,
	                     flipx,flipy,
	                     sx, sy,
						 &Machine->visible_area,
	                     TRANSPARENCY_PEN, 0);
	                }
	          }
		}
	
	    /* Draw foreground bitmap */
		if (flipscreen)
		{
			for (y=0; y<0x20*8; y++)
			{
				for (x=0; x<0x20*8; x++)
				{
					int data=((UINT8 *)screen_bitmap->line[y])[x];
					if (data)
					{
						plot_pixel(bitmap,255-x,255-y,Machine->pens[data]);
					}
				}
			}
		}
		else
		{
			for (y=0; y<0x20*8; y++)
			{
				for (x=0; x<0x20*8; x++)
				{
					int data=((UINT8 *)screen_bitmap->line[y])[x];
					if (data)
					{
						plot_pixel(bitmap,x,y,Machine->pens[data]);
					}
				}
			}
	
		}
	}
}
