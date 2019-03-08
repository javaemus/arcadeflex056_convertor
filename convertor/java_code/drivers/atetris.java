/***************************************************************************

	Atari Tetris hardware

	driver by Zsolt Vasvari

	Games supported:
		* Tetris

	Known bugs:
		* none at this time

****************************************************************************

	Memory map

****************************************************************************

	========================================================================
	CPU #1
	========================================================================
	0000-0FFF   R/W   xxxxxxxx    Program RAM
	1000-1FFF   R/W   xxxxxxxx    Playfield RAM
	                  xxxxxxxx       (byte 0: LSB of character code)
	                  -----xxx       (byte 1: MSB of character code)
	                  xxxx----       (byte 1: palette index)
	2000-20FF   R/W   xxxxxxxx    Palette RAM
	                  xxx----        (red component)
	                  ---xxx--       (green component)
	                  ------xx       (blue component)
	2400-25FF   R/W   xxxxxxxx    EEPROM
	2800-280F   R/W   xxxxxxxx    POKEY #1
	2810-281F   R/W   xxxxxxxx    POKEY #2
	3000          W   --------    Watchdog
	3400          W   --------    EEPROM write enable
	3800          W   --------    IRQ acknowledge
	3C00          W   --xx----    Coin counters
	              W   --x-----       (right coin counter)
	              W   ---x----       (left coin counter)
	4000-7FFF   R     xxxxxxxx    Banked program ROM
	8000-FFFF   R     xxxxxxxx    Program ROM
	========================================================================
	Interrupts:
		IRQ generated by 32V
	========================================================================

***************************************************************************/


/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package drivers;

public class atetris
{
	
	
	#define ATARI_CLOCK_14MHz	14318180
	
	
	
	
	
	
	/* Local variables */
	static UINT8 *nvram;
	static size_t nvram_size;
	static UINT8 *slapstic_source;
	static UINT8 *slapstic_base;
	static UINT8 current_bank;
	
	static UINT8 nvram_write_enable;
	
	
	
	/*************************************
	 *
	 *	Interrupt generation
	 *
	 *************************************/
	
	static void interrupt_gen(int scanline)
	{
		/* assert/deassert the interrupt */
		cpu_set_irq_line(0, 0, (scanline & 32) ? ASSERT_LINE : CLEAR_LINE);
	
		/* set the next timer */
		scanline += 32;
		if (scanline >= 256)
			scanline -= 256;
		timer_set(cpu_getscanlinetime(scanline), scanline, interrupt_gen);
	}
	
	
	public static WriteHandlerPtr irq_ack_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		cpu_set_irq_line(0, 0, CLEAR_LINE);
	} };
	
	
	
	/*************************************
	 *
	 *	Machine init
	 *
	 *************************************/
	
	static void init_machine(void)
	{
		/* reset the slapstic */
		slapstic_reset();
		current_bank = slapstic_bank() & 1;
		memcpy(slapstic_base, &slapstic_source[current_bank * 0x4000], 0x4000);
	
		/* start interrupts going (32V clocked by 16V) */
		timer_set(cpu_getscanlinetime(48), 48, interrupt_gen);
	}
	
	
	
	/*************************************
	 *
	 *	Slapstic handler
	 *
	 *************************************/
	
	public static ReadHandlerPtr atetris_slapstic_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int result = slapstic_base[0x2000 + offset];
		int new_bank = slapstic_tweak(offset) & 1;
	
		/* update for the new bank */
		if (new_bank != current_bank)
		{
			current_bank = new_bank;
			memcpy(slapstic_base, &slapstic_source[current_bank * 0x4000], 0x4000);
		}
		return result;
	} };
	
	
	
	/*************************************
	 *
	 *	Coin counters
	 *
	 *************************************/
	
	public static WriteHandlerPtr coincount_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		coin_counter_w(0, (data >> 5) & 1);
		coin_counter_w(1, (data >> 4) & 1);
	} };
	
	
	
	/*************************************
	 *
	 *	NVRAM handlers
	 *
	 *************************************/
	
	public static WriteHandlerPtr nvram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (nvram_write_enable)
			nvram[offset] = data;
		nvram_write_enable = 0;
	} };
	
	
	public static WriteHandlerPtr nvram_enable_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		nvram_write_enable = 1;
	} };
	
	
	
	/*************************************
	 *
	 *	NVRAM handler
	 *
	 *************************************/
	
	static void nvram_handler(void *file,int read_or_write)
	{
		if (read_or_write)
			osd_fwrite(file, nvram, nvram_size);
		else if (file)
			osd_fread(file, nvram, nvram_size);
		else
			memset(nvram, 0xff, nvram_size);
	}
	
	
	
	/*************************************
	 *
	 *	Main CPU memory handlers
	 *
	 *************************************/
	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x20ff, MRA_RAM ),
		new Memory_ReadAddress( 0x2400, 0x25ff, MRA_RAM ),
		new Memory_ReadAddress( 0x2800, 0x280f, pokey1_r ),
		new Memory_ReadAddress( 0x2810, 0x281f, pokey2_r ),
		new Memory_ReadAddress( 0x4000, 0x5fff, MRA_ROM ),
		new Memory_ReadAddress( 0x6000, 0x7fff, atetris_slapstic_r ),
		new Memory_ReadAddress( 0x8000, 0xffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x0fff, MWA_RAM ),
		new Memory_WriteAddress( 0x1000, 0x1fff, atetris_videoram_w, videoram, videoram_size ),
		new Memory_WriteAddress( 0x2000, 0x20ff, paletteram_RRRGGGBB_w, paletteram ),
		new Memory_WriteAddress( 0x2400, 0x25ff, nvram_w, nvram, nvram_size ),
		new Memory_WriteAddress( 0x2800, 0x280f, pokey1_w ),
		new Memory_WriteAddress( 0x2810, 0x281f, pokey2_w ),
		new Memory_WriteAddress( 0x3000, 0x3000, watchdog_reset_w ),
		new Memory_WriteAddress( 0x3400, 0x3400, nvram_enable_w ),
		new Memory_WriteAddress( 0x3800, 0x3800, irq_ack_w ),
		new Memory_WriteAddress( 0x3c00, 0x3c00, coincount_w ),
		new Memory_WriteAddress( 0x4000, 0xffff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	
	/*************************************
	 *
	 *	Port definitions
	 *
	 *************************************/
	
	static InputPortPtr input_ports_atetris = new InputPortPtr(){ public void handler() { 
		// These ports are read via the Pokeys
		PORT_START();       /* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_COIN2 );
		PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_COIN1 );
		PORT_DIPNAME( 0x04, 0x00, "Freeze" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_BITX(0x08, IP_ACTIVE_HIGH, IPT_SERVICE, "Freeze Step", KEYCODE_F1, IP_JOY_NONE );
		PORT_BIT( 0x30, IP_ACTIVE_HIGH, IPT_UNUSED );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_VBLANK );
		PORT_SERVICE( 0x80, IP_ACTIVE_HIGH );
	
		PORT_START();       /* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER1);
		PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2);
		PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_PLAYER2 | IPF_8WAY );
		PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_PLAYER2 | IPF_8WAY );
		PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_PLAYER2 | IPF_8WAY );
	INPUT_PORTS_END(); }}; 
	
	
	// Same as the regular one except they added a Flip Controls switch
	static InputPortPtr input_ports_atetcktl = new InputPortPtr(){ public void handler() { 
		// These ports are read via the Pokeys
		PORT_START();       /* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_COIN2 );
		PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_COIN1 );
		PORT_DIPNAME( 0x04, 0x00, "Freeze" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_BITX(0x08, IP_ACTIVE_HIGH, IPT_SERVICE, "Freeze Step", KEYCODE_F1, IP_JOY_NONE );
		PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_UNUSED );
		PORT_DIPNAME( 0x20, 0x00, "Flip Controls" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x20, DEF_STR( "On") );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_VBLANK );
		PORT_SERVICE( 0x80, IP_ACTIVE_HIGH );
	
		PORT_START();       /* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER1);
		PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2);
		PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_PLAYER2 | IPF_8WAY );
		PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_PLAYER2 | IPF_8WAY );
		PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_PLAYER2 | IPF_8WAY );
	INPUT_PORTS_END(); }}; 
	
	
	
	/*************************************
	 *
	 *	Graphics layouts
	 *
	 *************************************/
	
	static GfxLayout charlayout = new GfxLayout
	(
	   8,8,
	   RGN_FRAC(1,1),
	   4,
	   new int[] { 0,1,2,3 },
	   new int[] { 0*4, 1*4, 2*4, 3*4, 4*4, 5*4, 6*4, 7*4},
	   new int[] { 0*4*8, 1*4*8, 2*4*8, 3*4*8, 4*4*8, 5*4*8, 6*4*8, 7*4*8},
	   8*8*4
	);
	
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, charlayout, 0, 16 ),
		new GfxDecodeInfo( -1 )
	};
	
	
	
	/*************************************
	 *
	 *	Sound definitions
	 *
	 *************************************/
	
	static struct POKEYinterface pokey_interface =
	{
		2,
		ATARI_CLOCK_14MHz/8,
		{ 50, 50 },
		/* The 8 pot handlers */
		{ 0, 0 },
		{ 0, 0 },
		{ 0, 0 },
		{ 0, 0 },
		{ 0, 0 },
		{ 0, 0 },
		{ 0, 0 },
		{ 0, 0 },
		/* The allpot handler */
		{ input_port_0_r, input_port_1_r }
	};
	
	
	
	/*************************************
	 *
	 *	Machine driver
	 *
	 *************************************/
	
	static MachineDriver machine_driver_atetris = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_M6502,
				ATARI_CLOCK_14MHz/8,
				readmem,writemem,null,null,
				ignore_interrupt,0
			)
		},
		60, DEFAULT_REAL_60HZ_VBLANK_DURATION,
		1,
		init_machine,
	
		/* video hardware */
		42*8, 30*8, new rectangle( 0*8, 42*8-1, 0*8, 30*8-1 ),
		gfxdecodeinfo,
		256, null,
		0,
	
		VIDEO_TYPE_RASTER | VIDEO_SUPPORTS_DIRTY,
		null,
		atetris_vh_start,
		null,
		atetris_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound( SOUND_POKEY, pokey_interface )
		},
	
		nvram_handler
	);
	
	
	
	/*************************************
	 *
	 *	ROM definitions
	 *
	 *************************************/
	
	static RomLoadPtr rom_atetris = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x18000, REGION_CPU1, 0 );
		ROM_LOAD( "1100.45f",     0x10000, 0x8000, 0x2acbdb09 );
		ROM_CONTINUE(             0x08000, 0x8000 );
	
		ROM_REGION( 0x10000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "1101.35a",     0x0000, 0x10000, 0x84a1939f );
	ROM_END(); }}; 
	
	
	static RomLoadPtr rom_atetrisa = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x18000, REGION_CPU1, 0 );
		ROM_LOAD( "d1",           0x10000, 0x8000, 0x2bcab107 );
		ROM_CONTINUE(             0x08000, 0x8000 );
	
		ROM_REGION( 0x10000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "1101.35a",     0x0000, 0x10000, 0x84a1939f );
	ROM_END(); }}; 
	
	
	static RomLoadPtr rom_atetrisb = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x18000, REGION_CPU1, 0 );
		ROM_LOAD( "tetris.01",    0x10000, 0x8000, 0x944d15f6 );
		ROM_CONTINUE(             0x08000, 0x8000 );
	
		ROM_REGION( 0x10000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "tetris.02",    0x0000, 0x10000, 0x5c4e7258 );
	
		/* there's an extra EEPROM, maybe used for protection crack, which */
		/* however doesn't seem to be required to run the game in this driver. */
		ROM_REGION( 0x0800, REGION_USER1, 0 );
		ROM_LOAD( "tetris.03",    0x0000, 0x0800, 0x26618c0b );
	ROM_END(); }}; 
	
	
	static RomLoadPtr rom_atetcktl = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x18000, REGION_CPU1, 0 );
		ROM_LOAD( "tetcktl1.rom", 0x10000, 0x8000, 0x9afd1f4a );
		ROM_CONTINUE(             0x08000, 0x8000 );
	
		ROM_REGION( 0x10000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "1103.35a",     0x0000, 0x10000, 0xec2a7f93 );
	ROM_END(); }}; 
	
	
	static RomLoadPtr rom_atetckt2 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x18000, REGION_CPU1, 0 );
		ROM_LOAD( "1102.45f",     0x10000, 0x8000, 0x1bd28902 );
		ROM_CONTINUE(             0x08000, 0x8000 );
	
		ROM_REGION( 0x10000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "1103.35a",     0x0000, 0x10000, 0xec2a7f93 );
	ROM_END(); }}; 
	
	
	
	/*************************************
	 *
	 *	Driver init
	 *
	 *************************************/
	
	static public static InitDriverPtr init_atetris = new InitDriverPtr() { public void handler() 
	{
		slapstic_init(101);
		slapstic_source = &memory_region(REGION_CPU1)[0x10000];
		slapstic_base = &memory_region(REGION_CPU1)[0x04000];
	} };
	
	
	
	/*************************************
	 *
	 *	Game drivers
	 *
	 *************************************/
	
	public static GameDriver driver_atetris	   = new GameDriver("1988"	,"atetris"	,"atetris.java"	,rom_atetris,null	,machine_driver_atetris	,input_ports_atetris	,init_atetris	,ROT0	,	"Atari Games", "Tetris (set 1)" )
	public static GameDriver driver_atetrisa	   = new GameDriver("1988"	,"atetrisa"	,"atetris.java"	,rom_atetrisa,driver_atetris	,machine_driver_atetris	,input_ports_atetris	,init_atetris	,ROT0	,	"Atari Games", "Tetris (set 2)" )
	public static GameDriver driver_atetrisb	   = new GameDriver("1988"	,"atetrisb"	,"atetris.java"	,rom_atetrisb,driver_atetris	,machine_driver_atetris	,input_ports_atetris	,init_atetris	,ROT0	,	"bootleg",     "Tetris (bootleg)" )
	public static GameDriver driver_atetcktl	   = new GameDriver("1989"	,"atetcktl"	,"atetris.java"	,rom_atetcktl,driver_atetris	,machine_driver_atetris	,input_ports_atetcktl	,init_atetris	,ROT270	,	"Atari Games", "Tetris (Cocktail set 1)" )
	public static GameDriver driver_atetckt2	   = new GameDriver("1989"	,"atetckt2"	,"atetris.java"	,rom_atetckt2,driver_atetris	,machine_driver_atetris	,input_ports_atetcktl	,init_atetris	,ROT270	,	"Atari Games", "Tetris (Cocktail set 2)" )
}
