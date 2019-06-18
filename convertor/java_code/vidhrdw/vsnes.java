/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package vidhrdw;

public class vsnes
{
	
	/* from machine */
	
	
	public static VhConvertColorPromPtr vsnes_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		ppu2c03b_init_palette( palette );
	} };
	
	public static VhConvertColorPromPtr vsdual_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		ppu2c03b_init_palette( palette );
		ppu2c03b_init_palette( &palette[3*64] );
	} };
	
	static void ppu_irq( int num )
	{
		cpu_set_nmi_line( num, PULSE_LINE );
	}
	
	/* our ppu interface											*/
	static struct ppu2c03b_interface ppu_interface =
	{
		1,						/* num */
		{ REGION_GFX1 },		/* vrom gfx region */
		{ 0 },					/* gfxlayout num */
		{ 0 },					/* color base */
		{ PPU_MIRROR_NONE },	/* mirroring */
		{ ppu_irq }				/* irq */
	};
	
	/* our ppu interface for dual games								*/
	static struct ppu2c03b_interface ppu_dual_interface =
	{
		2,										/* num */
		{ REGION_GFX1, REGION_GFX2 },			/* vrom gfx region */
		{ 0, 1 },								/* gfxlayout num */
		{ 0, 64 },								/* color base */
		{ PPU_MIRROR_NONE, PPU_MIRROR_NONE },	/* mirroring */
		{ ppu_irq, ppu_irq }					/* irq */
	};
	
	public static VhStartPtr vsnes_vh_start = new VhStartPtr() { public int handler() 
	{
		return ppu2c03b_init( &ppu_interface );
	} };
	
	public static VhStartPtr vsdual_vh_start = new VhStartPtr() { public int handler() 
	{
		return ppu2c03b_init( &ppu_dual_interface );
	} };
	
	public static VhStopPtr vsnes_vh_stop = new VhStopPtr() { public void handler() 
	{
		ppu2c03b_dispose();
	} };
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	public static VhUpdatePtr vsnes_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		/* render the ppu */
		ppu2c03b_render( 0, bitmap, 0, 0, 0, 0 );
	
			/* if this is a gun game, draw a simple crosshair */
			if ( vsnes_gun_controller )
			{
				int x_center = readinputport( 4 );
				int y_center = readinputport( 5 );
	
				draw_crosshair(bitmap,x_center,y_center,&Machine->visible_area);
	
			}
	
		} };
	
	
	public static VhUpdatePtr vsdual_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		/* render the ppu's */
		ppu2c03b_render( 0, bitmap, 0, 0, 0, 0 );
		ppu2c03b_render( 1, bitmap, 0, 0, 32*8, 0 );
	} };
}
