 /****************************************************************************
 *									  *
 * vidhrdw.c								*
 *									  *
 * Functions to emulate the video hardware of the machine.		  *
 *									  *
 ****************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package vidhrdw;

public class speedbal
{
	
	#define SPRITE_X		0
	#define SPRITE_NUMBER	   1
	#define SPRITE_PALETTE	  2
	#define SPRITE_Y		3
	
	extern unsigned char *speedbal_background_videoram;
	extern unsigned char *speedbal_foreground_videoram;
	extern unsigned char *speedbal_sprites_dataram;
	
	extern size_t speedbal_background_videoram_size;
	extern size_t speedbal_foreground_videoram_size;
	extern size_t speedbal_sprites_dataram_size;
	
	static unsigned char *bg_dirtybuffer;	  /* background tiles */
	static unsigned char *ch_dirtybuffer;	  /* foreground char  */
	
	static struct mame_bitmap *bitmap_bg;   /* background tiles */
	static struct mame_bitmap *bitmap_ch;   /* foreground char  */
	
	public static VhConvertColorPromPtr speedbal_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		int i;
		#define TOTAL_COLORS(gfxn) (Machine->gfx[gfxn]->total_colors * Machine->gfx[gfxn]->color_granularity)
		#define COLOR(gfxn,offs) (colortable[Machine->drv->gfxdecodeinfo[gfxn].color_codes_start + offs])
	
	
		for (i = 0;i < Machine->drv->total_colors;i++)
		{
			int bit0,bit1,bit2;
	
	
			/* red component */
			bit0 = (*color_prom >> 0) & 0x01;
			bit1 = (*color_prom >> 1) & 0x01;
			bit2 = (*color_prom >> 2) & 0x01;
			*(palette++) = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
			/* green component */
			bit0 = (*color_prom >> 3) & 0x01;
			bit1 = (*color_prom >> 4) & 0x01;
			bit2 = (*color_prom >> 5) & 0x01;
			*(palette++) = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
			/* blue component */
			bit0 = 0;
			bit1 = (*color_prom >> 6) & 0x01;
			bit2 = (*color_prom >> 7) & 0x01;
			*(palette++) = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
	
			color_prom++;
		}
	
	
		/* characters */
		for (i = 0;i < TOTAL_COLORS(0);i++)
			COLOR(0,i) = *(color_prom++) ;
	
		/* tiles */
		for (i = 0;i < TOTAL_COLORS(1);i++)
			COLOR(1,i) = *(color_prom++) & 0x0f;
	
		/* sprites */
		for (i = 0;i < TOTAL_COLORS(2);i++)
			COLOR(2,i) = *(color_prom++) & 0x0f;
	
	} };
	
	
	/*************************************
	 *				   *
	 *	    Start-Stop	     *
	 *				   *
	 *************************************/
	
	public static VhStartPtr speedbal_vh_start = new VhStartPtr() { public int handler() 
	{
		if ((bg_dirtybuffer = malloc (speedbal_background_videoram_size)) == 0)
		{
			return 1;
		}
		if ((ch_dirtybuffer = malloc (speedbal_foreground_videoram_size)) == 0)
		{
			free (bg_dirtybuffer);
			return 1;
		}
	
		/* foreground bitmap */
		if ((bitmap_ch = bitmap_alloc (Machine->drv->screen_width,Machine->drv->screen_height)) == 0)
		{
			free (bg_dirtybuffer);
			free (ch_dirtybuffer);
			return 1;
		}
	
		/* background bitmap */
		if ((bitmap_bg = bitmap_alloc (Machine->drv->screen_width*2,Machine->drv->screen_height*2)) == 0)
		{
			free (bg_dirtybuffer);
			free (ch_dirtybuffer);
			bitmap_free (bitmap_ch);
			return 1;
		}
	
		memset (ch_dirtybuffer,1,speedbal_foreground_videoram_size / 2);
		memset (bg_dirtybuffer,1,speedbal_background_videoram_size / 2);
		return 0;
	
	} };
	
	public static VhStopPtr speedbal_vh_stop = new VhStopPtr() { public void handler() 
	{
		bitmap_free (bitmap_ch);
		bitmap_free (bitmap_bg);
		free (bg_dirtybuffer);
		free (ch_dirtybuffer);
	} };
	
	
	
	/*************************************
	 *				   *
	 *      Foreground characters RAM    *
	 *				   *
	 *************************************/
	
	public static WriteHandlerPtr speedbal_foreground_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	   ch_dirtybuffer[offset] = 1;
	   speedbal_foreground_videoram[offset]=data;
	} };
	
	public static ReadHandlerPtr speedbal_foreground_videoram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	   return speedbal_foreground_videoram[offset];
	} };
	
	
	
	/*************************************
	 *				   *
	 *	Background tiles RAM       *
	 *				   *
	 *************************************/
	
	public static WriteHandlerPtr speedbal_background_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	   bg_dirtybuffer[offset] = 1;
	   speedbal_background_videoram[offset] = data;
	} };
	
	public static ReadHandlerPtr speedbal_background_videoram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	   return speedbal_background_videoram[offset];
	} };
	
	
	
	
	/*************************************
	 *				   *
	 *	 Sprite drawing	    *
	 *				   *
	 *************************************/
	
	void speedbal_draw_sprites (struct mame_bitmap *bitmap)
	{
		int SPTX,SPTY,SPTTile,SPTColor,offset,f;
		unsigned char carac;
		unsigned char *SPTRegs;
	
		/* Drawing sprites: 64 in total */
	
		for (offset = 0;offset < speedbal_sprites_dataram_size;offset += 4)
		{
			SPTRegs = &speedbal_sprites_dataram[offset];
	
	
			SPTX = 243 - SPTRegs[SPRITE_Y];
			SPTY = 239 - SPTRegs[SPRITE_X];
	
			carac  =  SPTRegs[SPRITE_NUMBER ];
			SPTTile=0;
			for (f=0;f<8;f++) SPTTile+= ((carac >>f)&1)<<(7-f);
			SPTColor = (SPTRegs[SPRITE_PALETTE]&0x0f);
	
			if (!(SPTRegs[SPRITE_PALETTE]&0x40)) SPTTile+=256;
	
			drawgfx (bitmap,Machine->gfx[2],
					SPTTile,
					SPTColor,
					0,0,
					SPTX,SPTY,
					&Machine->visible_area,TRANSPARENCY_PEN,0);
		}
	}
	
	
	
	/*************************************
	 *				   *
	 *     Background drawing: Tiles     *
	 *				   *
	 *************************************/
	
	void speedbal_draw_background (struct mame_bitmap *bitmap)
	{
		int sx,sy,code,tile,offset,color;
	
		for (offset = 0;offset < speedbal_background_videoram_size ;offset+=2)
		{
		    if (bg_dirtybuffer[offset])
			{
				bg_dirtybuffer[offset] = 0;
	
				tile = speedbal_background_videoram[offset+0];
				code = speedbal_background_videoram[offset+1];
				tile += (code & 0x30) << 4;
				color=(code&0x0f);
	
				sx = 15 - (offset / 2) / 16;
				sy = (offset / 2) % 16;
	
				drawgfx (bitmap_bg,Machine->gfx[1],
						tile,
						color,
						0,0,
						16*sx,16*sy,
						0,TRANSPARENCY_NONE,0);
			}
		}
	
		copybitmap (bitmap,bitmap_bg,0,0,0,0,&Machine->visible_area,TRANSPARENCY_NONE,0);
	}
	
	
	/*************************************
	 *				   *
	 *   Foreground drawing: 8x8 graphs  *
	 *				   *
	 *************************************/
	
	void speedbal_draw_foreground1 (struct mame_bitmap *bitmap)
	{
		int sx,sy,code,caracter,color,offset;
	
		for (offset = 0;offset < speedbal_foreground_videoram_size ;offset+=2)
		{
	//	    if (ch_dirtybuffer[offset])
			{
				caracter = speedbal_foreground_videoram[offset];
				code     = speedbal_foreground_videoram[offset+1];
				caracter += (code & 0x30) << 4;
	
				color=(code&0x0f);
	
				sx = 31 - (offset / 2) / 32;
				sy = (offset / 2) % 32;
	
				drawgfx (bitmap,Machine->gfx[0],
						caracter,
						color,
						0,0,
						8*sx,8*sy,
						&Machine->visible_area,TRANSPARENCY_PEN,0);
	
				ch_dirtybuffer[offset] = 0;
			}
	
		}
	}
	
	
	/*************************************
	 *				   *
	 *	 Refresh   screen	  *
	 *				   *
	 *************************************/
	
	public static VhUpdatePtr speedbal_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		// first background
		speedbal_draw_background (bitmap);
	
		// second characters (general)
		speedbal_draw_foreground1 (bitmap);
	
		// thirth sprites
		speedbal_draw_sprites (bitmap);
	} };
}
