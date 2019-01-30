/***************************************************************************

	Victory system

    driver by Aaron Giles

****************************************************************************

	Memory map

****************************************************************************

	========================================================================
	CPU #1
	========================================================================
	0000-BFFF   R     xxxxxxxx   Program ROM
	C000        R     xxxxxxxx   Foreground collision X position
	C001        R     xxxxxxxx   Foreground collision Y position/IRQ clear
	C002        R     xxxxxx--   Background collision X position
	C003        R     xxxxxxxx   Background collision Y position/IRQ clear
	C004        R     xxxxx---   Interrupt/microcode status
	            R     x-------      (Microcode busy)
	            R     -x------      (Foreground collision IRQ)
	            R     --x-----      (Video VBLANK IRQ)
	            R     ---x----      (Background collision IRQ)
	            R     ----x---      (Scanline bit 8)
	C100-C101     W   xxxxxxxx   Microcode register I
	C102          W   xxxxxxxx   Microcode command register
	C103          W   xxxxxxxx   Microcode register G
	C104          W   xxxxxxxx   Microcode register X
	C105          W   xxxxxxxx   Microcode register Y
	C106          W   xxxxxxxx   Microcode register R
	C107          W   xxxxxxxx   Microcode register B
	C108          W   xxxxxxxx   Background X scroll
	C109          W   xxxxxxxx   Background Y scroll
	C10A          W   xxxxxxx-   Video control register
	              W   x-------      (HLMBK??)
	              W   -x------      (VLMBK??)
	              W   --x-----      (Background collision IRQ enable)
	              W   ---x----      (Refresh rate select 50/60)
	              W   ----x---      (Screen invert)
	              W   -----x--      (Background collision select)
	              W   ------x-      (SELOVER??)
	C10B          W   --------   Video IRQ clear
	C200-C3FF     W   xxxxxxxx   Palette RAM
	              W   xx------      (Red, 2 LSB; MSB comes from A7)
	              W   --xxx---      (Blue)
	              W   -----xxx      (Green)
	C400-C7FF   R/W   xxxxxxxx   Background tile RAM
	C800-CFFF   R/W   xxxxxxxx   Red background character RAM
	D000-D7FF   R/W   xxxxxxxx   Blue background character RAM
	D800-DFFF   R/W   xxxxxxxx   Green background character RAM
	E000-EFFF   R/W   xxxxxxxx   Program RAM
	F000-F7FF   R/W   xxxxxxxx   NVRAM
	F800        R     xxxxxxxx   Sound CPU response
	F800          W   xxxxxxxx   Sound CPU command
	F801        R     xx------   Sound CPU status
	            R     x-------      (Command pending)
	            R     -x------      (Response pending)
	========================================================================
	  00-03     R     xxxxxxxx   DIP switch 1
	            R     x-------      (Refresh rate select 50/60)
	            R     -----xxx      (Unknown)
	  04-07     R     xxxxxxxx   DIP switch 2
	  08-0B     R/W   xxxxxxxx   PIO #1
	  0C-0F     R/W   xxxxxxxx   PIO #2
	  10-13       W   xxxxx---   Lamp/coin control
	              W   x-------      (Thrust button lamp)
	              W   -x------      (Fire button lamp)
	              W   --x-----      (Doomsday button lamp)
	              W   ---x----      (Shields button lamp)
	              W   ----x---      (Coin counter)
	========================================================================
	Interrupts:
	   INT generated by collision IRQs and VBLANK
	========================================================================

	========================================================================
	CPU #2
	========================================================================
	0000-01FF   R/W   xxxxxxxx   Program RAM
	1000-1FFF   R/W   xxxxxxxx   6532 RIOT timer and I/O
	2000-2FFF   R/W   xxxxxxxx   6821 PIA I/O
	3000-3FFF   R/W   xxxxxxxx   8253 timer
	5000-5FFF   R/W   xxxxxxxx   6840 timer
	6000-6FFF     W   ------xx   Control bits
	              W   ------x-      (6840 Channel 1 output enable)
	              W   -------x      (sound effects noise frequency select)
	C000-FFFF   R     xxxxxxxx   Program ROM
	========================================================================
	Interrupts:
	   INT generated by 6532 RIOT
	========================================================================

***************************************************************************/


/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package drivers;

public class victory
{
	
	
	#define LOG_SOUND		0
	
	
	static UINT8 *nvram;
	static size_t nvram_size;
	
	static UINT8 sound_response;
	static UINT8 sound_response_ack_clk;
	
	
	/* sound driver data & functions */
	int victory_sh_start(const struct MachineSound *msound);
	
	READ_HANDLER( exidy_shriot_r );
	READ_HANDLER( exidy_sh6840_r );
	READ_HANDLER( exidy_sh8253_r );
	WRITE_HANDLER( exidy_shriot_w );
	WRITE_HANDLER( exidy_sh6840_w );
	WRITE_HANDLER( exidy_sh8253_w );
	WRITE_HANDLER( exidy_sfxctrl_w );
	
	
	/* video driver data & functions */
	extern UINT8 *victory_charram;
	
	int victory_vh_start(void);
	void victory_vh_stop(void);
	void victory_vh_eof(void);
	void victory_vh_screenrefresh(struct mame_bitmap *bitmap, int full_refresh);
	int victory_vblank_interrupt(void);
	
	READ_HANDLER( victory_video_control_r );
	WRITE_HANDLER( victory_video_control_w );
	WRITE_HANDLER( victory_paletteram_w );
	WRITE_HANDLER( victory_videoram_w );
	WRITE_HANDLER( victory_charram_w );
	
	
	
	/*************************************
	 *
	 *	Machine setup
	 *
	 *************************************/
	
	static void nvram_handler(void *file, int read_or_write)
	{
		if (read_or_write)
		{
			osd_fwrite(file, nvram, nvram_size);
		}
		else if (file)
		{
			osd_fread(file, nvram, nvram_size);
		}
		else
		{
			memset(nvram, 0x00, nvram_size);
		}
	}
	
	
	
	/*************************************
	 *
	 *	Sound CPU control
	 *
	 *************************************/
	
	public static ReadHandlerPtr sound_response_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if (LOG_SOUND) logerror("%04X:!!!! Sound response read = %02X\n", cpu_getpreviouspc(), sound_response);
		pia_0_cb1_w(0, 0);
		return sound_response;
	} };
	
	
	public static ReadHandlerPtr sound_status_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if (LOG_SOUND) logerror("%04X:!!!! Sound status read = %02X\n", cpu_getpreviouspc(), (pia_0_ca1_r(0) << 7) | (pia_0_cb1_r(0) << 6));
		return (pia_0_ca1_r(0) << 7) | (pia_0_cb1_r(0) << 6);
	} };
	
	
	static void delayed_command_w(int data)
	{
		pia_0_porta_w(0, data);
		pia_0_ca1_w(0, 0);
		if (LOG_SOUND) logerror("%04X:!!!! Sound command = %02X\n", cpu_getpreviouspc(), data);
	}
	
	public static WriteHandlerPtr sound_command_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		timer_set(TIME_NOW, data, delayed_command_w);
	} };
	
	
	WRITE_HANDLER( victory_sound_response_w )
	{
		sound_response = data;
		if (LOG_SOUND) logerror("%04X:!!!! Sound response = %02X\n", cpu_getpreviouspc(), data);
	}
	
	
	WRITE_HANDLER( victory_sound_irq_clear_w )
	{
		if (LOG_SOUND) logerror("%04X:!!!! Sound IRQ clear = %02X\n", cpu_getpreviouspc(), data);
		if (data == 0) pia_0_ca1_w(0, 1);
	}
	
	
	WRITE_HANDLER( victory_main_ack_w )
	{
		if (LOG_SOUND) logerror("%04X:!!!! Sound ack = %02X\n", cpu_getpreviouspc(), data);
		if (sound_response_ack_clk && !data)
			pia_0_cb1_w(0, 1);
		sound_response_ack_clk = data;
	}
	
	
	
	/*************************************
	 *
	 *	Misc I/O
	 *
	 *************************************/
	
	public static WriteHandlerPtr lamp_control_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		set_led_status(0,data & 0x80);
		set_led_status(1,data & 0x40);
		set_led_status(2,data & 0x20);
		set_led_status(3,data & 0x10);
	} };
	
	
	
	/*************************************
	 *
	 *	Main CPU memory handlers
	 *
	 *************************************/
	
	public static Memory_ReadAddress main_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0xbfff, MRA_ROM ),
		new Memory_ReadAddress( 0xc000, 0xc0ff, victory_video_control_r ),
		new Memory_ReadAddress( 0xc400, 0xc7ff, MRA_RAM ),
		new Memory_ReadAddress( 0xc800, 0xdfff, MRA_RAM ),
		new Memory_ReadAddress( 0xe000, 0xefff, MRA_RAM ),
		new Memory_ReadAddress( 0xf000, 0xf7ff, MRA_RAM ),
		new Memory_ReadAddress( 0xf800, 0xf800, sound_response_r ),
		new Memory_ReadAddress( 0xf801, 0xf801, sound_status_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	
	public static Memory_WriteAddress main_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0xbfff, MWA_ROM ),
		new Memory_WriteAddress( 0xc100, 0xc1ff, victory_video_control_w ),
		new Memory_WriteAddress( 0xc200, 0xc3ff, victory_paletteram_w, paletteram ),
		new Memory_WriteAddress( 0xc400, 0xc7ff, victory_videoram_w, videoram ),
		new Memory_WriteAddress( 0xc800, 0xdfff, victory_charram_w, victory_charram ),
		new Memory_WriteAddress( 0xe000, 0xefff, MWA_RAM ),
		new Memory_WriteAddress( 0xf000, 0xf7ff, MWA_RAM, nvram, nvram_size ),
		new Memory_WriteAddress( 0xf800, 0xf800, sound_command_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	public static IO_ReadPort main_readport[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_ReadPort( 0x00, 0x03, input_port_0_r ),
		new IO_ReadPort( 0x04, 0x07, input_port_1_r ),
		new IO_ReadPort( 0x08, 0x08, input_port_2_r ),
		new IO_ReadPort( 0x0a, 0x0a, input_port_3_r ),
		new IO_ReadPort( 0x0c, 0x0c, input_port_4_r ),
		new IO_ReadPort( 0x0e, 0x0e, input_port_5_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	
	
	public static IO_WritePort main_writeport[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_WritePort( 0x10, 0x13, lamp_control_w ),
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	
	
	/*************************************
	 *
	 *	Sound CPU memory handlers
	 *
	 *************************************/
	
	public static Memory_ReadAddress sound_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x01ff, MRA_RAM ),
		new Memory_ReadAddress( 0x1000, 0x1fff, exidy_shriot_r ),
		new Memory_ReadAddress( 0x2000, 0x200f, pia_0_r ),
		new Memory_ReadAddress( 0x3000, 0x3fff, exidy_sh8253_r ),
		new Memory_ReadAddress( 0x5000, 0x5fff, exidy_sh6840_r ),
		new Memory_ReadAddress( 0xc000, 0xffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	
	public static Memory_WriteAddress sound_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x01ff, MWA_RAM ),
		new Memory_WriteAddress( 0x1000, 0x1fff, exidy_shriot_w ),
		new Memory_WriteAddress( 0x2000, 0x200f, pia_0_w ),
		new Memory_WriteAddress( 0x3000, 0x3fff, exidy_sh8253_w ),
		new Memory_WriteAddress( 0x5000, 0x5fff, exidy_sh6840_w ),
		new Memory_WriteAddress( 0x6000, 0x6fff, exidy_sfxctrl_w ),
		new Memory_WriteAddress( 0xc000, 0xffff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	
	/*************************************
	 *
	 *	Port definitions
	 *
	 *************************************/
	
	INPUT_PORTS_START( victory )
		PORT_START	/* $00-$03 = SW2 */
		PORT_DIPNAME( 0x07, 0x00, "????" )
		PORT_DIPSETTING(    0x00, "0" )
		PORT_DIPSETTING(    0x01, "1" )
		PORT_DIPSETTING(    0x02, "2" )
		PORT_DIPSETTING(    0x03, "3" )
		PORT_DIPSETTING(    0x04, "4" )
		PORT_DIPSETTING(    0x05, "5" )
		PORT_DIPSETTING(    0x06, "6" )
		PORT_DIPSETTING(    0x07, "7" )
		PORT_BIT( 0x78, IP_ACTIVE_LOW, IPT_UNUSED )
		PORT_DIPNAME( 0x80, 0x00, "Refresh" )
		PORT_DIPSETTING(    0x00, "60 Hz" )
		PORT_DIPSETTING(    0x80, "50 Hz" )
	
		PORT_START	/* $04-$07 = SW1 */
		PORT_BIT( 0xff, IP_ACTIVE_LOW, IPT_UNUSED )
	
		PORT_START	/* $08-$09 = PIO K8 port A */
		PORT_ANALOG( 0xff, 0x80, IPT_DIAL | IPF_REVERSE, 25, 10, 0x00, 0xff )
	
		PORT_START	/* $0A-$0B = PIO K8 port B */
		PORT_BIT( 0xf8, IP_ACTIVE_LOW, IPT_UNUSED )
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN1 )
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 )
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN3 )
	
		PORT_START	/* $0C-$0D = PIO L8 port A */
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_BUTTON1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON2 )
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON4 )
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON3 )
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START1 )
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_START2 )
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_SERVICE1 )
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN )
	
		PORT_START	/* $0E-$0F = PIO L8 port B */
		PORT_BIT( 0xff, IP_ACTIVE_LOW, IPT_UNUSED )
	INPUT_PORTS_END
	
	
	
	/*************************************
	 *
	 *	Sound definitions
	 *
	 *************************************/
	
	static struct CustomSound_interface custom_interface =
	{
	    victory_sh_start,
	    0,
		0
	};
	
	
	static struct TMS5220interface tms5220_interface =
	{
		640000,
		100,
		0
	};
	
	
	
	/*************************************
	 *
	 *	Machine driver
	 *
	 *************************************/
	
	static const struct MachineDriver machine_driver_victory =
	{
		/* basic machine hardware */
		{
			{
				CPU_Z80,
				4000000,
				main_readmem,main_writemem,main_readport,main_writeport,
				victory_vblank_interrupt,1
			},
			{
				CPU_M6502 | CPU_AUDIO_CPU,
				3579545/4,
				sound_readmem,sound_writemem,0,0,
				ignore_interrupt,0
			}
		},
		60, DEFAULT_REAL_60HZ_VBLANK_DURATION,
		1,
		0,
	
		/* video hardware */
		256, 256, { 0, 255, 0, 255 },
		0,
		64, 0,
		0,
	
		VIDEO_TYPE_RASTER | VIDEO_UPDATE_BEFORE_VBLANK,
		victory_vh_eof,
		victory_vh_start,
		victory_vh_stop,
		victory_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		{
			{ SOUND_CUSTOM,  &custom_interface },
			{ SOUND_TMS5220, &tms5220_interface },
		},
	
		nvram_handler
	};
	
	
	
	/*************************************
	 *
	 *	ROM definitions
	 *
	 *************************************/
	
	static RomLoadPtr rom_victory = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );
		ROM_LOAD( "vic3.j2",  0x0000, 0x1000, 0x4b614440 );
		ROM_LOAD( "vic3.k2",  0x1000, 0x1000, 0x9f9eb12b );
		ROM_LOAD( "vic3.kl2", 0x2000, 0x1000, 0xa0db4bf9 );
		ROM_LOAD( "vic3.l2",  0x3000, 0x1000, 0x69855b46 );
		ROM_LOAD( "vic3.m2",  0x4000, 0x1000, 0x1ddbe9d4 );
		ROM_LOAD( "vic3.n2",  0x5000, 0x1000, 0xdbb53f1f );
		ROM_LOAD( "vic3.p2",  0x6000, 0x1000, 0x9959e1c4 );
		ROM_LOAD( "vic3.t2",  0x7000, 0x1000, 0x8f1b997a );
		ROM_LOAD( "vic3.j1",  0x8000, 0x1000, 0x27e9e87b );
		ROM_LOAD( "vic3.k1",  0x9000, 0x1000, 0x418d9b80 );
		ROM_LOAD( "vic3.kl1", 0xa000, 0x1000, 0x2b7e626f );
		ROM_LOAD( "vic3.l1",  0xb000, 0x1000, 0x7bb8e1f5 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );
		ROM_LOAD( "vic1.7bc", 0xc000, 0x1000, 0xd4927560 );
		ROM_LOAD( "vic1.7c",  0xd000, 0x1000, 0x059efab5 );
		ROM_LOAD( "vic1.7d",  0xe000, 0x1000, 0x82c4767c );
		ROM_LOAD( "vic1.7e",  0xf000, 0x1000, 0xa19be034 );
	
		ROM_REGION( 0x1e0, REGION_PROMS, 0 );
		ROM_LOAD( "hsc17l",   0x0000, 0x0100, 0xb2c75dee );
		ROM_LOAD( "hsc13e",   0x0100, 0x0020, 0xa107c4f5 );
		ROM_LOAD( "hsc16a",   0x0120, 0x0020, 0x5f06ad26 );
		ROM_LOAD( "hsc19b",   0x0140, 0x0020, 0x86165f1e );
		ROM_LOAD( "hsc19c",   0x0160, 0x0020, 0xfd27a57a );
		ROM_LOAD( "hsc19d",   0x0180, 0x0020, 0x09c4dbf6 );
		ROM_LOAD( "hsc19e",   0x01a0, 0x0020, 0xce1464f4 );
		ROM_LOAD( "3j",       0x01c0, 0x0020, 0x5fb6b158 );
	ROM_END(); }}; 
	
	
	static RomLoadPtr rom_victorba = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );
		ROM_LOAD( "j2.rom",  0x0000, 0x1000, 0xdd788e93 );
		ROM_LOAD( "k2.rom",  0x1000, 0x1000, 0xf47bf046 );
		ROM_LOAD( "kl2.rom", 0x2000, 0x1000, 0xbaef885e );
		ROM_LOAD( "l2.rom",  0x3000, 0x1000, 0x739e4799 );
		ROM_LOAD( "m2.rom",  0x4000, 0x1000, 0xa88185e6 );
		ROM_LOAD( "n2.rom",  0x5000, 0x1000, 0x6724eb01 );
		ROM_LOAD( "p2.rom",  0x6000, 0x1000, 0x2cf34ad7 );
		ROM_LOAD( "t2.rom",  0x7000, 0x1000, 0x89bb0359 );
		ROM_LOAD( "j1.rom",  0x8000, 0x1000, 0x5e415084 );
		ROM_LOAD( "k1.rom",  0x9000, 0x1000, 0x3f327dff );
		ROM_LOAD( "kl1.rom", 0xa000, 0x1000, 0x6c82ebca );
		ROM_LOAD( "l1.rom",  0xb000, 0x1000, 0x03b89d8a );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );
		ROM_LOAD( "vic1.7bc", 0xc000, 0x1000, 0xd4927560 );
		ROM_LOAD( "vic1.7c",  0xd000, 0x1000, 0x059efab5 );
		ROM_LOAD( "vic1.7d",  0xe000, 0x1000, 0x82c4767c );
		ROM_LOAD( "vic1.7e",  0xf000, 0x1000, 0xa19be034 );
	
		ROM_REGION( 0x1e0, REGION_PROMS, 0 );
		ROM_LOAD( "hsc17l",   0x0000, 0x0100, 0xb2c75dee );
		ROM_LOAD( "hsc13e",   0x0100, 0x0020, 0xa107c4f5 );
		ROM_LOAD( "hsc16a",   0x0120, 0x0020, 0x5f06ad26 );
		ROM_LOAD( "hsc19b",   0x0140, 0x0020, 0x86165f1e );
		ROM_LOAD( "hsc19c",   0x0160, 0x0020, 0xfd27a57a );
		ROM_LOAD( "hsc19d",   0x0180, 0x0020, 0x09c4dbf6 );
		ROM_LOAD( "hsc19e",   0x01a0, 0x0020, 0xce1464f4 );
		ROM_LOAD( "3j",       0x01c0, 0x0020, 0x5fb6b158 );
	ROM_END(); }}; 
	
	
	
	/*************************************
	 *
	 *	Game drivers
	 *
	 *************************************/
	
	GAME( 1982, victory,  0,        victory, victory, 0,     ROT0, "Exidy", "Victory" )
	GAME( 1982, victorba, victory,  victory, victory, 0,     ROT0, "Exidy", "Victor Banana" )
}
