/*************************************************************************

	Driver for Midway Wolf-unit games

	TMS34010 processor @ 6.25MHz
	Williams compressed digital sound board, with ADSP2105 @ 10.24MHz and a DAC


	Created by Aaron Giles and Ernesto Corvi
	Based on older drivers by Alex Pasadyn and Zsolt Vasvari with
	some help from Kurt Mahan


	Currently playable:
	------------------
	- Mortal Kombat 3
	- Ultimate Mortal Kombat 3
	- NBA Hangtime
	- NBA Maximum Hangtime
	- 2 On 2 Open Ice Challenge
	- WWF Wrestlemania
	- Rampage World Tour


	Currently unplayable:
	--------------------


	Currently undumped:
	-------------------


	Known Bugs:
	----------
	- WWF has an unimplemented and not Y2K compatible real-time clock

**************************************************************************/


/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package drivers;

public class wmswolfu
{
	
	
	/* these are accurate for MK Rev 5 according to measurements done by Bryan on a real board */
	/* due to the way the TMS34010 core operates, however, we need to use 0 for our VBLANK */
	/* duration (263ms is the measured value) */
	#define MKLA5_VBLANK_DURATION		0
	#define MKLA5_FPS					53.204950
	
	
	/* code-related variables */
	extern data16_t *wms_code_rom;
	extern data16_t *wms_scratch_ram;
	extern UINT8 *	wms_wolfu_decode_memory;
	
	/* CMOS-related variables */
	extern data16_t *wms_cmos_ram;
	
	
	/* driver-specific initialization */
	
	/* general machine init */
	
	
	/* external read handlers */
	READ16_HANDLER( wms_wolfu_io_r );
	READ16_HANDLER( wms_tunit_dma_r );
	READ16_HANDLER( wms_tunit_vram_r );
	READ16_HANDLER( wms_wolfu_cmos_r );
	READ16_HANDLER( wms_wolfu_input_r );
	READ16_HANDLER( wms_wolfu_sound_r );
	READ16_HANDLER( wms_wolfu_gfxrom_r );
	READ16_HANDLER( wms_wolfu_control_r );
	READ16_HANDLER( wms_wolfu_security_r );
	
	/* external write handlers */
	WRITE16_HANDLER( wms_wolfu_io_w );
	WRITE16_HANDLER( wms_tunit_dma_w );
	WRITE16_HANDLER( wms_tunit_vram_w );
	WRITE16_HANDLER( wms_wolfu_cmos_w );
	WRITE16_HANDLER( wms_wolfu_cmos_enable_w );
	WRITE16_HANDLER( wms_wolfu_control_w );
	WRITE16_HANDLER( wms_wolfu_sound_w );
	WRITE16_HANDLER( wms_wolfu_security_w );
	WRITE16_HANDLER( wms_tunit_paletteram_w );
	
	
	/* external video routines */
	void wms_tunit_to_shiftreg(offs_t address, unsigned short *shiftreg);
	void wms_tunit_from_shiftreg(offs_t address, unsigned short *shiftreg);
	void wms_tunit_display_addr_changed(UINT32 offs, int rowbytes, int scanline);
	
	
	
	/*************************************
	 *
	 *	CMOS read/write
	 *
	 *************************************/
	
	static void nvram_handler(void *file, int read_or_write)
	{
		if (read_or_write)
			osd_fwrite(file, wms_cmos_ram, 0xc000);
		else if (file)
			osd_fread(file, wms_cmos_ram, 0xc000);
		else
			memset(wms_cmos_ram, 0, 0xc000);
	}
	
	
	
	/*************************************
	 *
	 *	Memory maps
	 *
	 *************************************/
	
	static MEMORY_READ16_START( readmem )
		{ TOBYTE(0x00000000), TOBYTE(0x003fffff), wms_tunit_vram_r },
		{ TOBYTE(0x01000000), TOBYTE(0x013fffff), MRA16_RAM },
		{ TOBYTE(0x01400000), TOBYTE(0x0145ffff), wms_wolfu_cmos_r },
		{ TOBYTE(0x01600000), TOBYTE(0x0160001f), wms_wolfu_security_r },
		{ TOBYTE(0x01680000), TOBYTE(0x0168001f), wms_wolfu_sound_r },
		{ TOBYTE(0x01800000), TOBYTE(0x0187ffff), wms_wolfu_io_r },
		{ TOBYTE(0x01880000), TOBYTE(0x018fffff), MRA16_RAM },
		{ TOBYTE(0x01a00000), TOBYTE(0x01a000ff), wms_tunit_dma_r },
		{ TOBYTE(0x01a80000), TOBYTE(0x01a800ff), wms_tunit_dma_r },
		{ TOBYTE(0x01b00000), TOBYTE(0x01b0001f), wms_wolfu_control_r },
		{ TOBYTE(0x02000000), TOBYTE(0x06ffffff), wms_wolfu_gfxrom_r },
		{ TOBYTE(0xc0000000), TOBYTE(0xc00001ff), tms34010_io_register_r },
		{ TOBYTE(0xff800000), TOBYTE(0xffffffff), MRA16_RAM },
	MEMORY_END
	
	static MEMORY_WRITE16_START( writemem )
		{ TOBYTE(0x00000000), TOBYTE(0x003fffff), wms_tunit_vram_w },
		{ TOBYTE(0x01000000), TOBYTE(0x013fffff), MWA16_RAM, &wms_scratch_ram },
		{ TOBYTE(0x01400000), TOBYTE(0x0145ffff), wms_wolfu_cmos_w, &wms_cmos_ram },
		{ TOBYTE(0x01480000), TOBYTE(0x014fffff), wms_wolfu_cmos_enable_w },
		{ TOBYTE(0x01600000), TOBYTE(0x0160001f), wms_wolfu_security_w },
		{ TOBYTE(0x01680000), TOBYTE(0x0168001f), wms_wolfu_sound_w },
		{ TOBYTE(0x01800000), TOBYTE(0x0187ffff), wms_wolfu_io_w },
		{ TOBYTE(0x01880000), TOBYTE(0x018fffff), wms_tunit_paletteram_w, &paletteram16 },
		{ TOBYTE(0x01a00000), TOBYTE(0x01a000ff), wms_tunit_dma_w },
		{ TOBYTE(0x01a80000), TOBYTE(0x01a800ff), wms_tunit_dma_w },
		{ TOBYTE(0x01b00000), TOBYTE(0x01b0001f), wms_wolfu_control_w },
		{ TOBYTE(0x02000000), TOBYTE(0x06ffffff), MWA16_ROM, (data16_t **)&wms_wolfu_decode_memory },
		{ TOBYTE(0xc0000000), TOBYTE(0xc00001ff), tms34010_io_register_w },
		{ TOBYTE(0xff800000), TOBYTE(0xffffffff), MWA16_ROM, &wms_code_rom },
	MEMORY_END
	
	
	
	/*************************************
	 *
	 *	Input ports
	 *
	 *************************************/
	
	static InputPortPtr input_ports_mk3 = new InputPortPtr(){ public void handler() { 
		PORT_START(); 
		PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
		PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER2 | IPF_8WAY );
		PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER2 | IPF_8WAY );
		PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER2 | IPF_8WAY );
		PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER2 | IPF_8WAY );
		PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 
		PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER1 );
		PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER1 );
		PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_BUTTON6 | IPF_PLAYER1 );
		PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER2 );
		PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER2 );
		PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_BUTTON6 | IPF_PLAYER2 );
		PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0xff00, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 
		PORT_DIPNAME( 0x0001, 0x0001, "Test Switch" );
		PORT_DIPSETTING(      0x0001, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x0000, DEF_STR( "On") );
		PORT_DIPNAME( 0x0002, 0x0000, "Counters" );
		PORT_DIPSETTING(      0x0002, "One" );
		PORT_DIPSETTING(      0x0000, "Two" );
		PORT_DIPNAME( 0x007c, 0x007c, DEF_STR( "Coinage") );
		PORT_DIPSETTING(      0x007c, "USA-1" );
		PORT_DIPSETTING(      0x003c, "USA-2" );
		PORT_DIPSETTING(      0x005c, "USA-3" );
		PORT_DIPSETTING(      0x001c, "USA-4" );
		PORT_DIPSETTING(      0x006c, "USA-ECA" );
		PORT_DIPSETTING(      0x0074, "German-1" );
		PORT_DIPSETTING(      0x0034, "German-2" );
		PORT_DIPSETTING(      0x0054, "German-3" );
		PORT_DIPSETTING(      0x0014, "German-4" );
		PORT_DIPSETTING(      0x0064, "German-5" );
		PORT_DIPSETTING(      0x0078, "French-1" );
		PORT_DIPSETTING(      0x0038, "French-2" );
		PORT_DIPSETTING(      0x0058, "French-3" );
		PORT_DIPSETTING(      0x0018, "French-4" );
		PORT_DIPSETTING(      0x0068, "French-ECA" );
		PORT_DIPSETTING(      0x000c, DEF_STR( "Free_Play") );
		PORT_DIPNAME( 0x0080, 0x0000, "Coinage Source" );
		PORT_DIPSETTING(      0x0080, "Dipswitch" );
		PORT_DIPSETTING(      0x0000, "CMOS" );
		PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_DIPNAME( 0x0200, 0x0000, "Powerup Test" );
		PORT_DIPSETTING(      0x0000, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x0200, DEF_STR( "On") );
		PORT_DIPNAME( 0x0400, 0x0400, "Bill Validator" );
		PORT_DIPSETTING(      0x0400, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x0000, DEF_STR( "On") );
		PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_DIPNAME( 0x1000, 0x1000, "Attract Sound" );
		PORT_DIPSETTING(      0x0000, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x1000, DEF_STR( "On") );
		PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_DIPNAME( 0x4000, 0x4000, "Blood" );
		PORT_DIPSETTING(      0x0000, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x4000, DEF_STR( "On") );
		PORT_DIPNAME( 0x8000, 0x8000, "Violence" );
		PORT_DIPSETTING(      0x0000, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x8000, DEF_STR( "On") );
	
		PORT_START(); 
		PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_TILT );/* Slam Switch */
		PORT_SERVICE( 0x0010, IP_ACTIVE_LOW );
		PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_COIN3 );
		PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_COIN4 );
		PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_UNUSED );/* volume down */
		PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_UNUSED );/* volume up */
		PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_UNUSED );/* interrupt */
		PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNUSED );
	INPUT_PORTS_END(); }}; 
	
	
	static InputPortPtr input_ports_openice = new InputPortPtr(){ public void handler() { 
		PORT_START(); 
		PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
		PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER2 | IPF_8WAY );
		PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER2 | IPF_8WAY );
		PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER2 | IPF_8WAY );
		PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER2 | IPF_8WAY );
		PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 
		PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER3 | IPF_8WAY );
		PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER3 | IPF_8WAY );
		PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER3 | IPF_8WAY );
		PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER3 | IPF_8WAY );
		PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER3 );
		PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER3 );
		PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER3 );
		PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER4 | IPF_8WAY );
		PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER4 | IPF_8WAY );
		PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER4 | IPF_8WAY );
		PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER4 | IPF_8WAY );
		PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER4 );
		PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER4 );
		PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER4 );
		PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 
		PORT_DIPNAME( 0x0001, 0x0000, "Coinage Source" );
		PORT_DIPSETTING(      0x0001, "Dipswitch" );
		PORT_DIPSETTING(      0x0000, "CMOS" );
		PORT_DIPNAME( 0x003e, 0x003e, DEF_STR( "Coinage") );
		PORT_DIPSETTING(      0x003e, "USA-1" );
		PORT_DIPSETTING(      0x003c, "USA-2" );
		PORT_DIPSETTING(      0x003a, "USA-3" );
		PORT_DIPSETTING(      0x0038, "USA-4" );
		PORT_DIPSETTING(      0x0034, "USA-9" );
		PORT_DIPSETTING(      0x0032, "USA-10" );
		PORT_DIPSETTING(      0x0036, "USA-ECA" );
		PORT_DIPSETTING(      0x002e, "German-1" );
		PORT_DIPSETTING(      0x002c, "German-2" );
		PORT_DIPSETTING(      0x002a, "German-3" );
		PORT_DIPSETTING(      0x0028, "German-4" );
		PORT_DIPSETTING(      0x0024, "German-5" );
		PORT_DIPSETTING(      0x0026, "German-ECA" );
		PORT_DIPSETTING(      0x001e, "French-1" );
		PORT_DIPSETTING(      0x001c, "French-2" );
		PORT_DIPSETTING(      0x001a, "French-3" );
		PORT_DIPSETTING(      0x0018, "French-4" );
		PORT_DIPSETTING(      0x0014, "French-11" );
		PORT_DIPSETTING(      0x0012, "French-12" );
		PORT_DIPSETTING(      0x0016, "French-ECA" );
		PORT_DIPSETTING(      0x0030, DEF_STR( "Free_Play") );
		PORT_DIPNAME( 0x0040, 0x0000, "Counters" );
		PORT_DIPSETTING(      0x0040, "One" );
		PORT_DIPSETTING(      0x0000, "Two" );
		PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_DIPNAME( 0x0100, 0x0100, "Bill Validator" );
		PORT_DIPSETTING(      0x0100, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x0000, DEF_STR( "On") );
		PORT_DIPNAME( 0x0200, 0x0200, "Attract Sound" );
		PORT_DIPSETTING(      0x0000, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x0200, DEF_STR( "On") );
		PORT_DIPNAME( 0x0400, 0x0000, "Powerup Test" );
		PORT_DIPSETTING(      0x0000, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x0400, DEF_STR( "On") );
		PORT_DIPNAME( 0x0800, 0x0800, "Head Size" );
		PORT_DIPSETTING(      0x0800, "Normal" );
		PORT_DIPSETTING(      0x0000, "Large" );
		PORT_DIPNAME( 0x1000, 0x0000, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(      0x0000, "2-player" );
		PORT_DIPSETTING(      0x1000, "4-player" );
		PORT_BIT( 0x6000, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_DIPNAME( 0x8000, 0x8000, "Test Switch" );
		PORT_DIPSETTING(      0x8000, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x0000, DEF_STR( "On") );
	
		PORT_START(); 
		PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_TILT );/* Slam Switch */
		PORT_SERVICE( 0x0010, IP_ACTIVE_LOW );
		PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_COIN3 );
		PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_COIN4 );
		PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_START3 );
		PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_START4 );
		PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_UNUSED );/* volume down */
		PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_UNUSED );/* volume up */
		PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_UNUSED );/* interlock */
		PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNUSED );
	INPUT_PORTS_END(); }}; 
	
	
	static InputPortPtr input_ports_nbahangt = new InputPortPtr(){ public void handler() { 
		PORT_START(); 
		PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
		PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER2 | IPF_8WAY );
		PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER2 | IPF_8WAY );
		PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER2 | IPF_8WAY );
		PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER2 | IPF_8WAY );
		PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 
		PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER3 | IPF_8WAY );
		PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER3 | IPF_8WAY );
		PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER3 | IPF_8WAY );
		PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER3 | IPF_8WAY );
		PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER3 );
		PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER3 );
		PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER3 );
		PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER4 | IPF_8WAY );
		PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER4 | IPF_8WAY );
		PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER4 | IPF_8WAY );
		PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER4 | IPF_8WAY );
		PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER4 );
		PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER4 );
		PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER4 );
		PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 
		PORT_DIPNAME( 0x0001, 0x0001, "Test Switch" );
		PORT_DIPSETTING(      0x0001, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x0000, DEF_STR( "On") );
		PORT_DIPNAME( 0x0002, 0x0000, "Powerup Test" );
		PORT_DIPSETTING(      0x0000, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x0002, DEF_STR( "On") );
		PORT_BIT( 0x003c, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_DIPNAME( 0x0040, 0x0040, "Bill Validator" );
		PORT_DIPSETTING(      0x0040, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x0000, DEF_STR( "On") );
		PORT_DIPNAME( 0x0080, 0x0000, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(      0x0000, "2-player" );
		PORT_DIPSETTING(      0x0080, "4-player" );
		PORT_DIPNAME( 0x0300, 0x0300, "Counters" );
		PORT_DIPSETTING(      0x0300, "One, 1/1" );
		PORT_DIPSETTING(      0x0200, "One, Totalizing" );
		PORT_DIPSETTING(      0x0100, "Two, 1/1" );
		PORT_DIPNAME( 0x7c00, 0x7c00, DEF_STR( "Coinage") );
		PORT_DIPSETTING(      0x7c00, "USA-1" );
		PORT_DIPSETTING(      0x3c00, "USA-2" );
		PORT_DIPSETTING(      0x5c00, "USA-3" );
		PORT_DIPSETTING(      0x1c00, "USA-4" );
		PORT_DIPSETTING(      0x6c00, "USA-ECA" );
		PORT_DIPSETTING(      0x7400, "German-1" );
		PORT_DIPSETTING(      0x3400, "German-2" );
		PORT_DIPSETTING(      0x5400, "German-3" );
		PORT_DIPSETTING(      0x1400, "German-4" );
		PORT_DIPSETTING(      0x6400, "German-ECA" );
		PORT_DIPSETTING(      0x7800, "French-1" );
		PORT_DIPSETTING(      0x3800, "French-2" );
		PORT_DIPSETTING(      0x5800, "French-3" );
		PORT_DIPSETTING(      0x1800, "French-4" );
		PORT_DIPSETTING(      0x6800, "French-ECA" );
		PORT_DIPSETTING(      0x0c00, DEF_STR( "Free_Play") );
		PORT_DIPNAME( 0x8000, 0x0000, "Coinage Source" );
		PORT_DIPSETTING(      0x8000, "Dipswitch" );
		PORT_DIPSETTING(      0x0000, "CMOS" );
	
		PORT_START(); 
		PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_TILT );/* Slam Switch */
		PORT_SERVICE( 0x0010, IP_ACTIVE_LOW );
		PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_COIN3 );
		PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_COIN4 );
		PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_START3 );
		PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_START4 );
		PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_UNUSED );/* volume down */
		PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_UNUSED );/* volume up */
		PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_UNUSED );/* interlock */
		PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNUSED );
	INPUT_PORTS_END(); }}; 
	
	
	static InputPortPtr input_ports_rmpgwt = new InputPortPtr(){ public void handler() { 
		PORT_START(); 
		PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
		PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER2 | IPF_8WAY );
		PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER2 | IPF_8WAY );
		PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER2 | IPF_8WAY );
		PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER2 | IPF_8WAY );
		PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 
		PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER3 | IPF_8WAY );
		PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER3 | IPF_8WAY );
		PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER3 | IPF_8WAY );
		PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER3 | IPF_8WAY );
		PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER3 );
		PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER3 );
		PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER3 );
		PORT_BIT( 0xff80, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 
		PORT_DIPNAME( 0x0001, 0x0000, "Coinage Source" );
		PORT_DIPSETTING(      0x0001, "Dipswitch" );
		PORT_DIPSETTING(      0x0000, "CMOS" );
		PORT_DIPNAME( 0x003e, 0x003e, DEF_STR( "Coinage") );
		PORT_DIPSETTING(      0x003e, "USA-1" );
		PORT_DIPSETTING(      0x003c, "USA-2" );
		PORT_DIPSETTING(      0x003a, "USA-3" );
		PORT_DIPSETTING(      0x0038, "USA-4" );
		PORT_DIPSETTING(      0x0034, "USA-9" );
		PORT_DIPSETTING(      0x0032, "USA-10" );
		PORT_DIPSETTING(      0x0036, "USA-ECA" );
		PORT_DIPSETTING(      0x002e, "German-1" );
		PORT_DIPSETTING(      0x002c, "German-2" );
		PORT_DIPSETTING(      0x002a, "German-3" );
		PORT_DIPSETTING(      0x0028, "German-4" );
		PORT_DIPSETTING(      0x0024, "German-5" );
		PORT_DIPSETTING(      0x0026, "German-ECA" );
		PORT_DIPSETTING(      0x001e, "French-1" );
		PORT_DIPSETTING(      0x001c, "French-2" );
		PORT_DIPSETTING(      0x001a, "French-3" );
		PORT_DIPSETTING(      0x0018, "French-4" );
		PORT_DIPSETTING(      0x0014, "French-11" );
		PORT_DIPSETTING(      0x0012, "French-12" );
		PORT_DIPSETTING(      0x0016, "French-ECA" );
		PORT_DIPSETTING(      0x0030, DEF_STR( "Free_Play") );
		PORT_DIPNAME( 0x0040, 0x0000, "Counters" );
		PORT_DIPSETTING(      0x0040, "One" );
		PORT_DIPSETTING(      0x0000, "Two" );
		PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_DIPNAME( 0x0100, 0x0100, "Bill Validator" );
		PORT_DIPSETTING(      0x0100, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x0000, DEF_STR( "On") );
		PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_DIPNAME( 0x0400, 0x0000, "Powerup Test" );
		PORT_DIPSETTING(      0x0000, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x0400, DEF_STR( "On") );
		PORT_BIT( 0x7800, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_DIPNAME( 0x8000, 0x8000, "Test Switch" );
		PORT_DIPSETTING(      0x8000, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x0000, DEF_STR( "On") );
	
		PORT_START(); 
		PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_TILT );/* Slam Switch */
		PORT_SERVICE( 0x0010, IP_ACTIVE_LOW );
		PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_COIN3 );
		PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_COIN4 );
		PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_START3 );
		PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_START4 );
		PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_UNUSED );/* volume down */
		PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_UNUSED );/* volume up */
		PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_UNUSED );/* interlock */
		PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNUSED );
	INPUT_PORTS_END(); }}; 
	
	
	static InputPortPtr input_ports_wwfmania = new InputPortPtr(){ public void handler() { 
		PORT_START(); 
		PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER1 | IPF_8WAY );
		PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
		PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER2 | IPF_8WAY );
		PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER2 | IPF_8WAY );
		PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER2 | IPF_8WAY );
		PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER2 | IPF_8WAY );
		PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 
		PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER1 );
		PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER1 );
		PORT_BIT( 0x000c, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER2 );
		PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER2 );
		PORT_BIT( 0x00c0, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0xff00, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 
		PORT_DIPNAME( 0x0001, 0x0001, "Test Switch" );
		PORT_DIPSETTING(      0x0001, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x0000, DEF_STR( "On") );
		PORT_DIPNAME( 0x0002, 0x0000, "Powerup Test" );
		PORT_DIPSETTING(      0x0000, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x0002, DEF_STR( "On") );
		PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_DIPNAME( 0x0008, 0x0008, "Realtime Clock" );
		PORT_DIPSETTING(      0x0008, DEF_STR( "No") );
		PORT_DIPSETTING(      0x0000, DEF_STR( "Yes") );
		PORT_BIT( 0x0030, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_DIPNAME( 0x0040, 0x0040, "Bill Validator" );
		PORT_DIPSETTING(      0x0040, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x0000, DEF_STR( "On") );
		PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_DIPNAME( 0x0300, 0x0300, "Counters" );
		PORT_DIPSETTING(      0x0300, "One, 1/1" );
		PORT_DIPSETTING(      0x0200, "One, Totalizing" );
		PORT_DIPSETTING(      0x0100, "Two, 1/1" );
		PORT_DIPNAME( 0x7c00, 0x7c00, DEF_STR( "Coinage") );
		PORT_DIPSETTING(      0x7c00, "USA-1" );
		PORT_DIPSETTING(      0x3c00, "USA-2" );
		PORT_DIPSETTING(      0x5c00, "USA-3" );
		PORT_DIPSETTING(      0x1c00, "USA-4" );
		PORT_DIPSETTING(      0x6c00, "USA-ECA" );
		PORT_DIPSETTING(      0x7400, "German-1" );
		PORT_DIPSETTING(      0x3400, "German-2" );
		PORT_DIPSETTING(      0x5400, "German-3" );
		PORT_DIPSETTING(      0x1400, "German-4" );
		PORT_DIPSETTING(      0x6400, "German-ECA" );
		PORT_DIPSETTING(      0x7800, "French-1" );
		PORT_DIPSETTING(      0x3800, "French-2" );
		PORT_DIPSETTING(      0x5800, "French-3" );
		PORT_DIPSETTING(      0x1800, "French-4" );
		PORT_DIPSETTING(      0x6800, "French-ECA" );
		PORT_DIPSETTING(      0x0c00, DEF_STR( "Free_Play") );
		PORT_DIPNAME( 0x8000, 0x0000, "Coinage Source" );
		PORT_DIPSETTING(      0x8000, "Dipswitch" );
		PORT_DIPSETTING(      0x0000, "CMOS" );
	
		PORT_START(); 
		PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_TILT );/* Slam Switch */
		PORT_SERVICE( 0x0010, IP_ACTIVE_LOW );
		PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_COIN3 );
		PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_COIN4 );
		PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_UNUSED );/* volume down */
		PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_UNUSED );/* volume up */
		PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_UNUSED );/* interrupt */
		PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNUSED );
	INPUT_PORTS_END(); }}; 
	
	
	
	/*************************************
	 *
	 *	34010 configuration
	 *
	 *************************************/
	
	static struct tms34010_config cpu_config =
	{
		0,								/* halt on reset */
		NULL,							/* generate interrupt */
		wms_tunit_to_shiftreg,			/* write to shiftreg function */
		wms_tunit_from_shiftreg,		/* read from shiftreg function */
		wms_tunit_display_addr_changed,	/* display address changed */
		0								/* display interrupt callback */
	};
	
	
	
	/*************************************
	 *
	 *	Machine drivers
	 *
	 *************************************/
	
	static MachineDriver machine_driver_wolfu = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_TMS34010,
				50000000/TMS34010_CLOCK_DIVIDER,	/* 50 MHz */
				readmem,writemem,null,null,
				ignore_interrupt,0,
				null,null,cpu_config
			),
			SOUND_CPU_WILLIAMS_DCS
		},
		MKLA5_FPS, MKLA5_VBLANK_DURATION,	/* frames per second, vblank duration */
		1,
		wms_wolfu_init_machine,
	
		/* video hardware */
		512, 288, new rectangle( 56, 450, 1, 253 ),
	
		null,
		32768, 0,
		0,
	
		VIDEO_TYPE_RASTER,
		null,
		wms_wolfu_vh_start,
		wms_tunit_vh_stop,
		wms_tunit_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			SOUND_WILLIAMS_DCS
		},
		nvram_handler
	);
	
	
	
	/*************************************
	 *
	 *	ROM definitions
	 *
	 *************************************/
	
	static RomLoadPtr rom_mk3 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10, REGION_CPU1, 0 );	/* 34010 dummy region */
	
		ROM_REGION( ADSP2100_SIZE + 0x400000, REGION_CPU2, 0 );/* ADSP-2105 data */
		ROM_LOAD( "umk3-u2.bin",  ADSP2100_SIZE + 0x000000, 0x100000, 0x3838cfe5 );
		ROM_LOAD( "umk3-u3.bin",  ADSP2100_SIZE + 0x100000, 0x100000, 0x856fe411 );
		ROM_LOAD( "umk3-u4.bin",  ADSP2100_SIZE + 0x200000, 0x100000, 0x428a406f );
		ROM_LOAD( "umk3-u5.bin",  ADSP2100_SIZE + 0x300000, 0x100000, 0x3b98a09f );
	
		ROM_REGION16_LE( 0x100000, REGION_USER1, ROMREGION_DISPOSE );/* 34010 code */
		ROM_LOAD16_BYTE( "mk321u54.bin",  0x00000, 0x80000, 0x9e344401 );
		ROM_LOAD16_BYTE( "mk321u63.bin",  0x00001, 0x80000, 0x64d34776 );
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROM_LOAD( "umk-u133.bin",  0x0000000, 0x100000, 0x79b94667 );
		ROM_LOAD( "umk-u132.bin",  0x0100000, 0x100000, 0x13e95228 );
		ROM_LOAD( "umk-u131.bin",  0x0200000, 0x100000, 0x41001e30 );
		ROM_LOAD( "umk-u130.bin",  0x0300000, 0x100000, 0x49379dd7 );
	
		ROM_LOAD( "umk-u129.bin",  0x0400000, 0x100000, 0xa8b41803 );
		ROM_LOAD( "umk-u128.bin",  0x0500000, 0x100000, 0xb410d72f );
		ROM_LOAD( "umk-u127.bin",  0x0600000, 0x100000, 0xbd985be7 );
		ROM_LOAD( "umk-u126.bin",  0x0700000, 0x100000, 0xe7c32cf4 );
	
		ROM_LOAD( "umk-u125.bin",  0x0800000, 0x100000, 0x9a52227e );
		ROM_LOAD( "umk-u124.bin",  0x0900000, 0x100000, 0x5c750ebc );
		ROM_LOAD( "umk-u123.bin",  0x0a00000, 0x100000, 0xf0ab88a8 );
		ROM_LOAD( "umk-u122.bin",  0x0b00000, 0x100000, 0x9b87cdac );
	
		ROM_LOAD( "mk3-u121.bin",  0x0c00000, 0x100000, 0xb6c6296a );
		ROM_LOAD( "mk3-u120.bin",  0x0d00000, 0x100000, 0x8d1ccc3b );
		ROM_LOAD( "mk3-u119.bin",  0x0e00000, 0x100000, 0x63215b59 );
		ROM_LOAD( "mk3-u118.bin",  0x0f00000, 0x100000, 0x8b681e34 );
	
		ROM_LOAD( "mk3-u117.bin",  0x1000000, 0x080000, 0x1ab20377 );
		ROM_LOAD( "mk3-u116.bin",  0x1100000, 0x080000, 0xba246ad0 );
		ROM_LOAD( "mk3-u115.bin",  0x1200000, 0x080000, 0x3ee8b124 );
		ROM_LOAD( "mk3-u114.bin",  0x1300000, 0x080000, 0xa8d99922 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_mk3r20 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10, REGION_CPU1, 0 );	/* 34010 dummy region */
	
		ROM_REGION( ADSP2100_SIZE + 0x400000, REGION_CPU2, 0 );/* ADSP-2105 data */
		ROM_LOAD( "umk3-u2.bin",  ADSP2100_SIZE + 0x000000, 0x100000, 0x3838cfe5 );
		ROM_LOAD( "umk3-u3.bin",  ADSP2100_SIZE + 0x100000, 0x100000, 0x856fe411 );
		ROM_LOAD( "umk3-u4.bin",  ADSP2100_SIZE + 0x200000, 0x100000, 0x428a406f );
		ROM_LOAD( "umk3-u5.bin",  ADSP2100_SIZE + 0x300000, 0x100000, 0x3b98a09f );
	
		ROM_REGION16_LE( 0x100000, REGION_USER1, ROMREGION_DISPOSE );/* 34010 code */
		ROM_LOAD16_BYTE( "mk320u54.bin",  0x00000, 0x80000, 0x453da302 );
		ROM_LOAD16_BYTE( "mk320u63.bin",  0x00001, 0x80000, 0xf8dc0600 );
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROM_LOAD( "umk-u133.bin",  0x0000000, 0x100000, 0x79b94667 );
		ROM_LOAD( "umk-u132.bin",  0x0100000, 0x100000, 0x13e95228 );
		ROM_LOAD( "umk-u131.bin",  0x0200000, 0x100000, 0x41001e30 );
		ROM_LOAD( "umk-u130.bin",  0x0300000, 0x100000, 0x49379dd7 );
	
		ROM_LOAD( "umk-u129.bin",  0x0400000, 0x100000, 0xa8b41803 );
		ROM_LOAD( "umk-u128.bin",  0x0500000, 0x100000, 0xb410d72f );
		ROM_LOAD( "umk-u127.bin",  0x0600000, 0x100000, 0xbd985be7 );
		ROM_LOAD( "umk-u126.bin",  0x0700000, 0x100000, 0xe7c32cf4 );
	
		ROM_LOAD( "umk-u125.bin",  0x0800000, 0x100000, 0x9a52227e );
		ROM_LOAD( "umk-u124.bin",  0x0900000, 0x100000, 0x5c750ebc );
		ROM_LOAD( "umk-u123.bin",  0x0a00000, 0x100000, 0xf0ab88a8 );
		ROM_LOAD( "umk-u122.bin",  0x0b00000, 0x100000, 0x9b87cdac );
	
		ROM_LOAD( "mk3-u121.bin",  0x0c00000, 0x100000, 0xb6c6296a );
		ROM_LOAD( "mk3-u120.bin",  0x0d00000, 0x100000, 0x8d1ccc3b );
		ROM_LOAD( "mk3-u119.bin",  0x0e00000, 0x100000, 0x63215b59 );
		ROM_LOAD( "mk3-u118.bin",  0x0f00000, 0x100000, 0x8b681e34 );
	
		ROM_LOAD( "mk3-u117.bin",  0x1000000, 0x080000, 0x1ab20377 );
		ROM_LOAD( "mk3-u116.bin",  0x1100000, 0x080000, 0xba246ad0 );
		ROM_LOAD( "mk3-u115.bin",  0x1200000, 0x080000, 0x3ee8b124 );
		ROM_LOAD( "mk3-u114.bin",  0x1300000, 0x080000, 0xa8d99922 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_mk3r10 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10, REGION_CPU1, 0 );	/* 34010 dummy region */
	
		ROM_REGION( ADSP2100_SIZE + 0x400000, REGION_CPU2, 0 );/* ADSP-2105 data */
		ROM_LOAD( "umk3-u2.bin",  ADSP2100_SIZE + 0x000000, 0x100000, 0x3838cfe5 );
		ROM_LOAD( "umk3-u3.bin",  ADSP2100_SIZE + 0x100000, 0x100000, 0x856fe411 );
		ROM_LOAD( "umk3-u4.bin",  ADSP2100_SIZE + 0x200000, 0x100000, 0x428a406f );
		ROM_LOAD( "umk3-u5.bin",  ADSP2100_SIZE + 0x300000, 0x100000, 0x3b98a09f );
	
		ROM_REGION16_LE( 0x100000, REGION_USER1, ROMREGION_DISPOSE );/* 34010 code */
		ROM_LOAD16_BYTE( "mk310u54.bin",  0x00000, 0x80000, 0x41829228 );
		ROM_LOAD16_BYTE( "mk310u63.bin",  0x00001, 0x80000, 0xb074e1e8 );
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROM_LOAD( "umk-u133.bin",  0x0000000, 0x100000, 0x79b94667 );
		ROM_LOAD( "umk-u132.bin",  0x0100000, 0x100000, 0x13e95228 );
		ROM_LOAD( "umk-u131.bin",  0x0200000, 0x100000, 0x41001e30 );
		ROM_LOAD( "umk-u130.bin",  0x0300000, 0x100000, 0x49379dd7 );
	
		ROM_LOAD( "umk-u129.bin",  0x0400000, 0x100000, 0xa8b41803 );
		ROM_LOAD( "umk-u128.bin",  0x0500000, 0x100000, 0xb410d72f );
		ROM_LOAD( "umk-u127.bin",  0x0600000, 0x100000, 0xbd985be7 );
		ROM_LOAD( "umk-u126.bin",  0x0700000, 0x100000, 0xe7c32cf4 );
	
		ROM_LOAD( "umk-u125.bin",  0x0800000, 0x100000, 0x9a52227e );
		ROM_LOAD( "umk-u124.bin",  0x0900000, 0x100000, 0x5c750ebc );
		ROM_LOAD( "umk-u123.bin",  0x0a00000, 0x100000, 0xf0ab88a8 );
		ROM_LOAD( "umk-u122.bin",  0x0b00000, 0x100000, 0x9b87cdac );
	
		ROM_LOAD( "mk3-u121.bin",  0x0c00000, 0x100000, 0xb6c6296a );
		ROM_LOAD( "mk3-u120.bin",  0x0d00000, 0x100000, 0x8d1ccc3b );
		ROM_LOAD( "mk3-u119.bin",  0x0e00000, 0x100000, 0x63215b59 );
		ROM_LOAD( "mk3-u118.bin",  0x0f00000, 0x100000, 0x8b681e34 );
	
		ROM_LOAD( "mk3-u117.bin",  0x1000000, 0x080000, 0x1ab20377 );
		ROM_LOAD( "mk3-u116.bin",  0x1100000, 0x080000, 0xba246ad0 );
		ROM_LOAD( "mk3-u115.bin",  0x1200000, 0x080000, 0x3ee8b124 );
		ROM_LOAD( "mk3-u114.bin",  0x1300000, 0x080000, 0xa8d99922 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_umk3 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10, REGION_CPU1, 0 );	/* 34010 dummy region */
	
		ROM_REGION( ADSP2100_SIZE + 0x400000, REGION_CPU2, 0 );/* ADSP-2105 data */
		ROM_LOAD( "umk3-u2.bin",  ADSP2100_SIZE + 0x000000, 0x100000, 0x3838cfe5 );
		ROM_LOAD( "umk3-u3.bin",  ADSP2100_SIZE + 0x100000, 0x100000, 0x856fe411 );
		ROM_LOAD( "umk3-u4.bin",  ADSP2100_SIZE + 0x200000, 0x100000, 0x428a406f );
		ROM_LOAD( "umk3-u5.bin",  ADSP2100_SIZE + 0x300000, 0x100000, 0x3b98a09f );
	
		ROM_REGION16_LE( 0x100000, REGION_USER1, ROMREGION_DISPOSE );/* 34010 code */
		ROM_LOAD16_BYTE( "um312u54.bin",  0x00000, 0x80000, 0x712b4db6 );
		ROM_LOAD16_BYTE( "um312u63.bin",  0x00001, 0x80000, 0x6d301faf );
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROM_LOAD( "umk-u133.bin",  0x0000000, 0x100000, 0x79b94667 );
		ROM_LOAD( "umk-u132.bin",  0x0100000, 0x100000, 0x13e95228 );
		ROM_LOAD( "umk-u131.bin",  0x0200000, 0x100000, 0x41001e30 );
		ROM_LOAD( "umk-u130.bin",  0x0300000, 0x100000, 0x49379dd7 );
	
		ROM_LOAD( "umk-u129.bin",  0x0400000, 0x100000, 0xa8b41803 );
		ROM_LOAD( "umk-u128.bin",  0x0500000, 0x100000, 0xb410d72f );
		ROM_LOAD( "umk-u127.bin",  0x0600000, 0x100000, 0xbd985be7 );
		ROM_LOAD( "umk-u126.bin",  0x0700000, 0x100000, 0xe7c32cf4 );
	
		ROM_LOAD( "umk-u125.bin",  0x0800000, 0x100000, 0x9a52227e );
		ROM_LOAD( "umk-u124.bin",  0x0900000, 0x100000, 0x5c750ebc );
		ROM_LOAD( "umk-u123.bin",  0x0a00000, 0x100000, 0xf0ab88a8 );
		ROM_LOAD( "umk-u122.bin",  0x0b00000, 0x100000, 0x9b87cdac );
	
		ROM_LOAD( "umk-u121.bin",  0x0c00000, 0x100000, 0xcc4b95db );
		ROM_LOAD( "umk-u120.bin",  0x0d00000, 0x100000, 0x1c8144cd );
		ROM_LOAD( "umk-u119.bin",  0x0e00000, 0x100000, 0x5f10c543 );
		ROM_LOAD( "umk-u118.bin",  0x0f00000, 0x100000, 0xde0c4488 );
	
		ROM_LOAD( "umk-u113.bin",  0x1400000, 0x100000, 0x99d74a1e );
		ROM_LOAD( "umk-u112.bin",  0x1500000, 0x100000, 0xb5a46488 );
		ROM_LOAD( "umk-u111.bin",  0x1600000, 0x100000, 0xa87523c8 );
		ROM_LOAD( "umk-u110.bin",  0x1700000, 0x100000, 0x0038f205 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_umk3r11 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10, REGION_CPU1, 0 );	/* 34010 dummy region */
	
		ROM_REGION( ADSP2100_SIZE + 0x400000, REGION_CPU2, 0 );/* ADSP-2105 data */
		ROM_LOAD( "umk3-u2.bin",  ADSP2100_SIZE + 0x000000, 0x100000, 0x3838cfe5 );
		ROM_LOAD( "umk3-u3.bin",  ADSP2100_SIZE + 0x100000, 0x100000, 0x856fe411 );
		ROM_LOAD( "umk3-u4.bin",  ADSP2100_SIZE + 0x200000, 0x100000, 0x428a406f );
		ROM_LOAD( "umk3-u5.bin",  ADSP2100_SIZE + 0x300000, 0x100000, 0x3b98a09f );
	
		ROM_REGION16_LE( 0x100000, REGION_USER1, ROMREGION_DISPOSE );/* 34010 code */
		ROM_LOAD16_BYTE( "um311u54.bin",  0x00000, 0x80000, 0x8bb27659 );
		ROM_LOAD16_BYTE( "um311u63.bin",  0x00001, 0x80000, 0xea731783 );
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROM_LOAD( "umk-u133.bin",  0x0000000, 0x100000, 0x79b94667 );
		ROM_LOAD( "umk-u132.bin",  0x0100000, 0x100000, 0x13e95228 );
		ROM_LOAD( "umk-u131.bin",  0x0200000, 0x100000, 0x41001e30 );
		ROM_LOAD( "umk-u130.bin",  0x0300000, 0x100000, 0x49379dd7 );
	
		ROM_LOAD( "umk-u129.bin",  0x0400000, 0x100000, 0xa8b41803 );
		ROM_LOAD( "umk-u128.bin",  0x0500000, 0x100000, 0xb410d72f );
		ROM_LOAD( "umk-u127.bin",  0x0600000, 0x100000, 0xbd985be7 );
		ROM_LOAD( "umk-u126.bin",  0x0700000, 0x100000, 0xe7c32cf4 );
	
		ROM_LOAD( "umk-u125.bin",  0x0800000, 0x100000, 0x9a52227e );
		ROM_LOAD( "umk-u124.bin",  0x0900000, 0x100000, 0x5c750ebc );
		ROM_LOAD( "umk-u123.bin",  0x0a00000, 0x100000, 0xf0ab88a8 );
		ROM_LOAD( "umk-u122.bin",  0x0b00000, 0x100000, 0x9b87cdac );
	
		ROM_LOAD( "umk-u121.bin",  0x0c00000, 0x100000, 0xcc4b95db );
		ROM_LOAD( "umk-u120.bin",  0x0d00000, 0x100000, 0x1c8144cd );
		ROM_LOAD( "umk-u119.bin",  0x0e00000, 0x100000, 0x5f10c543 );
		ROM_LOAD( "umk-u118.bin",  0x0f00000, 0x100000, 0xde0c4488 );
	
		ROM_LOAD( "umk-u113.bin",  0x1400000, 0x100000, 0x99d74a1e );
		ROM_LOAD( "umk-u112.bin",  0x1500000, 0x100000, 0xb5a46488 );
		ROM_LOAD( "umk-u111.bin",  0x1600000, 0x100000, 0xa87523c8 );
		ROM_LOAD( "umk-u110.bin",  0x1700000, 0x100000, 0x0038f205 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_openice = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10, REGION_CPU1, 0 );	/* 34010 dummy region */
	
		ROM_REGION( ADSP2100_SIZE + 0x400000, REGION_CPU2, 0 );/* ADSP-2105 data */
		ROM_LOAD( "oiceu2.bin",  ADSP2100_SIZE + 0x000000, 0x100000, 0x8adb5aab );
		ROM_LOAD( "oiceu3.bin",  ADSP2100_SIZE + 0x100000, 0x100000, 0x11c61ad6 );
		ROM_LOAD( "oiceu4.bin",  ADSP2100_SIZE + 0x200000, 0x100000, 0x04279290 );
		ROM_LOAD( "oiceu5.bin",  ADSP2100_SIZE + 0x300000, 0x100000, 0xe90ad61f );
	
		ROM_REGION16_LE( 0x100000, REGION_USER1, ROMREGION_DISPOSE );/* 34010 code */
		ROM_LOAD16_BYTE( "oiceu54.bin",  0x00000, 0x80000, 0xe4225284 );
		ROM_LOAD16_BYTE( "oiceu63.bin",  0x00001, 0x80000, 0x97d308a3 );
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROM_LOAD( "oiceu133.bin",  0x0000000, 0x100000, 0x8a81605c );
		ROM_LOAD( "oiceu132.bin",  0x0100000, 0x100000, 0xcfdd6702 );
		ROM_LOAD( "oiceu131.bin",  0x0200000, 0x100000, 0xcc428eb7 );
		ROM_LOAD( "oiceu130.bin",  0x0300000, 0x100000, 0x74c2d50c );
	
		ROM_LOAD( "oiceu129.bin",  0x0400000, 0x100000, 0x9e2ff012 );
		ROM_LOAD( "oiceu128.bin",  0x0500000, 0x100000, 0x35d2e610 );
		ROM_LOAD( "oiceu127.bin",  0x0600000, 0x100000, 0xbcbf19fe );
		ROM_LOAD( "oiceu126.bin",  0x0700000, 0x100000, 0x8e3106ae );
	
		ROM_LOAD( "oiceu125.bin",  0x0800000, 0x100000, 0xa7b54550 );
		ROM_LOAD( "oiceu124.bin",  0x0900000, 0x100000, 0x7c02cb50 );
		ROM_LOAD( "oiceu123.bin",  0x0a00000, 0x100000, 0xd543bd9d );
		ROM_LOAD( "oiceu122.bin",  0x0b00000, 0x100000, 0x3744d291 );
	
		ROM_LOAD( "oiceu121.bin",  0x0c00000, 0x100000, 0xacd2f7c7 );
		ROM_LOAD( "oiceu120.bin",  0x0d00000, 0x100000, 0x4295686a );
		ROM_LOAD( "oiceu119.bin",  0x0e00000, 0x100000, 0x948b9b27 );
		ROM_LOAD( "oiceu118.bin",  0x0f00000, 0x100000, 0x9eaaf93e );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_nbahangt = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10, REGION_CPU1, 0 );	/* 34010 dummy region */
	
		ROM_REGION( ADSP2100_SIZE + 0x400000, REGION_CPU2, 0 );/* ADSP-2105 data */
		ROM_LOAD( "mhtu2.bin",  ADSP2100_SIZE + 0x000000, 0x100000, 0x3f0b0d0a );
		ROM_LOAD( "mhtu3.bin",  ADSP2100_SIZE + 0x100000, 0x100000, 0xec1db988 );
		ROM_LOAD( "mhtu4.bin",  ADSP2100_SIZE + 0x200000, 0x100000, 0xc7f847a3 );
		ROM_LOAD( "mhtu5.bin",  ADSP2100_SIZE + 0x300000, 0x100000, 0xef19316a );
	
		ROM_REGION16_LE( 0x100000, REGION_USER1, ROMREGION_DISPOSE );/* 34010 code */
		ROM_LOAD16_BYTE( "htime54.bin",  0x00000, 0x80000, 0xc2875d98 );
		ROM_LOAD16_BYTE( "htime63.bin",  0x00001, 0x80000, 0x6f4728c3 );
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROM_LOAD( "mhtu133.bin",  0x0000000, 0x100000, 0x3163feed );
		ROM_LOAD( "mhtu132.bin",  0x0100000, 0x100000, 0x428eaf44 );
		ROM_LOAD( "mhtu131.bin",  0x0200000, 0x100000, 0x5f7c5111 );
		ROM_LOAD( "mhtu130.bin",  0x0300000, 0x100000, 0xc7c0c514 );
	
		ROM_LOAD( "mhtu129.bin",  0x0400000, 0x100000, 0xb3d0daa0 );
		ROM_LOAD( "mhtu128.bin",  0x0500000, 0x100000, 0x3704ee69 );
		ROM_LOAD( "mhtu127.bin",  0x0600000, 0x100000, 0x4ea64d5a );
		ROM_LOAD( "mhtu126.bin",  0x0700000, 0x100000, 0x0c5c19b7 );
	
		ROM_LOAD( "mhtu125.bin",  0x0800000, 0x100000, 0x46c43d67 );
		ROM_LOAD( "mhtu124.bin",  0x0900000, 0x100000, 0xed495156 );
		ROM_LOAD( "mhtu123.bin",  0x0a00000, 0x100000, 0xb48aa5da );
		ROM_LOAD( "mhtu122.bin",  0x0b00000, 0x100000, 0xb18cd181 );
	
		ROM_LOAD( "mhtu121.bin",  0x0c00000, 0x100000, 0x5acb267a );
		ROM_LOAD( "mhtu120.bin",  0x0d00000, 0x100000, 0x28e05f86 );
		ROM_LOAD( "mhtu119.bin",  0x0e00000, 0x100000, 0xb4f604ea );
		ROM_LOAD( "mhtu118.bin",  0x0f00000, 0x100000, 0xa257b973 );
	
		ROM_LOAD( "mhtu113.bin",  0x1400000, 0x100000, 0xd712a779 );
		ROM_LOAD( "mhtu112.bin",  0x1500000, 0x100000, 0x644e1bca );
		ROM_LOAD( "mhtu111.bin",  0x1600000, 0x100000, 0x10d3b768 );
		ROM_LOAD( "mhtu110.bin",  0x1700000, 0x100000, 0x8575aeb2 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_nbamaxht = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10, REGION_CPU1, 0 );	/* 34010 dummy region */
	
		ROM_REGION( ADSP2100_SIZE + 0x400000, REGION_CPU2, 0 );/* ADSP-2105 data */
		ROM_LOAD( "mhtu2.bin",  ADSP2100_SIZE + 0x000000, 0x100000, 0x3f0b0d0a );
		ROM_LOAD( "mhtu3.bin",  ADSP2100_SIZE + 0x100000, 0x100000, 0xec1db988 );
		ROM_LOAD( "mhtu4.bin",  ADSP2100_SIZE + 0x200000, 0x100000, 0xc7f847a3 );
		ROM_LOAD( "mhtu5.bin",  ADSP2100_SIZE + 0x300000, 0x100000, 0xef19316a );
	
		ROM_REGION16_LE( 0x100000, REGION_USER1, ROMREGION_DISPOSE );/* 34010 code */
		ROM_LOAD16_BYTE( "mhtu54.bin",  0x00000, 0x80000, 0xdfb6b3ae );
		ROM_LOAD16_BYTE( "mhtu63.bin",  0x00001, 0x80000, 0x78da472c );
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROM_LOAD( "mhtu133.bin",  0x0000000, 0x100000, 0x3163feed );
		ROM_LOAD( "mhtu132.bin",  0x0100000, 0x100000, 0x428eaf44 );
		ROM_LOAD( "mhtu131.bin",  0x0200000, 0x100000, 0x5f7c5111 );
		ROM_LOAD( "mhtu130.bin",  0x0300000, 0x100000, 0xc7c0c514 );
	
		ROM_LOAD( "mhtu129.bin",  0x0400000, 0x100000, 0xb3d0daa0 );
		ROM_LOAD( "mhtu128.bin",  0x0500000, 0x100000, 0x3704ee69 );
		ROM_LOAD( "mhtu127.bin",  0x0600000, 0x100000, 0x4ea64d5a );
		ROM_LOAD( "mhtu126.bin",  0x0700000, 0x100000, 0x0c5c19b7 );
	
		ROM_LOAD( "mhtu125.bin",  0x0800000, 0x100000, 0x46c43d67 );
		ROM_LOAD( "mhtu124.bin",  0x0900000, 0x100000, 0xed495156 );
		ROM_LOAD( "mhtu123.bin",  0x0a00000, 0x100000, 0xb48aa5da );
		ROM_LOAD( "mhtu122.bin",  0x0b00000, 0x100000, 0xb18cd181 );
	
		ROM_LOAD( "mhtu121.bin",  0x0c00000, 0x100000, 0x5acb267a );
		ROM_LOAD( "mhtu120.bin",  0x0d00000, 0x100000, 0x28e05f86 );
		ROM_LOAD( "mhtu119.bin",  0x0e00000, 0x100000, 0xb4f604ea );
		ROM_LOAD( "mhtu118.bin",  0x0f00000, 0x100000, 0xa257b973 );
	
		ROM_LOAD( "mhtu113.bin",  0x1400000, 0x100000, 0xd712a779 );
		ROM_LOAD( "mhtu112.bin",  0x1500000, 0x100000, 0x644e1bca );
		ROM_LOAD( "mhtu111.bin",  0x1600000, 0x100000, 0x10d3b768 );
		ROM_LOAD( "mhtu110.bin",  0x1700000, 0x100000, 0x8575aeb2 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_rmpgwt = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10, REGION_CPU1, 0 );	/* 34010 dummy region */
	
		ROM_REGION( ADSP2100_SIZE + 0x400000, REGION_CPU2, 0 );/* ADSP-2105 data */
		ROM_LOAD( "rwt.2",  ADSP2100_SIZE + 0x000000, 0x100000, 0x0e82f83d );
		ROM_LOAD( "rwt.3",  ADSP2100_SIZE + 0x100000, 0x100000, 0x3ff54d15 );
		ROM_LOAD( "rwt.4",  ADSP2100_SIZE + 0x200000, 0x100000, 0x5c7f5656 );
		ROM_LOAD( "rwt.5",  ADSP2100_SIZE + 0x300000, 0x100000, 0xfd9aaf24 );
	
		ROM_REGION16_LE( 0x100000, REGION_USER1, ROMREGION_DISPOSE );/* 34010 code */
		ROM_LOAD16_BYTE( "rwtr13.54",  0x00000, 0x80000, 0x2a8f6e1e );
		ROM_LOAD16_BYTE( "rwtr13.63",  0x00001, 0x80000, 0x403ae41e );
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROM_LOAD( "rwt.133",  0x0000000, 0x100000, 0x5b5ac449 );
		ROM_LOAD( "rwt.132",  0x0100000, 0x100000, 0x7b3f09c6 );
		ROM_LOAD( "rwt.131",  0x0200000, 0x100000, 0xfdecf12e );
		ROM_LOAD( "rwt.130",  0x0300000, 0x100000, 0x4a983b05 );
	
		ROM_LOAD( "rwt.129",  0x0400000, 0x100000, 0xdc495c6e );
		ROM_LOAD( "rwt.128",  0x0500000, 0x100000, 0x5545503d );
		ROM_LOAD( "rwt.127",  0x0600000, 0x100000, 0x6e1756ba );
		ROM_LOAD( "rwt.126",  0x0700000, 0x100000, 0xc300eb1b );
	
		ROM_LOAD( "rwt.125",  0x0800000, 0x100000, 0x7369bf5d );
		ROM_LOAD( "rwt.124",  0x0900000, 0x100000, 0xc0bf88c8 );
		ROM_LOAD( "rwt.123",  0x0a00000, 0x100000, 0xac4c712a );
		ROM_LOAD( "rwt.122",  0x0b00000, 0x100000, 0x609862a2 );
	
		ROM_LOAD( "rwt.121",  0x0c00000, 0x100000, 0xf65119b7 );
		ROM_LOAD( "rwt.120",  0x0d00000, 0x100000, 0x6d643dee );
		ROM_LOAD( "rwt.119",  0x0e00000, 0x100000, 0x4e49c133 );
		ROM_LOAD( "rwt.118",  0x0f00000, 0x100000, 0x43a6f51e );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_rmpgwt11 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10, REGION_CPU1, 0 );	/* 34010 dummy region */
	
		ROM_REGION( ADSP2100_SIZE + 0x400000, REGION_CPU2, 0 );/* ADSP-2105 data */
		ROM_LOAD( "rwt.2",  ADSP2100_SIZE + 0x000000, 0x100000, 0x0e82f83d );
		ROM_LOAD( "rwt.3",  ADSP2100_SIZE + 0x100000, 0x100000, 0x3ff54d15 );
		ROM_LOAD( "rwt.4",  ADSP2100_SIZE + 0x200000, 0x100000, 0x5c7f5656 );
		ROM_LOAD( "rwt.5",  ADSP2100_SIZE + 0x300000, 0x100000, 0xfd9aaf24 );
	
		ROM_REGION16_LE( 0x100000, REGION_USER1, ROMREGION_DISPOSE );/* 34010 code */
		ROM_LOAD16_BYTE( "rwtr11.54",  0x00000, 0x80000, 0x3aa514eb );
		ROM_LOAD16_BYTE( "rwtr11.63",  0x00001, 0x80000, 0x031c908f );
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROM_LOAD( "rwt.133",  0x0000000, 0x100000, 0x5b5ac449 );
		ROM_LOAD( "rwt.132",  0x0100000, 0x100000, 0x7b3f09c6 );
		ROM_LOAD( "rwt.131",  0x0200000, 0x100000, 0xfdecf12e );
		ROM_LOAD( "rwt.130",  0x0300000, 0x100000, 0x4a983b05 );
	
		ROM_LOAD( "rwt.129",  0x0400000, 0x100000, 0xdc495c6e );
		ROM_LOAD( "rwt.128",  0x0500000, 0x100000, 0x5545503d );
		ROM_LOAD( "rwt.127",  0x0600000, 0x100000, 0x6e1756ba );
		ROM_LOAD( "rwt.126",  0x0700000, 0x100000, 0xc300eb1b );
	
		ROM_LOAD( "rwt.125",  0x0800000, 0x100000, 0x7369bf5d );
		ROM_LOAD( "rwt.124",  0x0900000, 0x100000, 0xc0bf88c8 );
		ROM_LOAD( "rwt.123",  0x0a00000, 0x100000, 0xac4c712a );
		ROM_LOAD( "rwt.122",  0x0b00000, 0x100000, 0x609862a2 );
	
		ROM_LOAD( "rwt.121",  0x0c00000, 0x100000, 0xf65119b7 );
		ROM_LOAD( "rwt.120",  0x0d00000, 0x100000, 0x6d643dee );
		ROM_LOAD( "rwt.119",  0x0e00000, 0x100000, 0x4e49c133 );
		ROM_LOAD( "rwt.118",  0x0f00000, 0x100000, 0x43a6f51e );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_wwfmania = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10, REGION_CPU1, 0 );	/* 34010 dummy region */
	
		ROM_REGION( ADSP2100_SIZE + 0x400000, REGION_CPU2, 0 );/* ADSP-2105 data */
		ROM_LOAD( "wwf.2",  ADSP2100_SIZE + 0x000000, 0x100000, 0xa9acb250 );
		ROM_LOAD( "wwf.3",  ADSP2100_SIZE + 0x100000, 0x100000, 0x9442b6c9 );
		ROM_LOAD( "wwf.4",  ADSP2100_SIZE + 0x200000, 0x100000, 0xcee78fac );
		ROM_LOAD( "wwf.5",  ADSP2100_SIZE + 0x300000, 0x100000, 0x5b31fd40 );
	
		ROM_REGION16_LE( 0x100000, REGION_USER1, ROMREGION_DISPOSE );/* 34010 code */
		ROM_LOAD16_BYTE( "wwf.54",  0x00000, 0x80000, 0xeeb7bf58 );
		ROM_LOAD16_BYTE( "wwf.63",  0x00001, 0x80000, 0x09759529 );
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROM_LOAD( "wwf.133",  0x0000000, 0x100000, 0x5e1b1e3d );
		ROM_LOAD( "wwf.132",  0x0100000, 0x100000, 0x5943b3b2 );
		ROM_LOAD( "wwf.131",  0x0200000, 0x100000, 0x0815db22 );
		ROM_LOAD( "wwf.130",  0x0300000, 0x100000, 0x9ee9a145 );
	
		ROM_LOAD( "wwf.129",  0x0400000, 0x100000, 0xc644c2f4 );
		ROM_LOAD( "wwf.128",  0x0500000, 0x100000, 0xfcda4e9a );
		ROM_LOAD( "wwf.127",  0x0600000, 0x100000, 0x45be7428 );
		ROM_LOAD( "wwf.126",  0x0700000, 0x100000, 0xeaa276a8 );
	
		ROM_LOAD( "wwf.125",  0x0800000, 0x100000, 0xa19ebeed );
		ROM_LOAD( "wwf.124",  0x0900000, 0x100000, 0xdc7d3dbb );
		ROM_LOAD( "wwf.123",  0x0a00000, 0x100000, 0xe0ade56f );
		ROM_LOAD( "wwf.122",  0x0b00000, 0x100000, 0x2800c78d );
	
		ROM_LOAD( "wwf.121",  0x0c00000, 0x100000, 0xa28ffcba );
		ROM_LOAD( "wwf.120",  0x0d00000, 0x100000, 0x3a05d371 );
		ROM_LOAD( "wwf.119",  0x0e00000, 0x100000, 0x97ffa659 );
		ROM_LOAD( "wwf.118",  0x0f00000, 0x100000, 0x46668e97 );
	ROM_END(); }}; 
	
	
	
	/*************************************
	 *
	 *	Game drivers
	 *
	 *************************************/
	
	public static GameDriver driver_mk3	   = new GameDriver("1994"	,"mk3"	,"wmswolfu.java"	,rom_mk3,null	,machine_driver_wolfu	,input_ports_mk3	,init_mk3	,ROT0	,	"Midway", "Mortal Kombat 3 (rev 2.1)" )
	public static GameDriver driver_mk3r20	   = new GameDriver("1994"	,"mk3r20"	,"wmswolfu.java"	,rom_mk3r20,driver_mk3	,machine_driver_wolfu	,input_ports_mk3	,init_mk3r20	,ROT0	,	"Midway", "Mortal Kombat 3 (rev 2.0)" )
	public static GameDriver driver_mk3r10	   = new GameDriver("1994"	,"mk3r10"	,"wmswolfu.java"	,rom_mk3r10,driver_mk3	,machine_driver_wolfu	,input_ports_mk3	,init_mk3r10	,ROT0	,	"Midway", "Mortal Kombat 3 (rev 1.0)" )
	public static GameDriver driver_umk3	   = new GameDriver("1994"	,"umk3"	,"wmswolfu.java"	,rom_umk3,driver_mk3	,machine_driver_wolfu	,input_ports_mk3	,init_umk3	,ROT0	,	"Midway", "Ultimate Mortal Kombat 3 (rev 1.2)" )
	public static GameDriver driver_umk3r11	   = new GameDriver("1994"	,"umk3r11"	,"wmswolfu.java"	,rom_umk3r11,driver_mk3	,machine_driver_wolfu	,input_ports_mk3	,init_umk3r11	,ROT0	,	"Midway", "Ultimate Mortal Kombat 3 (rev 1.1)" )
	
	public static GameDriver driver_wwfmania	   = new GameDriver("1995"	,"wwfmania"	,"wmswolfu.java"	,rom_wwfmania,null	,machine_driver_wolfu	,input_ports_wwfmania	,init_wwfmania	,ROT0	,	"Midway", "WWF: Wrestlemania (rev 1.30)" )
	public static GameDriver driver_openice	   = new GameDriver("1995"	,"openice"	,"wmswolfu.java"	,rom_openice,null	,machine_driver_wolfu	,input_ports_openice	,init_openice	,ROT0	,	"Midway", "2 On 2 Open Ice Challenge (rev 1.21)" )
	public static GameDriver driver_nbahangt	   = new GameDriver("1996"	,"nbahangt"	,"wmswolfu.java"	,rom_nbahangt,null	,machine_driver_wolfu	,input_ports_nbahangt	,init_nbahangt	,ROT0	,	"Midway", "NBA Hangtime (rev L1.1)" )
	public static GameDriver driver_nbamaxht	   = new GameDriver("1996"	,"nbamaxht"	,"wmswolfu.java"	,rom_nbamaxht,driver_nbahangt	,machine_driver_wolfu	,input_ports_nbahangt	,init_nbahangt	,ROT0	,	"Midway", "NBA Maximum Hangtime (rev 1.0)" )
	public static GameDriver driver_rmpgwt	   = new GameDriver("1997"	,"rmpgwt"	,"wmswolfu.java"	,rom_rmpgwt,null	,machine_driver_wolfu	,input_ports_rmpgwt	,init_rmpgwt	,ROT0	,	"Midway", "Rampage: World Tour (rev 1.3)" )
	public static GameDriver driver_rmpgwt11	   = new GameDriver("1997"	,"rmpgwt11"	,"wmswolfu.java"	,rom_rmpgwt11,driver_rmpgwt	,machine_driver_wolfu	,input_ports_rmpgwt	,init_rmpgwt	,ROT0	,	"Midway", "Rampage: World Tour (rev 1.1)" )
}
