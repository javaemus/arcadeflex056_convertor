/***************************************************************************

	Prehistoric Isle in 1930 (World)		(c) 1989 SNK
	Prehistoric Isle in 1930 (USA)			(c) 1989 SNK
	Genshi-Tou 1930's (Japan)               (c) 1989 SNK

	Emulation by Bryan McPhail, mish@tendril.co.uk

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package drivers;

public class prehisle
{
	
	void prehisle_vh_screenrefresh(struct mame_bitmap *bitmap, int full_refresh);
	WRITE16_HANDLER( prehisle_video16_w );
	WRITE16_HANDLER( prehisle_control16_w );
	READ16_HANDLER( prehisle_control16_r );
	void prehisle_vh_stop (void);
	int prehisle_vh_start (void);
	
	static data16_t *prehisle_ram16;
	extern data16_t *prehisle_video16;
	
	/******************************************************************************/
	
	static WRITE16_HANDLER( prehisle_sound16_w )
	{
		soundlatch_w(0, data & 0xff);
		cpu_set_nmi_line(1, PULSE_LINE);
	}
	
	/*******************************************************************************/
	
	static MEMORY_READ16_START( prehisle_readmem )
		{ 0x000000, 0x03ffff, MRA16_ROM },
		{ 0x070000, 0x073fff, MRA16_RAM },
		{ 0x090000, 0x0907ff, MRA16_RAM },
		{ 0x0a0000, 0x0a07ff, MRA16_RAM },
		{ 0x0b0000, 0x0b3fff, MRA16_RAM },
		{ 0x0d0000, 0x0d07ff, MRA16_RAM },
		{ 0x0e0000, 0x0e00ff, prehisle_control16_r },
	MEMORY_END
	
	static MEMORY_WRITE16_START( prehisle_writemem )
		{ 0x000000, 0x03ffff, MWA16_ROM },
		{ 0x070000, 0x073fff, MWA16_RAM, &prehisle_ram16 },
		{ 0x090000, 0x0907ff, MWA16_RAM, &videoram16 },
		{ 0x0a0000, 0x0a07ff, MWA16_RAM, &spriteram16 },
		{ 0x0b0000, 0x0b3fff, prehisle_video16_w, &prehisle_video16 },
		{ 0x0d0000, 0x0d07ff, paletteram16_RRRRGGGGBBBBxxxx_word_w, &paletteram16 },
		{ 0x0f0070, 0x0ff071, prehisle_sound16_w },
		{ 0x0f0000, 0x0ff0ff, prehisle_control16_w },
	MEMORY_END
	
	/******************************************************************************/
	
	public static WriteHandlerPtr D7759_write_port_0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		UPD7759_reset_w (0,0);
		UPD7759_message_w(offset,data);
		UPD7759_start_w (0,0);
	} };
	
	public static Memory_ReadAddress prehisle_sound_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0xefff, MRA_ROM ),
		new Memory_ReadAddress( 0xf000, 0xf7ff, MRA_RAM ),
		new Memory_ReadAddress( 0xf800, 0xf800, soundlatch_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress prehisle_sound_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0xefff, MWA_ROM ),
		new Memory_WriteAddress( 0xf000, 0xf7ff, MWA_RAM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static IO_ReadPort prehisle_sound_readport[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_ReadPort( 0x00, 0x00, YM3812_status_port_0_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	
	public static IO_WritePort prehisle_sound_writeport[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_WritePort( 0x00, 0x00, YM3812_control_port_0_w ),
		new IO_WritePort( 0x20, 0x20, YM3812_write_port_0_w ),
		new IO_WritePort( 0x40, 0x40, D7759_write_port_0_w),
		new IO_WritePort( 0x80, 0x80, MWA_NOP ), /* IRQ ack? */
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	/******************************************************************************/
	
	static InputPortPtr input_ports_prehisle = new InputPortPtr(){ public void handler() { 
		PORT_START	/* Player 1 controls */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		PORT_START	/* Player 2 controls */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2  );
	
		PORT_START	/* coin */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN3 );
		PORT_BITX(0x08, IP_ACTIVE_LOW, IPT_SERVICE, DEF_STR( "Service_Mode") ); KEYCODE_F2, IP_JOY_NONE )
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_TILT );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START	/* Dip switches */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(	0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(	0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, "Level Select" );
		PORT_DIPSETTING(	0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(	0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(	0x04, "Only Twice" );
		PORT_DIPSETTING(	0x00, "Always" );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(	0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(	0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Coinage") );
		PORT_DIPSETTING(	0x00, "A 4/1 B 1/4" );
		PORT_DIPSETTING(	0x10, "A 3/1 B 1/3" );
		PORT_DIPSETTING(	0x20, "A 2/1 B 1/2" );
		PORT_DIPSETTING(	0x30, DEF_STR( "1C_1C") );
		PORT_DIPNAME( 0xc0, 0xc0, DEF_STR( "Lives") );
		PORT_DIPSETTING(	0x80, "2" );
		PORT_DIPSETTING(	0xc0, "3" );
		PORT_DIPSETTING(	0x40, "4" );
		PORT_DIPSETTING(	0x00, "5" );
	
		PORT_START
		PORT_DIPNAME( 0x03, 0x03, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(	0x02, "Easy" );
		PORT_DIPSETTING(	0x03, "Normal" );
		PORT_DIPSETTING(	0x01, "Hard" );
		PORT_DIPSETTING(	0x00, "Hardest" );
		PORT_DIPNAME( 0x0c, 0x0c, "Game Mode" );
		PORT_DIPSETTING(	0x08, "Demo Sounds Off" );
		PORT_DIPSETTING(	0x0c, "Demo Sounds On" );
		PORT_DIPSETTING(	0x00, "Freeze" );
		PORT_BITX( 0,		0x04, IPT_DIPSWITCH_SETTING | IPF_CHEAT, "Infinite Lives", IP_KEY_NONE, IP_JOY_NONE );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(	0x30, "100000 200000" );
		PORT_DIPSETTING(	0x20, "150000 300000" );
		PORT_DIPSETTING(	0x10, "300000 500000" );
		PORT_DIPSETTING(	0x00, "None" );
		PORT_DIPNAME( 0x40, 0x40, "Allow Continue" );
		PORT_DIPSETTING(	0x00, DEF_STR( "No") );
		PORT_DIPSETTING(	0x40, DEF_STR( "Yes") );
		PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_VBLANK );
	INPUT_PORTS_END(); }}; 
	
	/******************************************************************************/
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,	/* 8*8 characters */
		1024,
		4,		/* 4 bits per pixel */
		new int[] { 0, 1, 2, 3 },
		new int[] { 0, 4, 8, 12, 16, 20, 24, 28},
		new int[] { 0*32, 1*32, 2*32, 3*32, 4*32, 5*32, 6*32, 7*32 },
		32*8	/* every char takes 32 consecutive bytes */
	);
	
	static GfxLayout tilelayout = new GfxLayout
	(
		16,16,	/* 16*16 sprites */
		0x800,
		4,	/* 4 bits per pixel */
		new int[] { 0, 1, 2, 3 },
		new int[] { 0,4,8,12,16,20,24,28,
			0+64*8,4+64*8,8+64*8,12+64*8,16+64*8,20+64*8,24+64*8,28+64*8 },
		new int[] { 0*32, 1*32, 2*32, 3*32, 4*32, 5*32, 6*32, 7*32,
			8*32, 9*32, 10*32, 11*32, 12*32, 13*32, 14*32, 15*32 },
		128*8	/* every sprite takes 64 consecutive bytes */
	);
	
	static GfxLayout spritelayout = new GfxLayout
	(
		16,16,	/* 16*16 sprites */
		5120,
		4,	/* 4 bits per pixel */
		new int[] { 0, 1, 2, 3 },
		new int[] { 0,4,8,12,16,20,24,28,
			0+64*8,4+64*8,8+64*8,12+64*8,16+64*8,20+64*8,24+64*8,28+64*8 },
		new int[] { 0*32, 1*32, 2*32, 3*32, 4*32, 5*32, 6*32, 7*32,
			8*32, 9*32, 10*32, 11*32, 12*32, 13*32, 14*32, 15*32 },
		128*8	/* every sprite takes 64 consecutive bytes */
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, charlayout,	 0, 16 ),
		new GfxDecodeInfo( REGION_GFX2, 0, tilelayout, 768, 16 ),
		new GfxDecodeInfo( REGION_GFX3, 0, tilelayout, 512, 16 ),
		new GfxDecodeInfo( REGION_GFX4, 0, spritelayout, 256, 16 ),
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	/******************************************************************************/
	
	static void irqhandler(int irq)
	{
		cpu_set_irq_line(1,0,irq ? ASSERT_LINE : CLEAR_LINE);
	}
	
	static struct YM3812interface ym3812_interface =
	{
		1,			/* 1 chip */
		4000000,	/* 4 MHz */
		{ 50 },
		{ irqhandler },
	};
	
	static struct UPD7759_interface upd7759_interface =
	{
		1,							/* number of chips */
		UPD7759_STANDARD_CLOCK,
		{ 50 }, 					/* volume */
		{ REGION_SOUND1 },			/* memory region */
		UPD7759_STANDALONE_MODE,	/* chip mode */
		{ NULL }
	};
	
	/******************************************************************************/
	
	static MachineDriver machine_driver_prehisle = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_M68000,
				12000000,
				prehisle_readmem,prehisle_writemem,null,null,
				m68_level4_irq,1
			),
			new MachineCPU(
				CPU_Z80 | CPU_AUDIO_CPU,
				4000000,
				prehisle_sound_readmem,prehisle_sound_writemem,
				prehisle_sound_readport,prehisle_sound_writeport,
				ignore_interrupt,0
			)
		},
		60, DEFAULT_REAL_60HZ_VBLANK_DURATION,
		1,
		null,
	
		/* video hardware */
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 2*8, 30*8-1 ),
	
		gfxdecodeinfo,
		1024, null,
		0,
	
		VIDEO_TYPE_RASTER,
		null,
		prehisle_vh_start,
		prehisle_vh_stop,
		prehisle_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_YM3812,
				ym3812_interface
			),
			new MachineSound(
				SOUND_UPD7759,
				upd7759_interface
			)
		}
	);
	
	/******************************************************************************/
	
	static RomLoadPtr rom_prehisle = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x40000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "gt.2", 0x00000, 0x20000, 0x7083245a );
		ROM_LOAD16_BYTE( "gt.3", 0x00001, 0x20000, 0x6d8cdf58 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* Sound CPU */
		ROM_LOAD( "gt.1",  0x000000, 0x10000, 0x80a4c093 );
	
		ROM_REGION( 0x008000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "gt15.b15",   0x000000, 0x08000, 0xac652412 );
	
		ROM_REGION( 0x040000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "pi8914.b14", 0x000000, 0x40000, 0x207d6187 );
	
		ROM_REGION( 0x040000, REGION_GFX3, ROMREGION_DISPOSE );
		ROM_LOAD( "pi8916.h16", 0x000000, 0x40000, 0x7cffe0f6 );
	
		ROM_REGION( 0x0a0000, REGION_GFX4, ROMREGION_DISPOSE );
		ROM_LOAD( "pi8910.k14", 0x000000, 0x80000, 0x5a101b0b );
		ROM_LOAD( "gt.5",       0x080000, 0x20000, 0x3d3ab273 );
	
		ROM_REGION( 0x10000, REGION_GFX5, 0 );/* background tilemaps */
		ROM_LOAD( "gt.11",  0x000000, 0x10000, 0xb4f0fcf0 );
	
		ROM_REGION( 0x20000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "gt.4",  0x000000, 0x20000, 0x85dfb9ec );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_prehislu = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x40000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "gt-u2.2h", 0x00000, 0x20000, 0xa14f49bb );
		ROM_LOAD16_BYTE( "gt-u3.3h", 0x00001, 0x20000, 0xf165757e );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* Sound CPU */
		ROM_LOAD( "gt.1",  0x000000, 0x10000, 0x80a4c093 );
	
		ROM_REGION( 0x008000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "gt15.b15",   0x000000, 0x08000, 0xac652412 );
	
		ROM_REGION( 0x040000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "pi8914.b14", 0x000000, 0x40000, 0x207d6187 );
	
		ROM_REGION( 0x040000, REGION_GFX3, ROMREGION_DISPOSE );
		ROM_LOAD( "pi8916.h16", 0x000000, 0x40000, 0x7cffe0f6 );
	
		ROM_REGION( 0x0a0000, REGION_GFX4, ROMREGION_DISPOSE );
		ROM_LOAD( "pi8910.k14", 0x000000, 0x80000, 0x5a101b0b );
		ROM_LOAD( "gt.5",       0x080000, 0x20000, 0x3d3ab273 );
	
		ROM_REGION( 0x10000, REGION_GFX5, 0 );/* background tilemaps */
		ROM_LOAD( "gt.11",  0x000000, 0x10000, 0xb4f0fcf0 );
	
		ROM_REGION( 0x20000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "gt.4",  0x000000, 0x20000, 0x85dfb9ec );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_gensitou = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x40000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "gt2j.bin", 0x00000, 0x20000, 0xa2da0b6b );
		ROM_LOAD16_BYTE( "gt3j.bin", 0x00001, 0x20000, 0xc1a0ae8e );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* Sound CPU */
		ROM_LOAD( "gt.1",  0x000000, 0x10000, 0x80a4c093 );
	
		ROM_REGION( 0x008000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "gt15.b15",   0x000000, 0x08000, 0xac652412 );
	
		ROM_REGION( 0x040000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "pi8914.b14", 0x000000, 0x40000, 0x207d6187 );
	
		ROM_REGION( 0x040000, REGION_GFX3, ROMREGION_DISPOSE );
		ROM_LOAD( "pi8916.h16", 0x000000, 0x40000, 0x7cffe0f6 );
	
		ROM_REGION( 0x0a0000, REGION_GFX4, ROMREGION_DISPOSE );
		ROM_LOAD( "pi8910.k14", 0x000000, 0x80000, 0x5a101b0b );
		ROM_LOAD( "gt.5",       0x080000, 0x20000, 0x3d3ab273 );
	
		ROM_REGION( 0x10000, REGION_GFX5, 0 );/* background tilemaps */
		ROM_LOAD( "gt.11",  0x000000, 0x10000, 0xb4f0fcf0 );
	
		ROM_REGION( 0x20000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "gt.4",  0x000000, 0x20000, 0x85dfb9ec );
	ROM_END(); }}; 
	
	/******************************************************************************/
	
	static READ16_HANDLER( world_cycle_r )
	{
		int pc=cpu_get_pc();
		int ret=prehisle_ram16[0x12];
	
		if ((ret&0x8000) && (pc==0x260c || pc==0x268a || pc==0x2b0a || pc==0x34a8 || pc==0x6ae4 || pc==0x83ac || pc==0x25ce || pc==0x29c4)) {
			cpu_spinuntil_int();
			return ret&0x7fff;
		}
		return ret;
	}
	
	static void init_prehisle(void)
	{
		install_mem_read16_handler(0, 0x70024, 0x70025, world_cycle_r);
	}
	
	static READ16_HANDLER( usa_cycle_r )
	{
		int pc=cpu_get_pc();
		int ret=prehisle_ram16[0x12];
	
		if ((ret&0x8000) && (pc==0x281e || pc==0x28a6 || pc==0x295a || pc==0x2868 || pc==0x8f98 || pc==0x3b1e)) {
			cpu_spinuntil_int();
			return ret&0x7fff;
		}
		return ret;
	}
	
	static void init_prehislu(void)
	{
		install_mem_read16_handler(0, 0x70024, 0x70025, usa_cycle_r);
	}
	
	static READ16_HANDLER( jap_cycle_r )
	{
		int pc=cpu_get_pc();
		int ret=prehisle_ram16[0x12];
	
		if ((ret&0x8000) && (pc==0x34b6 /* Todo! */ )) {
			cpu_spinuntil_int();
			return ret&0x7fff;
		}
		return ret;
	}
	
	static void init_gensitou(void)
	{
		install_mem_read16_handler(0, 0x70024, 0x70025, jap_cycle_r);
	}
	
	/******************************************************************************/
	
	public static GameDriver driver_prehisle	   = new GameDriver("1989"	,"prehisle"	,"prehisle.java"	,rom_prehisle,null	,machine_driver_prehisle	,input_ports_prehisle	,init_prehisle	,ROT0	,	"SNK", "Prehistoric Isle in 1930 (World)", GAME_NO_COCKTAIL )
	public static GameDriver driver_prehislu	   = new GameDriver("1989"	,"prehislu"	,"prehisle.java"	,rom_prehislu,driver_prehisle	,machine_driver_prehisle	,input_ports_prehisle	,init_prehislu	,ROT0	,	"SNK of America", "Prehistoric Isle in 1930 (US)", GAME_NO_COCKTAIL )
	public static GameDriver driver_gensitou	   = new GameDriver("1989"	,"gensitou"	,"prehisle.java"	,rom_gensitou,driver_prehisle	,machine_driver_prehisle	,input_ports_prehisle	,init_gensitou	,ROT0	,	"SNK", "Genshi-Tou 1930's", GAME_NO_COCKTAIL )
}
