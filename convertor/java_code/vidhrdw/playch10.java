/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package vidhrdw;

public class playch10
{
	
	/* from machine */
	
	void playch10_vh_convert_color_prom( unsigned char *palette, unsigned short *colortable,const unsigned char *color_prom )
	{
		int i;
	
		for ( i = 0;i < 256; i++ )
		{
			int bit0,bit1,bit2,bit3;
	
			/* red component */
			bit0 = ~(color_prom[0] >> 0) & 0x01;
			bit1 = ~(color_prom[0] >> 1) & 0x01;
			bit2 = ~(color_prom[0] >> 2) & 0x01;
			bit3 = ~(color_prom[0] >> 3) & 0x01;
			*palette++ = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
			/* green component */
			bit0 = ~(color_prom[256] >> 0) & 0x01;
			bit1 = ~(color_prom[256] >> 1) & 0x01;
			bit2 = ~(color_prom[256] >> 2) & 0x01;
			bit3 = ~(color_prom[256] >> 3) & 0x01;
			*palette++ = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
			/* blue component */
			bit0 = ~(color_prom[2*256] >> 0) & 0x01;
			bit1 = ~(color_prom[2*256] >> 1) & 0x01;
			bit2 = ~(color_prom[2*256] >> 2) & 0x01;
			bit3 = ~(color_prom[2*256] >> 3) & 0x01;
			*palette++ = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
	
			color_prom++;
		}
	
		ppu2c03b_init_palette( palette );
	}
	
	
	static void ppu_irq( int num )
	{
		cpu_set_nmi_line( 1, PULSE_LINE );
		pc10_int_detect = 1;
	}
	
	/* our ppu interface											*/
	/* things like mirroring and wether to use vrom or vram			*/
	/* can be set by calling 'ppu2c03b_override_hardware_options'	*/
	static struct ppu2c03b_interface ppu_interface =
	{
		1,						/* num */
		{ REGION_GFX2 },		/* vrom gfx region */
		{ 1 },					/* gfxlayout num */
		{ 256 },				/* color base */
		{ PPU_MIRROR_NONE },	/* mirroring */
		{ ppu_irq }				/* irq */
	};
	
	public static VhStartPtr playch10_vh_start = new VhStartPtr() { public int handler() 
	{
		if ( ppu2c03b_init( &ppu_interface ) )
			return 1;
	
		/* the bios uses the generic stuff */
		return generic_vh_start();
	} };
	
	public static VhStopPtr playch10_vh_stop = new VhStopPtr() { public void handler() 
	{
		ppu2c03b_dispose();
		generic_vh_stop();
	} };
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	public static VhUpdatePtr playch10_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
	
		struct rectangle top_monitor = Machine->visible_area;
		struct rectangle bottom_monitor = Machine->visible_area;
	
		top_monitor.max_y = ( top_monitor.max_y - top_monitor.min_y ) / 2;
		bottom_monitor.min_y = ( bottom_monitor.max_y - bottom_monitor.min_y ) / 2;
	
		if ( full_refresh )
			memset( dirtybuffer, 1, videoram_size );
	
		/* On Playchoice 10 single monitor, this bit toggles	*/
		/* between PPU and BIOS display.						*/
		/* We support the multi-monitor layout. In this case,	*/
		/* if the bit is not set, then we should display		*/
		/* the PPU portion.										*/
	
		if (pc10_dispmask == 0)
		{
			/* render the ppu */
			ppu2c03b_render( 0, bitmap, 0, 0, 0, 30*8 );
	
			/* if this is a gun game, draw a simple crosshair */
			if ( pc10_gun_controller )
			{
				int x_center = readinputport( 5 );
				int y_center = readinputport( 6 ) + 30*8;
	
				draw_crosshair(bitmap,x_center,y_center,&Machine->visible_area);
	
			}
		}
		else
		{
			/* the ppu is masked, clear out the area */
			fillbitmap( bitmap, Machine->pens[0], &bottom_monitor );
		}
	
		/* When the bios is accessing vram, the video circuitry cant access it */
		if ( pc10_sdcs )
		{
			fillbitmap( bitmap, Machine->pens[0], &top_monitor );
			return;
		}
	
		for( offs = videoram_size - 2; offs >= 0; offs -= 2 )
		{
			if ( dirtybuffer[offs] || dirtybuffer[offs+1] )
			{
				int offs2 = offs / 2;
	
				int sx = offs2 % 32;
				int sy = offs2 / 32;
	
				int tilenum = videoram[offs] + ( ( videoram[offs+1] & 7 ) << 8 );
				int color = ( videoram[offs+1] >> 3 ) & 0x1f;
	
				dirtybuffer[offs] = dirtybuffer[offs+1] = 0;
	
				drawgfx( tmpbitmap, Machine->gfx[0],
						 tilenum,
						 color,
						 0, 0,
						 8 * sx, 8 * sy,
						 &Machine->visible_area, TRANSPARENCY_NONE, 0 );
			}
		}
	
		/* copy the temporary bitmap to the screen */
		copybitmap( bitmap, tmpbitmap, 0, 0, 0, 0, &top_monitor, TRANSPARENCY_NONE, 0 );
	} };
}
