/***************************************************************************

  Black Tiger

  Driver provided by Paul Leaman

  Thanks to Ishmair for providing information about the screen
  layout on level 3.

Notes:
- sprites/tile priority is a guess. I didn't find a PROM that would simply
  translate to the scheme I implemented.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package drivers;

public class blktiger
{
	
	
	extern unsigned char *blktiger_txvideoram;
	
	
	
	int blktiger_vh_start(void);
	void blktiger_vh_stop(void);
	void blktiger_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh);
	void blktiger_eof_callback(void);
	
	
	
	/* this is a protection check. The game crashes (thru a jump to 0x8000) */
	/* if a read from this address doesn't return the value it expects. */
	public static ReadHandlerPtr blktiger_protection_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int data = cpu_get_reg(Z80_DE) >> 8;
		logerror("protection read, PC: %04x Result:%02x\n",cpu_get_pc(),data);
		return data;
	} };
	
	public static WriteHandlerPtr blktiger_bankswitch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int bankaddress;
		unsigned char *rom = memory_region(REGION_CPU1);
	
	
		bankaddress = 0x10000 + (data & 0x0f) * 0x4000;
		cpu_setbank(1,&rom[bankaddress]);
	} };
	
	public static WriteHandlerPtr blktiger_coinlockout_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		coin_lockout_w(0,~data & 0x01);
		coin_lockout_w(1,~data & 0x02);
	} };
	
	
	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0xbfff, MRA_BANK1 ),
		new Memory_ReadAddress( 0xc000, 0xcfff, blktiger_bgvideoram_r ),
		new Memory_ReadAddress( 0xd000, 0xffff, MRA_RAM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0xbfff, MWA_ROM ),
		new Memory_WriteAddress( 0xc000, 0xcfff, blktiger_bgvideoram_w ),
		new Memory_WriteAddress( 0xd000, 0xd7ff, blktiger_txvideoram_w, blktiger_txvideoram ),
		new Memory_WriteAddress( 0xd800, 0xdbff, paletteram_xxxxBBBBRRRRGGGG_split1_w, paletteram ),
		new Memory_WriteAddress( 0xdc00, 0xdfff, paletteram_xxxxBBBBRRRRGGGG_split2_w, paletteram_2 ),
		new Memory_WriteAddress( 0xe000, 0xfdff, MWA_RAM ),
		new Memory_WriteAddress( 0xfe00, 0xffff, MWA_RAM, spriteram, spriteram_size ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static IO_ReadPort readport[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_ReadPort( 0x00, 0x00, input_port_0_r ),
		new IO_ReadPort( 0x01, 0x01, input_port_1_r ),
		new IO_ReadPort( 0x02, 0x02, input_port_2_r ),
		new IO_ReadPort( 0x03, 0x03, input_port_3_r ),
		new IO_ReadPort( 0x04, 0x04, input_port_4_r ),
		new IO_ReadPort( 0x05, 0x05, input_port_5_r ),
		new IO_ReadPort( 0x07, 0x07, blktiger_protection_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	
	public static IO_WritePort writeport[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_WritePort( 0x00, 0x00, soundlatch_w ),
		new IO_WritePort( 0x01, 0x01, blktiger_bankswitch_w ),
		new IO_WritePort( 0x03, 0x03, blktiger_coinlockout_w ),
		new IO_WritePort( 0x04, 0x04, blktiger_video_control_w ),
		new IO_WritePort( 0x06, 0x06, watchdog_reset_w ),
		new IO_WritePort( 0x07, 0x07, IOWP_NOP ), /* Software protection (7) */
		new IO_WritePort( 0x08, 0x09, blktiger_scrollx_w ),
		new IO_WritePort( 0x0a, 0x0b, blktiger_scrolly_w ),
		new IO_WritePort( 0x0c, 0x0c, blktiger_video_enable_w ),
		new IO_WritePort( 0x0d, 0x0d, blktiger_bgvideoram_bank_w ),
		new IO_WritePort( 0x0e, 0x0e, blktiger_screen_layout_w ),
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	
	public static Memory_ReadAddress sound_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0xc000, 0xc7ff, MRA_RAM ),
		new Memory_ReadAddress( 0xc800, 0xc800, soundlatch_r ),
		new Memory_ReadAddress( 0xe000, 0xe000, YM2203_status_port_0_r ),
		new Memory_ReadAddress( 0xe001, 0xe001, YM2203_read_port_0_r ),
		new Memory_ReadAddress( 0xe002, 0xe002, YM2203_status_port_1_r ),
		new Memory_ReadAddress( 0xe003, 0xe003, YM2203_read_port_1_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress sound_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0xc000, 0xc7ff, MWA_RAM ),
		new Memory_WriteAddress( 0xe000, 0xe000, YM2203_control_port_0_w ),
		new Memory_WriteAddress( 0xe001, 0xe001, YM2203_write_port_0_w ),
		new Memory_WriteAddress( 0xe002, 0xe002, YM2203_control_port_1_w ),
		new Memory_WriteAddress( 0xe003, 0xe003, YM2203_write_port_1_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	
	static InputPortPtr input_ports_blktiger = new InputPortPtr(){ public void handler() { 
		PORT_START	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN2 );
	
		PORT_START	/* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
	
		PORT_START	/* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_COCKTAIL );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
	
		PORT_START	/* DSW0 */
		PORT_DIPNAME( 0x07, 0x07, DEF_STR( "Coin_A") );
		PORT_DIPSETTING(    0x00, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x01, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x02, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x07, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x06, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x05, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x04, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0x03, DEF_STR( "1C_5C") );
		PORT_DIPNAME( 0x38, 0x38, DEF_STR( "Coin_B") );
		PORT_DIPSETTING(    0x00, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x08, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x10, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x38, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x30, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x28, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x20, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0x18, DEF_STR( "1C_5C") );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x80, IP_ACTIVE_LOW );
	
		PORT_START      /* DSW1 */
		PORT_DIPNAME( 0x03, 0x03, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x02, "2" );
		PORT_DIPSETTING(    0x03, "3" );
		PORT_DIPSETTING(    0x01, "5" );
		PORT_DIPSETTING(    0x00, "7");
		PORT_DIPNAME( 0x1c, 0x1c, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x1c, "1 (Easiest);
		PORT_DIPSETTING(    0x18, "2" );
		PORT_DIPSETTING(    0x14, "3" );
		PORT_DIPSETTING(    0x10, "4" );
		PORT_DIPSETTING(    0x0c, "5 (Normal); )
		PORT_DIPSETTING(    0x08, "6" );
		PORT_DIPSETTING(    0x04, "7" );
		PORT_DIPSETTING(    0x00, "8 (Hardest); )
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x20, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x40, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "No") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Cocktail") );
	
		PORT_START
		PORT_DIPNAME( 0x01, 0x01, "Freeze" );/* could be VBLANK */
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	INPUT_PORTS_END(); }}; 
	
	
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,
		RGN_FRAC(1,1),
		2,
		new int[] { 4, 0 },
		new int[] { 0, 1, 2, 3, 8+0, 8+1, 8+2, 8+3 },
		new int[] { 0*16, 1*16, 2*16, 3*16, 4*16, 5*16, 6*16, 7*16 },
		16*8
	);
	
	static GfxLayout spritelayout = new GfxLayout
	(
		16,16,
		RGN_FRAC(1,2),
		4,
		new int[] { RGN_FRAC(1,2)+4, RGN_FRAC(1,2)+0, 4, 0 },
		new int[] { 0, 1, 2, 3, 8+0, 8+1, 8+2, 8+3,
				16*16+0, 16*16+1, 16*16+2, 16*16+3, 16*16+8+0, 16*16+8+1, 16*16+8+2, 16*16+8+3 },
		new int[] { 0*16, 1*16, 2*16, 3*16, 4*16, 5*16, 6*16, 7*16,
				8*16, 9*16, 10*16, 11*16, 12*16, 13*16, 14*16, 15*16 },
		32*16
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, charlayout,   0x300, 32 ),	/* colors 0x300-0x37f */
		new GfxDecodeInfo( REGION_GFX2, 0, spritelayout, 0x000, 16 ),	/* colors 0x000-0x0ff */
		new GfxDecodeInfo( REGION_GFX3, 0, spritelayout, 0x200,  8 ),	/* colors 0x200-0x27f */
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	
	/* handler called by the 2203 emulator when the internal timers cause an IRQ */
	static void irqhandler(int irq)
	{
		cpu_set_irq_line(1,0,irq ? ASSERT_LINE : CLEAR_LINE);
	}
	
	static struct YM2203interface ym2203_interface =
	{
		2,			/* 2 chips */
		3579545,	/* 3.579 MHz ? (hand tuned) */
		{ YM2203_VOL(15,15), YM2203_VOL(15,15) },
		{ 0 },
		{ 0 },
		{ 0 },
		{ 0 },
		{ irqhandler }
	};
	
	
	
	static const struct MachineDriver machine_driver_blktiger =
	{
		/* basic machine hardware */
		{
			{
				CPU_Z80,
				4000000,	/* 4 MHz (?) */
				readmem,writemem,readport,writeport,
				interrupt,1
			},
			{
				CPU_Z80 | CPU_AUDIO_CPU,
				3000000,	/* 3 MHz (?) */
				sound_readmem,sound_writemem,0,0,
				ignore_interrupt,0	/* IRQs are triggered by the YM2203 */
			}
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		1,	/* 1 CPU slice per frame - interleaving is forced when a sound command is written */
		0,
	
		/* video hardware */
		32*8, 32*8, { 0*8, 32*8-1, 2*8, 30*8-1 },
		gfxdecodeinfo,
		1024, 0,
		0,
	
		VIDEO_TYPE_RASTER | VIDEO_BUFFERS_SPRITERAM,
		blktiger_eof_callback,
		blktiger_vh_start,
		blktiger_vh_stop,
		blktiger_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		{
			{
				SOUND_YM2203,
				&ym2203_interface
			}
		}
	};
	
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	static RomLoadPtr rom_blktiger = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x50000, REGION_CPU1, 0 );/* 64k for code + banked ROMs images */
		ROM_LOAD( "blktiger.5e",  0x00000, 0x08000, 0xa8f98f22 );/* CODE */
		ROM_LOAD( "blktiger.6e",  0x10000, 0x10000, 0x7bef96e8 );/* 0+1 */
		ROM_LOAD( "blktiger.8e",  0x20000, 0x10000, 0x4089e157 );/* 2+3 */
		ROM_LOAD( "blktiger.9e",  0x30000, 0x10000, 0xed6af6ec );/* 4+5 */
		ROM_LOAD( "blktiger.10e", 0x40000, 0x10000, 0xae59b72e );/* 6+7 */
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64k for the audio CPU */
		ROM_LOAD( "blktiger.1l",  0x0000, 0x8000, 0x2cf54274 );
	
		ROM_REGION( 0x08000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "blktiger.2n",  0x00000, 0x08000, 0x70175d78 );/* characters */
	
		ROM_REGION( 0x40000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "blktiger.5b",  0x00000, 0x10000, 0xc4524993 );/* tiles */
		ROM_LOAD( "blktiger.4b",  0x10000, 0x10000, 0x7932c86f );
		ROM_LOAD( "blktiger.9b",  0x20000, 0x10000, 0xdc49593a );
		ROM_LOAD( "blktiger.8b",  0x30000, 0x10000, 0x7ed7a122 );
	
		ROM_REGION( 0x40000, REGION_GFX3, ROMREGION_DISPOSE );
		ROM_LOAD( "blktiger.5a",  0x00000, 0x10000, 0xe2f17438 );/* sprites */
		ROM_LOAD( "blktiger.4a",  0x10000, 0x10000, 0x5fccbd27 );
		ROM_LOAD( "blktiger.9a",  0x20000, 0x10000, 0xfc33ccc6 );
		ROM_LOAD( "blktiger.8a",  0x30000, 0x10000, 0xf449de01 );
	
		ROM_REGION( 0x0400, REGION_PROMS, 0 );/* PROMs (function unknown) */
		ROM_LOAD( "mb7114e.8j",   0x0000, 0x0100, 0x29b459e5 );
		ROM_LOAD( "mb7114e.9j",   0x0100, 0x0100, 0x8b741e66 );
		ROM_LOAD( "mb7114e.11k",  0x0200, 0x0100, 0x27201c75 );
		ROM_LOAD( "mb7114e.11l",  0x0300, 0x0100, 0xe5490b68 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_bktigerb = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x50000, REGION_CPU1, 0 );/* 64k for code + banked ROMs images */
		ROM_LOAD( "btiger1.f6",   0x00000, 0x08000, 0x9d8464e8 );/* CODE */
		ROM_LOAD( "blktiger.6e",  0x10000, 0x10000, 0x7bef96e8 );/* 0+1 */
		ROM_LOAD( "btiger3.j6",   0x20000, 0x10000, 0x52c56ed1 );/* 2+3 */
		ROM_LOAD( "blktiger.9e",  0x30000, 0x10000, 0xed6af6ec );/* 4+5 */
		ROM_LOAD( "blktiger.10e", 0x40000, 0x10000, 0xae59b72e );/* 6+7 */
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64k for the audio CPU */
		ROM_LOAD( "blktiger.1l",  0x0000, 0x8000, 0x2cf54274 );
	
		ROM_REGION( 0x08000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "blktiger.2n",  0x00000, 0x08000, 0x70175d78 );/* characters */
	
		ROM_REGION( 0x40000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "blktiger.5b",  0x00000, 0x10000, 0xc4524993 );/* tiles */
		ROM_LOAD( "blktiger.4b",  0x10000, 0x10000, 0x7932c86f );
		ROM_LOAD( "blktiger.9b",  0x20000, 0x10000, 0xdc49593a );
		ROM_LOAD( "blktiger.8b",  0x30000, 0x10000, 0x7ed7a122 );
	
		ROM_REGION( 0x40000, REGION_GFX3, ROMREGION_DISPOSE );
		ROM_LOAD( "blktiger.5a",  0x00000, 0x10000, 0xe2f17438 );/* sprites */
		ROM_LOAD( "blktiger.4a",  0x10000, 0x10000, 0x5fccbd27 );
		ROM_LOAD( "blktiger.9a",  0x20000, 0x10000, 0xfc33ccc6 );
		ROM_LOAD( "blktiger.8a",  0x30000, 0x10000, 0xf449de01 );
	
		ROM_REGION( 0x0400, REGION_PROMS, 0 );/* PROMs (function unknown) */
		ROM_LOAD( "mb7114e.8j",   0x0000, 0x0100, 0x29b459e5 );
		ROM_LOAD( "mb7114e.9j",   0x0100, 0x0100, 0x8b741e66 );
		ROM_LOAD( "mb7114e.11k",  0x0200, 0x0100, 0x27201c75 );
		ROM_LOAD( "mb7114e.11l",  0x0300, 0x0100, 0xe5490b68 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_blkdrgon = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x50000, REGION_CPU1, 0 );/* 64k for code + banked ROMs images */
		ROM_LOAD( "blkdrgon.5e",  0x00000, 0x08000, 0x27ccdfbc );/* CODE */
		ROM_LOAD( "blkdrgon.6e",  0x10000, 0x10000, 0x7d39c26f );/* 0+1 */
		ROM_LOAD( "blkdrgon.8e",  0x20000, 0x10000, 0xd1bf3757 );/* 2+3 */
		ROM_LOAD( "blkdrgon.9e",  0x30000, 0x10000, 0x4d1d6680 );/* 4+5 */
		ROM_LOAD( "blkdrgon.10e", 0x40000, 0x10000, 0xc8d0c45e );/* 6+7 */
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64k for the audio CPU */
		ROM_LOAD( "blktiger.1l",  0x0000, 0x8000, 0x2cf54274 );
	
		ROM_REGION( 0x08000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "blkdrgon.2n",  0x00000, 0x08000, 0x3821ab29 );/* characters */
	
		ROM_REGION( 0x40000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "blkdrgon.5b",  0x00000, 0x10000, 0x22d0a4b0 );/* tiles */
		ROM_LOAD( "blkdrgon.4b",  0x10000, 0x10000, 0xc8b5fc52 );
		ROM_LOAD( "blkdrgon.9b",  0x20000, 0x10000, 0x9498c378 );
		ROM_LOAD( "blkdrgon.8b",  0x30000, 0x10000, 0x5b0df8ce );
	
		ROM_REGION( 0x40000, REGION_GFX3, ROMREGION_DISPOSE );
		ROM_LOAD( "blktiger.5a",  0x00000, 0x10000, 0xe2f17438 );/* sprites */
		ROM_LOAD( "blktiger.4a",  0x10000, 0x10000, 0x5fccbd27 );
		ROM_LOAD( "blktiger.9a",  0x20000, 0x10000, 0xfc33ccc6 );
		ROM_LOAD( "blktiger.8a",  0x30000, 0x10000, 0xf449de01 );
	
		ROM_REGION( 0x0400, REGION_PROMS, 0 );/* PROMs (function unknown) */
		ROM_LOAD( "mb7114e.8j",   0x0000, 0x0100, 0x29b459e5 );
		ROM_LOAD( "mb7114e.9j",   0x0100, 0x0100, 0x8b741e66 );
		ROM_LOAD( "mb7114e.11k",  0x0200, 0x0100, 0x27201c75 );
		ROM_LOAD( "mb7114e.11l",  0x0300, 0x0100, 0xe5490b68 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_blkdrgnb = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x50000, REGION_CPU1, 0 );/* 64k for code + banked ROMs images */
		ROM_LOAD( "j1-5e",        0x00000, 0x08000, 0x97e84412 );/* CODE */
		ROM_LOAD( "blkdrgon.6e",  0x10000, 0x10000, 0x7d39c26f );/* 0+1 */
		ROM_LOAD( "j3-8e",        0x20000, 0x10000, 0xf4cd0f39 );/* 2+3 */
		ROM_LOAD( "blkdrgon.9e",  0x30000, 0x10000, 0x4d1d6680 );/* 4+5 */
		ROM_LOAD( "blkdrgon.10e", 0x40000, 0x10000, 0xc8d0c45e );/* 6+7 */
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64k for the audio CPU */
		ROM_LOAD( "blktiger.1l",  0x0000, 0x8000, 0x2cf54274 );
	
		ROM_REGION( 0x08000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "j15-2n",       0x00000, 0x08000, 0x852ad2b7 );/* characters */
	
		ROM_REGION( 0x40000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "blkdrgon.5b",  0x00000, 0x10000, 0x22d0a4b0 );/* tiles */
		ROM_LOAD( "j11-4b",       0x10000, 0x10000, 0x053ab15c );
		ROM_LOAD( "blkdrgon.9b",  0x20000, 0x10000, 0x9498c378 );
		ROM_LOAD( "j13-8b",       0x30000, 0x10000, 0x663d5afa );
	
		ROM_REGION( 0x40000, REGION_GFX3, ROMREGION_DISPOSE );
		ROM_LOAD( "blktiger.5a",  0x00000, 0x10000, 0xe2f17438 );/* sprites */
		ROM_LOAD( "blktiger.4a",  0x10000, 0x10000, 0x5fccbd27 );
		ROM_LOAD( "blktiger.9a",  0x20000, 0x10000, 0xfc33ccc6 );
		ROM_LOAD( "blktiger.8a",  0x30000, 0x10000, 0xf449de01 );
	
		ROM_REGION( 0x0400, REGION_PROMS, 0 );/* PROMs (function unknown) */
		ROM_LOAD( "mb7114e.8j",   0x0000, 0x0100, 0x29b459e5 );
		ROM_LOAD( "mb7114e.9j",   0x0100, 0x0100, 0x8b741e66 );
		ROM_LOAD( "mb7114e.11k",  0x0200, 0x0100, 0x27201c75 );
		ROM_LOAD( "mb7114e.11l",  0x0300, 0x0100, 0xe5490b68 );
	ROM_END(); }}; 
	
	
	
	public static GameDriver driver_blktiger	   = new GameDriver("1987"	,"blktiger"	,"blktiger.java"	,rom_blktiger,null	,machine_driver_blktiger	,input_ports_blktiger	,null	,ROT0	,	Capcom", "Black Tiger" )
	public static GameDriver driver_bktigerb	   = new GameDriver("1987"	,"bktigerb"	,"blktiger.java"	,rom_bktigerb,driver_blktiger	,machine_driver_blktiger	,input_ports_blktiger	,null	,ROT0	,	bootleg", "Black Tiger (bootleg)" )
	public static GameDriver driver_blkdrgon	   = new GameDriver("1987"	,"blkdrgon"	,"blktiger.java"	,rom_blkdrgon,driver_blktiger	,machine_driver_blktiger	,input_ports_blktiger	,null	,ROT0	,	Capcom", "Black Dragon" )
	public static GameDriver driver_blkdrgnb	   = new GameDriver("1987"	,"blkdrgnb"	,"blktiger.java"	,rom_blkdrgnb,driver_blktiger	,machine_driver_blktiger	,input_ports_blktiger	,null	,ROT0	,	bootleg", "Black Dragon (bootleg)" )
}
