/***************************************************************************

	Atari Tetris hardware

***************************************************************************/

#include "driver.h"
#include "vidhrdw/generic.h"


static struct tilemap *tilemap;


/*************************************
 *
 *	Tilemap callback
 *
 *************************************/

static void get_tile_info(int tile_index)
{
	int code = videoram[tile_index * 2] | ((videoram[tile_index * 2 + 1] & 7) << 8);
	int color = (videoram[tile_index * 2 + 1] & 0xf0) >> 4;

	SET_TILE_INFO(0, code, color, 0);
}



/*************************************
 *
 *	Video RAM write
 *
 *************************************/

WRITE_HANDLER( atetris_videoram_w )
{
	videoram[offset] = data;
	tilemap_mark_tile_dirty(tilemap, offset / 2);
}



/*************************************
 *
 *	Video system start
 *
 *************************************/

int atetris_vh_start(void)
{
	tilemap = tilemap_create(get_tile_info, tilemap_scan_rows, TILEMAP_OPAQUE, 8,8, 64,32);
	if (!tilemap)
		return 1;
	return 0;
}



/*************************************
 *
 *	Main refresh
 *
 *************************************/

void atetris_vh_screenrefresh(struct mame_bitmap *bitmap, int full_refresh)
{
	tilemap_draw(bitmap, tilemap, 0,0);
}
