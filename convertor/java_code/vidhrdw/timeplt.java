/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package vidhrdw;

public class timeplt
{
	
	extern unsigned char *spriteram,*spriteram_2;
	extern size_t spriteram_size;
	
	unsigned char *timeplt_videoram,*timeplt_colorram;
	static struct tilemap *bg_tilemap;
	
	/*
	sprites are multiplexed, so we have to buffer the spriteram
	scanline by scanline.
	*/
	static unsigned char *sprite_mux_buffer,*sprite_mux_buffer_2;
	static int scanline;
	
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  Time Pilot has two 32x8 palette PROMs and two 256x4 lookup table PROMs
	  (one for characters, one for sprites).
	  The palette PROMs are connected to the RGB output this way:
	
	  bit 7 -- 390 ohm resistor  -- BLUE
	        -- 470 ohm resistor  -- BLUE
	        -- 560 ohm resistor  -- BLUE
	        -- 820 ohm resistor  -- BLUE
	        -- 1.2kohm resistor  -- BLUE
	        -- 390 ohm resistor  -- GREEN
	        -- 470 ohm resistor  -- GREEN
	  bit 0 -- 560 ohm resistor  -- GREEN
	
	  bit 7 -- 820 ohm resistor  -- GREEN
	        -- 1.2kohm resistor  -- GREEN
	        -- 390 ohm resistor  -- RED
	        -- 470 ohm resistor  -- RED
	        -- 560 ohm resistor  -- RED
	        -- 820 ohm resistor  -- RED
	        -- 1.2kohm resistor  -- RED
	  bit 0 -- not connected
	
	***************************************************************************/
	void timeplt_vh_convert_color_prom(unsigned char *obsolete,unsigned short *colortable,const unsigned char *color_prom)
	{
		int i;
		#define TOTAL_COLORS(gfxn) (Machine->gfx[gfxn]->total_colors * Machine->gfx[gfxn]->color_granularity)
		#define COLOR(gfxn,offs) (colortable[Machine->drv->gfxdecodeinfo[gfxn].color_codes_start + offs])
	
	
		for (i = 0;i < Machine->drv->total_colors;i++)
		{
			int bit0,bit1,bit2,bit3,bit4,r,g,b;
	
	
			bit0 = (color_prom[i + Machine->drv->total_colors] >> 1) & 0x01;
			bit1 = (color_prom[i + Machine->drv->total_colors] >> 2) & 0x01;
			bit2 = (color_prom[i + Machine->drv->total_colors] >> 3) & 0x01;
			bit3 = (color_prom[i + Machine->drv->total_colors] >> 4) & 0x01;
			bit4 = (color_prom[i + Machine->drv->total_colors] >> 5) & 0x01;
			r = 0x19 * bit0 + 0x24 * bit1 + 0x35 * bit2 + 0x40 * bit3 + 0x4d * bit4;
			bit0 = (color_prom[i + Machine->drv->total_colors] >> 6) & 0x01;
			bit1 = (color_prom[i + Machine->drv->total_colors] >> 7) & 0x01;
			bit2 = (color_prom[i] >> 0) & 0x01;
			bit3 = (color_prom[i] >> 1) & 0x01;
			bit4 = (color_prom[i] >> 2) & 0x01;
			g = 0x19 * bit0 + 0x24 * bit1 + 0x35 * bit2 + 0x40 * bit3 + 0x4d * bit4;
			bit0 = (color_prom[i] >> 3) & 0x01;
			bit1 = (color_prom[i] >> 4) & 0x01;
			bit2 = (color_prom[i] >> 5) & 0x01;
			bit3 = (color_prom[i] >> 6) & 0x01;
			bit4 = (color_prom[i] >> 7) & 0x01;
			b = 0x19 * bit0 + 0x24 * bit1 + 0x35 * bit2 + 0x40 * bit3 + 0x4d * bit4;
	
			palette_set_color(i,r,g,b);
		}
	
		color_prom += 2*Machine->drv->total_colors;
		/* color_prom now points to the beginning of the lookup table */
	
	
		/* sprites */
		for (i = 0;i < TOTAL_COLORS(1);i++)
			COLOR(1,i) = *(color_prom++) & 0x0f;
	
		/* characters */
		for (i = 0;i < TOTAL_COLORS(0);i++)
			COLOR(0,i) = (*(color_prom++) & 0x0f) + 0x10;
	}
	
	
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static void get_tile_info(int tile_index)
	{
		unsigned char attr = timeplt_colorram[tile_index];
		tile_info.priority = (attr & 0x10) >> 4;
		SET_TILE_INFO(
				0,
				timeplt_videoram[tile_index] + ((attr & 0x20) << 3),
				attr & 0x1f,
				TILE_FLIPYX((attr & 0xc0) >> 6))
	}
	
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStopPtr timeplt_vh_stop = new VhStopPtr() { public void handler() 
	{
		free(sprite_mux_buffer);
		free(sprite_mux_buffer_2);
		sprite_mux_buffer = NULL;
		sprite_mux_buffer_2 = NULL;
	} };
	
	public static VhStartPtr timeplt_vh_start = new VhStartPtr() { public int handler() 
	{
		bg_tilemap = tilemap_create(get_tile_info,tilemap_scan_rows,TILEMAP_OPAQUE,8,8,32,32);
	
		sprite_mux_buffer = malloc(256 * spriteram_size[0]);
		sprite_mux_buffer_2 = malloc(256 * spriteram_size[0]);
	
		if (!bg_tilemap || !sprite_mux_buffer || !sprite_mux_buffer_2)
		{
			timeplt_vh_stop();
			return 1;
		}
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr timeplt_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (timeplt_videoram[offset] != data)
		{
			timeplt_videoram[offset] = data;
			tilemap_mark_tile_dirty(bg_tilemap,offset);
		}
	} };
	
	public static WriteHandlerPtr timeplt_colorram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (timeplt_colorram[offset] != data)
		{
			timeplt_colorram[offset] = data;
			tilemap_mark_tile_dirty(bg_tilemap,offset);
		}
	} };
	
	public static WriteHandlerPtr timeplt_flipscreen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		flip_screen_set(~data & 1);
	} };
	
	/* Return the current video scan line */
	public static ReadHandlerPtr timeplt_scanline_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return scanline;
	} };
	
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	static void draw_sprites(struct mame_bitmap *bitmap)
	{
		const struct GfxElement *gfx = Machine->gfx[1];
		struct rectangle clip = Machine->visible_area;
		int offs;
		int line;
	
	
		for (line = 0;line < 256;line++)
		{
			if (line >= Machine->visible_area.min_y && line <= Machine->visible_area.max_y)
			{
				unsigned char *sr,*sr2;
	
				sr = sprite_mux_buffer + line * spriteram_size;
				sr2 = sprite_mux_buffer_2 + line * spriteram_size;
				clip.min_y = clip.max_y = line;
	
				for (offs = spriteram_size - 2;offs >= 0;offs -= 2)
				{
					int code,color,sx,sy,flipx,flipy;
	
					sx = sr[offs];
					sy = 241 - sr2[offs + 1];
	
					if (sy > line-16 && sy <= line)
					{
						code = sr[offs + 1];
						color = sr2[offs] & 0x3f;
						flipx = ~sr2[offs] & 0x40;
						flipy = sr2[offs] & 0x80;
	
						drawgfx(bitmap,gfx,
								code,
								color,
								flipx,flipy,
								sx,sy,
								&clip,TRANSPARENCY_PEN,0);
					}
				}
			}
		}
	}
	
	public static VhUpdatePtr timeplt_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		tilemap_draw(bitmap,bg_tilemap,0,0);
		draw_sprites(bitmap);
		tilemap_draw(bitmap,bg_tilemap,1,0);
	} };
	
	
	public static InterruptPtr timeplt_interrupt = new InterruptPtr() { public int handler() 
	{
		scanline = 255 - cpu_getiloops();
	
		memcpy(sprite_mux_buffer + scanline * spriteram_size,spriteram,spriteram_size);
		memcpy(sprite_mux_buffer_2 + scanline * spriteram_size,spriteram_2,spriteram_size);
	
		if (scanline == 255)
			return nmi_interrupt();
		else
			return ignore_interrupt();
	} };
}
