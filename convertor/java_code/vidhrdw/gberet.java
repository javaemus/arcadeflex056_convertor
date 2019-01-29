/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package vidhrdw;

public class gberet
{
	
	
	
	unsigned char *gberet_videoram,*gberet_colorram;
	unsigned char *gberet_spritebank;
	unsigned char *gberet_scrollram;
	static struct tilemap *bg_tilemap;
	static int interruptenable;
	static int flipscreen;
	
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  Green Beret has a 32 bytes palette PROM and two 256 bytes color lookup table
	  PROMs (one for sprites, one for characters).
	  The palette PROM is connected to the RGB output, this way:
	
	  bit 7 -- 220 ohm resistor  -- BLUE
	        -- 470 ohm resistor  -- BLUE
	        -- 220 ohm resistor  -- GREEN
	        -- 470 ohm resistor  -- GREEN
	        -- 1  kohm resistor  -- GREEN
	        -- 220 ohm resistor  -- RED
	        -- 470 ohm resistor  -- RED
	  bit 0 -- 1  kohm resistor  -- RED
	
	***************************************************************************/
	
	void gberet_vh_convert_color_prom(unsigned char *palette, unsigned short *colortable,const unsigned char *color_prom)
	{
		int i;
		#define TOTAL_COLORS(gfxn) (Machine->gfx[gfxn]->total_colors * Machine->gfx[gfxn]->color_granularity)
		#define COLOR(gfxn,offs) (colortable[Machine->drv->gfxdecodeinfo[gfxn].color_codes_start + offs])
	
	
		for (i = 0;i < Machine->drv->total_colors;i++)
		{
			int bit0,bit1,bit2;
	
	
			bit0 = (*color_prom >> 0) & 0x01;
			bit1 = (*color_prom >> 1) & 0x01;
			bit2 = (*color_prom >> 2) & 0x01;
			*(palette++) = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
			bit0 = (*color_prom >> 3) & 0x01;
			bit1 = (*color_prom >> 4) & 0x01;
			bit2 = (*color_prom >> 5) & 0x01;
			*(palette++) = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
			bit0 = 0;
			bit1 = (*color_prom >> 6) & 0x01;
			bit2 = (*color_prom >> 7) & 0x01;
			*(palette++) = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
	
			color_prom++;
		}
	
		for (i = 0;i < TOTAL_COLORS(1);i++)
		{
			if (*color_prom & 0x0f) COLOR(1,i) = *color_prom & 0x0f;
			else COLOR(1,i) = 0;
			color_prom++;
		}
		for (i = 0;i < TOTAL_COLORS(0);i++)
		{
			COLOR(0,i) = (*(color_prom++) & 0x0f) + 0x10;
		}
	}
	
	
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static void get_tile_info(int tile_index)
	{
		unsigned char attr = gberet_colorram[tile_index];
		SET_TILE_INFO(
				0,
				gberet_videoram[tile_index] + ((attr & 0x40) << 2),
				attr & 0x0f,
				TILE_FLIPYX((attr & 0x30) >> 4))
		tile_info.priority = (attr & 0x80) >> 7;
	}
	
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	int gberet_vh_start(void)
	{
		bg_tilemap = tilemap_create(get_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT_COLOR,8,8,64,32);
	
		if (!bg_tilemap)
			return 0;
	
		tilemap_set_transparent_pen(bg_tilemap,0x10);
		tilemap_set_scroll_rows(bg_tilemap,32);
	
		return 0;
	}
	
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	WRITE_HANDLER( gberet_videoram_w )
	{
		if (gberet_videoram[offset] != data)
		{
			gberet_videoram[offset] = data;
			tilemap_mark_tile_dirty(bg_tilemap,offset);
		}
	}
	
	WRITE_HANDLER( gberet_colorram_w )
	{
		if (gberet_colorram[offset] != data)
		{
			gberet_colorram[offset] = data;
			tilemap_mark_tile_dirty(bg_tilemap,offset);
		}
	}
	
	WRITE_HANDLER( gberet_e044_w )
	{
		/* bit 0 enables interrupts */
		interruptenable = data & 1;
	
		/* bit 3 flips screen */
		flipscreen = data & 0x08;
		tilemap_set_flip(ALL_TILEMAPS,flipscreen ? (TILEMAP_FLIPY | TILEMAP_FLIPX) : 0);
	
		/* don't know about the other bits */
	}
	
	WRITE_HANDLER( gberet_scroll_w )
	{
		int scroll;
	
		gberet_scrollram[offset] = data;
	
		scroll = gberet_scrollram[offset & 0x1f] | (gberet_scrollram[offset | 0x20] << 8);
		tilemap_set_scrollx(bg_tilemap,offset & 0x1f,scroll);
	}
	
	WRITE_HANDLER( gberetb_scroll_w )
	{
		int scroll;
	
		scroll = data;
		if (offset) scroll |= 0x100;
	
		for (offset = 6;offset < 29;offset++)
			tilemap_set_scrollx(bg_tilemap,offset,scroll + 64-8);
	}
	
	
	int gberet_interrupt(void)
	{
		if (cpu_getiloops() == 0) return interrupt();
		else if (cpu_getiloops() % 2)
		{
			if (interruptenable) return nmi_interrupt();
		}
	
		return ignore_interrupt();
	}
	
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	static void draw_sprites(struct mame_bitmap *bitmap)
	{
		int offs;
		unsigned char *sr;
	
		if (*gberet_spritebank & 0x08)
			sr = spriteram_2;
		else sr = spriteram;
	
		for (offs = 0;offs < spriteram_size;offs += 4)
		{
			if (sr[offs+3])
			{
				int sx,sy,flipx,flipy;
	
	
				sx = sr[offs+2] - 2*(sr[offs+1] & 0x80);
				sy = sr[offs+3];
				flipx = sr[offs+1] & 0x10;
				flipy = sr[offs+1] & 0x20;
	
				if (flipscreen)
				{
					sx = 240 - sx;
					sy = 240 - sy;
					flipx = !flipx;
					flipy = !flipy;
				}
	
				drawgfx(bitmap,Machine->gfx[1],
						sr[offs+0] + ((sr[offs+1] & 0x40) << 2),
						sr[offs+1] & 0x0f,
						flipx,flipy,
						sx,sy,
						&Machine->visible_area,TRANSPARENCY_COLOR,0);
			}
		}
	}
	
	static void draw_sprites_bootleg(struct mame_bitmap *bitmap)
	{
		int offs;
		unsigned char *sr;
	
		sr = spriteram;
	
		for (offs = spriteram_size - 4;offs >= 0;offs -= 4)
		{
			if (sr[offs+1])
			{
				int sx,sy,flipx,flipy;
	
	
				sx = sr[offs+2] - 2*(sr[offs+3] & 0x80);
				sy = sr[offs+1];
				sy = 240 - sy;
				flipx = sr[offs+3] & 0x10;
				flipy = sr[offs+3] & 0x20;
	
				if (flipscreen)
				{
					sx = 240 - sx;
					sy = 240 - sy;
					flipx = !flipx;
					flipy = !flipy;
				}
	
				drawgfx(bitmap,Machine->gfx[1],
						sr[offs+0] + ((sr[offs+3] & 0x40) << 2),
						sr[offs+3] & 0x0f,
						flipx,flipy,
						sx,sy,
						&Machine->visible_area,TRANSPARENCY_COLOR,0);
			}
		}
	}
	
	
	void gberet_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh)
	{
		tilemap_draw(bitmap,bg_tilemap,TILEMAP_IGNORE_TRANSPARENCY|0,0);
		tilemap_draw(bitmap,bg_tilemap,TILEMAP_IGNORE_TRANSPARENCY|1,0);
		draw_sprites(bitmap);
		tilemap_draw(bitmap,bg_tilemap,0,0);
	}
	
	void gberetb_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh)
	{
		tilemap_draw(bitmap,bg_tilemap,TILEMAP_IGNORE_TRANSPARENCY|0,0);
		tilemap_draw(bitmap,bg_tilemap,TILEMAP_IGNORE_TRANSPARENCY|1,0);
		draw_sprites_bootleg(bitmap);
		tilemap_draw(bitmap,bg_tilemap,0,0);
	}
}