/*
**	Video Driver for Taito Samurai (1985)
*/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package vidhrdw;

public class tsamurai
{
	
	
	/*
	** variables
	*/
	unsigned char *tsamurai_videoram;
	static int bgcolor;
	static int textbank1, textbank2;
	
	static struct tilemap *background, *foreground;
	
	
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static void get_bg_tile_info(int tile_index)
	{
		unsigned char attributes = tsamurai_videoram[2*tile_index+1];
		int tile_number = tsamurai_videoram[2*tile_index];
		tile_number += (( attributes & 0xc0 ) >> 6 ) * 256;	 /* legacy */
		tile_number += (( attributes & 0x20 ) >> 5 ) * 1024; /* Mission 660 add-on*/
		SET_TILE_INFO(
				0,
				tile_number,
				attributes & 0x1f,
				0)
	}
	
	static void get_fg_tile_info(int tile_index)
	{
		int tile_number = videoram.read(tile_index);
		if (textbank1 & 0x01) tile_number += 256; /* legacy */
		if (textbank2 & 0x01) tile_number += 512; /* Mission 660 add-on */
		SET_TILE_INFO(
				1,
				tile_number,
				colorram.read(((tile_index&0x1f)*2)+1)& 0x1f,
				0)
	}
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr tsamurai_vh_start = new VhStartPtr() { public int handler() 
	{
		background = tilemap_create(get_bg_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,32,32);
		foreground = tilemap_create(get_fg_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,32,32);
	
		if (!background || !foreground)
			return 1;
	
		tilemap_set_transparent_pen(background,0);
		tilemap_set_transparent_pen(foreground,0);
	
		return 0;
	} };
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr tsamurai_scrolly_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		tilemap_set_scrolly( background, 0, data );
	} };
	
	public static WriteHandlerPtr tsamurai_scrollx_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		tilemap_set_scrollx( background, 0, data );
	} };
	
	public static WriteHandlerPtr tsamurai_bgcolor_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		bgcolor = data;
	} };
	
	public static WriteHandlerPtr tsamurai_textbank1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( textbank1!=data )
		{
			textbank1 = data;
			tilemap_mark_all_tiles_dirty( foreground );
		}
	} };
	
	public static WriteHandlerPtr tsamurai_textbank2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( textbank2!=data )
		{
			textbank2 = data;
			tilemap_mark_all_tiles_dirty( foreground );
		}
	} };
	
	public static WriteHandlerPtr tsamurai_bg_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( tsamurai_videoram[offset]!=data )
		{
			tsamurai_videoram[offset]=data;
			offset = offset/2;
			tilemap_mark_tile_dirty(background,offset);
		}
	} };
	public static WriteHandlerPtr tsamurai_fg_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( videoram.read(offset)!=data )
		{
			videoram.write(offset,data);
			tilemap_mark_tile_dirty(foreground,offset);
		}
	} };
	public static WriteHandlerPtr tsamurai_fg_colorram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( colorram.read(offset)!=data )
		{
			colorram.write(offset,data);
			if (offset & 1)
			{
				int col = offset/2;
				int row;
				for (row = 0;row < 32;row++)
					tilemap_mark_tile_dirty(foreground,32*row+col);
			}
		}
	} };
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	static void draw_sprites( struct mame_bitmap *bitmap )
	{
		struct GfxElement *gfx = Machine->gfx[2];
		const struct rectangle *clip = &Machine->visible_area;
		const unsigned char *source = spriteram+32*4-4;
		const unsigned char *finish = spriteram; /* ? */
		static int flicker;
		flicker = 1-flicker;
	
		while( source>=finish )
		{
			int attributes = source[2]; /* bit 0x10 is usually, but not always set */
	
			int sx = source[3] - 16;
			int sy = 240-source[0];
			int sprite_number = source[1];
			int color = attributes&0x1f;
			//color = 0x2d - color; nunchakun fix?
			if( sy<-16 ) sy += 256;
	
			if( flip_screen )
			{
				drawgfx( bitmap,gfx,
					sprite_number&0x7f,
					color,
					1,(sprite_number&0x80)?0:1,
					256-32-sx,256-32-sy,
					clip,TRANSPARENCY_PEN,0 );
			}
			else
			{
				drawgfx( bitmap,gfx,
					sprite_number&0x7f,
					color,
					0,sprite_number&0x80,
					sx,sy,
					clip,TRANSPARENCY_PEN,0 );
			}
	
			source -= 4;
		}
	}
	
	public static VhUpdatePtr tsamurai_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int fullrefresh) 
	{
		int i;
	
	/* Do the column scroll used for the "660" logo on the title screen */
		tilemap_set_scroll_cols(foreground, 32);
		for (i = 0 ; i < 32 ; i++)
		{
			tilemap_set_scrolly(foreground, i, colorram.read(i*2));
		}
	/* end of column scroll code */
	
		/*
			This following isn't particularly efficient.  We'd be better off to
			dynamically change every 8th palette to the background color, so we
			could draw the background as an opaque tilemap.
	
			Note that the background color register isn't well understood
			(screenshots would be helpful)
		*/
		fillbitmap(bitmap,Machine->pens[bgcolor],&Machine->visible_area);
		tilemap_draw(bitmap,background,0,0);
		draw_sprites(bitmap);
		tilemap_draw(bitmap,foreground,0,0);
	} };
	
	/***************************************************************************
	
	VS Gong Fight runs on older hardware
	
	***************************************************************************/
	
	int vsgongf_color;
	
	public static WriteHandlerPtr vsgongf_color_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( vsgongf_color != data )
		{
			vsgongf_color = data;
			tilemap_mark_all_tiles_dirty( foreground );
		}
	} };
	
	static void get_vsgongf_tile_info(int tile_index)
	{
		int tile_number = videoram.read(tile_index);
		int color = vsgongf_color&0x1f;
		if( textbank1 ) tile_number += 0x100;
		SET_TILE_INFO(
				1,
				tile_number,
				color,
				0)
	}
	
	public static VhStartPtr vsgongf_vh_start = new VhStartPtr() { public int handler() 
	{
		foreground = tilemap_create(get_vsgongf_tile_info,tilemap_scan_rows,TILEMAP_OPAQUE,8,8,32,32);
		if (foreground == 0) return 1;
		return 0;
	} };
	
	public static VhUpdatePtr vsgongf_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int fullrefresh) 
	{
		static int k;
		if( keyboard_pressed( KEYCODE_Q ) ){
			while( keyboard_pressed( KEYCODE_Q ) ){}
			k++;
			vsgongf_color = k;
			tilemap_mark_all_tiles_dirty( foreground );
		}
	
		tilemap_draw(bitmap,foreground,0,0);
		draw_sprites(bitmap);
	} };
}
