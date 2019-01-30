/***************************************************************************

Mermaid

Driver by Zsolt Vasvari

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package drivers;

public class mermaid
{
	
	
	extern unsigned char* mermaid_background_videoram;
	extern unsigned char* mermaid_foreground_videoram;
	extern unsigned char* mermaid_foreground_colorram;
	extern unsigned char* mermaid_background_scrollram;
	extern unsigned char* mermaid_foreground_scrollram;
	
	
	void mermaid_vh_convert_color_prom(unsigned char *palette, unsigned short *colortable,const unsigned char *color_prom);
	void mermaid_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh);
	
	
	static unsigned char *mermaid_AY8910_enable;
	
	public static WriteHandlerPtr mermaid_AY8910_write_port_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (mermaid_AY8910_enable[0])  AY8910_write_port_0_w(offset, data);
		if (mermaid_AY8910_enable[1])  AY8910_write_port_1_w(offset, data);
	} };
	
	public static WriteHandlerPtr mermaid_AY8910_control_port_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (mermaid_AY8910_enable[0])  AY8910_control_port_0_w(offset, data);
		if (mermaid_AY8910_enable[1])  AY8910_control_port_1_w(offset, data);
	} };
	
	
	public static ReadHandlerPtr mermaid_f800_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		// collision register active LO
		// Bit 0
		// Bit 1 - Sprite - Foreground
		//return rand() & 0xff;
		return 0x00;
	} };
	
	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x9fff, MRA_ROM ),
		new Memory_ReadAddress( 0xc000, 0xcbff, MRA_RAM ),
		new Memory_ReadAddress( 0xd000, 0xd3ff, MRA_RAM ),
		new Memory_ReadAddress( 0xd800, 0xd81f, MRA_RAM ),
		new Memory_ReadAddress( 0xd840, 0xd8bf, MRA_RAM ),
		new Memory_ReadAddress( 0xdc00, 0xdfff, MRA_RAM ),
		new Memory_ReadAddress( 0xe000, 0xe000, input_port_0_r ),
		new Memory_ReadAddress( 0xe800, 0xe800, input_port_1_r ),
		new Memory_ReadAddress( 0xf000, 0xf000, input_port_2_r ),
		new Memory_ReadAddress( 0xf800, 0xf800, mermaid_f800_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x9fff, MWA_ROM ),
		new Memory_WriteAddress( 0xc000, 0xc7ff, MWA_RAM ),
		new Memory_WriteAddress( 0xc800, 0xcbff, MWA_RAM, mermaid_background_videoram, videoram_size ),
		new Memory_WriteAddress( 0xd000, 0xd3ff, MWA_RAM, mermaid_foreground_videoram ),
		new Memory_WriteAddress( 0xd800, 0xd81f, MWA_RAM, mermaid_background_scrollram ),
		new Memory_WriteAddress( 0xd840, 0xd85f, MWA_RAM, mermaid_foreground_scrollram ),
		new Memory_WriteAddress( 0xd880, 0xd8bf, MWA_RAM, spriteram, spriteram_size ),
		new Memory_WriteAddress( 0xdc00, 0xdfff, MWA_RAM, mermaid_foreground_colorram ),
		new Memory_WriteAddress( 0xe000, 0xe001, MWA_RAM, mermaid_AY8910_enable ),
		new Memory_WriteAddress( 0xe007, 0xe007, interrupt_enable_w ),
		new Memory_WriteAddress( 0xe807, 0xe807, MWA_NOP ),	/* watchdog? */
		new Memory_WriteAddress( 0xf802, 0xf802, MWA_NOP ),	/* ??? see memory map */
		new Memory_WriteAddress( 0xf806, 0xf806, mermaid_AY8910_write_port_w ),
		new Memory_WriteAddress( 0xf807, 0xf807, mermaid_AY8910_control_port_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	INPUT_PORTS_START( mermaid )
		PORT_START      /* DSW */
		PORT_DIPNAME( 0x01, 0x00, DEF_STR( Cabinet ) )
		PORT_DIPSETTING(    0x00, DEF_STR( Upright ) )
		PORT_DIPSETTING(    0x01, DEF_STR( Cocktail ) )
		PORT_DIPNAME( 0x30, 0x00, DEF_STR( Lives ) )
		PORT_DIPSETTING(    0x00, "3" )
		PORT_DIPSETTING(    0x10, "4" )
		PORT_DIPSETTING(    0x20, "5" )
		PORT_DIPSETTING(    0x30, "6" )
	
		PORT_START      /* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP | IPF_8WAY )
		PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_8WAY )
		PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_8WAY )
		PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_8WAY )
		PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 )
		PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_START1 )
		PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_START2 )
	
		PORT_START      /* IN1 */
		PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_COIN1 )
		PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_COIN2 )
	
	INPUT_PORTS_END
	
	
	static GfxLayout background_charlayout = new GfxLayout
	(
		8,8,    /* 8*8 chars */
		256,    /* 256 characters */
		1,      /* 1 bit per pixel */
		new int[] { 0 },  /* single bitplane */
		new int[] { 0, 1, 2, 3, 4, 5, 6, 7},
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8},
		8*8     /* every char takes 8 consecutive bytes */
	);
	
	static GfxLayout foreground_charlayout = new GfxLayout
	(
		8,8,    /* 8*8 chars */
		1024,   /* 1024 characters */
		2,      /* 2 bits per pixel */
		new int[] { 0, 1024*8*8 },  /* the two bitplanes are separated */
		new int[] { 0, 1, 2, 3, 4, 5, 6, 7},
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8},
		8*8     /* every char takes 8 consecutive bytes */
	);
	
	static GfxLayout spritelayout = new GfxLayout
	(
		16,16,	/* 16*16 sprites */
		256,	/* 256 sprites */
		2,		/* 2 bits per pixel */
		new int[] { 0, 256*32*8 },	/* the two bitplanes are separated */
		new int[] { 0, 1, 2, 3, 4, 5, 6, 7,
		  8*8+0, 8*8+1, 8*8+2, 8*8+3, 8*8+4, 8*8+5, 8*8+6, 8*8+7 },
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8,
		  16*8, 17*8, 18*8, 19*8, 20*8, 21*8, 22*8, 23*8 },
		32*8	/* every sprite takes 32 consecutive bytes */
	);
	
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, foreground_charlayout,     0, 16 ),
		new GfxDecodeInfo( REGION_GFX1, 0, spritelayout,              0, 16 ),
		new GfxDecodeInfo( REGION_GFX2, 0, background_charlayout,  4*16, 2  ),
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	static struct AY8910interface ay8910_interface =
	{
		2,	/* 2 chips */
		1500000,	/* 1.5 MHz ? */
		{ 25, 25 },
		{ 0 },
		{ 0 },
		{ 0 },
		{ 0 }
	};
	
	
	static const struct MachineDriver machine_driver_mermaid =
	{
		/* basic machine hardware */
		{
			{
				CPU_Z80,
				4000000,        /* 4.00 MHz??? */
				readmem,writemem,0,0,
				nmi_interrupt,1
			}
		},
		60, DEFAULT_REAL_60HZ_VBLANK_DURATION,  /* frames per second, vblank duration */
		1,	/* 1 CPU slice per frame - interleaving is forced when a sound command is written */
		0,
	
		/* video hardware */
		32*8, 32*8, { 0*8, 32*8-1, 2*8, 30*8-1 },
		gfxdecodeinfo,
		4*16+1, 4*16+2*2,
		mermaid_vh_convert_color_prom,
	
		VIDEO_TYPE_RASTER,
		0,
		generic_vh_start,
		generic_vh_stop,
		mermaid_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		{
			{
				SOUND_AY8910,
				&ay8910_interface
			}
		}
	};
	
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	ROM_START( mermaid )
		ROM_REGION( 0x10000, REGION_CPU1, 0 )       /* 64k for code */
		ROM_LOAD( "g960_32.15",	  0x0000, 0x1000, 0x8311f090 )
		ROM_LOAD( "g960_33.16",	  0x1000, 0x1000, 0x9f274fc4 )
		ROM_LOAD( "g960_34.17",	  0x2000, 0x1000, 0x5f910179 )
		ROM_LOAD( "g960_35.18",	  0x3000, 0x1000, 0xdb1868a1 )
		ROM_LOAD( "g960_36.19",	  0x4000, 0x1000, 0x178a3567 )
		ROM_LOAD( "g960_37.20",	  0x5000, 0x1000, 0x7d602527 )
		ROM_LOAD( "g960_38.21",	  0x6000, 0x1000, 0xbf9f623c )
		ROM_LOAD( "g960_39.22",	  0x7000, 0x1000, 0xdf0db390 )
		ROM_LOAD( "g960_40.23",	  0x8000, 0x1000, 0xfb7aba3f )
		ROM_LOAD( "g960_41.24",	  0x9000, 0x1000, 0xd022981d )
	
		ROM_REGION( 0x4000, REGION_GFX1, ROMREGION_DISPOSE )
		ROM_LOAD( "g960_45.77",	  0x0000, 0x1000, 0x1f6b735e )
		ROM_LOAD( "g960_44.76",	  0x1000, 0x1000, 0xfd76074e )
		ROM_LOAD( "g960_47.79",	  0x2000, 0x1000, 0x3b7d4ad0 )
		ROM_LOAD( "g960_46.78",	  0x3000, 0x1000, 0x50c117cd )
	
		ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE )
		ROM_LOAD( "g960_43.26",	  0x0000, 0x1000, 0x6f077417 )
	
		ROM_REGION( 0x0040, REGION_PROMS, 0 )
		ROM_LOAD( "col_a", 	      0x0000, 0x0020, 0xef87bcd6 )
		ROM_LOAD( "col_b", 	      0x0020, 0x0020, 0xca48abdd )
	
		ROM_REGION( 0x1000, REGION_USER1, 0 )	/* unknown */
		ROM_LOAD( "g960_42.39",	  0x0000, 0x1000, 0x287840bb )
	ROM_END
	
	
	GAMEX( 1982, mermaid, 0, mermaid, mermaid, 0, ROT0, "Rock-ola", "Mermaid", GAME_NOT_WORKING )
}
