/***************************************************************************


***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package drivers;

public class citycon
{
	
	
	extern data8_t *citycon_videoram;
	extern data8_t *citycon_scroll;
	extern data8_t *citycon_linecolor;
	WRITE_HANDLER( citycon_videoram_w );
	WRITE_HANDLER( citycon_linecolor_w );
	WRITE_HANDLER( citycon_background_w );
	
	void citycon_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh);
	int  citycon_vh_start(void);
	void citycon_vh_stop(void);
	
	
	READ_HANDLER( citycon_in_r )
	{
		return readinputport(flip_screen ? 1 : 0);
	}
	
	
	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x1fff, MRA_RAM ),
		new Memory_ReadAddress( 0x3000, 0x3000, citycon_in_r ),	/* player 1  2 inputs multiplexed */
		new Memory_ReadAddress( 0x3001, 0x3001, input_port_2_r ),
		new Memory_ReadAddress( 0x3002, 0x3002, input_port_3_r ),
		new Memory_ReadAddress( 0x3007, 0x3007, watchdog_reset_r ),	/* ? */
		new Memory_ReadAddress( 0x4000, 0xffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x0fff, MWA_RAM ),
		new Memory_WriteAddress( 0x1000, 0x1fff, citycon_videoram_w, citycon_videoram ),
		new Memory_WriteAddress( 0x2000, 0x20ff, citycon_linecolor_w, citycon_linecolor ),
		new Memory_WriteAddress( 0x2800, 0x28ff, MWA_RAM, spriteram, spriteram_size ),
		new Memory_WriteAddress( 0x3000, 0x3000, citycon_background_w ),
		new Memory_WriteAddress( 0x3001, 0x3001, soundlatch_w ),
		new Memory_WriteAddress( 0x3002, 0x3002, soundlatch2_w ),
		new Memory_WriteAddress( 0x3004, 0x3005, MWA_RAM, citycon_scroll ),
		new Memory_WriteAddress( 0x3800, 0x3cff, paletteram_RRRRGGGGBBBBxxxx_swap_w, paletteram ),
		new Memory_WriteAddress( 0x4000, 0xffff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress readmem_sound[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x0fff, MRA_RAM ),
	//	new Memory_ReadAddress( 0x4002, 0x4002, AY8910_read_port_0_r ),	/* ?? */
		new Memory_ReadAddress( 0x6001, 0x6001, YM2203_read_port_0_r ),
		new Memory_ReadAddress( 0x8000, 0xffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem_sound[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x0fff, MWA_RAM ),
		new Memory_WriteAddress( 0x4000, 0x4000, AY8910_control_port_0_w ),
		new Memory_WriteAddress( 0x4001, 0x4001, AY8910_write_port_0_w ),
		new Memory_WriteAddress( 0x6000, 0x6000, YM2203_control_port_0_w ),
		new Memory_WriteAddress( 0x6001, 0x6001, YM2203_write_port_0_w ),
		new Memory_WriteAddress( 0x8000, 0xffff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	
	INPUT_PORTS_START( citycon )
		PORT_START	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY )
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY )
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY )
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY )
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 )
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START1 )
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 )
	
		PORT_START
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_COCKTAIL )
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_COCKTAIL )
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_COCKTAIL )
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_COCKTAIL )
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL )
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_COCKTAIL )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNUSED )
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED )
	
		PORT_START
		PORT_DIPNAME( 0x03, 0x00, DEF_STR( Lives ) )
		PORT_DIPSETTING(    0x00, "3" )
		PORT_DIPSETTING(    0x01, "4" )
		PORT_DIPSETTING(    0x02, "5" )
		PORT_BITX( 0,       0x03, IPT_DIPSWITCH_SETTING | IPF_CHEAT, "Infinite", IP_KEY_NONE, IP_JOY_NONE )
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( Unknown ) )
		PORT_DIPSETTING(    0x00, DEF_STR( Off ) )
		PORT_DIPSETTING(    0x04, DEF_STR( On ) )
		PORT_DIPNAME( 0x08, 0x00, DEF_STR( Unknown ) )
		PORT_DIPSETTING(    0x00, DEF_STR( Off ) )
		PORT_DIPSETTING(    0x08, DEF_STR( On ) )
		PORT_DIPNAME( 0x10, 0x00, DEF_STR( Unknown ) )
		PORT_DIPSETTING(    0x00, DEF_STR( Off ) )
		PORT_DIPSETTING(    0x10, DEF_STR( On ) )
		PORT_DIPNAME( 0x20, 0x00, DEF_STR( Demo_Sounds ) )
		PORT_DIPSETTING(    0x20, DEF_STR( Off ) )
		PORT_DIPSETTING(    0x00, DEF_STR( On ) )
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( Cabinet ) )
		PORT_DIPSETTING(    0x00, DEF_STR( Upright ) )
		PORT_DIPSETTING(    0x40, DEF_STR( Cocktail ) )
		/* the coin input must stay low for exactly 2 frames to be consistently recognized. */
		PORT_BIT_IMPULSE( 0x80, IP_ACTIVE_LOW, IPT_COIN1, 2 )
	
		PORT_START
		PORT_DIPNAME( 0x07, 0x00, DEF_STR( Coinage ) )
		PORT_DIPSETTING(    0x07, DEF_STR( 5C_1C ) )
		PORT_DIPSETTING(    0x06, DEF_STR( 4C_1C ) )
		PORT_DIPSETTING(    0x05, DEF_STR( 3C_1C ) )
		PORT_DIPSETTING(    0x04, DEF_STR( 2C_1C ) )
		PORT_DIPSETTING(    0x00, DEF_STR( 1C_1C ) )
		PORT_DIPSETTING(    0x01, DEF_STR( 1C_2C ) )
		PORT_DIPSETTING(    0x02, DEF_STR( 1C_3C ) )
		PORT_DIPSETTING(    0x03, DEF_STR( 1C_4C ) )
		PORT_DIPNAME( 0x08, 0x00, DEF_STR( Unknown ) )
		PORT_DIPSETTING(    0x00, DEF_STR( Off ) )
		PORT_DIPSETTING(    0x08, DEF_STR( On ) )
		PORT_DIPNAME( 0x10, 0x00, DEF_STR( Unknown ) )
		PORT_DIPSETTING(    0x00, DEF_STR( Off ) )
		PORT_DIPSETTING(    0x10, DEF_STR( On ) )
		PORT_DIPNAME( 0x20, 0x00, DEF_STR( Unknown ) )
		PORT_DIPSETTING(    0x00, DEF_STR( Off ) )
		PORT_DIPSETTING(    0x20, DEF_STR( On ) )
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( Unknown ) )
		PORT_DIPSETTING(    0x00, DEF_STR( Off ) )
		PORT_DIPSETTING(    0x40, DEF_STR( On ) )
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( Unknown ) )
		PORT_DIPSETTING(    0x80, DEF_STR( Off ) )
		PORT_DIPSETTING(    0x00, DEF_STR( On ) )
	INPUT_PORTS_END
	
	
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,
		RGN_FRAC(1,2),
		5,
		new int[] { 16, 12, 8, 4, 0 },
		new int[] { 0, 1, 2, 3, RGN_FRAC(1,2)+0, RGN_FRAC(1,2)+1, RGN_FRAC(1,2)+2, RGN_FRAC(1,2)+3 },
		new int[] { 0*24, 1*24, 2*24, 3*24, 4*24, 5*24, 6*24, 7*24 },
		24*8
	);
	
	static GfxLayout tilelayout = new GfxLayout
	(
		8,8,	/* 8*8 characters */
		256,	/* 256 characters */
		4,	/* 4 bits per pixel */
		new int[] { 4, 0, 0xc000*8+4, 0xc000*8+0 },
		new int[] { 0, 1, 2, 3, 256*8*8+0, 256*8*8+1, 256*8*8+2, 256*8*8+3 },
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
		8*8	/* every char takes 8 consecutive bytes */
	);
	
	static GfxLayout spritelayout = new GfxLayout
	(
		8,16,	/* 8*16 sprites */
		128,	/* 128 sprites */
		4,	/* 4 bits per pixel */
		new int[] { 4, 0, 0x2000*8+4, 0x2000*8+0 },
		new int[] { 0, 1, 2, 3, 128*16*8+0, 128*16*8+1, 128*16*8+2, 128*16*8+3 },
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8,
	            8*8, 9*8, 10*8, 11*8, 12*8, 13*8, 14*8, 15*8 },
		16*8	/* every sprite takes 16 consecutive bytes */
	);
	
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
	//	new GfxDecodeInfo( REGION_GFX1, 0x00000, charlayout, 512, 32 ),	/* colors 512-639 */
		new GfxDecodeInfo( REGION_GFX1, 0x00000, charlayout, 640, 32 ),	/* colors 512-639 */
		new GfxDecodeInfo( REGION_GFX2, 0x00000, spritelayout, 0, 16 ),	/* colors 0-255 */
		new GfxDecodeInfo( REGION_GFX2, 0x01000, spritelayout, 0, 16 ),
		new GfxDecodeInfo( REGION_GFX3, 0x00000, tilelayout, 256, 16 ),	/* colors 256-511 */
		new GfxDecodeInfo( REGION_GFX3, 0x01000, tilelayout, 256, 16 ),
		new GfxDecodeInfo( REGION_GFX3, 0x02000, tilelayout, 256, 16 ),
		new GfxDecodeInfo( REGION_GFX3, 0x03000, tilelayout, 256, 16 ),
		new GfxDecodeInfo( REGION_GFX3, 0x04000, tilelayout, 256, 16 ),
		new GfxDecodeInfo( REGION_GFX3, 0x05000, tilelayout, 256, 16 ),
		new GfxDecodeInfo( REGION_GFX3, 0x06000, tilelayout, 256, 16 ),
		new GfxDecodeInfo( REGION_GFX3, 0x07000, tilelayout, 256, 16 ),
		new GfxDecodeInfo( REGION_GFX3, 0x08000, tilelayout, 256, 16 ),
		new GfxDecodeInfo( REGION_GFX3, 0x09000, tilelayout, 256, 16 ),
		new GfxDecodeInfo( REGION_GFX3, 0x0a000, tilelayout, 256, 16 ),
		new GfxDecodeInfo( REGION_GFX3, 0x0b000, tilelayout, 256, 16 ),
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	
	static struct AY8910interface ay8910_interface =
	{
		1,			/* 1 chip */
		1250000,	/* 1.25 MHz */
		{ MIXERG(20,MIXER_GAIN_2x,MIXER_PAN_CENTER) },
		{ 0 },
		{ 0 },
		{ 0 },
		{ 0 }
	};
	
	static struct YM2203interface ym2203_interface =
	{
		1,			/* 1 chip */
		1250000,	/* 1.25 MHz */
		{ YM2203_VOL(20,MIXERG(20,MIXER_GAIN_2x,MIXER_PAN_CENTER)) },
		{ soundlatch_r },
		{ soundlatch2_r },
		{ 0 },
		{ 0 }
	};
	
	
	
	static const struct MachineDriver machine_driver_citycon =
	{
		/* basic machine hardware */
		{
			{
				CPU_M6809,
				2048000,        /* 2.048 MHz ??? */
				readmem,writemem,0,0,
				interrupt,1
			},
			{
				CPU_M6809 | CPU_AUDIO_CPU,
				640000,        /* 0.640 MHz ??? */
				readmem_sound,writemem_sound,0,0,
				interrupt,1
			}
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		1,	/* 1 CPU slice per frame - interleaving is forced when a sound command is written */
		0,
	
		/* video hardware */
		32*8, 32*8, { 1*8, 31*8-1, 2*8, 30*8-1 },
		gfxdecodeinfo,
		640+1024, 0,	/* 640 real palette + 1024 virtual palette */
		0,
	
		VIDEO_TYPE_RASTER,
		0,
		citycon_vh_start,
		0,
		citycon_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		{
			{
				SOUND_AY8910,
				&ay8910_interface
			},
			{
				SOUND_YM2203,
				&ym2203_interface
			}
		}
	};
	
	
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	ROM_START( citycon )
		ROM_REGION( 0x10000, REGION_CPU1, 0 )     /* 64k for code */
		ROM_LOAD( "c10",          0x4000, 0x4000, 0xae88b53c )
		ROM_LOAD( "c11",          0x8000, 0x8000, 0x139eb1aa )
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 )	/* 64k for the audio CPU */
		ROM_LOAD( "c1",           0x8000, 0x8000, 0x1fad7589 )
	
		ROM_REGION( 0x03000, REGION_GFX1, ROMREGION_DISPOSE )
		ROM_LOAD( "c4",           0x00000, 0x2000, 0xa6b32fc6 )	/* Characters */
	
		ROM_REGION( 0x04000, REGION_GFX2, ROMREGION_DISPOSE )
		ROM_LOAD( "c12",          0x00000, 0x2000, 0x08eaaccd )	/* Sprites    */
		ROM_LOAD( "c13",          0x02000, 0x2000, 0x1819aafb )
	
		ROM_REGION( 0x18000, REGION_GFX3, ROMREGION_DISPOSE )
		ROM_LOAD( "c9",           0x00000, 0x8000, 0x8aeb47e6 )	/* Background tiles */
		ROM_LOAD( "c8",           0x08000, 0x4000, 0x0d7a1eeb )
		ROM_LOAD( "c6",           0x0c000, 0x8000, 0x2246fe9d )
		ROM_LOAD( "c7",           0x14000, 0x4000, 0xe8b97de9 )
	
		ROM_REGION( 0xe000, REGION_GFX4, 0 )	/* background tilemaps */
		ROM_LOAD( "c2",           0x0000, 0x8000, 0xf2da4f23 )	/* background maps */
		ROM_LOAD( "c3",           0x8000, 0x4000, 0x7ef3ac1b )
		ROM_LOAD( "c5",           0xc000, 0x2000, 0xc03d8b1b )	/* color codes for the background */
	ROM_END
	
	ROM_START( citycona )
		ROM_REGION( 0x10000, REGION_CPU1, 0 )     /* 64k for code */
		ROM_LOAD( "c10",          0x4000, 0x4000, 0xae88b53c )
		ROM_LOAD( "c11b",         0x8000, 0x8000, 0xd64af468 )
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 )	/* 64k for the audio CPU */
		ROM_LOAD( "c1",           0x8000, 0x8000, 0x1fad7589 )
	
		ROM_REGION( 0x03000, REGION_GFX1, ROMREGION_DISPOSE )
		ROM_LOAD( "c4",           0x00000, 0x2000, 0xa6b32fc6 )	/* Characters */
	
		ROM_REGION( 0x04000, REGION_GFX2, ROMREGION_DISPOSE )
		ROM_LOAD( "c12",          0x00000, 0x2000, 0x08eaaccd )	/* Sprites    */
		ROM_LOAD( "c13",          0x02000, 0x2000, 0x1819aafb )
	
		ROM_REGION( 0x18000, REGION_GFX3, ROMREGION_DISPOSE )
		ROM_LOAD( "c9",           0x00000, 0x8000, 0x8aeb47e6 )	/* Background tiles */
		ROM_LOAD( "c8",           0x08000, 0x4000, 0x0d7a1eeb )
		ROM_LOAD( "c6",           0x0c000, 0x8000, 0x2246fe9d )
		ROM_LOAD( "c7",           0x14000, 0x4000, 0xe8b97de9 )
	
		ROM_REGION( 0xe000, REGION_GFX4, 0 )	/* background tilemaps */
		ROM_LOAD( "c2",           0x0000, 0x8000, 0xf2da4f23 )	/* background maps */
		ROM_LOAD( "c3",           0x8000, 0x4000, 0x7ef3ac1b )
		ROM_LOAD( "c5",           0xc000, 0x2000, 0xc03d8b1b )	/* color codes for the background */
	ROM_END
	
	ROM_START( cruisin )
		ROM_REGION( 0x10000, REGION_CPU1, 0 )     /* 64k for code */
		ROM_LOAD( "cr10",         0x4000, 0x4000, 0xcc7c52f3 )
		ROM_LOAD( "cr11",         0x8000, 0x8000, 0x5422f276 )
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 )	/* 64k for the audio CPU */
		ROM_LOAD( "c1",           0x8000, 0x8000, 0x1fad7589 )
	
		ROM_REGION( 0x03000, REGION_GFX1, ROMREGION_DISPOSE )
		ROM_LOAD( "cr4",          0x00000, 0x2000, 0x8cd0308e )	/* Characters */
	
		ROM_REGION( 0x04000, REGION_GFX2, ROMREGION_DISPOSE )
		ROM_LOAD( "c12",          0x00000, 0x2000, 0x08eaaccd )	/* Sprites    */
		ROM_LOAD( "c13",          0x02000, 0x2000, 0x1819aafb )
	
		ROM_REGION( 0x18000, REGION_GFX3, ROMREGION_DISPOSE )
		ROM_LOAD( "c9",           0x00000, 0x8000, 0x8aeb47e6 )	/* Background tiles */
		ROM_LOAD( "c8",           0x08000, 0x4000, 0x0d7a1eeb )
		ROM_LOAD( "c6",           0x0c000, 0x8000, 0x2246fe9d )
		ROM_LOAD( "c7",           0x14000, 0x4000, 0xe8b97de9 )
	
		ROM_REGION( 0xe000, REGION_GFX4, 0 )	/* background tilemaps */
		ROM_LOAD( "c2",           0x0000, 0x8000, 0xf2da4f23 )	/* background maps */
		ROM_LOAD( "c3",           0x8000, 0x4000, 0x7ef3ac1b )
		ROM_LOAD( "c5",           0xc000, 0x2000, 0xc03d8b1b )	/* color codes for the background */
	ROM_END
	
	
	
	static void init_citycon(void)
	{
		UINT8 *rom = memory_region(REGION_GFX1);
		int i;
	
	
		/*
		  City Connection controls the text color code for each _scanline_, not
		  for each character as happens in most games. To handle that conveniently,
		  I convert the 2bpp char data into 5bpp, and create a virtual palette so
		  characters can still be drawn in one pass.
		  */
		for (i = 0x0fff;i >= 0;i--)
		{
			int mask;
	
			rom[3*i] = rom[i];
			rom[3*i+1] = 0;
			rom[3*i+2] = 0;
			mask = rom[i] | (rom[i] << 4) | (rom[i] >> 4);
			if (i & 0x01) rom[3*i+1] |= mask & 0xf0;
			if (i & 0x02) rom[3*i+1] |= mask & 0x0f;
			if (i & 0x04) rom[3*i+2] |= mask & 0xf0;
		}
	}
	
	
	
	GAME( 1985, citycon,  0,       citycon, citycon, citycon, ROT0, "Jaleco", "City Connection (set 1)" )
	GAME( 1985, citycona, citycon, citycon, citycon, citycon, ROT0, "Jaleco", "City Connection (set 2)" )
	GAME( 1985, cruisin,  citycon, citycon, citycon, citycon, ROT0, "Jaleco (Kitkorp license)", "Cruisin" )
}
