/***************************************************************************

  Capcom System 2
  ===============


    Driver by Paul Leaman (paul@vortexcomputing.demon.co.uk)

    Thanks to Raz, Crashtest and the CPS2 decryption team without whom
    none of this would have been possible.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package drivers;

public class cps2
{
	
	
	/*
	Export this function so that the vidhrdw routine can drive the
	Q-Sound hardware
	*/
	WRITE16_HANDLER( cps2_qsound_sharedram_w )
	{
	    qsound_sharedram1_w(offset/2, data, 0xff00);
	}
	
	/* Maximum size of Q Sound Z80 region */
	#define QSOUND_SIZE 0x50000
	
	/* Maximum 680000 code size */
	#undef  CODE_SIZE
	#define CODE_SIZE   0x0400000
	
	
	extern data16_t *cps2_objram1,*cps2_objram2;
	extern data16_t *cps2_output;
	extern size_t cps2_output_size;
	extern 
	
	int cps2_interrupt(void)
	{
		/* 2 is vblank, 4 is some sort of scanline interrupt, 6 is both at the same time. */
	
	//usrintf_showmessage("%04x %04x %04x",cps1_output[0x4e/2],cps1_output[0x50/2],cps1_output[0x52/2]);
		if (cpu_getiloops() == 0)
			return 2;
		else if (~cps1_output[0x4e/2] & 0x200)
		{
			if (cps1_output[0x52/2])	/* scanline counter? */
				cps1_output[0x52/2]--;
			return 4;
		}
	
		return ignore_interrupt();
	}
	
	
	
	static struct EEPROM_interface cps2_eeprom_interface =
	{
		6,		/* address bits */
		16,		/* data bits */
		"0110",	/*  read command */
		"0101",	/* write command */
		"0111"	/* erase command */
	};
	
	static void cps2_nvram_handler(void *file,int read_or_write)
	{
		if (read_or_write)
			EEPROM_save(file);
		else
		{
	        EEPROM_init(&cps2_eeprom_interface);
	
			if (file)
				EEPROM_load(file);
		}
	}
	
	READ16_HANDLER( cps2_eeprom_port_r )
	{
	    return (input_port_2_word_r(offset,0) & 0xfffe) | EEPROM_read_bit();
	}
	
	WRITE16_HANDLER( cps2_eeprom_port_w )
	{
	    if (ACCESSING_MSB)
	    {
			/* bit 0 - Unused */
			/* bit 1 - Unused */
	        /* bit 2 - Unused */
	        /* bit 3 - Unused */
	        /* bit 4 - Eeprom data  */
	        /* bit 5 - Eeprom clock */
	        /* bit 6 - */
	        /* bit 7 - */
	
	        /* EEPROM */
	        EEPROM_write_bit(data & 0x1000);
	        EEPROM_set_clock_line((data & 0x2000) ? ASSERT_LINE : CLEAR_LINE);
	        EEPROM_set_cs_line((data & 0x4000) ? CLEAR_LINE : ASSERT_LINE);
	
		}
	    else
	    {
	        /* bit 0 - coin counter */
			/* bit 1 - Unused */
	        /* bit 2 - Unused */
	        /* bit 3 - On all the time - allows access to Z80 address space */
	        /* bit 4 - lock 1  */
	        /* bit 5 - */
	        /* bit 6 - */
	        /* bit 7 - */
	
	        coin_counter_w(0, data & 0x0001);
	        coin_lockout_w(0,~data & 0x0010);
	        coin_lockout_w(1,~data & 0x0020);
	        coin_lockout_w(2,~data & 0x0040);
	        coin_lockout_w(3,~data & 0x0080);
	        /*
	        set_led_status(0,data & 0x01);
	        set_led_status(1,data & 0x10);
	        set_led_status(2,data & 0x20);
	        */
	    }
	}
	
	READ16_HANDLER( cps2_qsound_volume_r )
	{
		/* Extra adapter memory (0x660000-0x663fff) available when bit 14 = 0 */
		/* Network adapter (ssf2tb) present when bit 15 = 0 */
		/* Only game known to use both these so far is SSF2TB */
	
	const char *gamename = Machine->gamedrv->name;
		if(strcmp(gamename,"ssf2tb")==0)
			return 0x2021;
		else
			return 0xe021;
	}
	
	
	static data8_t CPS2_Read8(offs_t address)
	{
		return m68k_read_pcrelative_8(address);
	}
	
	static data16_t CPS2_Read16(offs_t address)
	{
		return m68k_read_pcrelative_16(address);
	}
	
	static data32_t CPS2_Read32(offs_t address)
	{
		return m68k_read_pcrelative_32(address);
	}
	
	
	
	static READ16_HANDLER( kludge_r )
	{
		return 0xffff;
	}
	
	
	static MEMORY_READ16_START( cps2_readmem )
		{ 0x000000, 0x3fffff, MRA16_ROM },				/* 68000 ROM */
		{ 0x400000, 0x40000b, MRA16_RAM },				/* CPS2 object output */
		{ 0x618000, 0x619fff, qsound_sharedram1_r },		/* Q RAM */
		{ 0x662000, 0x662001, MRA16_RAM },				/* Network adapter related, accessed in SSF2TB */
		{ 0x662008, 0x662009, MRA16_RAM },				/* Network adapter related, accessed in SSF2TB */
		{ 0x662020, 0x662021, MRA16_RAM },				/* Network adapter related, accessed in SSF2TB */
		{ 0x660000, 0x663fff, MRA16_RAM },				/* When bit 14 of 0x804030 equals 0 this space is available. Many games store highscores and other info here if available. */
		{ 0x664000, 0x664001, MRA16_RAM },				/* Unknown - Only used if 0x660000-0x663fff available (could be RAM enable?) */
		{ 0x708000, 0x709fff, cps2_objram2_r },			/* Object RAM */
		{ 0x70a000, 0x70bfff, cps2_objram2_r },			/* mirror */
		{ 0x70c000, 0x70dfff, cps2_objram2_r },			/* mirror */
		{ 0x70e000, 0x70ffff, cps2_objram2_r },			/* mirror */
		{ 0x800100, 0x8001ff, cps1_output_r },			/* Output ports mirror (sfa) */
		{ 0x804000, 0x804001, input_port_0_word_r },		/* IN0 */
		{ 0x804010, 0x804011, input_port_1_word_r },		/* IN1 */
		{ 0x804020, 0x804021, cps2_eeprom_port_r  },		/* IN2 + EEPROM */
		{ 0x804030, 0x804031, cps2_qsound_volume_r },		/* Master volume. Also when bit 14=0 addon memory is present, when bit 15=0 network adapter present. */
		{ 0x8040b0, 0x8040b3, kludge_r },  				/* unknown (xmcotaj hangs if this is 0) */
		{ 0x804100, 0x8041ff, cps1_output_r },			/* CPS1 Output ports */
		{ 0x900000, 0x92ffff, MRA16_RAM },				/* Video RAM */
		{ 0xff0000, 0xffffff, MRA16_RAM },				/* RAM */
	MEMORY_END
	
	static WRITE16_HANDLER( pip )
	{
		usrintf_showmessage("%04x",data);
	}
	
	static MEMORY_WRITE16_START( cps2_writemem )
		{ 0x000000, 0x3fffff, MWA16_ROM },				/* ROM */
		{ 0x400000, 0x40000b, MWA16_RAM, &cps2_output, &cps2_output_size },	/* CPS2 output */
		{ 0x618000, 0x619fff, qsound_sharedram1_w },		/* Q RAM */
		{ 0x662000, 0x662001, MWA16_RAM },				/* Network adapter related, accessed in SSF2TB */
		{ 0x662008, 0x662009, MWA16_RAM },				/* Network adapter related, accessed in SSF2TB (not sure if this port is write only yet)*/
		{ 0x662020, 0x662021, MWA16_RAM },				/* Network adapter related, accessed in SSF2TB */
		{ 0x660000, 0x663fff, MWA16_RAM },				/* When bit 14 of 0x804030 equals 0 this space is available. Many games store highscores and other info here if available. */
		{ 0x664000, 0x664001, MWA16_RAM },				/* Unknown - Only used if 0x660000-0x663fff available (could be RAM enable?) */
		{ 0x700000, 0x701fff, cps2_objram1_w, &cps2_objram1 },	/* Object RAM, no game seems to use it directly */
		{ 0x708000, 0x709fff, cps2_objram2_w, &cps2_objram2 },	/* Object RAM */
		{ 0x70a000, 0x70bfff, cps2_objram2_w },			/* mirror */
		{ 0x70c000, 0x70dfff, cps2_objram2_w },			/* mirror */
		{ 0x70e000, 0x70ffff, cps2_objram2_w },			/* mirror */
		{ 0x800100, 0x8001ff, cps1_output_w },			/* Output ports mirror (sfa) */
		{ 0x804040, 0x804041, cps2_eeprom_port_w },		/* EEPROM */
		{ 0x8040a0, 0x8040a1, MWA16_NOP },				/* Unknown (reset once on startup) */
		{ 0x8040e0, 0x8040e1, cps2_objram_bank_w },		/* bit 0 = Object ram bank swap */
		{ 0x804100, 0x8041ff, cps1_output_w, &cps1_output, &cps1_output_size },  /* Output ports */
		{ 0x900000, 0x92ffff, cps1_gfxram_w, &cps1_gfxram, &cps1_gfxram_size },
		{ 0xff0000, 0xffffff, MWA16_RAM },				/* RAM */
	MEMORY_END
	
	
	
	
	static InputPortPtr input_ports_19xx = new InputPortPtr(){ public void handler() { 
	    PORT_START();       /* IN0 (0x00) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
	    PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
	    PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
	    PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
	    PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
	    PORT_START();       /* IN1 (0x10) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0xff00, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
	    PORT_START();       /* IN2 (0x20) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_SPECIAL );  /* EEPROM bit */
		PORT_BITX(0x0002, IP_ACTIVE_LOW, IPT_SERVICE, DEF_STR( "Service_Mode") ); KEYCODE_F2, IP_JOY_NONE )
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_SERVICE1 );
	    PORT_BIT( 0x00f8, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_START1 );
	    PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_START2 );
	    PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_COIN1 );
	    PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_COIN2 );
	    PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_cybotsj = new InputPortPtr(){ public void handler() { 
	    PORT_START();       /* IN0 (0x00) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
	    PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
	    PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
	    PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
	    PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
	    PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
	    PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
	    PORT_START();       /* IN1 (0x10) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER1 );
	    PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER2 );
	    PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0xff00, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
	    PORT_START();       /* IN2 (0x20) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_SPECIAL );  /* EEPROM bit */
		PORT_BITX(0x0002, IP_ACTIVE_LOW, IPT_SERVICE, DEF_STR( "Service_Mode") ); KEYCODE_F2, IP_JOY_NONE )
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_SERVICE1 );
	    PORT_BIT( 0x00f8, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_START1 );
	    PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_START2 );
	    PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_COIN1 );
	    PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_COIN2 );
	    PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_ssf2 = new InputPortPtr(){ public void handler() { 
	    PORT_START();       /* IN0 (0x00) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
	    PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
	    PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
	    PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
	    PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
	    PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
	    PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
	    PORT_START();       /* IN1 (0x10) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER1 );
	    PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER1 );
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_BUTTON6 | IPF_PLAYER1 );
	    PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER2 );
	    PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER2 );
	    PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0xff00, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
	    PORT_START();       /* IN2 (0x20) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_SPECIAL );  /* EEPROM bit */
		PORT_BITX(0x0002, IP_ACTIVE_LOW, IPT_SERVICE, DEF_STR( "Service_Mode") ); KEYCODE_F2, IP_JOY_NONE )
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_SERVICE1 );
	    PORT_BIT( 0x00f8, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_START1 );
	    PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_START2 );
	    PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_COIN1 );
	    PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_COIN2 );
	    PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_BUTTON6 | IPF_PLAYER2 );
	    PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_ddtod = new InputPortPtr(){ public void handler() { 
	    PORT_START();       /* IN0 (0x00) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
	    PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
	    PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
	    PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER1 );
	    PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
	    PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
	    PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
	    PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER2 );
	
	    PORT_START();       /* IN1 (0x10) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER3 );
	    PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER3 );
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER3 );
	    PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER3 );
	    PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER3 );
	    PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER3 );
	    PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER3 );
	    PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER3 );
	    PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER4 );
	    PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER4 );
	    PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER4 );
	    PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER4 );
	    PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER4 );
	    PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER4 );
	    PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER4 );
	    PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER4 );
	
	    PORT_START();       /* IN2 (0x20) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_SPECIAL );  /* EEPROM bit */
		PORT_BITX(0x0002, IP_ACTIVE_LOW, IPT_SERVICE, DEF_STR( "Service_Mode") ); KEYCODE_F2, IP_JOY_NONE )
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_SERVICE1 );
	    PORT_BIT( 0x00f8, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_START1 );
	    PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_START2 );
	    PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_START3 );
	    PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_START4 );
	    PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_COIN1 );
	    PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_COIN2 );
	    PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_COIN3 );
	    PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_COIN4 );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_avsp = new InputPortPtr(){ public void handler() { 
	    PORT_START();       /* IN0 (0x00) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
	    PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
	    PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
	    PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
	    PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
	    PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
	    PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
	    PORT_START();       /* IN1 (0x10) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER3 );
	    PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER3 );
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER3 );
	    PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER3 );
	    PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER3 );
	    PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER3 );
	    PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER3 );
	    PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0xff00, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
	    PORT_START();       /* IN2 (0x20) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_SPECIAL );  /* EEPROM bit */
		PORT_BITX(0x0002, IP_ACTIVE_LOW, IPT_SERVICE, DEF_STR( "Service_Mode") ); KEYCODE_F2, IP_JOY_NONE )
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_SERVICE1 );
	    PORT_BIT( 0x00f8, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_START1 );
	    PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_START2 );
	    PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_START3 );
	    PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_COIN1 );
	    PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_COIN2 );
	    PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_COIN3 );
	    PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_qndream = new InputPortPtr(){ public void handler() { 
	    PORT_START();       /* IN0 (0x00) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER1 );
	    PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
	    PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
	    PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER2 );
	    PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
	    PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
	    PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
	    PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
	    PORT_START();       /* IN1 (0x10) */
	    PORT_BIT( 0xff00, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
	    PORT_START();       /* IN2 (0x20) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_SPECIAL );  /* EEPROM bit */
		PORT_BITX(0x0002, IP_ACTIVE_LOW, IPT_SERVICE, DEF_STR( "Service_Mode") ); KEYCODE_F2, IP_JOY_NONE )
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_SERVICE1 );
	    PORT_BIT( 0x00f8, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_START1 );
	    PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_START2 );
	    PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_COIN1 );
	    PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_COIN2 );
	    PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_batcir = new InputPortPtr(){ public void handler() { 
	    PORT_START();       /* IN0 (0x00) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
	    PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
	    PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
	    PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
	    PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
	    PORT_START();       /* IN1 (0x10) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER3 );
	    PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER3 );
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER3 );
	    PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER3 );
	    PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER3 );
	    PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER3 );
	    PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER4 );
	    PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER4 );
	    PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER4 );
	    PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER4 );
	    PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER4 );
	    PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER4 );
	    PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
	    PORT_START();       /* IN2 (0x20) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_SPECIAL );  /* EEPROM bit */
		PORT_BITX(0x0002, IP_ACTIVE_LOW, IPT_SERVICE, DEF_STR( "Service_Mode") ); KEYCODE_F2, IP_JOY_NONE )
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_SERVICE1 );
	    PORT_BIT( 0x00f8, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_START1 );
	    PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_START2 );
	    PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_START3 );
	    PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_START4 );
	    PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_COIN1 );
	    PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_COIN2 );
	    PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_COIN3 );
	    PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_COIN4 );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_sgemf = new InputPortPtr(){ public void handler() { 
	    PORT_START();       /* IN0 (0x00) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
	    PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
	    PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
	    PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
	    PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
	    PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
	    PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
	    PORT_START();       /* IN1 (0x10) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0xff00, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
	    PORT_START();       /* IN2 (0x20) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_SPECIAL );  /* EEPROM bit */
		PORT_BITX(0x0002, IP_ACTIVE_LOW, IPT_SERVICE, DEF_STR( "Service_Mode") ); KEYCODE_F2, IP_JOY_NONE )
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_SERVICE1 );
	    PORT_BIT( 0x00f8, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_START1 );
	    PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_START2 );
	    PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_COIN1 );
	    PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_COIN2 );
	    PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_cps2 = new InputPortPtr(){ public void handler() { 
	    PORT_START();       /* IN0 (0x00) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER1 );
	    PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
	    PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
	    PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
	    PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
	    PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
	    PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
	    PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
	    PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
	    PORT_START();       /* IN1 (0x10) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER1 );
	    PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER1 );
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_BUTTON6 | IPF_PLAYER1 );
	    PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER2 );
	    PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER2 );
	    PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0xff00, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
	    PORT_START();       /* IN2 (0x20) */
	    PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_SPECIAL );  /* EEPROM bit */
		PORT_BITX(0x0002, IP_ACTIVE_LOW, IPT_SERVICE, DEF_STR( "Service_Mode") ); KEYCODE_F2, IP_JOY_NONE )
	    PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_SERVICE1 );
	    PORT_BIT( 0x00f8, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_START1 );
	    PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_START2 );
	    PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_UNKNOWN );
	    PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_COIN1 );
	    PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_COIN2 );
	    PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_BUTTON6 | IPF_PLAYER2 );
	    PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	
	
	static GfxLayout layout8x8 = new GfxLayout
	(
		8,8,
		RGN_FRAC(1,1),
		4,
		new int[] { GFX_RAW },
		new int[] { 4*8 },	/* org displacement - 8x8 tiles are taken from the RIGHT side of the 16x16 tile
					   (fixes cawing which uses character 0x0002 as space, typo instead of 0x20?) */
		{ 8*8 },	/* line modulo */
		64*8		/* char modulo */
	);
	
	static GfxLayout layout16x16 = new GfxLayout
	(
		16,16,
		RGN_FRAC(1,1),
		4,
		new int[] { GFX_RAW },
		new int[] { 0 },		/* org displacement */
		new int[] { 8*8 },	/* line modulo */
		128*8		/* char modulo */
	);
	
	static GfxLayout layout32x32 = new GfxLayout
	(
		32,32,
		RGN_FRAC(1,1),
		4,
		new int[] { GFX_RAW },
		new int[] { 0 },		/* org displacement */
		new int[] { 16*8 },	/* line modulo */
		512*8		/* char modulo */
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, layout8x8,   0, 0x100 ),
		new GfxDecodeInfo( REGION_GFX1, 0, layout16x16, 0, 0x100 ),
		new GfxDecodeInfo( REGION_GFX1, 0, layout32x32, 0, 0x100 ),
		new GfxDecodeInfo( -1 )
	};
	
	
	
	static struct m68k_encryption_interface cps2_encryption =
	{
		CPS2_Read8, CPS2_Read16, CPS2_Read32,
		CPS2_Read16, CPS2_Read32
	};
	
	static MachineDriver machine_driver_cps2 = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_M68000,
				11800000,
				cps2_readmem,cps2_writemem,null,null,
				cps2_interrupt, 1,	// 262  /* ??? interrupts per frame */
				null,null,
				cps2_encryption
			),
			new MachineCPU(
				CPU_Z80,
				8000000,
				qsound_readmem,qsound_writemem,null,null,
				null,null,
				interrupt,250	/* ?? */
			)
		},
	//	59.633333, DEFAULT_60HZ_VBLANK_DURATION,
		59.633333, 400,		//ks
		1,
		null,
	
		/* video hardware */
		64*8, 32*8, new rectangle( 8*8, (64-8)*8-1, 2*8, 30*8-1 ),
		gfxdecodeinfo,
		4096, 0,
		null,
	
	//	VIDEO_TYPE_RASTER | VIDEO_NEEDS_6BITS_PER_GUN,
		VIDEO_TYPE_RASTER | VIDEO_NEEDS_6BITS_PER_GUN | VIDEO_UPDATE_AFTER_VBLANK,		//ks
		cps1_eof_callback,
		cps2_vh_start,
		cps1_vh_stop,
		cps1_vh_screenrefresh,
	
		/* sound hardware */
		SOUND_SUPPORTS_STEREO,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_QSOUND,
				qsound_interface
			)
		},
		cps2_nvram_handler
	);
	
	
	
	static RomLoadPtr rom_1944j = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "nffj.03", 0x000000, 0x80000, 0x247521ef )
		ROM_LOAD16_WORD_SWAP( "nffj.04", 0x080000, 0x80000, 0xdba1c66e )
		ROM_LOAD16_WORD_SWAP( "nffj.05", 0x100000, 0x80000, 0x7f20c2ef )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "nffjx.03", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "nffjx.04", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "nff.13",   0x0000000, 0x400000, 0x00000000, ROM_GROUPWORD | ROM_SKIP(6) ) // Not dumped due to data being on a SIMM
		ROMX_LOAD( "nff.15",   0x0000002, 0x400000, 0x00000000, ROM_GROUPWORD | ROM_SKIP(6) ) // Not dumped due to data being on a SIMM
		ROMX_LOAD( "nff.17",   0x0000004, 0x400000, 0x00000000, ROM_GROUPWORD | ROM_SKIP(6) ) // Not dumped due to data being on a SIMM
		ROMX_LOAD( "nff.19",   0x0000006, 0x400000, 0x00000000, ROM_GROUPWORD | ROM_SKIP(6) ) // Not dumped due to data being on a SIMM
		ROMX_LOAD( "nff.14",   0x1000000, 0x400000, 0x00000000, ROM_GROUPWORD | ROM_SKIP(6) ) // Not dumped due to data being on a SIMM
		ROMX_LOAD( "nff.16",   0x1000002, 0x400000, 0x00000000, ROM_GROUPWORD | ROM_SKIP(6) ) // Not dumped due to data being on a SIMM
		ROMX_LOAD( "nff.18",   0x1000004, 0x400000, 0x00000000, ROM_GROUPWORD | ROM_SKIP(6) ) // Not dumped due to data being on a SIMM
		ROMX_LOAD( "nff.20",   0x1000006, 0x400000, 0x00000000, ROM_GROUPWORD | ROM_SKIP(6) ) // Not dumped due to data being on a SIMM
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "nff.01",   0x00000, 0x08000, 0xd2e44318 );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "nff.11",   0x000000, 0x400000, 0x00000000 ) // Not dumped
		ROM_LOAD16_WORD_SWAP( "nff.12",   0x400000, 0x400000, 0x00000000 ) // Not dumped
	ROM_END(); }}; 
	
	static RomLoadPtr rom_19xx = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "19xu.03", 0x000000, 0x80000, 0x05955268 )
		ROM_LOAD16_WORD_SWAP( "19xu.04", 0x080000, 0x80000, 0x3111ab7f )
		ROM_LOAD16_WORD_SWAP( "19xu.05", 0x100000, 0x80000, 0x38df4a63 )
		ROM_LOAD16_WORD_SWAP( "19xu.06", 0x180000, 0x80000, 0x5c7e60d3 )
		ROM_LOAD16_WORD_SWAP( "19x.07",  0x200000, 0x80000, 0x61c0296c )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "19xux.03", 0x000000, 0x80000, 0x239a08ae )
		ROM_LOAD16_WORD_SWAP( "19xux.04", 0x080000, 0x80000, 0xc13a1072 )
		ROM_LOAD16_WORD_SWAP( "19xux.05", 0x100000, 0x80000, 0x8c066ec3 )
		ROM_LOAD16_WORD_SWAP( "19xux.06", 0x180000, 0x80000, 0x4b1caeb9 )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROMX_LOAD( "19x.13",   0x0000000, 0x080000, 0x427aeb18, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.15",   0x0000002, 0x080000, 0x63bdbf54, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.17",   0x0000004, 0x080000, 0x2dfe18b5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.19",   0x0000006, 0x080000, 0xcbef9579, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.14",   0x0800000, 0x200000, 0xe916967c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.16",   0x0800002, 0x200000, 0x6e75f3db, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.18",   0x0800004, 0x200000, 0x2213e798, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.20",   0x0800006, 0x200000, 0xab9d5b96, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "19x.01",   0x00000, 0x08000, 0xef55195e );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "19x.11",   0x000000, 0x200000, 0xd38beef3 )
		ROM_LOAD16_WORD_SWAP( "19x.12",   0x200000, 0x200000, 0xd47c96e2 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_19xxj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "19xj.03a", 0x000000, 0x80000, 0xed08bdd1 )
		ROM_LOAD16_WORD_SWAP( "19xj.04a", 0x080000, 0x80000, 0x5254caab )
		ROM_LOAD16_WORD_SWAP( "19xj.05a", 0x100000, 0x80000, 0xaa508ac4 )
		ROM_LOAD16_WORD_SWAP( "19xj.06a", 0x180000, 0x80000, 0xff2d785b )
		ROM_LOAD16_WORD_SWAP( "19x.07",   0x200000, 0x80000, 0x61c0296c )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "19xjx.03a", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "19xjx.04a", 0x080000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "19xjx.05a", 0x100000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "19xjx.06a", 0x180000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROMX_LOAD( "19x.13",   0x0000000, 0x080000, 0x427aeb18, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.15",   0x0000002, 0x080000, 0x63bdbf54, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.17",   0x0000004, 0x080000, 0x2dfe18b5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.19",   0x0000006, 0x080000, 0xcbef9579, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.14",   0x0800000, 0x200000, 0xe916967c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.16",   0x0800002, 0x200000, 0x6e75f3db, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.18",   0x0800004, 0x200000, 0x2213e798, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.20",   0x0800006, 0x200000, 0xab9d5b96, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "19x.01",   0x00000, 0x08000, 0xef55195e );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "19x.11",   0x000000, 0x200000, 0xd38beef3 )
		ROM_LOAD16_WORD_SWAP( "19x.12",   0x200000, 0x200000, 0xd47c96e2 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_19xxjr1 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "19xj.03", 0x000000, 0x80000, 0x26a381ed )
		ROM_LOAD16_WORD_SWAP( "19xj.04", 0x080000, 0x80000, 0x30100cca )
		ROM_LOAD16_WORD_SWAP( "19xj.05", 0x100000, 0x80000, 0xde67e938 )
		ROM_LOAD16_WORD_SWAP( "19xj.06", 0x180000, 0x80000, 0x39f9a409 )
		ROM_LOAD16_WORD_SWAP( "19x.07",  0x200000, 0x80000, 0x61c0296c )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "19xjx.03", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "19xjx.04", 0x080000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "19xjx.05", 0x100000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "19xjx.06", 0x180000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROMX_LOAD( "19x.13",   0x0000000, 0x080000, 0x427aeb18, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.15",   0x0000002, 0x080000, 0x63bdbf54, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.17",   0x0000004, 0x080000, 0x2dfe18b5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.19",   0x0000006, 0x080000, 0xcbef9579, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.14",   0x0800000, 0x200000, 0xe916967c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.16",   0x0800002, 0x200000, 0x6e75f3db, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.18",   0x0800004, 0x200000, 0x2213e798, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.20",   0x0800006, 0x200000, 0xab9d5b96, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "19x.01",   0x00000, 0x08000, 0xef55195e );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "19x.11",   0x000000, 0x200000, 0xd38beef3 )
		ROM_LOAD16_WORD_SWAP( "19x.12",   0x200000, 0x200000, 0xd47c96e2 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_19xxh = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "19xh.03a", 0x000000, 0x80000, 0x357be2ac )
		ROM_LOAD16_WORD_SWAP( "19xh.04a", 0x080000, 0x80000, 0xbb13ea3b )
		ROM_LOAD16_WORD_SWAP( "19xh.05a", 0x100000, 0x80000, 0xcbd76601 )
		ROM_LOAD16_WORD_SWAP( "19xh.06a", 0x180000, 0x80000, 0xb362de8b )
		ROM_LOAD16_WORD_SWAP( "19x.07",   0x200000, 0x80000, 0x61c0296c )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "19xhx.03a", 0x000000, 0x80000, 0x374ce871 )
		ROM_LOAD16_WORD_SWAP( "19xhx.04a", 0x080000, 0x80000, 0xebd16e33 )
		ROM_LOAD16_WORD_SWAP( "19xhx.05a", 0x100000, 0x80000, 0x0bb5ad27 )
		ROM_LOAD16_WORD_SWAP( "19xhx.06a", 0x180000, 0x80000, 0x3663c8d2 )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROMX_LOAD( "19x.13",   0x0000000, 0x080000, 0x427aeb18, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.15",   0x0000002, 0x080000, 0x63bdbf54, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.17",   0x0000004, 0x080000, 0x2dfe18b5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.19",   0x0000006, 0x080000, 0xcbef9579, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.14",   0x0800000, 0x200000, 0xe916967c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.16",   0x0800002, 0x200000, 0x6e75f3db, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.18",   0x0800004, 0x200000, 0x2213e798, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "19x.20",   0x0800006, 0x200000, 0xab9d5b96, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "19x.01",   0x00000, 0x08000, 0xef55195e );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "19x.11",   0x000000, 0x200000, 0xd38beef3 )
		ROM_LOAD16_WORD_SWAP( "19x.12",   0x200000, 0x200000, 0xd47c96e2 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_armwar = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "pwge.03b", 0x000000, 0x80000, 0xe822e3e9 )
		ROM_LOAD16_WORD_SWAP( "pwge.04b", 0x080000, 0x80000, 0x4f89de39 )
		ROM_LOAD16_WORD_SWAP( "pwge.05a", 0x100000, 0x80000, 0x83df24e5 )
		ROM_LOAD16_WORD_SWAP( "pwg.06",   0x180000, 0x80000, 0x87a60ce8 )
		ROM_LOAD16_WORD_SWAP( "pwg.07",   0x200000, 0x80000, 0xf7b148df )
		ROM_LOAD16_WORD_SWAP( "pwg.08",   0x280000, 0x80000, 0xcc62823e )
		ROM_LOAD16_WORD_SWAP( "pwg.09",   0x300000, 0x80000, 0xddc85ca6 )
		ROM_LOAD16_WORD_SWAP( "pwg.10",   0x380000, 0x80000, 0x07c4fb28 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "pwgex.03b", 0x000000, 0x80000, 0x741fc2b0 )
		ROM_LOAD16_WORD_SWAP( "pwgex.04b", 0x080000, 0x80000, 0x5bb96a5d )
	
		ROM_REGION( 0x1400000, REGION_GFX1, 0 );
		ROMX_LOAD( "pwg.13",   0x0000000, 0x400000, 0xae8fe08e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.15",   0x0000002, 0x400000, 0xdb560f58, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.17",   0x0000004, 0x400000, 0xbc475b94, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.19",   0x0000006, 0x400000, 0x07439ff7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.14",   0x1000000, 0x100000, 0xc3f9ba63, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.16",   0x1000002, 0x100000, 0x815b0e7b, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.18",   0x1000004, 0x100000, 0x0109c71b, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.20",   0x1000006, 0x100000, 0xeb75ffbe, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "pwg.01",   0x00000, 0x08000, 0x18a5c0e4 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "pwg.02",   0x28000, 0x20000, 0xc9dfffa6 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "pwg.11",   0x000000, 0x200000, 0xa78f7433 )
		ROM_LOAD16_WORD_SWAP( "pwg.12",   0x200000, 0x200000, 0x77438ed0 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_armwaru = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "pwgu.03b", 0x000000, 0x80000, 0x8b95497a )
		ROM_LOAD16_WORD_SWAP( "pwgu.04b", 0x080000, 0x80000, 0x29eb5661 )
		ROM_LOAD16_WORD_SWAP( "pwgu.05b", 0x100000, 0x80000, 0xa54e9e44 )
		ROM_LOAD16_WORD_SWAP( "pwg.06",   0x180000, 0x80000, 0x87a60ce8 )
		ROM_LOAD16_WORD_SWAP( "pwg.07",   0x200000, 0x80000, 0xf7b148df )
		ROM_LOAD16_WORD_SWAP( "pwg.08",   0x280000, 0x80000, 0xcc62823e )
		ROM_LOAD16_WORD_SWAP( "pwg.09a",  0x300000, 0x80000, 0x4c26baee )
		ROM_LOAD16_WORD_SWAP( "pwg.10",   0x380000, 0x80000, 0x07c4fb28 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "pwgux.03b", 0x000000, 0x80000, 0x5d41ddde )
		ROM_LOAD16_WORD_SWAP( "pwgux.04b", 0x080000, 0x80000, 0x4d0619f3 )
	
		ROM_REGION( 0x1400000, REGION_GFX1, 0 );
		ROMX_LOAD( "pwg.13",   0x0000000, 0x400000, 0xae8fe08e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.15",   0x0000002, 0x400000, 0xdb560f58, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.17",   0x0000004, 0x400000, 0xbc475b94, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.19",   0x0000006, 0x400000, 0x07439ff7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.14",   0x1000000, 0x100000, 0xc3f9ba63, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.16",   0x1000002, 0x100000, 0x815b0e7b, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.18",   0x1000004, 0x100000, 0x0109c71b, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.20",   0x1000006, 0x100000, 0xeb75ffbe, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "pwg.01",   0x00000, 0x08000, 0x18a5c0e4 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "pwg.02",   0x28000, 0x20000, 0xc9dfffa6 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "pwg.11",   0x000000, 0x200000, 0xa78f7433 )
		ROM_LOAD16_WORD_SWAP( "pwg.12",   0x200000, 0x200000, 0x77438ed0 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_pgear = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "pwgj.03a", 0x000000, 0x80000, 0xc79c0c02 )
		ROM_LOAD16_WORD_SWAP( "pwgj.04a", 0x080000, 0x80000, 0x167c6ed8 )
		ROM_LOAD16_WORD_SWAP( "pwgj.05a", 0x100000, 0x80000, 0xa63fb400 )
		ROM_LOAD16_WORD_SWAP( "pwg.06",   0x180000, 0x80000, 0x87a60ce8 )
		ROM_LOAD16_WORD_SWAP( "pwg.07",   0x200000, 0x80000, 0xf7b148df )
		ROM_LOAD16_WORD_SWAP( "pwg.08",   0x280000, 0x80000, 0xcc62823e )
		ROM_LOAD16_WORD_SWAP( "pwg.09a",  0x300000, 0x80000, 0x4c26baee )
		ROM_LOAD16_WORD_SWAP( "pwg.10",   0x380000, 0x80000, 0x07c4fb28 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "pwgjx.03a", 0x000000, 0x80000, 0xcf0284a9 )
		ROM_LOAD16_WORD_SWAP( "pwgjx.04a", 0x080000, 0x80000, 0x99437cf1 )
	
		ROM_REGION( 0x1400000, REGION_GFX1, 0 );
		ROMX_LOAD( "pwg.13",   0x0000000, 0x400000, 0xae8fe08e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.15",   0x0000002, 0x400000, 0xdb560f58, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.17",   0x0000004, 0x400000, 0xbc475b94, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.19",   0x0000006, 0x400000, 0x07439ff7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.14",   0x1000000, 0x100000, 0xc3f9ba63, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.16",   0x1000002, 0x100000, 0x815b0e7b, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.18",   0x1000004, 0x100000, 0x0109c71b, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.20",   0x1000006, 0x100000, 0xeb75ffbe, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "pwg.01",   0x00000, 0x08000, 0x18a5c0e4 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "pwg.02",   0x28000, 0x20000, 0xc9dfffa6 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "pwg.11",   0x000000, 0x200000, 0xa78f7433 )
		ROM_LOAD16_WORD_SWAP( "pwg.12",   0x200000, 0x200000, 0x77438ed0 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_pgearr1 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "pwgj.03", 0x000000, 0x80000, 0xf264e74b )
		ROM_LOAD16_WORD_SWAP( "pwgj.04", 0x080000, 0x80000, 0x23a84983 )
		ROM_LOAD16_WORD_SWAP( "pwgj.05", 0x100000, 0x80000, 0xbef58c62 )
		ROM_LOAD16_WORD_SWAP( "pwg.06",  0x180000, 0x80000, 0x87a60ce8 )
		ROM_LOAD16_WORD_SWAP( "pwg.07",  0x200000, 0x80000, 0xf7b148df )
		ROM_LOAD16_WORD_SWAP( "pwg.08",  0x280000, 0x80000, 0xcc62823e )
		ROM_LOAD16_WORD_SWAP( "pwg.09",  0x300000, 0x80000, 0xddc85ca6 )
		ROM_LOAD16_WORD_SWAP( "pwg.10",  0x380000, 0x80000, 0x07c4fb28 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "pwgjx.03", 0x000000, 0x80000, 0xebd9e371 )
		ROM_LOAD16_WORD_SWAP( "pwgjx.04", 0x080000, 0x80000, 0xe05a4756 )
	
		ROM_REGION( 0x1400000, REGION_GFX1, 0 );
		ROMX_LOAD( "pwg.13",   0x0000000, 0x400000, 0xae8fe08e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.15",   0x0000002, 0x400000, 0xdb560f58, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.17",   0x0000004, 0x400000, 0xbc475b94, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.19",   0x0000006, 0x400000, 0x07439ff7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.14",   0x1000000, 0x100000, 0xc3f9ba63, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.16",   0x1000002, 0x100000, 0x815b0e7b, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.18",   0x1000004, 0x100000, 0x0109c71b, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.20",   0x1000006, 0x100000, 0xeb75ffbe, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "pwg.01",   0x00000, 0x08000, 0x18a5c0e4 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "pwg.02",   0x28000, 0x20000, 0xc9dfffa6 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "pwg.11",   0x000000, 0x200000, 0xa78f7433 )
		ROM_LOAD16_WORD_SWAP( "pwg.12",   0x200000, 0x200000, 0x77438ed0 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_armwara = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "pwga.03a", 0x000000, 0x80000, 0x8d474ab1 )
		ROM_LOAD16_WORD_SWAP( "pwga.04a", 0x080000, 0x80000, 0x81b5aec7 )
		ROM_LOAD16_WORD_SWAP( "pwga.05a", 0x100000, 0x80000, 0x2618e819 )
		ROM_LOAD16_WORD_SWAP( "pwg.06",   0x180000, 0x80000, 0x87a60ce8 )
		ROM_LOAD16_WORD_SWAP( "pwg.07",   0x200000, 0x80000, 0xf7b148df )
		ROM_LOAD16_WORD_SWAP( "pwg.08",   0x280000, 0x80000, 0xcc62823e )
		ROM_LOAD16_WORD_SWAP( "pwg.09",   0x300000, 0x80000, 0xddc85ca6 )
		ROM_LOAD16_WORD_SWAP( "pwg.10",   0x380000, 0x80000, 0x07c4fb28 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "pwgax.03a", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "pwgax.04a", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x1400000, REGION_GFX1, 0 );
		ROMX_LOAD( "pwg.13",   0x0000000, 0x400000, 0xae8fe08e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.15",   0x0000002, 0x400000, 0xdb560f58, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.17",   0x0000004, 0x400000, 0xbc475b94, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.19",   0x0000006, 0x400000, 0x07439ff7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.14",   0x1000000, 0x100000, 0xc3f9ba63, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.16",   0x1000002, 0x100000, 0x815b0e7b, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.18",   0x1000004, 0x100000, 0x0109c71b, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pwg.20",   0x1000006, 0x100000, 0xeb75ffbe, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "pwg.01",   0x00000, 0x08000, 0x18a5c0e4 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "pwg.02",   0x28000, 0x20000, 0xc9dfffa6 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "pwg.11",   0x000000, 0x200000, 0xa78f7433 )
		ROM_LOAD16_WORD_SWAP( "pwg.12",   0x200000, 0x200000, 0x77438ed0 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_avsp = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "avpe.03d", 0x000000, 0x80000, 0x774334a9 )
		ROM_LOAD16_WORD_SWAP( "avpe.04d", 0x080000, 0x80000, 0x7fa83769 )
		ROM_LOAD16_WORD_SWAP( "avp.05d",  0x100000, 0x80000, 0xfbfb5d7a )
		ROM_LOAD16_WORD_SWAP( "avp.06",   0x180000, 0x80000, 0x190b817f )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "avpex.03d", 0x000000, 0x80000, 0x73dd740e )
		ROM_LOAD16_WORD_SWAP( "avpex.04d", 0x080000, 0x80000, 0x185f8c43 )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROMX_LOAD( "avp.13",   0x0000000, 0x200000, 0x8f8b5ae4, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.15",   0x0000002, 0x200000, 0xb00280df, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.17",   0x0000004, 0x200000, 0x94403195, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.19",   0x0000006, 0x200000, 0xe1981245, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.14",   0x0800000, 0x200000, 0xebba093e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.16",   0x0800002, 0x200000, 0xfb228297, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.18",   0x0800004, 0x200000, 0x34fb7232, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.20",   0x0800006, 0x200000, 0xf90baa21, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "avp.01",   0x00000, 0x08000, 0x2d3b4220 );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "avp.11",   0x000000, 0x200000, 0x83499817 )
		ROM_LOAD16_WORD_SWAP( "avp.12",   0x200000, 0x200000, 0xf4110d49 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_avspu = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "avpu.03d", 0x000000, 0x80000, 0x42757950 )
		ROM_LOAD16_WORD_SWAP( "avpu.04d", 0x080000, 0x80000, 0x5abcdee6 )
		ROM_LOAD16_WORD_SWAP( "avp.05d",  0x100000, 0x80000, 0xfbfb5d7a )
		ROM_LOAD16_WORD_SWAP( "avp.06",   0x180000, 0x80000, 0x190b817f )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "avpux.03d", 0x000000, 0x80000, 0xd5b01046 )
		ROM_LOAD16_WORD_SWAP( "avpux.04d", 0x080000, 0x80000, 0x94bd7603 )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROMX_LOAD( "avp.13",   0x0000000, 0x200000, 0x8f8b5ae4, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.15",   0x0000002, 0x200000, 0xb00280df, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.17",   0x0000004, 0x200000, 0x94403195, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.19",   0x0000006, 0x200000, 0xe1981245, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.14",   0x0800000, 0x200000, 0xebba093e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.16",   0x0800002, 0x200000, 0xfb228297, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.18",   0x0800004, 0x200000, 0x34fb7232, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.20",   0x0800006, 0x200000, 0xf90baa21, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "avp.01",   0x00000, 0x08000, 0x2d3b4220 );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "avp.11",   0x000000, 0x200000, 0x83499817 )
		ROM_LOAD16_WORD_SWAP( "avp.12",   0x200000, 0x200000, 0xf4110d49 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_avspj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "avpj.03d", 0x000000, 0x80000, 0x49799119 )
		ROM_LOAD16_WORD_SWAP( "avpj.04d", 0x080000, 0x80000, 0x8cd2bba8 )
		ROM_LOAD16_WORD_SWAP( "avp.05d",  0x100000, 0x80000, 0xfbfb5d7a )
		ROM_LOAD16_WORD_SWAP( "avp.06",   0x180000, 0x80000, 0x190b817f )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "avpjx.03d", 0x000000, 0x80000, 0x94095fb0 )
		ROM_LOAD16_WORD_SWAP( "avpjx.04d", 0x080000, 0x80000, 0xa56b00ae )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROMX_LOAD( "avp.13",   0x0000000, 0x200000, 0x8f8b5ae4, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.15",   0x0000002, 0x200000, 0xb00280df, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.17",   0x0000004, 0x200000, 0x94403195, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.19",   0x0000006, 0x200000, 0xe1981245, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.14",   0x0800000, 0x200000, 0xebba093e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.16",   0x0800002, 0x200000, 0xfb228297, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.18",   0x0800004, 0x200000, 0x34fb7232, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.20",   0x0800006, 0x200000, 0xf90baa21, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "avp.01",   0x00000, 0x08000, 0x2d3b4220 );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "avp.11",   0x000000, 0x200000, 0x83499817 )
		ROM_LOAD16_WORD_SWAP( "avp.12",   0x200000, 0x200000, 0xf4110d49 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_avspa = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "avpa.03d", 0x000000, 0x80000, 0x6c1c1858 )
		ROM_LOAD16_WORD_SWAP( "avpa.04d", 0x080000, 0x80000, 0x94f50b0c )
		ROM_LOAD16_WORD_SWAP( "avp.05d",  0x100000, 0x80000, 0xfbfb5d7a )
		ROM_LOAD16_WORD_SWAP( "avp.06",   0x180000, 0x80000, 0x190b817f )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "avpax.03d", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "avpax.04d", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROMX_LOAD( "avp.13",   0x0000000, 0x200000, 0x8f8b5ae4, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.15",   0x0000002, 0x200000, 0xb00280df, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.17",   0x0000004, 0x200000, 0x94403195, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.19",   0x0000006, 0x200000, 0xe1981245, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.14",   0x0800000, 0x200000, 0xebba093e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.16",   0x0800002, 0x200000, 0xfb228297, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.18",   0x0800004, 0x200000, 0x34fb7232, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "avp.20",   0x0800006, 0x200000, 0xf90baa21, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "avp.01",   0x00000, 0x08000, 0x2d3b4220 );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "avp.11",   0x000000, 0x200000, 0x83499817 )
		ROM_LOAD16_WORD_SWAP( "avp.12",   0x200000, 0x200000, 0xf4110d49 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_batcirj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "btcj.03", 0x000000, 0x80000, 0x6b7e168d )
		ROM_LOAD16_WORD_SWAP( "btcj.04", 0x080000, 0x80000, 0x46ba3467 )
		ROM_LOAD16_WORD_SWAP( "btcj.05", 0x100000, 0x80000, 0x0e23a859 )
		ROM_LOAD16_WORD_SWAP( "btcj.06", 0x180000, 0x80000, 0xa853b59c )
		ROM_LOAD16_WORD_SWAP( "btc.07",  0x200000, 0x80000, 0x7322d5db )
		ROM_LOAD16_WORD_SWAP( "btc.08",  0x280000, 0x80000, 0x6aac85ab )
		ROM_LOAD16_WORD_SWAP( "btc.09",  0x300000, 0x80000, 0x1203db08 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "btcjx.03", 0x000000, 0x80000, 0x01482d08 )
		ROM_LOAD16_WORD_SWAP( "btcjx.04", 0x080000, 0x80000, 0x3d4c976b )
		ROM_LOAD16_WORD_SWAP( "btcjx.05", 0x100000, 0x80000, 0x5bf819e1 )
		ROM_LOAD16_WORD_SWAP( "btcjx.06", 0x180000, 0x80000, 0x5d2fd190 )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROMX_LOAD( "btc.13",   0x000000, 0x400000, 0xdc705bad, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "btc.15",   0x000002, 0x400000, 0xe5779a3c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "btc.17",   0x000004, 0x400000, 0xb33f4112, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "btc.19",   0x000006, 0x400000, 0xa6fcdb7e, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "btc.01",   0x00000, 0x08000, 0x1e194310 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "btc.02",   0x28000, 0x20000, 0x01aeb8e6 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "btc.11",   0x000000, 0x200000, 0xc27f2229 )
		ROM_LOAD16_WORD_SWAP( "btc.12",   0x200000, 0x200000, 0x418a2e33 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_batcira = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "btca.03", 0x000000, 0x80000, 0x1ad20d87 )
		ROM_LOAD16_WORD_SWAP( "btca.04", 0x080000, 0x80000, 0x2b3f4dbe )
		ROM_LOAD16_WORD_SWAP( "btca.05", 0x100000, 0x80000, 0x8238a3d9 )
		ROM_LOAD16_WORD_SWAP( "btca.06", 0x180000, 0x80000, 0x446c7c02 )
		ROM_LOAD16_WORD_SWAP( "btc.07",  0x200000, 0x80000, 0x7322d5db )
		ROM_LOAD16_WORD_SWAP( "btc.08",  0x280000, 0x80000, 0x6aac85ab )
		ROM_LOAD16_WORD_SWAP( "btc.09",  0x300000, 0x80000, 0x1203db08 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "btcax.03", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "btcax.04", 0x080000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "btcax.05", 0x100000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "btcax.06", 0x180000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROMX_LOAD( "btc.13",   0x000000, 0x400000, 0xdc705bad, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "btc.15",   0x000002, 0x400000, 0xe5779a3c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "btc.17",   0x000004, 0x400000, 0xb33f4112, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "btc.19",   0x000006, 0x400000, 0xa6fcdb7e, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "btc.01",   0x00000, 0x08000, 0x1e194310 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "btc.02",   0x28000, 0x20000, 0x01aeb8e6 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "btc.11",   0x000000, 0x200000, 0xc27f2229 )
		ROM_LOAD16_WORD_SWAP( "btc.12",   0x200000, 0x200000, 0x418a2e33 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_csclubj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "cscj.03", 0x000000, 0x80000, 0xec4ddaa2 )
		ROM_LOAD16_WORD_SWAP( "cscj.04", 0x080000, 0x80000, 0x60c632bb )
		ROM_LOAD16_WORD_SWAP( "cscj.05", 0x100000, 0x80000, 0xad042003 )
		ROM_LOAD16_WORD_SWAP( "cscj.06", 0x180000, 0x80000, 0x169e4d40 )
		ROM_LOAD16_WORD_SWAP( "csc.07",  0x200000, 0x80000, 0x01b05caa )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "cscjx.03", 0x000000, 0x80000, 0x2de1d45d )
		ROM_LOAD16_WORD_SWAP( "cscjx.04", 0x080000, 0x80000, 0x81b25d76 )
		ROM_LOAD16_WORD_SWAP( "cscjx.05", 0x100000, 0x80000, 0x5adb1c93 )
		ROM_LOAD16_WORD_SWAP( "cscjx.06", 0x180000, 0x80000, 0xf5558f79 )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROM_FILL(              0x000000, 0x800000, 0 );
		ROMX_LOAD( "csc.14",   0x800000, 0x200000, 0xe8904afa, ROM_GROUPWORD | ROM_SKIP(6) ) /* roms 73 to 76 joined in all eprom version */
		ROMX_LOAD( "csc.16",   0x800002, 0x200000, 0xc98c8079, ROM_GROUPWORD | ROM_SKIP(6) ) /* roms 63 to 66 joined in all eprom version */
		ROMX_LOAD( "csc.18",   0x800004, 0x200000, 0xc030df5a, ROM_GROUPWORD | ROM_SKIP(6) ) /* roms 83 to 86 joined in all eprom version */
		ROMX_LOAD( "csc.20",   0x800006, 0x200000, 0xb4e55863, ROM_GROUPWORD | ROM_SKIP(6) ) /* roms 93 to 96 joined in all eprom version */
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "csc.01",   0x00000, 0x08000, 0xee162111 );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "csc.11",   0x000000, 0x200000, 0xa027b827 ) /* roms 51 to 54 joined in all eprom version */
		ROM_LOAD16_WORD_SWAP( "csc.12",   0x200000, 0x200000, 0xcb7f6e55 ) /* roms 55 to 58 joined in all eprom version */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_cscluba = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "csca.03", 0x000000, 0x80000, 0xb6acd708 )
		ROM_LOAD16_WORD_SWAP( "csca.04", 0x080000, 0x80000, 0xd44ae35f )
		ROM_LOAD16_WORD_SWAP( "csca.05", 0x100000, 0x80000, 0x8da76aec )
		ROM_LOAD16_WORD_SWAP( "csca.06", 0x180000, 0x80000, 0xa1b7b1ee )
		ROM_LOAD16_WORD_SWAP( "csc.07",  0x200000, 0x80000, 0x01b05caa )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "cscax.03", 0x000000, 0x80000, 0x9f95b1e1 )
		ROM_LOAD16_WORD_SWAP( "cscax.04", 0x080000, 0x80000, 0x08e85ab2 )
		ROM_LOAD16_WORD_SWAP( "cscax.05", 0x100000, 0x80000, 0x1b2fae1d )
		ROM_LOAD16_WORD_SWAP( "cscax.06", 0x180000, 0x80000, 0x9e548ba8 )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROM_FILL(              0x000000, 0x800000, 0 );
		ROMX_LOAD( "csc.14",   0x800000, 0x200000, 0xe8904afa, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "csc.16",   0x800002, 0x200000, 0xc98c8079, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "csc.18",   0x800004, 0x200000, 0xc030df5a, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "csc.20",   0x800006, 0x200000, 0xb4e55863, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "csc.01",   0x00000, 0x08000, 0xee162111 );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "csc.11",   0x000000, 0x200000, 0xa027b827 )
		ROM_LOAD16_WORD_SWAP( "csc.12",   0x200000, 0x200000, 0xcb7f6e55 )
	ROM_END(); }}; 
	
	
	static RomLoadPtr rom_cybotsj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "cybj.03", 0x000000, 0x80000, 0x6096eada )
		ROM_LOAD16_WORD_SWAP( "cybj.04", 0x080000, 0x80000, 0x7b0ffaa9 )
		ROM_LOAD16_WORD_SWAP( "cybj.05", 0x100000, 0x80000, 0xec40408e )
		ROM_LOAD16_WORD_SWAP( "cybj.06", 0x180000, 0x80000, 0x1ad0bed2 )
		ROM_LOAD16_WORD_SWAP( "cybj.07", 0x200000, 0x80000, 0x6245a39a )
		ROM_LOAD16_WORD_SWAP( "cybj.08", 0x280000, 0x80000, 0x4b48e223 )
		ROM_LOAD16_WORD_SWAP( "cybj.09", 0x300000, 0x80000, 0xe15238f6 )
		ROM_LOAD16_WORD_SWAP( "cybj.10", 0x380000, 0x80000, 0x75f4003b )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "cybjx.03", 0x000000, 0x80000, 0x867c9acd )
		ROM_LOAD16_WORD_SWAP( "cybjx.04", 0x080000, 0x80000, 0x57ed677f )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "cyb.13",   0x0000000, 0x400000, 0xf0dce192, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "cyb.15",   0x0000002, 0x400000, 0x187aa39c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "cyb.17",   0x0000004, 0x400000, 0x8a0e4b12, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "cyb.19",   0x0000006, 0x400000, 0x34b62612, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "cyb.14",   0x1000000, 0x400000, 0xc1537957, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "cyb.16",   0x1000002, 0x400000, 0x15349e86, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "cyb.18",   0x1000004, 0x400000, 0xd83e977d, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "cyb.20",   0x1000006, 0x400000, 0x77cdad5c, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "cyb.01",   0x00000, 0x08000, 0x9c0fb079 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "cyb.02",   0x28000, 0x20000, 0x51cb0c4e );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "cyb.11",   0x000000, 0x200000, 0x362ccab2 )
		ROM_LOAD16_WORD_SWAP( "cyb.12",   0x200000, 0x200000, 0x7066e9cc )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ddtod = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "dade.03c", 0x000000, 0x80000, 0x8e73533d )
		ROM_LOAD16_WORD_SWAP( "dade.04c", 0x080000, 0x80000, 0x00c2e82e )
		ROM_LOAD16_WORD_SWAP( "dade.05c", 0x100000, 0x80000, 0xea996008 )
		ROM_LOAD16_WORD_SWAP( "dade.06a", 0x180000, 0x80000, 0x6225495a )
		ROM_LOAD16_WORD_SWAP( "dade.07a", 0x200000, 0x80000, 0xb3480ec3 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "dadex.03c", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "dadex.04c", 0x080000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "dadex.05c", 0x100000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0xc00000, REGION_GFX1, 0 );
		ROMX_LOAD( "dad.13",   0x000000, 0x200000, 0xda3cb7d6, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.15",   0x000002, 0x200000, 0x92b63172, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.17",   0x000004, 0x200000, 0xb98757f5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.19",   0x000006, 0x200000, 0x8121ce46, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.14",   0x800000, 0x100000, 0x837e6f3f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.16",   0x800002, 0x100000, 0xf0916bdb, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.18",   0x800004, 0x100000, 0xcef393ef, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.20",   0x800006, 0x100000, 0x8953fe9e, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "dad.01",   0x00000, 0x08000, 0x3f5e2424 );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "dad.11",   0x000000, 0x200000, 0x0c499b67 )
		ROM_LOAD16_WORD_SWAP( "dad.12",   0x200000, 0x200000, 0x2f0b5a4e )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ddtodu = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "dadu.03b", 0x000000, 0x80000, 0xa519905f )
		ROM_LOAD16_WORD_SWAP( "dadu.04b", 0x080000, 0x80000, 0x52562d38 )
		ROM_LOAD16_WORD_SWAP( "dadu.05b", 0x100000, 0x80000, 0xee1cfbfe )
		ROM_LOAD16_WORD_SWAP( "dad.06",   0x180000, 0x80000, 0x13aa3e56 )
		ROM_LOAD16_WORD_SWAP( "dad.07",   0x200000, 0x80000, 0x431cb6dd )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "dadux.03b", 0x000000, 0x80000, 0xf59ee70c )
		ROM_LOAD16_WORD_SWAP( "dadux.04b", 0x080000, 0x80000, 0x622628ae )
		ROM_LOAD16_WORD_SWAP( "dadux.05b", 0x100000, 0x80000, 0x424bd6e3 )
	
		ROM_REGION( 0xc00000, REGION_GFX1, 0 );
		ROMX_LOAD( "dad.13",   0x000000, 0x200000, 0xda3cb7d6, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.15",   0x000002, 0x200000, 0x92b63172, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.17",   0x000004, 0x200000, 0xb98757f5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.19",   0x000006, 0x200000, 0x8121ce46, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.14",   0x800000, 0x100000, 0x837e6f3f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.16",   0x800002, 0x100000, 0xf0916bdb, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.18",   0x800004, 0x100000, 0xcef393ef, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.20",   0x800006, 0x100000, 0x8953fe9e, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "dad.01",   0x00000, 0x08000, 0x3f5e2424 );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "dad.11",   0x000000, 0x200000, 0x0c499b67 )
		ROM_LOAD16_WORD_SWAP( "dad.12",   0x200000, 0x200000, 0x2f0b5a4e )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ddtodur1 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
	  	ROM_LOAD16_WORD_SWAP( "dadu.03a", 0x000000, 0x80000, 0x4413f177 )
		ROM_LOAD16_WORD_SWAP( "dadu.04a", 0x080000, 0x80000, 0x168de230 )
		ROM_LOAD16_WORD_SWAP( "dadu.05a", 0x100000, 0x80000, 0x03d39e91 )
		ROM_LOAD16_WORD_SWAP( "dad.06",   0x180000, 0x80000, 0x13aa3e56 )
		ROM_LOAD16_WORD_SWAP( "dad.07",   0x200000, 0x80000, 0x431cb6dd )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "dadux.03a", 0x000000, 0x80000, 0xf9ba14b6 )
		ROM_LOAD16_WORD_SWAP( "dadux.04a", 0x080000, 0x80000, 0xed85ec29 )
		ROM_LOAD16_WORD_SWAP( "dadux.05a", 0x100000, 0x80000, 0xdbae3d1b )
	
		ROM_REGION( 0xc00000, REGION_GFX1, 0 );
		ROMX_LOAD( "dad.13",   0x000000, 0x200000, 0xda3cb7d6, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.15",   0x000002, 0x200000, 0x92b63172, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.17",   0x000004, 0x200000, 0xb98757f5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.19",   0x000006, 0x200000, 0x8121ce46, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.14",   0x800000, 0x100000, 0x837e6f3f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.16",   0x800002, 0x100000, 0xf0916bdb, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.18",   0x800004, 0x100000, 0xcef393ef, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.20",   0x800006, 0x100000, 0x8953fe9e, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "dad.01",   0x00000, 0x08000, 0x3f5e2424 );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "dad.11",   0x000000, 0x200000, 0x0c499b67 )
		ROM_LOAD16_WORD_SWAP( "dad.12",   0x200000, 0x200000, 0x2f0b5a4e )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ddtodj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "dadj.03a", 0x000000, 0x80000, 0x711638dc )
		ROM_LOAD16_WORD_SWAP( "dadj.04a", 0x080000, 0x80000, 0x4869639c )
		ROM_LOAD16_WORD_SWAP( "dadj.05a", 0x100000, 0x80000, 0x484c0efa )
		ROM_LOAD16_WORD_SWAP( "dad.06",   0x180000, 0x80000, 0x13aa3e56 )
		ROM_LOAD16_WORD_SWAP( "dad.07",   0x200000, 0x80000, 0x431cb6dd )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "dadjx.03a", 0x000000, 0x80000, 0x4c7334d3 )
		ROM_LOAD16_WORD_SWAP( "dadjx.04a", 0x080000, 0x80000, 0xcfd15109 )
		ROM_LOAD16_WORD_SWAP( "dadjx.05a", 0x100000, 0x80000, 0xd23bcd71 )
	
		ROM_REGION( 0xc00000, REGION_GFX1, 0 );
		ROMX_LOAD( "dad.13",   0x000000, 0x200000, 0xda3cb7d6, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.15",   0x000002, 0x200000, 0x92b63172, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.17",   0x000004, 0x200000, 0xb98757f5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.19",   0x000006, 0x200000, 0x8121ce46, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.14",   0x800000, 0x100000, 0x837e6f3f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.16",   0x800002, 0x100000, 0xf0916bdb, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.18",   0x800004, 0x100000, 0xcef393ef, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.20",   0x800006, 0x100000, 0x8953fe9e, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "dad.01",   0x00000, 0x08000, 0x3f5e2424 );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "dad.11",   0x000000, 0x200000, 0x0c499b67 )
		ROM_LOAD16_WORD_SWAP( "dad.12",   0x200000, 0x200000, 0x2f0b5a4e )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ddtoda = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "dada.03a", 0x000000, 0x80000, 0xfc6f2dd7 )
		ROM_LOAD16_WORD_SWAP( "dada.04a", 0x080000, 0x80000, 0xd4be4009 )
		ROM_LOAD16_WORD_SWAP( "dada.05a", 0x100000, 0x80000, 0x6712d1cf )
		ROM_LOAD16_WORD_SWAP( "dad.06",   0x180000, 0x80000, 0x13aa3e56 )
		ROM_LOAD16_WORD_SWAP( "dad.07",   0x200000, 0x80000, 0x431cb6dd )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "dadax.03a", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "dadax.04a", 0x080000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "dadax.05a", 0x100000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0xc00000, REGION_GFX1, 0 );
		ROMX_LOAD( "dad.13",   0x000000, 0x200000, 0xda3cb7d6, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.15",   0x000002, 0x200000, 0x92b63172, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.17",   0x000004, 0x200000, 0xb98757f5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.19",   0x000006, 0x200000, 0x8121ce46, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.14",   0x800000, 0x100000, 0x837e6f3f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.16",   0x800002, 0x100000, 0xf0916bdb, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.18",   0x800004, 0x100000, 0xcef393ef, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dad.20",   0x800006, 0x100000, 0x8953fe9e, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "dad.01",   0x00000, 0x08000, 0x3f5e2424 );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "dad.11",   0x000000, 0x200000, 0x0c499b67 )
		ROM_LOAD16_WORD_SWAP( "dad.12",   0x200000, 0x200000, 0x2f0b5a4e )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ddsom = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "dd2e.03b", 0x000000, 0x80000, 0xcd2deb66 )
		ROM_LOAD16_WORD_SWAP( "dd2e.04d", 0x080000, 0x80000, 0xbfee43cc )
		ROM_LOAD16_WORD_SWAP( "dd2e.05b", 0x100000, 0x80000, 0x049ab19d )
		ROM_LOAD16_WORD_SWAP( "dd2e.06d", 0x180000, 0x80000, 0x3994fb8b )
		ROM_LOAD16_WORD_SWAP( "dd2e.07",  0x200000, 0x80000, 0xbb777a02 )
		ROM_LOAD16_WORD_SWAP( "dd2e.08",  0x280000, 0x80000, 0x30970890 )
		ROM_LOAD16_WORD_SWAP( "dd2e.09",  0x300000, 0x80000, 0x99e2194d )
		ROM_LOAD16_WORD_SWAP( "dd2e.10",  0x380000, 0x80000, 0xe198805e )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "dd2ex.03b", 0x000000, 0x80000, 0xe4924c80 )
		ROM_LOAD16_WORD_SWAP( "dd2ex.04d", 0x080000, 0x80000, 0x13c8b16f )
	
		ROM_REGION( 0x1800000, REGION_GFX1, 0 );
		ROMX_LOAD( "dd2.13",   0x0000000, 0x400000, 0xa46b4e6e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.15",   0x0000002, 0x400000, 0xd5fc50fc, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.17",   0x0000004, 0x400000, 0x837c0867, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.19",   0x0000006, 0x400000, 0xbb0ec21c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.14",   0x1000000, 0x200000, 0x6d824ce2, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.16",   0x1000002, 0x200000, 0x79682ae5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.18",   0x1000004, 0x200000, 0xacddd149, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.20",   0x1000006, 0x200000, 0x117fb0c0, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "dd2.01",   0x00000, 0x08000, 0x99d657e5 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "dd2.02",   0x28000, 0x20000, 0x117a3824 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "dd2.11",   0x000000, 0x200000, 0x98d0c325 )
		ROM_LOAD16_WORD_SWAP( "dd2.12",   0x200000, 0x200000, 0x5ea2e7fa )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ddsomu = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "dd2u.03d", 0x000000, 0x80000, 0x0f700d84 )
		ROM_LOAD16_WORD_SWAP( "dd2u.04d", 0x080000, 0x80000, 0xb99eb254 )
		ROM_LOAD16_WORD_SWAP( "dd2u.05d", 0x100000, 0x80000, 0xb23061f3 )
		ROM_LOAD16_WORD_SWAP( "dd2u.06d", 0x180000, 0x80000, 0x8bf1d8ce )
		ROM_LOAD16_WORD_SWAP( "dd2.07",   0x200000, 0x80000, 0x909a0b8b )
		ROM_LOAD16_WORD_SWAP( "dd2.08",   0x280000, 0x80000, 0xe53c4d01 )
		ROM_LOAD16_WORD_SWAP( "dd2.09",   0x300000, 0x80000, 0x5f86279f )
		ROM_LOAD16_WORD_SWAP( "dd2.10",   0x380000, 0x80000, 0xad954c26 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "dd2ux.03d", 0x000000, 0x80000, 0x5cecbdb3 )
		ROM_LOAD16_WORD_SWAP( "dd2ux.04d", 0x080000, 0x80000, 0x1307a77d )
	
		ROM_REGION( 0x1800000, REGION_GFX1, 0 );
		ROMX_LOAD( "dd2.13",   0x0000000, 0x400000, 0xa46b4e6e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.15",   0x0000002, 0x400000, 0xd5fc50fc, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.17",   0x0000004, 0x400000, 0x837c0867, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.19",   0x0000006, 0x400000, 0xbb0ec21c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.14",   0x1000000, 0x200000, 0x6d824ce2, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.16",   0x1000002, 0x200000, 0x79682ae5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.18",   0x1000004, 0x200000, 0xacddd149, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.20",   0x1000006, 0x200000, 0x117fb0c0, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "dd2.01",   0x00000, 0x08000, 0x99d657e5 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "dd2.02",   0x28000, 0x20000, 0x117a3824 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "dd2.11",   0x000000, 0x200000, 0x98d0c325 )
		ROM_LOAD16_WORD_SWAP( "dd2.12",   0x200000, 0x200000, 0x5ea2e7fa )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ddsomj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "dd2j.03g", 0x000000, 0x80000, 0xe6c8c985 )
		ROM_LOAD16_WORD_SWAP( "dd2j.04g", 0x080000, 0x80000, 0x8386c0bd )
		ROM_LOAD16_WORD_SWAP( "dd2j.05g", 0x100000, 0x80000, 0x5eb1991c )
		ROM_LOAD16_WORD_SWAP( "dd2j.06g", 0x180000, 0x80000, 0xc26b5e55 )
		ROM_LOAD16_WORD_SWAP( "dd2.07",   0x200000, 0x80000, 0x909a0b8b )
		ROM_LOAD16_WORD_SWAP( "dd2.08",   0x280000, 0x80000, 0xe53c4d01 )
		ROM_LOAD16_WORD_SWAP( "dd2.09",   0x300000, 0x80000, 0x5f86279f )
		ROM_LOAD16_WORD_SWAP( "dd2.10",   0x380000, 0x80000, 0xad954c26 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "dd2jx.03g", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "dd2jx.04g", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x1800000, REGION_GFX1, 0 );
		ROMX_LOAD( "dd2.13",   0x0000000, 0x400000, 0xa46b4e6e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.15",   0x0000002, 0x400000, 0xd5fc50fc, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.17",   0x0000004, 0x400000, 0x837c0867, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.19",   0x0000006, 0x400000, 0xbb0ec21c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.14",   0x1000000, 0x200000, 0x6d824ce2, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.16",   0x1000002, 0x200000, 0x79682ae5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.18",   0x1000004, 0x200000, 0xacddd149, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.20",   0x1000006, 0x200000, 0x117fb0c0, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "dd2.01",   0x00000, 0x08000, 0x99d657e5 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "dd2.02",   0x28000, 0x20000, 0x117a3824 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "dd2.11",   0x000000, 0x200000, 0x98d0c325 )
		ROM_LOAD16_WORD_SWAP( "dd2.12",   0x200000, 0x200000, 0x5ea2e7fa )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ddsomjr1 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "dd2j.03b", 0x000000, 0x80000, 0x965d74e5 )
		ROM_LOAD16_WORD_SWAP( "dd2j.04b", 0x080000, 0x80000, 0x958eb8f3 )
		ROM_LOAD16_WORD_SWAP( "dd2j.05b", 0x100000, 0x80000, 0xd38571ca )
		ROM_LOAD16_WORD_SWAP( "dd2j.06b", 0x180000, 0x80000, 0x6d5a3bbb )
		ROM_LOAD16_WORD_SWAP( "dd2.07",   0x200000, 0x80000, 0x909a0b8b )
		ROM_LOAD16_WORD_SWAP( "dd2.08",   0x280000, 0x80000, 0xe53c4d01 )
		ROM_LOAD16_WORD_SWAP( "dd2.09",   0x300000, 0x80000, 0x5f86279f )
		ROM_LOAD16_WORD_SWAP( "dd2.10",   0x380000, 0x80000, 0xad954c26 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "dd2jx.03b", 0x000000, 0x80000, 0xa63488bb )
		ROM_LOAD16_WORD_SWAP( "dd2jx.04b", 0x080000, 0x80000, 0xe3ff7985 )
	
		ROM_REGION( 0x1800000, REGION_GFX1, 0 );
		ROMX_LOAD( "dd2.13",   0x0000000, 0x400000, 0xa46b4e6e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.15",   0x0000002, 0x400000, 0xd5fc50fc, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.17",   0x0000004, 0x400000, 0x837c0867, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.19",   0x0000006, 0x400000, 0xbb0ec21c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.14",   0x1000000, 0x200000, 0x6d824ce2, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.16",   0x1000002, 0x200000, 0x79682ae5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.18",   0x1000004, 0x200000, 0xacddd149, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.20",   0x1000006, 0x200000, 0x117fb0c0, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "dd2.01",   0x00000, 0x08000, 0x99d657e5 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "dd2.02",   0x28000, 0x20000, 0x117a3824 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "dd2.11",   0x000000, 0x200000, 0x98d0c325 )
		ROM_LOAD16_WORD_SWAP( "dd2.12",   0x200000, 0x200000, 0x5ea2e7fa )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ddsoma = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "dd2a.03g", 0x000000, 0x80000, 0x0b4fec22 )
		ROM_LOAD16_WORD_SWAP( "dd2a.04g", 0x080000, 0x80000, 0x055b7019 )
		ROM_LOAD16_WORD_SWAP( "dd2a.05g", 0x100000, 0x80000, 0x5eb1991c )
		ROM_LOAD16_WORD_SWAP( "dd2a.06g", 0x180000, 0x80000, 0xc26b5e55 )
		ROM_LOAD16_WORD_SWAP( "dd2.07",   0x200000, 0x80000, 0x909a0b8b )
		ROM_LOAD16_WORD_SWAP( "dd2.08",   0x280000, 0x80000, 0xe53c4d01 )
		ROM_LOAD16_WORD_SWAP( "dd2.09",   0x300000, 0x80000, 0x5f86279f )
		ROM_LOAD16_WORD_SWAP( "dd2.10",   0x380000, 0x80000, 0xad954c26 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "dd2ax.03g", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "dd2ax.04g", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x1800000, REGION_GFX1, 0 );
		ROMX_LOAD( "dd2.13",   0x0000000, 0x400000, 0xa46b4e6e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.15",   0x0000002, 0x400000, 0xd5fc50fc, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.17",   0x0000004, 0x400000, 0x837c0867, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.19",   0x0000006, 0x400000, 0xbb0ec21c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.14",   0x1000000, 0x200000, 0x6d824ce2, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.16",   0x1000002, 0x200000, 0x79682ae5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.18",   0x1000004, 0x200000, 0xacddd149, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "dd2.20",   0x1000006, 0x200000, 0x117fb0c0, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "dd2.01",   0x00000, 0x08000, 0x99d657e5 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "dd2.02",   0x28000, 0x20000, 0x117a3824 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "dd2.11",   0x000000, 0x200000, 0x98d0c325 )
		ROM_LOAD16_WORD_SWAP( "dd2.12",   0x200000, 0x200000, 0x5ea2e7fa )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_dimahoo = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "gmdu.03", 0x000000, 0x80000, 0x43bcb15f )
		ROM_LOAD16_WORD_SWAP( "gmd.04",  0x080000, 0x80000, 0x37485567 )
		ROM_LOAD16_WORD_SWAP( "gmd.05",  0x100000, 0x80000, 0xda269ffb )
		ROM_LOAD16_WORD_SWAP( "gmd.06",  0x180000, 0x80000, 0x55b483c9 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "gmdux.03", 0x000000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROMX_LOAD( "gmd.13",   0x000000, 0x400000, 0x80dd19f0, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "gmd.15",   0x000002, 0x400000, 0xdfd93a78, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "gmd.17",   0x000004, 0x400000, 0x16356520, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "gmd.19",   0x000006, 0x400000, 0xdfc33031, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "gmd.01",   0x00000, 0x08000, 0x3f9bc985 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "gmd.02",   0x28000, 0x20000, 0x3fd39dde );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "gmd.11",   0x000000, 0x400000, 0x06a65542 )
		ROM_LOAD16_WORD_SWAP( "gmd.12",   0x400000, 0x400000, 0x50bc7a31 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_gmahou = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "gmdj.03", 0x000000, 0x80000, 0xcd6979e3 )
		ROM_LOAD16_WORD_SWAP( "gmd.04",  0x080000, 0x80000, 0x37485567 )
		ROM_LOAD16_WORD_SWAP( "gmd.05",  0x100000, 0x80000, 0xda269ffb )
		ROM_LOAD16_WORD_SWAP( "gmd.06",  0x180000, 0x80000, 0x55b483c9 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "gmdjx.03", 0x000000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROMX_LOAD( "gmd.13",   0x000000, 0x400000, 0x80dd19f0, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "gmd.15",   0x000002, 0x400000, 0xdfd93a78, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "gmd.17",   0x000004, 0x400000, 0x16356520, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "gmd.19",   0x000006, 0x400000, 0xdfc33031, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "gmd.01",   0x00000, 0x08000, 0x3f9bc985 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "gmd.02",   0x28000, 0x20000, 0x3fd39dde );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "gmd.11",   0x000000, 0x400000, 0x06a65542 )
		ROM_LOAD16_WORD_SWAP( "gmd.12",   0x400000, 0x400000, 0x50bc7a31 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_dstlk = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "vamu.03b", 0x000000, 0x80000, 0x68a6343f )
		ROM_LOAD16_WORD_SWAP( "vamu.04b", 0x080000, 0x80000, 0x58161453 )
		ROM_LOAD16_WORD_SWAP( "vamu.05b", 0x100000, 0x80000, 0xdfc038b8 )
		ROM_LOAD16_WORD_SWAP( "vamu.06b", 0x180000, 0x80000, 0xc3842c89 )
		ROM_LOAD16_WORD_SWAP( "vamu.07b", 0x200000, 0x80000, 0x25b60b6e )
		ROM_LOAD16_WORD_SWAP( "vamu.08b", 0x280000, 0x80000, 0x2113c596 )
		ROM_LOAD16_WORD_SWAP( "vamu.09b", 0x300000, 0x80000, 0x2d1e9ae5 )
		ROM_LOAD16_WORD_SWAP( "vamu.10b", 0x380000, 0x80000, 0x81145622 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "vamux.03b", 0x000000, 0x80000, 0x15ff2d3e )
		ROM_LOAD16_WORD_SWAP( "vamux.04b", 0x080000, 0x80000, 0x4cf62f1b )
	
		ROM_REGION( 0x1400000, REGION_GFX1, 0 );
		ROMX_LOAD( "vam.13",   0x0000000, 0x400000, 0xc51baf99, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.15",   0x0000002, 0x400000, 0x3ce83c77, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.17",   0x0000004, 0x400000, 0x4f2408e0, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.19",   0x0000006, 0x400000, 0x9ff60250, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.14",   0x1000000, 0x100000, 0xbd87243c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.16",   0x1000002, 0x100000, 0xafec855f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.18",   0x1000004, 0x100000, 0x3a033625, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.20",   0x1000006, 0x100000, 0x2bff6a89, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "vam.01",   0x00000, 0x08000, 0x64b685d5 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "vam.02",   0x28000, 0x20000, 0xcf7c97c7 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "vam.11",   0x000000, 0x200000, 0x4a39deb2 )
		ROM_LOAD16_WORD_SWAP( "vam.12",   0x200000, 0x200000, 0x1a3e5c03 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_dstlkr1 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "vamu.03a", 0x000000, 0x80000, 0x628899f9 )
		ROM_LOAD16_WORD_SWAP( "vamu.04a", 0x080000, 0x80000, 0x696d9b25 )
		ROM_LOAD16_WORD_SWAP( "vamu.05a", 0x100000, 0x80000, 0x673ed50a )
		ROM_LOAD16_WORD_SWAP( "vamu.06a", 0x180000, 0x80000, 0xf2377be7 )
		ROM_LOAD16_WORD_SWAP( "vamu.07a", 0x200000, 0x80000, 0xd8f498c4 )
		ROM_LOAD16_WORD_SWAP( "vamu.08a", 0x280000, 0x80000, 0xe6a8a1a0 )
		ROM_LOAD16_WORD_SWAP( "vamu.09a", 0x300000, 0x80000, 0x8dd55b24 )
		ROM_LOAD16_WORD_SWAP( "vamu.10a", 0x380000, 0x80000, 0xc1a3d9be )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "vamux.03a", 0x000000, 0x80000, 0xdb0837f5 )
		ROM_LOAD16_WORD_SWAP( "vamux.04a", 0x080000, 0x80000, 0x8a924055 )
	
		ROM_REGION( 0x1400000, REGION_GFX1, 0 );
		ROMX_LOAD( "vam.13",   0x0000000, 0x400000, 0xc51baf99, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.15",   0x0000002, 0x400000, 0x3ce83c77, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.17",   0x0000004, 0x400000, 0x4f2408e0, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.19",   0x0000006, 0x400000, 0x9ff60250, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.14",   0x1000000, 0x100000, 0xbd87243c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.16",   0x1000002, 0x100000, 0xafec855f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.18",   0x1000004, 0x100000, 0x3a033625, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.20",   0x1000006, 0x100000, 0x2bff6a89, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "vam.01",   0x00000, 0x08000, 0x64b685d5 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "vam.02",   0x28000, 0x20000, 0xcf7c97c7 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "vam.11",   0x000000, 0x200000, 0x4a39deb2 )
		ROM_LOAD16_WORD_SWAP( "vam.12",   0x200000, 0x200000, 0x1a3e5c03 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_vampj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "vamj.03a", 0x000000, 0x80000, 0xf55d3722 )
		ROM_LOAD16_WORD_SWAP( "vamj.04b", 0x080000, 0x80000, 0x4d9c43c4 )
		ROM_LOAD16_WORD_SWAP( "vamj.05a", 0x100000, 0x80000, 0x6c497e92 )
		ROM_LOAD16_WORD_SWAP( "vamj.06a", 0x180000, 0x80000, 0xf1bbecb6 )
		ROM_LOAD16_WORD_SWAP( "vamj.07a", 0x200000, 0x80000, 0x1067ad84 )
		ROM_LOAD16_WORD_SWAP( "vamj.08a", 0x280000, 0x80000, 0x4b89f41f )
		ROM_LOAD16_WORD_SWAP( "vamj.09a", 0x300000, 0x80000, 0xfc0a4aac )
		ROM_LOAD16_WORD_SWAP( "vamj.10a", 0x380000, 0x80000, 0x9270c26b )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "vamjx.03a", 0x000000, 0x80000, 0x2549f7bc )
		ROM_LOAD16_WORD_SWAP( "vamjx.04b", 0x080000, 0x80000, 0xbb5a30a5 )
	
		ROM_REGION( 0x1400000, REGION_GFX1, 0 );
		ROMX_LOAD( "vam.13",   0x0000000, 0x400000, 0xc51baf99, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.15",   0x0000002, 0x400000, 0x3ce83c77, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.17",   0x0000004, 0x400000, 0x4f2408e0, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.19",   0x0000006, 0x400000, 0x9ff60250, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.14",   0x1000000, 0x100000, 0xbd87243c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.16",   0x1000002, 0x100000, 0xafec855f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.18",   0x1000004, 0x100000, 0x3a033625, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.20",   0x1000006, 0x100000, 0x2bff6a89, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "vam.01",   0x00000, 0x08000, 0x64b685d5 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "vam.02",   0x28000, 0x20000, 0xcf7c97c7 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "vam.11",   0x000000, 0x200000, 0x4a39deb2 )
		ROM_LOAD16_WORD_SWAP( "vam.12",   0x200000, 0x200000, 0x1a3e5c03 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_vampja = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "vamj.03a", 0x000000, 0x80000, 0xf55d3722 )
		ROM_LOAD16_WORD_SWAP( "vamj.04a", 0x080000, 0x80000, 0xfdcbdae3 )
		ROM_LOAD16_WORD_SWAP( "vamj.05a", 0x100000, 0x80000, 0x6c497e92 )
		ROM_LOAD16_WORD_SWAP( "vamj.06a", 0x180000, 0x80000, 0xf1bbecb6 )
		ROM_LOAD16_WORD_SWAP( "vamj.07a", 0x200000, 0x80000, 0x1067ad84 )
		ROM_LOAD16_WORD_SWAP( "vamj.08a", 0x280000, 0x80000, 0x4b89f41f )
		ROM_LOAD16_WORD_SWAP( "vamj.09a", 0x300000, 0x80000, 0xfc0a4aac )
		ROM_LOAD16_WORD_SWAP( "vamj.10a", 0x380000, 0x80000, 0x9270c26b )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "vamjx.03a", 0x000000, 0x80000, 0x2549f7bc )
		ROM_LOAD16_WORD_SWAP( "vamjx.04a", 0x080000, 0x80000, 0xfe64a5cf )
	
		ROM_REGION( 0x1400000, REGION_GFX1, 0 );
		ROMX_LOAD( "vam.13",   0x0000000, 0x400000, 0xc51baf99, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.15",   0x0000002, 0x400000, 0x3ce83c77, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.17",   0x0000004, 0x400000, 0x4f2408e0, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.19",   0x0000006, 0x400000, 0x9ff60250, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.14",   0x1000000, 0x100000, 0xbd87243c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.16",   0x1000002, 0x100000, 0xafec855f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.18",   0x1000004, 0x100000, 0x3a033625, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.20",   0x1000006, 0x100000, 0x2bff6a89, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "vam.01",   0x00000, 0x08000, 0x64b685d5 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "vam.02",   0x28000, 0x20000, 0xcf7c97c7 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "vam.11",   0x000000, 0x200000, 0x4a39deb2 )
		ROM_LOAD16_WORD_SWAP( "vam.12",   0x200000, 0x200000, 0x1a3e5c03 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_vampjr1 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "vamj.03", 0x000000, 0x80000, 0x8895bf77 )
		ROM_LOAD16_WORD_SWAP( "vamj.04", 0x080000, 0x80000, 0x5027db3d )
		ROM_LOAD16_WORD_SWAP( "vamj.05", 0x100000, 0x80000, 0x97c66fdb )
		ROM_LOAD16_WORD_SWAP( "vamj.06", 0x180000, 0x80000, 0x9b4c3426 )
		ROM_LOAD16_WORD_SWAP( "vamj.07", 0x200000, 0x80000, 0x303bc4fd )
		ROM_LOAD16_WORD_SWAP( "vamj.08", 0x280000, 0x80000, 0x3dea3646 )
		ROM_LOAD16_WORD_SWAP( "vamj.09", 0x300000, 0x80000, 0xc119a827 )
		ROM_LOAD16_WORD_SWAP( "vamj.10", 0x380000, 0x80000, 0x46593b79 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "vamjx.03", 0x000000, 0x80000, 0xf6fe646a )
		ROM_LOAD16_WORD_SWAP( "vamjx.04", 0x080000, 0x80000, 0x566014e5 )
	
		ROM_REGION( 0x1400000, REGION_GFX1, 0 );
		ROMX_LOAD( "vam.13",   0x0000000, 0x400000, 0xc51baf99, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.15",   0x0000002, 0x400000, 0x3ce83c77, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.17",   0x0000004, 0x400000, 0x4f2408e0, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.19",   0x0000006, 0x400000, 0x9ff60250, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.14",   0x1000000, 0x100000, 0xbd87243c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.16",   0x1000002, 0x100000, 0xafec855f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.18",   0x1000004, 0x100000, 0x3a033625, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.20",   0x1000006, 0x100000, 0x2bff6a89, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "vam.01",   0x00000, 0x08000, 0x64b685d5 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "vam.02",   0x28000, 0x20000, 0xcf7c97c7 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "vam.11",   0x000000, 0x200000, 0x4a39deb2 )
		ROM_LOAD16_WORD_SWAP( "vam.12",   0x200000, 0x200000, 0x1a3e5c03 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_vampa = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "vama.03a", 0x000000, 0x80000, 0x294e0bec )
		ROM_LOAD16_WORD_SWAP( "vama.04a", 0x080000, 0x80000, 0xbc18e128 )
		ROM_LOAD16_WORD_SWAP( "vama.05a", 0x100000, 0x80000, 0xe709fa59 )
		ROM_LOAD16_WORD_SWAP( "vama.06a", 0x180000, 0x80000, 0x55e4d387 )
		ROM_LOAD16_WORD_SWAP( "vama.07a", 0x200000, 0x80000, 0x24e8f981 )
		ROM_LOAD16_WORD_SWAP( "vama.08a", 0x280000, 0x80000, 0x743f3a8e )
		ROM_LOAD16_WORD_SWAP( "vama.09a", 0x300000, 0x80000, 0x67fa5573 )
		ROM_LOAD16_WORD_SWAP( "vama.10a", 0x380000, 0x80000, 0x5e03d747 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "vamax.03a", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "vamax.04a", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x1400000, REGION_GFX1, 0 );
		ROMX_LOAD( "vam.13",   0x0000000, 0x400000, 0xc51baf99, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.15",   0x0000002, 0x400000, 0x3ce83c77, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.17",   0x0000004, 0x400000, 0x4f2408e0, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.19",   0x0000006, 0x400000, 0x9ff60250, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.14",   0x1000000, 0x100000, 0xbd87243c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.16",   0x1000002, 0x100000, 0xafec855f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.18",   0x1000004, 0x100000, 0x3a033625, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vam.20",   0x1000006, 0x100000, 0x2bff6a89, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "vam.01",   0x00000, 0x08000, 0x64b685d5 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "vam.02",   0x28000, 0x20000, 0xcf7c97c7 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "vam.11",   0x000000, 0x200000, 0x4a39deb2 )
		ROM_LOAD16_WORD_SWAP( "vam.12",   0x200000, 0x200000, 0x1a3e5c03 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ecofghtr = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "uece.03", 0x000000, 0x80000, 0xec2c1137 )
		ROM_LOAD16_WORD_SWAP( "uece.04", 0x080000, 0x80000, 0xb35f99db )
		ROM_LOAD16_WORD_SWAP( "uece.05", 0x100000, 0x80000, 0xd9d42d31 )
		ROM_LOAD16_WORD_SWAP( "uece.06", 0x180000, 0x80000, 0x9d9771cf )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "uecex.03", 0x000000, 0x80000, 0xe0ff3d51 )
		ROM_LOAD16_WORD_SWAP( "uecex.04", 0x080000, 0x80000, 0xb9f998e8 )
		ROM_LOAD16_WORD_SWAP( "uecex.05", 0x100000, 0x80000, 0x12410260 )
		ROM_LOAD16_WORD_SWAP( "uecex.06", 0x180000, 0x80000, 0xd5b4b1a2 )
	
		ROM_REGION( 0xc00000, REGION_GFX1, 0 );
		ROMX_LOAD( "uec.13",   0x000000, 0x200000, 0xdcaf1436, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "uec.15",   0x000002, 0x200000, 0x2807df41, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "uec.17",   0x000004, 0x200000, 0x8a708d02, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "uec.19",   0x000006, 0x200000, 0xde7be0ef, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "uec.14",   0x800000, 0x100000, 0x1a003558, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "uec.16",   0x800002, 0x100000, 0x4ff8a6f9, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "uec.18",   0x800004, 0x100000, 0xb167ae12, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "uec.20",   0x800006, 0x100000, 0x1064bdc2, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "uec.01",   0x00000, 0x08000, 0xc235bd15 );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "uec.11",   0x000000, 0x200000, 0x81b25d39 )
		ROM_LOAD16_WORD_SWAP( "uec.12",   0x200000, 0x200000, 0x27729e52 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_uecology = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "uecj.03", 0x000000, 0x80000, 0x94c40a4c )
		ROM_LOAD16_WORD_SWAP( "uecj.04", 0x080000, 0x80000, 0x8d6e3a09 )
		ROM_LOAD16_WORD_SWAP( "uecj.05", 0x100000, 0x80000, 0x8604ecd7 )
		ROM_LOAD16_WORD_SWAP( "uecj.06", 0x180000, 0x80000, 0xb7e1d31f )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "uecjx.03", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "uecjx.04", 0x080000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "uecjx.05", 0x100000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "uecjx.06", 0x180000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0xc00000, REGION_GFX1, 0 );
		ROMX_LOAD( "uec.13",   0x000000, 0x200000, 0xdcaf1436, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "uec.15",   0x000002, 0x200000, 0x2807df41, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "uec.17",   0x000004, 0x200000, 0x8a708d02, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "uec.19",   0x000006, 0x200000, 0xde7be0ef, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "uec.14",   0x800000, 0x100000, 0x1a003558, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "uec.16",   0x800002, 0x100000, 0x4ff8a6f9, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "uec.18",   0x800004, 0x100000, 0xb167ae12, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "uec.20",   0x800006, 0x100000, 0x1064bdc2, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "uec.01",   0x00000, 0x08000, 0xc235bd15 );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "uec.11",   0x000000, 0x200000, 0x81b25d39 )
		ROM_LOAD16_WORD_SWAP( "uec.12",   0x200000, 0x200000, 0x27729e52 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_gwingj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION(CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "ggwj.03a", 0x000000, 0x80000, 0xfdd23b91 )
		ROM_LOAD16_WORD_SWAP( "ggwj.04a", 0x080000, 0x80000, 0x8c6e093c )
		ROM_LOAD16_WORD_SWAP( "ggwj.05a", 0x100000, 0x80000, 0x43811454 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "ggwjx.03a", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "ggwjx.04a", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROMX_LOAD( "ggw.13",   0x000000, 0x400000, 0x105530a4, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ggw.15",   0x000002, 0x400000, 0x9e774ab9, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ggw.17",   0x000004, 0x400000, 0x466e0ba4, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ggw.19",   0x000006, 0x400000, 0x840c8dea, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION(QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "ggw.01",   0x00000, 0x08000, 0x4c6351d5 );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "ggw.11",   0x000000, 0x400000, 0xe172acf5 )
		ROM_LOAD16_WORD_SWAP( "ggw.12",   0x400000, 0x400000, 0x4bee4e8f )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_mmatrixj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "mmxj.03", 0x000000, 0x80000, 0x1d5de213 )
		ROM_LOAD16_WORD_SWAP( "mmxj.04", 0x080000, 0x80000, 0xd943a339 )
		ROM_LOAD16_WORD_SWAP( "mmxj.05", 0x100000, 0x80000, 0x0c8b4abb )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "mmxjx.03", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "mmxjx.04", 0x080000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "mmxjx.05", 0x100000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "mmx.13",   0x0000000, 0x400000, 0x04748718, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mmx.15",   0x0000002, 0x400000, 0x38074f44, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mmx.17",   0x0000004, 0x400000, 0xe4635e35, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mmx.19",   0x0000006, 0x400000, 0x4400a3f2, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mmx.14",   0x1000000, 0x400000, 0xd52bf491, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mmx.16",   0x1000002, 0x400000, 0x23f70780, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mmx.18",   0x1000004, 0x400000, 0x2562c9d5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mmx.20",   0x1000006, 0x400000, 0x583a9687, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "mmx.01",   0x00000, 0x08000, 0xc57e8171 );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "mmx.11",   0x000000, 0x400000, 0x4180b39f )
		ROM_LOAD16_WORD_SWAP( "mmx.12",   0x400000, 0x400000, 0x95e22a59 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_msh = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "mshu.03", 0x000000, 0x80000, 0xd2805bdd )
		ROM_LOAD16_WORD_SWAP( "mshu.04", 0x080000, 0x80000, 0x743f96ff )
		ROM_LOAD16_WORD_SWAP( "msh.05",  0x100000, 0x80000, 0x6a091b9e )
		ROM_LOAD16_WORD_SWAP( "msh.06",  0x180000, 0x80000, 0x803e3fa4 )
		ROM_LOAD16_WORD_SWAP( "msh.07",  0x200000, 0x80000, 0xc45f8e27 )
		ROM_LOAD16_WORD_SWAP( "msh.08",  0x280000, 0x80000, 0x9ca6f12c )
		ROM_LOAD16_WORD_SWAP( "msh.09",  0x300000, 0x80000, 0x82ec27af )
		ROM_LOAD16_WORD_SWAP( "msh.10",  0x380000, 0x80000, 0x8d931196 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "mshux.03", 0x000000, 0x80000, 0x10bfc357 )
		ROM_LOAD16_WORD_SWAP( "mshux.04", 0x080000, 0x80000, 0x871f0863 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "msh.13",   0x0000000, 0x400000, 0x09d14566, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "msh.15",   0x0000002, 0x400000, 0xee962057, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "msh.17",   0x0000004, 0x400000, 0x604ece14, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "msh.19",   0x0000006, 0x400000, 0x94a731e8, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "msh.14",   0x1000000, 0x400000, 0x4197973e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "msh.16",   0x1000002, 0x400000, 0x438da4a0, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "msh.18",   0x1000004, 0x400000, 0x4db92d94, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "msh.20",   0x1000006, 0x400000, 0xa2b0c6c0, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "msh.01",   0x00000, 0x08000, 0xc976e6f9 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "msh.02",   0x28000, 0x20000, 0xce67d0d9 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "msh.11",   0x000000, 0x200000, 0x37ac6d30 )
		ROM_LOAD16_WORD_SWAP( "msh.12",   0x200000, 0x200000, 0xde092570 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_mshj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "mshj.03f", 0x000000, 0x80000, 0xff172fd2 )
		ROM_LOAD16_WORD_SWAP( "mshj.04f", 0x080000, 0x80000, 0xebbb205a )
		ROM_LOAD16_WORD_SWAP( "msh.05",   0x100000, 0x80000, 0x6a091b9e )
		ROM_LOAD16_WORD_SWAP( "msh.06",   0x180000, 0x80000, 0x803e3fa4 )
		ROM_LOAD16_WORD_SWAP( "msh.07",   0x200000, 0x80000, 0xc45f8e27 )
		ROM_LOAD16_WORD_SWAP( "msh.08",   0x280000, 0x80000, 0x9ca6f12c )
		ROM_LOAD16_WORD_SWAP( "msh.09",   0x300000, 0x80000, 0x82ec27af )
		ROM_LOAD16_WORD_SWAP( "msh.10",   0x380000, 0x80000, 0x8d931196 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "mshjx.03f", 0x000000, 0x80000, 0xa15f32f2 )
		ROM_LOAD16_WORD_SWAP( "mshjx.04f", 0x080000, 0x80000, 0xc81be228 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "msh.13",   0x0000000, 0x400000, 0x09d14566, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "msh.15",   0x0000002, 0x400000, 0xee962057, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "msh.17",   0x0000004, 0x400000, 0x604ece14, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "msh.19",   0x0000006, 0x400000, 0x94a731e8, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "msh.14",   0x1000000, 0x400000, 0x4197973e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "msh.16",   0x1000002, 0x400000, 0x438da4a0, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "msh.18",   0x1000004, 0x400000, 0x4db92d94, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "msh.20",   0x1000006, 0x400000, 0xa2b0c6c0, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "msh.01",   0x00000, 0x08000, 0xc976e6f9 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "msh.02",   0x28000, 0x20000, 0xce67d0d9 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "msh.11",   0x000000, 0x200000, 0x37ac6d30 )
		ROM_LOAD16_WORD_SWAP( "msh.12",   0x200000, 0x200000, 0xde092570 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_mshh = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "mshh.03c", 0x000000, 0x80000, 0x8d84b0fa )
		ROM_LOAD16_WORD_SWAP( "mshh.04c", 0x080000, 0x80000, 0xd638f601 )
		ROM_LOAD16_WORD_SWAP( "mshh.05a", 0x100000, 0x80000, 0xf37539e6 )
		ROM_LOAD16_WORD_SWAP( "msh.06",   0x180000, 0x80000, 0x803e3fa4 )
		ROM_LOAD16_WORD_SWAP( "msh.07",   0x200000, 0x80000, 0xc45f8e27 )
		ROM_LOAD16_WORD_SWAP( "msh.08",   0x280000, 0x80000, 0x9ca6f12c )
		ROM_LOAD16_WORD_SWAP( "msh.09",   0x300000, 0x80000, 0x82ec27af )
		ROM_LOAD16_WORD_SWAP( "msh.10",   0x380000, 0x80000, 0x8d931196 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "mshhx.03c", 0x000000, 0x80000, 0x6daf52bb )
		ROM_LOAD16_WORD_SWAP( "mshhx.04c", 0x080000, 0x80000, 0x5684655a )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "msh.13",   0x0000000, 0x400000, 0x09d14566, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "msh.15",   0x0000002, 0x400000, 0xee962057, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "msh.17",   0x0000004, 0x400000, 0x604ece14, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "msh.19",   0x0000006, 0x400000, 0x94a731e8, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "msh.14",   0x1000000, 0x400000, 0x4197973e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "msh.16",   0x1000002, 0x400000, 0x438da4a0, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "msh.18",   0x1000004, 0x400000, 0x4db92d94, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "msh.20",   0x1000006, 0x400000, 0xa2b0c6c0, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "msh.01",   0x00000, 0x08000, 0xc976e6f9 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "msh.02",   0x28000, 0x20000, 0xce67d0d9 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "msh.11",   0x000000, 0x200000, 0x37ac6d30 )
		ROM_LOAD16_WORD_SWAP( "msh.12",   0x200000, 0x200000, 0xde092570 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_mshvsf = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "mvsu.03d", 0x000000, 0x80000, 0xae60a66a )
		ROM_LOAD16_WORD_SWAP( "mvsu.04d", 0x080000, 0x80000, 0x91f67d8a )
		ROM_LOAD16_WORD_SWAP( "mvs.05a",  0x100000, 0x80000, 0x1a5de0cb )
		ROM_LOAD16_WORD_SWAP( "mvs.06a",  0x180000, 0x80000, 0x959f3030 )
		ROM_LOAD16_WORD_SWAP( "mvs.07b",  0x200000, 0x80000, 0x7f915bdb )
		ROM_LOAD16_WORD_SWAP( "mvs.08a",  0x280000, 0x80000, 0xc2813884 )
		ROM_LOAD16_WORD_SWAP( "mvs.09b",  0x300000, 0x80000, 0x3ba08818 )
		ROM_LOAD16_WORD_SWAP( "mvs.10b",  0x380000, 0x80000, 0xcf0dba98 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "mvsux.03d", 0x000000, 0x80000, 0x281bcb48 )
		ROM_LOAD16_WORD_SWAP( "mvsux.04d", 0x080000, 0x80000, 0xa2d68628 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "mvs.13",   0x0000000, 0x400000, 0x29b05fd9, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.15",   0x0000002, 0x400000, 0xfaddccf1, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.17",   0x0000004, 0x400000, 0x97aaf4c7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.19",   0x0000006, 0x400000, 0xcb70e915, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.14",   0x1000000, 0x400000, 0xb3b1972d, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.16",   0x1000002, 0x400000, 0x08aadb5d, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.18",   0x1000004, 0x400000, 0xc1228b35, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.20",   0x1000006, 0x400000, 0x366cc6c2, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "mvs.01",   0x00000, 0x08000, 0x68252324 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "mvs.02",   0x28000, 0x20000, 0xb34e773d );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "mvs.11",   0x000000, 0x400000, 0x86219770 )
		ROM_LOAD16_WORD_SWAP( "mvs.12",   0x400000, 0x400000, 0xf2fd7f68 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_mshvsfj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "mvsj.03i", 0x000000, 0x80000, 0xd8cbb691 )
		ROM_LOAD16_WORD_SWAP( "mvsj.04i", 0x080000, 0x80000, 0x32741ace )
		ROM_LOAD16_WORD_SWAP( "mvs.05h",  0x100000, 0x80000, 0x77870dc3 )
		ROM_LOAD16_WORD_SWAP( "mvs.06a",  0x180000, 0x80000, 0x959f3030 )
		ROM_LOAD16_WORD_SWAP( "mvs.07b",  0x200000, 0x80000, 0x7f915bdb )
		ROM_LOAD16_WORD_SWAP( "mvs.08a",  0x280000, 0x80000, 0xc2813884 )
		ROM_LOAD16_WORD_SWAP( "mvs.09b",  0x300000, 0x80000, 0x3ba08818 )
		ROM_LOAD16_WORD_SWAP( "mvs.10b",  0x380000, 0x80000, 0xcf0dba98 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "mvsjx.03i", 0x000000, 0x80000, 0x55170c4c )
		ROM_LOAD16_WORD_SWAP( "mvsjx.04i", 0x080000, 0x80000, 0xe7883768 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "mvs.13",   0x0000000, 0x400000, 0x29b05fd9, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.15",   0x0000002, 0x400000, 0xfaddccf1, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.17",   0x0000004, 0x400000, 0x97aaf4c7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.19",   0x0000006, 0x400000, 0xcb70e915, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.14",   0x1000000, 0x400000, 0xb3b1972d, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.16",   0x1000002, 0x400000, 0x08aadb5d, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.18",   0x1000004, 0x400000, 0xc1228b35, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.20",   0x1000006, 0x400000, 0x366cc6c2, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "mvs.01",   0x00000, 0x08000, 0x68252324 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "mvs.02",   0x28000, 0x20000, 0xb34e773d );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "mvs.11",   0x000000, 0x400000, 0x86219770 )
		ROM_LOAD16_WORD_SWAP( "mvs.12",   0x400000, 0x400000, 0xf2fd7f68 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_mshvsfj1 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "mvsj.03h", 0x000000, 0x80000, 0xfbe2115f )
		ROM_LOAD16_WORD_SWAP( "mvsj.04h", 0x080000, 0x80000, 0xb528a367 )
		ROM_LOAD16_WORD_SWAP( "mvs.05g",  0x100000, 0x80000, 0x9515a245 )
		ROM_LOAD16_WORD_SWAP( "mvs.06a",  0x180000, 0x80000, 0x959f3030 )
		ROM_LOAD16_WORD_SWAP( "mvs.07b",  0x200000, 0x80000, 0x7f915bdb )
		ROM_LOAD16_WORD_SWAP( "mvs.08a",  0x280000, 0x80000, 0xc2813884 )
		ROM_LOAD16_WORD_SWAP( "mvs.09b",  0x300000, 0x80000, 0x3ba08818 )
		ROM_LOAD16_WORD_SWAP( "mvs.10b",  0x380000, 0x80000, 0xcf0dba98 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "mvsjx.03h", 0x000000, 0x80000, 0x6b4201c1 )
		ROM_LOAD16_WORD_SWAP( "mvsjx.04h", 0x080000, 0x80000, 0xab1b04cc )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "mvs.13",   0x0000000, 0x400000, 0x29b05fd9, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.15",   0x0000002, 0x400000, 0xfaddccf1, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.17",   0x0000004, 0x400000, 0x97aaf4c7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.19",   0x0000006, 0x400000, 0xcb70e915, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.14",   0x1000000, 0x400000, 0xb3b1972d, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.16",   0x1000002, 0x400000, 0x08aadb5d, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.18",   0x1000004, 0x400000, 0xc1228b35, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.20",   0x1000006, 0x400000, 0x366cc6c2, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "mvs.01",   0x00000, 0x08000, 0x68252324 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "mvs.02",   0x28000, 0x20000, 0xb34e773d );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "mvs.11",   0x000000, 0x400000, 0x86219770 )
		ROM_LOAD16_WORD_SWAP( "mvs.12",   0x400000, 0x400000, 0xf2fd7f68 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_mshvsfh = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "mvsh.03f", 0x000000, 0x80000, 0x4f60f41e )
		ROM_LOAD16_WORD_SWAP( "mvsh.04f", 0x080000, 0x80000, 0xdc08ec12 )
		ROM_LOAD16_WORD_SWAP( "mvs.05a",  0x100000, 0x80000, 0x1a5de0cb )
		ROM_LOAD16_WORD_SWAP( "mvs.06a",  0x180000, 0x80000, 0x959f3030 )
		ROM_LOAD16_WORD_SWAP( "mvs.07b",  0x200000, 0x80000, 0x7f915bdb )
		ROM_LOAD16_WORD_SWAP( "mvs.08a",  0x280000, 0x80000, 0xc2813884 )
		ROM_LOAD16_WORD_SWAP( "mvs.09b",  0x300000, 0x80000, 0x3ba08818 )
		ROM_LOAD16_WORD_SWAP( "mvs.10b",  0x380000, 0x80000, 0xcf0dba98 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "mvshx.03f", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "mvshx.04f", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "mvs.13",   0x0000000, 0x400000, 0x29b05fd9, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.15",   0x0000002, 0x400000, 0xfaddccf1, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.17",   0x0000004, 0x400000, 0x97aaf4c7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.19",   0x0000006, 0x400000, 0xcb70e915, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.14",   0x1000000, 0x400000, 0xb3b1972d, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.16",   0x1000002, 0x400000, 0x08aadb5d, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.18",   0x1000004, 0x400000, 0xc1228b35, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.20",   0x1000006, 0x400000, 0x366cc6c2, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "mvs.01",   0x00000, 0x08000, 0x68252324 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "mvs.02",   0x28000, 0x20000, 0xb34e773d );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "mvs.11",   0x000000, 0x400000, 0x86219770 )
		ROM_LOAD16_WORD_SWAP( "mvs.12",   0x400000, 0x400000, 0xf2fd7f68 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_mshvsfa = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "mvsa.03d", 0x000000, 0x80000, 0x5b863716 )
		ROM_LOAD16_WORD_SWAP( "mvsa.04d", 0x080000, 0x80000, 0x4886e65f )
		ROM_LOAD16_WORD_SWAP( "mvs.05a",  0x100000, 0x80000, 0x1a5de0cb )
		ROM_LOAD16_WORD_SWAP( "mvs.06a",  0x180000, 0x80000, 0x959f3030 )
		ROM_LOAD16_WORD_SWAP( "mvs.07b",  0x200000, 0x80000, 0x7f915bdb )
		ROM_LOAD16_WORD_SWAP( "mvs.08a",  0x280000, 0x80000, 0xc2813884 )
		ROM_LOAD16_WORD_SWAP( "mvs.09b",  0x300000, 0x80000, 0x3ba08818 )
		ROM_LOAD16_WORD_SWAP( "mvs.10b",  0x380000, 0x80000, 0xcf0dba98 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "mvsax.03d", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "mvsax.04d", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "mvs.13",   0x0000000, 0x400000, 0x29b05fd9, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.15",   0x0000002, 0x400000, 0xfaddccf1, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.17",   0x0000004, 0x400000, 0x97aaf4c7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.19",   0x0000006, 0x400000, 0xcb70e915, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.14",   0x1000000, 0x400000, 0xb3b1972d, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.16",   0x1000002, 0x400000, 0x08aadb5d, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.18",   0x1000004, 0x400000, 0xc1228b35, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.20",   0x1000006, 0x400000, 0x366cc6c2, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "mvs.01",   0x00000, 0x08000, 0x68252324 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "mvs.02",   0x28000, 0x20000, 0xb34e773d );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "mvs.11",   0x000000, 0x400000, 0x86219770 )
		ROM_LOAD16_WORD_SWAP( "mvs.12",   0x400000, 0x400000, 0xf2fd7f68 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_mshvsfa1 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "mvsa.03", 0x000000, 0x80000, 0x92ef1933 )
		ROM_LOAD16_WORD_SWAP( "mvsa.04", 0x080000, 0x80000, 0x4b24373c )
		ROM_LOAD16_WORD_SWAP( "mvs.05",  0x100000, 0x80000, 0xac180c1c )
		ROM_LOAD16_WORD_SWAP( "mvs.06a", 0x180000, 0x80000, 0x959f3030 )
		ROM_LOAD16_WORD_SWAP( "mvs.07b", 0x200000, 0x80000, 0x7f915bdb )
		ROM_LOAD16_WORD_SWAP( "mvs.08a", 0x280000, 0x80000, 0xc2813884 )
		ROM_LOAD16_WORD_SWAP( "mvs.09b", 0x300000, 0x80000, 0x3ba08818 )
		ROM_LOAD16_WORD_SWAP( "mvs.10b", 0x380000, 0x80000, 0xcf0dba98 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "mvsax.03", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "mvsax.04", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "mvs.13",   0x0000000, 0x400000, 0x29b05fd9, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.15",   0x0000002, 0x400000, 0xfaddccf1, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.17",   0x0000004, 0x400000, 0x97aaf4c7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.19",   0x0000006, 0x400000, 0xcb70e915, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.14",   0x1000000, 0x400000, 0xb3b1972d, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.16",   0x1000002, 0x400000, 0x08aadb5d, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.18",   0x1000004, 0x400000, 0xc1228b35, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvs.20",   0x1000006, 0x400000, 0x366cc6c2, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "mvs.01",   0x00000, 0x08000, 0x68252324 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "mvs.02",   0x28000, 0x20000, 0xb34e773d );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "mvs.11",   0x000000, 0x400000, 0x86219770 )
		ROM_LOAD16_WORD_SWAP( "mvs.12",   0x400000, 0x400000, 0xf2fd7f68 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_mvsc = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "mvcu.03d", 0x000000, 0x80000, 0xc6007557 )
		ROM_LOAD16_WORD_SWAP( "mvcu.04d", 0x080000, 0x80000, 0x724b2b20 )
		ROM_LOAD16_WORD_SWAP( "mvc.05a",  0x100000, 0x80000, 0x2d8c8e86 )
		ROM_LOAD16_WORD_SWAP( "mvc.06a",  0x180000, 0x80000, 0x8528e1f5 )
		ROM_LOAD16_WORD_SWAP( "mvc.07",   0x200000, 0x80000, 0xc3baa32b )
		ROM_LOAD16_WORD_SWAP( "mvc.08",   0x280000, 0x80000, 0xbc002fcd )
		ROM_LOAD16_WORD_SWAP( "mvc.09",   0x300000, 0x80000, 0xc67b26df )
		ROM_LOAD16_WORD_SWAP( "mvc.10",   0x380000, 0x80000, 0x0fdd1e26 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "mvcux.03d", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "mvcux.04d", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "mvc.13",   0x0000000, 0x400000, 0xfa5f74bc, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.15",   0x0000002, 0x400000, 0x71938a8f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.17",   0x0000004, 0x400000, 0x92741d07, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.19",   0x0000006, 0x400000, 0xbcb72fc6, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.14",   0x1000000, 0x400000, 0x7f1df4e4, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.16",   0x1000002, 0x400000, 0x90bd3203, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.18",   0x1000004, 0x400000, 0x67aaf727, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.20",   0x1000006, 0x400000, 0x8b0bade8, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "mvc.01",   0x00000, 0x08000, 0x41629e95 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "mvc.02",   0x28000, 0x20000, 0x963abf6b );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "mvc.11",   0x000000, 0x400000, 0x850fe663 )
		ROM_LOAD16_WORD_SWAP( "mvc.12",   0x400000, 0x400000, 0x7ccb1896 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_mvscj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "mvcj.03a", 0x000000, 0x80000, 0x3df18879 )
		ROM_LOAD16_WORD_SWAP( "mvcj.04a", 0x080000, 0x80000, 0x07d212e8 )
		ROM_LOAD16_WORD_SWAP( "mvc.05a",  0x100000, 0x80000, 0x2d8c8e86 )
		ROM_LOAD16_WORD_SWAP( "mvc.06a",  0x180000, 0x80000, 0x8528e1f5 )
		ROM_LOAD16_WORD_SWAP( "mvc.07",   0x200000, 0x80000, 0xc3baa32b )
		ROM_LOAD16_WORD_SWAP( "mvc.08",   0x280000, 0x80000, 0xbc002fcd )
		ROM_LOAD16_WORD_SWAP( "mvc.09",   0x300000, 0x80000, 0xc67b26df )
		ROM_LOAD16_WORD_SWAP( "mvc.10",   0x380000, 0x80000, 0x0fdd1e26 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "mvcjx.03a", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "mvcjx.04a", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "mvc.13",   0x0000000, 0x400000, 0xfa5f74bc, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.15",   0x0000002, 0x400000, 0x71938a8f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.17",   0x0000004, 0x400000, 0x92741d07, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.19",   0x0000006, 0x400000, 0xbcb72fc6, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.14",   0x1000000, 0x400000, 0x7f1df4e4, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.16",   0x1000002, 0x400000, 0x90bd3203, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.18",   0x1000004, 0x400000, 0x67aaf727, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.20",   0x1000006, 0x400000, 0x8b0bade8, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "mvc.01",   0x00000, 0x08000, 0x41629e95 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "mvc.02",   0x28000, 0x20000, 0x963abf6b );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "mvc.11",   0x000000, 0x400000, 0x850fe663 )
		ROM_LOAD16_WORD_SWAP( "mvc.12",   0x400000, 0x400000, 0x7ccb1896 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_mvscjr1 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "mvcj.03", 0x000000, 0x80000, 0x2164213f )
		ROM_LOAD16_WORD_SWAP( "mvcj.04", 0x080000, 0x80000, 0xc905c86f )
		ROM_LOAD16_WORD_SWAP( "mvc.05",  0x100000, 0x80000, 0x7db71ce9 )
		ROM_LOAD16_WORD_SWAP( "mvc.06",  0x180000, 0x80000, 0x4b0b6d3e )
		ROM_LOAD16_WORD_SWAP( "mvc.07",  0x200000, 0x80000, 0xc3baa32b )
		ROM_LOAD16_WORD_SWAP( "mvc.08",  0x280000, 0x80000, 0xbc002fcd )
		ROM_LOAD16_WORD_SWAP( "mvc.09",  0x300000, 0x80000, 0xc67b26df )
		ROM_LOAD16_WORD_SWAP( "mvc.10",  0x380000, 0x80000, 0x0fdd1e26 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "mvcjx.03", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "mvcjx.04", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "mvc.13",   0x0000000, 0x400000, 0xfa5f74bc, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.15",   0x0000002, 0x400000, 0x71938a8f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.17",   0x0000004, 0x400000, 0x92741d07, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.19",   0x0000006, 0x400000, 0xbcb72fc6, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.14",   0x1000000, 0x400000, 0x7f1df4e4, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.16",   0x1000002, 0x400000, 0x90bd3203, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.18",   0x1000004, 0x400000, 0x67aaf727, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.20",   0x1000006, 0x400000, 0x8b0bade8, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "mvc.01",   0x00000, 0x08000, 0x41629e95 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "mvc.02",   0x28000, 0x20000, 0x963abf6b );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "mvc.11",   0x000000, 0x400000, 0x850fe663 )
		ROM_LOAD16_WORD_SWAP( "mvc.12",   0x400000, 0x400000, 0x7ccb1896 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_mvsca = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "mvca.03", 0x000000, 0x80000, 0xfe5fa7b9 )
		ROM_LOAD16_WORD_SWAP( "mvca.04", 0x080000, 0x80000, 0x082b701c )
		ROM_LOAD16_WORD_SWAP( "mvc.05",  0x100000, 0x80000, 0x7db71ce9 )
		ROM_LOAD16_WORD_SWAP( "mvc.06",  0x180000, 0x80000, 0x4b0b6d3e )
		ROM_LOAD16_WORD_SWAP( "mvc.07",  0x200000, 0x80000, 0xc3baa32b )
		ROM_LOAD16_WORD_SWAP( "mvc.08",  0x280000, 0x80000, 0xbc002fcd )
		ROM_LOAD16_WORD_SWAP( "mvc.09",  0x300000, 0x80000, 0xc67b26df )
		ROM_LOAD16_WORD_SWAP( "mvc.10",  0x380000, 0x80000, 0x0fdd1e26 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "mvcax.03", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "mvcax.04", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "mvc.13",   0x0000000, 0x400000, 0xfa5f74bc, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.15",   0x0000002, 0x400000, 0x71938a8f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.17",   0x0000004, 0x400000, 0x92741d07, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.19",   0x0000006, 0x400000, 0xbcb72fc6, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.14",   0x1000000, 0x400000, 0x7f1df4e4, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.16",   0x1000002, 0x400000, 0x90bd3203, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.18",   0x1000004, 0x400000, 0x67aaf727, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.20",   0x1000006, 0x400000, 0x8b0bade8, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "mvc.01",   0x00000, 0x08000, 0x41629e95 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "mvc.02",   0x28000, 0x20000, 0x963abf6b );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "mvc.11",   0x000000, 0x400000, 0x850fe663 )
		ROM_LOAD16_WORD_SWAP( "mvc.12",   0x400000, 0x400000, 0x7ccb1896 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_mvsch = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "mvch.03", 0x000000, 0x80000, 0x6a0ec9f7 )
		ROM_LOAD16_WORD_SWAP( "mvch.04", 0x080000, 0x80000, 0x00f03fa4 )
		ROM_LOAD16_WORD_SWAP( "mvc.05a", 0x100000, 0x80000, 0x2d8c8e86 )
		ROM_LOAD16_WORD_SWAP( "mvc.06a", 0x180000, 0x80000, 0x8528e1f5 )
		ROM_LOAD16_WORD_SWAP( "mvc.07",  0x200000, 0x80000, 0xc3baa32b )
		ROM_LOAD16_WORD_SWAP( "mvc.08",  0x280000, 0x80000, 0xbc002fcd )
		ROM_LOAD16_WORD_SWAP( "mvc.09",  0x300000, 0x80000, 0xc67b26df )
		ROM_LOAD16_WORD_SWAP( "mvc.10",  0x380000, 0x80000, 0x0fdd1e26 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "mvchx.03", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "mvchx.04", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "mvc.13",   0x0000000, 0x400000, 0xfa5f74bc, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.15",   0x0000002, 0x400000, 0x71938a8f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.17",   0x0000004, 0x400000, 0x92741d07, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.19",   0x0000006, 0x400000, 0xbcb72fc6, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.14",   0x1000000, 0x400000, 0x7f1df4e4, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.16",   0x1000002, 0x400000, 0x90bd3203, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.18",   0x1000004, 0x400000, 0x67aaf727, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "mvc.20",   0x1000006, 0x400000, 0x8b0bade8, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "mvc.01",   0x00000, 0x08000, 0x41629e95 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "mvc.02",   0x28000, 0x20000, 0x963abf6b );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "mvc.11",   0x000000, 0x400000, 0x850fe663 )
		ROM_LOAD16_WORD_SWAP( "mvc.12",   0x400000, 0x400000, 0x7ccb1896 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_nwarr = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "vphu.03f", 0x000000, 0x80000, 0x85d6a359 )
		ROM_LOAD16_WORD_SWAP( "vphu.04c", 0x080000, 0x80000, 0xcb7fce77 )
		ROM_LOAD16_WORD_SWAP( "vphu.05e", 0x100000, 0x80000, 0xe08f2bba )
		ROM_LOAD16_WORD_SWAP( "vphu.06c", 0x180000, 0x80000, 0x08c04cdb )
		ROM_LOAD16_WORD_SWAP( "vphu.07b", 0x200000, 0x80000, 0xb5a5ab19 )
		ROM_LOAD16_WORD_SWAP( "vphu.08b", 0x280000, 0x80000, 0x51bb20fb )
		ROM_LOAD16_WORD_SWAP( "vphu.09b", 0x300000, 0x80000, 0x41a64205 )
		ROM_LOAD16_WORD_SWAP( "vphu.10b", 0x380000, 0x80000, 0x2b1d43ae )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "vphux.03f", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "vphux.04c", 0x080000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "vphux.05e", 0x100000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "vph.13",   0x0000000, 0x400000, 0xc51baf99, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.15",   0x0000002, 0x400000, 0x3ce83c77, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.17",   0x0000004, 0x400000, 0x4f2408e0, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.19",   0x0000006, 0x400000, 0x9ff60250, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.14",   0x1000000, 0x400000, 0x7a0e1add, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.16",   0x1000002, 0x400000, 0x2f41ca75, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.18",   0x1000004, 0x400000, 0x64498eed, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.20",   0x1000006, 0x400000, 0x17f2433f, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "vph.01",   0x00000, 0x08000, 0x5045dcac );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "vph.02",   0x28000, 0x20000, 0x86b60e59 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "vph.11",   0x000000, 0x200000, 0xe1837d33 )
		ROM_LOAD16_WORD_SWAP( "vph.12",   0x200000, 0x200000, 0xfbd3cd90 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_vhuntj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "vphj.03f", 0x000000, 0x80000, 0x3de2e333 )
		ROM_LOAD16_WORD_SWAP( "vphj.04c", 0x080000, 0x80000, 0xc95cf304 )
		ROM_LOAD16_WORD_SWAP( "vphj.05d", 0x100000, 0x80000, 0x50de5ddd )
		ROM_LOAD16_WORD_SWAP( "vphj.06c", 0x180000, 0x80000, 0xac3bd3d5 )
		ROM_LOAD16_WORD_SWAP( "vphj.07b", 0x200000, 0x80000, 0x0761309f )
		ROM_LOAD16_WORD_SWAP( "vphj.08b", 0x280000, 0x80000, 0x5a5c2bf5 )
		ROM_LOAD16_WORD_SWAP( "vphj.09b", 0x300000, 0x80000, 0x823d6d99 )
		ROM_LOAD16_WORD_SWAP( "vphj.10b", 0x380000, 0x80000, 0x32c7d8f0 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "vphjx.03f", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "vphjx.04c", 0x080000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "vphjx.05d", 0x100000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "vph.13",   0x0000000, 0x400000, 0xc51baf99, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.15",   0x0000002, 0x400000, 0x3ce83c77, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.17",   0x0000004, 0x400000, 0x4f2408e0, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.19",   0x0000006, 0x400000, 0x9ff60250, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.14",   0x1000000, 0x400000, 0x7a0e1add, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.16",   0x1000002, 0x400000, 0x2f41ca75, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.18",   0x1000004, 0x400000, 0x64498eed, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.20",   0x1000006, 0x400000, 0x17f2433f, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "vph.01",   0x00000, 0x08000, 0x5045dcac );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "vph.02",   0x28000, 0x20000, 0x86b60e59 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "vph.11",   0x000000, 0x200000, 0xe1837d33 )
		ROM_LOAD16_WORD_SWAP( "vph.12",   0x200000, 0x200000, 0xfbd3cd90 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_vhuntjr1 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "vphj.03b", 0x000000, 0x80000, 0x679c3fa9 )
		ROM_LOAD16_WORD_SWAP( "vphj.04a", 0x080000, 0x80000, 0xeb6e71e4 )
		ROM_LOAD16_WORD_SWAP( "vphj.05a", 0x100000, 0x80000, 0xeaf634ea )
		ROM_LOAD16_WORD_SWAP( "vphj.06a", 0x180000, 0x80000, 0xb70cc6be )
		ROM_LOAD16_WORD_SWAP( "vphj.07a", 0x200000, 0x80000, 0x46ab907d )
		ROM_LOAD16_WORD_SWAP( "vphj.08a", 0x280000, 0x80000, 0x1c00355e )
		ROM_LOAD16_WORD_SWAP( "vphj.09a", 0x300000, 0x80000, 0x026e6f82 )
		ROM_LOAD16_WORD_SWAP( "vphj.10a", 0x380000, 0x80000, 0xaadfb3ea )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "vphjx.03b", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "vphjx.04a", 0x080000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "vphjx.05a", 0x100000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "vph.13",   0x0000000, 0x400000, 0xc51baf99, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.15",   0x0000002, 0x400000, 0x3ce83c77, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.17",   0x0000004, 0x400000, 0x4f2408e0, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.19",   0x0000006, 0x400000, 0x9ff60250, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.14",   0x1000000, 0x400000, 0x7a0e1add, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.16",   0x1000002, 0x400000, 0x2f41ca75, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.18",   0x1000004, 0x400000, 0x64498eed, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.20",   0x1000006, 0x400000, 0x17f2433f, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "vph.01",   0x00000, 0x08000, 0x5045dcac );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "vph.02",   0x28000, 0x20000, 0x86b60e59 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "vph.11",   0x000000, 0x200000, 0xe1837d33 )
		ROM_LOAD16_WORD_SWAP( "vph.12",   0x200000, 0x200000, 0xfbd3cd90 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_nwarrh = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "vphh.03d", 0x000000, 0x80000, 0x6029c7be )
		ROM_LOAD16_WORD_SWAP( "vphh.04a", 0x080000, 0x80000, 0xd26625ee )
		ROM_LOAD16_WORD_SWAP( "vphh.05c", 0x100000, 0x80000, 0x73ee0b8a )
		ROM_LOAD16_WORD_SWAP( "vphh.06a", 0x180000, 0x80000, 0xa5b3a50a )
		ROM_LOAD16_WORD_SWAP( "vphh.07",  0x200000, 0x80000, 0x5fc2bdc1 )
		ROM_LOAD16_WORD_SWAP( "vphh.08",  0x280000, 0x80000, 0xe65588d9 )
		ROM_LOAD16_WORD_SWAP( "vphh.09",  0x300000, 0x80000, 0xa2ce6d63 )
		ROM_LOAD16_WORD_SWAP( "vphh.10",  0x380000, 0x80000, 0xe2f4f4b9 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "vphhx.03f", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "vphhx.04c", 0x080000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "vphhx.05e", 0x100000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "vph.13",   0x0000000, 0x400000, 0xc51baf99, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.15",   0x0000002, 0x400000, 0x3ce83c77, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.17",   0x0000004, 0x400000, 0x4f2408e0, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.19",   0x0000006, 0x400000, 0x9ff60250, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.14",   0x1000000, 0x400000, 0x7a0e1add, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.16",   0x1000002, 0x400000, 0x2f41ca75, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.18",   0x1000004, 0x400000, 0x64498eed, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vph.20",   0x1000006, 0x400000, 0x17f2433f, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "vph.01",   0x00000, 0x08000, 0x5045dcac );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "vph.02",   0x28000, 0x20000, 0x86b60e59 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "vph.11",   0x000000, 0x200000, 0xe1837d33 )
		ROM_LOAD16_WORD_SWAP( "vph.12",   0x200000, 0x200000, 0xfbd3cd90 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_puzloop2 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "pl2j.03a", 0x000000, 0x80000, 0x0a751bd0 )
		ROM_LOAD16_WORD_SWAP( "pl2j.04a", 0x080000, 0x80000, 0xc3f72afe )
		ROM_LOAD16_WORD_SWAP( "pl2j.05a", 0x100000, 0x80000, 0x6ea9dbfc )
		ROM_LOAD16_WORD_SWAP( "pl2j.06a", 0x180000, 0x80000, 0x0f14848d )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "pl2jx.03a", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "pl2jx.04a", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "pl2.13",   0x0000000, 0x400000, 0x00000000, ROM_GROUPWORD | ROM_SKIP(6) ) // Not dumped due to data being on a SIMM
		ROMX_LOAD( "pl2.15",   0x0000002, 0x400000, 0x00000000, ROM_GROUPWORD | ROM_SKIP(6) ) // Not dumped due to data being on a SIMM
		ROMX_LOAD( "pl2.17",   0x0000004, 0x400000, 0x00000000, ROM_GROUPWORD | ROM_SKIP(6) ) // Not dumped due to data being on a SIMM
		ROMX_LOAD( "pl2.19",   0x0000006, 0x400000, 0x00000000, ROM_GROUPWORD | ROM_SKIP(6) ) // Not dumped due to data being on a SIMM
		ROMX_LOAD( "pl2.14",   0x1000000, 0x400000, 0x00000000, ROM_GROUPWORD | ROM_SKIP(6) ) // Not dumped due to data being on a SIMM
		ROMX_LOAD( "pl2.16",   0x1000002, 0x400000, 0x00000000, ROM_GROUPWORD | ROM_SKIP(6) ) // Not dumped due to data being on a SIMM
		ROMX_LOAD( "pl2.18",   0x1000004, 0x400000, 0x00000000, ROM_GROUPWORD | ROM_SKIP(6) ) // Not dumped due to data being on a SIMM
		ROMX_LOAD( "pl2.20",   0x1000006, 0x400000, 0x00000000, ROM_GROUPWORD | ROM_SKIP(6) ) // Not dumped due to data being on a SIMM
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "pl2.01",   0x00000, 0x08000, 0x35697569 );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "pl2.11",   0x000000, 0x400000, 0x00000000 ) // Not dumped
		ROM_LOAD16_WORD_SWAP( "pl2.12",   0x400000, 0x400000, 0x00000000 ) // Not dumped
	ROM_END(); }}; 
	
	static RomLoadPtr rom_qndream = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION(CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "tqzj.03a", 0x000000, 0x80000, 0x7acf3e30 )
		ROM_LOAD16_WORD_SWAP( "tqzj.04",  0x080000, 0x80000, 0xf1044a87 )
		ROM_LOAD16_WORD_SWAP( "tqzj.05",  0x100000, 0x80000, 0x4105ba0e )
		ROM_LOAD16_WORD_SWAP( "tqzj.06",  0x180000, 0x80000, 0xc371e8a5 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "tqzjx.03a", 0x000000, 0x80000, 0x5804a8f8 )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROM_FILL(              0x000000, 0x800000, 0 );
		ROMX_LOAD( "tqz.14",   0x800000, 0x200000, 0x98af88a2, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "tqz.16",   0x800002, 0x200000, 0xdf82d491, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "tqz.18",   0x800004, 0x200000, 0x42f132ff, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "tqz.20",   0x800006, 0x200000, 0xb2e128a3, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION(QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "tqz.01",   0x00000, 0x08000, 0xe9ce9d0a );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "tqz.11",   0x000000, 0x200000, 0x78e7884f )
		ROM_LOAD16_WORD_SWAP( "tqz.12",   0x200000, 0x200000, 0x2e049b13 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_rckmanj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION(CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "rcmj.03a", 0x000000, 0x80000, 0x30559f60 )
		ROM_LOAD16_WORD_SWAP( "rcmj.04a", 0x080000, 0x80000, 0x5efc9366 )
		ROM_LOAD16_WORD_SWAP( "rcmj.05a", 0x100000, 0x80000, 0x517ccde2 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "rcmjx.03a", 0x000000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x0800000, REGION_GFX1, 0 );
		ROMX_LOAD( "rcm.64",   0x000000, 0x80000, 0x65c0464e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "rcm.74",   0x080000, 0x80000, 0x004ec725, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "rcm.84",   0x100000, 0x80000, 0xfb3097cc, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "rcm.94",   0x180000, 0x80000, 0x2e16557a, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "rcm.63",   0x000002, 0x80000, 0xacad7c62, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "rcm.73",   0x080002, 0x80000, 0x774c6e04, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "rcm.83",   0x100002, 0x80000, 0x6af30499, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "rcm.93",   0x180002, 0x80000, 0x7a5a5166, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "rcm.65",   0x000004, 0x80000, 0xecedad3d, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "rcm.75",   0x080004, 0x80000, 0x70a73f99, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "rcm.85",   0x100004, 0x80000, 0x3d6186d8, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "rcm.95",   0x180004, 0x80000, 0x8c7700f1, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "rcm.66",   0x000006, 0x80000, 0x1300eb7b, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "rcm.76",   0x080006, 0x80000, 0x89a889ad, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "rcm.86",   0x100006, 0x80000, 0x6d974ebd, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "rcm.96",   0x180006, 0x80000, 0x7da4cd24, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION(QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "rcm.01",   0x00000, 0x08000, 0xd60cf8a3 );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "rcm.51",   0x000000, 0x80000, 0xb6d07080 )
		ROM_LOAD16_WORD_SWAP( "rcm.52",   0x080000, 0x80000, 0xdfddc493 )
		ROM_LOAD16_WORD_SWAP( "rcm.53",   0x100000, 0x80000, 0x6062ae3a )
		ROM_LOAD16_WORD_SWAP( "rcm.54",   0x180000, 0x80000, 0x08c6f3bf )
		ROM_LOAD16_WORD_SWAP( "rcm.55",   0x200000, 0x80000, 0xf97dfccc )
		ROM_LOAD16_WORD_SWAP( "rcm.56",   0x280000, 0x80000, 0xade475bc )
		ROM_LOAD16_WORD_SWAP( "rcm.57",   0x300000, 0x80000, 0x075effb3 )
		ROM_LOAD16_WORD_SWAP( "rcm.58",   0x380000, 0x80000, 0x5b39ea33 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_rckman2j = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION(CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "rm2j.03", 0x000000, 0x80000, 0xdbaa1437 )
		ROM_LOAD16_WORD_SWAP( "rm2j.04", 0x080000, 0x80000, 0xcf5ba612 )
		ROM_LOAD16_WORD_SWAP( "rm2j.05", 0x100000, 0x80000, 0x02ee9efc )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "rm2jx.03", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "rm2jx.04", 0x080000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "rm2jx.05", 0x100000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x0800000, REGION_GFX1, 0 );
		ROMX_LOAD( "rm2.14",   0x000000, 0x200000, 0x9b1f00b4, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "rm2.16",   0x000002, 0x200000, 0xc2bb0c24, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "rm2.18",   0x000004, 0x200000, 0x12257251, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "rm2.20",   0x000006, 0x200000, 0xf9b6e786, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION(QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "rm2.01",   0x00000, 0x08000, 0xd18e7859 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "rm2.02",   0x28000, 0x20000, 0xc463ece0 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "rm2.11",   0x000000, 0x200000, 0x2106174d )
		ROM_LOAD16_WORD_SWAP( "rm2.12",   0x200000, 0x200000, 0x546c1636 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_sfa = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "sfze.03b", 0x000000, 0x80000, 0xebf2054d )
		ROM_LOAD16_WORD_SWAP( "sfz.04b",  0x080000, 0x80000, 0x8b73b0e5 )
		ROM_LOAD16_WORD_SWAP( "sfz.05a",  0x100000, 0x80000, 0x0810544d )
		ROM_LOAD16_WORD_SWAP( "sfz.06",   0x180000, 0x80000, 0x806e8f38 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "sfzex.03b", 0x000000, 0x80000, 0x505a1b4a )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROM_FILL(              0x000000, 0x800000, 0 );
		ROMX_LOAD( "sfz.14",   0x800000, 0x200000, 0x90fefdb3, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfz.16",   0x800002, 0x200000, 0x5354c948, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfz.18",   0x800004, 0x200000, 0x41a1e790, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfz.20",   0x800006, 0x200000, 0xa549df98, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "sfz.01",   0x00000, 0x08000, 0xffffec7d );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "sfz.02",   0x28000, 0x20000, 0x45f46a08 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "sfz.11",   0x000000, 0x200000, 0xc4b093cd )
		ROM_LOAD16_WORD_SWAP( "sfz.12",   0x200000, 0x200000, 0x8bdbc4b4 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_sfar1 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "sfze.03a", 0x000000, 0x80000, 0xfdbcd434 )
		ROM_LOAD16_WORD_SWAP( "sfz.04",   0x080000, 0x80000, 0x0c436d30 )
		ROM_LOAD16_WORD_SWAP( "sfz.05",   0x100000, 0x80000, 0x1f363612 )
		ROM_LOAD16_WORD_SWAP( "sfz.06",   0x180000, 0x80000, 0x806e8f38 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "sfzex.03a", 0x000000, 0x80000, 0xb50d87c7 )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROM_FILL(              0x000000, 0x800000, 0 );
		ROMX_LOAD( "sfz.14",   0x800000, 0x200000, 0x90fefdb3, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfz.16",   0x800002, 0x200000, 0x5354c948, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfz.18",   0x800004, 0x200000, 0x41a1e790, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfz.20",   0x800006, 0x200000, 0xa549df98, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "sfz.01",   0x00000, 0x08000, 0xffffec7d );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "sfz.02",   0x28000, 0x20000, 0x45f46a08 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "sfz.11",   0x000000, 0x200000, 0xc4b093cd )
		ROM_LOAD16_WORD_SWAP( "sfz.12",   0x200000, 0x200000, 0x8bdbc4b4 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_sfau = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "sfzu.03a", 0x000000, 0x80000, 0x49fc7db9 )
		ROM_LOAD16_WORD_SWAP( "sfz.04a",  0x080000, 0x80000, 0x5f99e9a5 )
		ROM_LOAD16_WORD_SWAP( "sfz.05a",  0x100000, 0x80000, 0x0810544d )
		ROM_LOAD16_WORD_SWAP( "sfz.06",   0x180000, 0x80000, 0x806e8f38 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "sfzux.03a", 0x000000, 0x80000, 0x1a3160ed )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROM_FILL(              0x000000, 0x800000, 0 );
		ROMX_LOAD( "sfz.14",   0x800000, 0x200000, 0x90fefdb3, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfz.16",   0x800002, 0x200000, 0x5354c948, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfz.18",   0x800004, 0x200000, 0x41a1e790, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfz.20",   0x800006, 0x200000, 0xa549df98, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "sfz.01",   0x00000, 0x08000, 0xffffec7d );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "sfz.02",   0x28000, 0x20000, 0x45f46a08 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "sfz.11",   0x000000, 0x200000, 0xc4b093cd )
		ROM_LOAD16_WORD_SWAP( "sfz.12",   0x200000, 0x200000, 0x8bdbc4b4 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_sfzj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "sfzj.03c", 0x000000, 0x80000, 0xf5444120 )
		ROM_LOAD16_WORD_SWAP( "sfz.04b",  0x080000, 0x80000, 0x8b73b0e5 )
		ROM_LOAD16_WORD_SWAP( "sfz.05a",  0x100000, 0x80000, 0x0810544d )
		ROM_LOAD16_WORD_SWAP( "sfz.06",   0x180000, 0x80000, 0x806e8f38 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "sfzjx.03c", 0x000000, 0x80000, 0xd6b17a9b )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROM_FILL(              0x000000, 0x800000, 0 );
		ROMX_LOAD( "sfz.14",   0x800000, 0x200000, 0x90fefdb3, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfz.16",   0x800002, 0x200000, 0x5354c948, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfz.18",   0x800004, 0x200000, 0x41a1e790, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfz.20",   0x800006, 0x200000, 0xa549df98, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "sfz.01",   0x00000, 0x08000, 0xffffec7d );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "sfz.02",   0x28000, 0x20000, 0x45f46a08 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "sfz.11",   0x000000, 0x200000, 0xc4b093cd )
		ROM_LOAD16_WORD_SWAP( "sfz.12",   0x200000, 0x200000, 0x8bdbc4b4 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_sfzjr1 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "sfzj.03b", 0x000000, 0x80000, 0x844220c2 )
		ROM_LOAD16_WORD_SWAP( "sfz.04a",  0x080000, 0x80000, 0x5f99e9a5 )
		ROM_LOAD16_WORD_SWAP( "sfz.05a",  0x100000, 0x80000, 0x0810544d )
		ROM_LOAD16_WORD_SWAP( "sfz.06",   0x180000, 0x80000, 0x806e8f38 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "sfzjx.03b", 0x000000, 0x80000, 0xb501f03c )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROM_FILL(              0x000000, 0x800000, 0 );
		ROMX_LOAD( "sfz.14",   0x800000, 0x200000, 0x90fefdb3, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfz.16",   0x800002, 0x200000, 0x5354c948, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfz.18",   0x800004, 0x200000, 0x41a1e790, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfz.20",   0x800006, 0x200000, 0xa549df98, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "sfz.01",   0x00000, 0x08000, 0xffffec7d );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "sfz.02",   0x28000, 0x20000, 0x45f46a08 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "sfz.11",   0x000000, 0x200000, 0xc4b093cd )
		ROM_LOAD16_WORD_SWAP( "sfz.12",   0x200000, 0x200000, 0x8bdbc4b4 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_sfzjr2 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "sfzj.03a", 0x000000, 0x80000, 0x3cfce93c )
		ROM_LOAD16_WORD_SWAP( "sfz.04",   0x080000, 0x80000, 0x0c436d30 )
		ROM_LOAD16_WORD_SWAP( "sfz.05",   0x100000, 0x80000, 0x1f363612 )
		ROM_LOAD16_WORD_SWAP( "sfz.06",   0x180000, 0x80000, 0x806e8f38 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "sfzjx.03a", 0x000000, 0x80000, 0x3cc138b5 )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROM_FILL(              0x000000, 0x800000, 0 );
		ROMX_LOAD( "sfz.14",   0x800000, 0x200000, 0x90fefdb3, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfz.16",   0x800002, 0x200000, 0x5354c948, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfz.18",   0x800004, 0x200000, 0x41a1e790, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfz.20",   0x800006, 0x200000, 0xa549df98, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "sfz.01",   0x00000, 0x08000, 0xffffec7d );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "sfz.02",   0x28000, 0x20000, 0x45f46a08 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "sfz.11",   0x000000, 0x200000, 0xc4b093cd )
		ROM_LOAD16_WORD_SWAP( "sfz.12",   0x200000, 0x200000, 0x8bdbc4b4 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_sfzh = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "sfzh.03c", 0x000000, 0x80000, 0xbce635aa )
		ROM_LOAD16_WORD_SWAP( "sfz.04a",  0x080000, 0x80000, 0x5f99e9a5 )
		ROM_LOAD16_WORD_SWAP( "sfz.05a",  0x100000, 0x80000, 0x0810544d )
		ROM_LOAD16_WORD_SWAP( "sfz.06",   0x180000, 0x80000, 0x806e8f38 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "sfzhx.03c", 0x000000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROM_FILL(              0x000000, 0x800000, 0 );
		ROMX_LOAD( "sfz.14",   0x800000, 0x200000, 0x90fefdb3, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfz.16",   0x800002, 0x200000, 0x5354c948, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfz.18",   0x800004, 0x200000, 0x41a1e790, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfz.20",   0x800006, 0x200000, 0xa549df98, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "sfz.01",   0x00000, 0x08000, 0xffffec7d );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "sfz.02",   0x28000, 0x20000, 0x45f46a08 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "sfz.11",   0x000000, 0x200000, 0xc4b093cd )
		ROM_LOAD16_WORD_SWAP( "sfz.12",   0x200000, 0x200000, 0x8bdbc4b4 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_sfa2 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "sz2u.03", 0x000000, 0x80000, 0x84a09006 )
		ROM_LOAD16_WORD_SWAP( "sz2u.04", 0x080000, 0x80000, 0xac46e5ed )
		ROM_LOAD16_WORD_SWAP( "sz2u.05", 0x100000, 0x80000, 0x6c0c79d3 )
		ROM_LOAD16_WORD_SWAP( "sz2u.06", 0x180000, 0x80000, 0xc5c8eb63 )
		ROM_LOAD16_WORD_SWAP( "sz2u.07", 0x200000, 0x80000, 0x5de01cc5 )
		ROM_LOAD16_WORD_SWAP( "sz2u.08", 0x280000, 0x80000, 0xbea11d56 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "sz2ux.03", 0x000000, 0x80000, 0x6bb6005f )
		ROM_LOAD16_WORD_SWAP( "sz2ux.04", 0x080000, 0x80000, 0x74308a4b )
	
		ROM_REGION( 0x1400000, REGION_GFX1, 0 );
		ROMX_LOAD( "sz2.13",   0x0000000, 0x400000, 0x4d1f1f22, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.15",   0x0000002, 0x400000, 0x19cea680, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.17",   0x0000004, 0x400000, 0xe01b4588, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.19",   0x0000006, 0x400000, 0x0feeda64, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.14",   0x1000000, 0x100000, 0x0560c6aa, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.16",   0x1000002, 0x100000, 0xae940f87, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.18",   0x1000004, 0x100000, 0x4bc3c8bc, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.20",   0x1000006, 0x100000, 0x39e674c0, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "sz2.01",   0x00000, 0x08000, 0x1bc323cf );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "sz2.02",   0x28000, 0x20000, 0xba6a5013 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "sz2.11",   0x000000, 0x200000, 0xaa47a601 )
		ROM_LOAD16_WORD_SWAP( "sz2.12",   0x200000, 0x200000, 0x2237bc53 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_sfz2j = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "sz2j.03a", 0x000000, 0x80000, 0x97461e28 )
		ROM_LOAD16_WORD_SWAP( "sz2j.04a", 0x080000, 0x80000, 0xae4851a9 )
		ROM_LOAD16_WORD_SWAP( "sz2j.05",  0x100000, 0x80000, 0x98e8e992 )
		ROM_LOAD16_WORD_SWAP( "sz2j.06",  0x180000, 0x80000, 0x5b1d49c0 )
		ROM_LOAD16_WORD_SWAP( "sz2j.07a", 0x200000, 0x80000, 0xd910b2a2 )
		ROM_LOAD16_WORD_SWAP( "sz2j.08",  0x280000, 0x80000, 0x0fe8585d )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "sz2jx.03a", 0x000000, 0x80000, 0x6a765c08 )
		ROM_LOAD16_WORD_SWAP( "sz2jx.04a", 0x080000, 0x80000, 0x66139273 )
	
		ROM_REGION( 0x1400000, REGION_GFX1, 0 );
		ROMX_LOAD( "sz2.13",   0x0000000, 0x400000, 0x4d1f1f22, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.15",   0x0000002, 0x400000, 0x19cea680, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.17",   0x0000004, 0x400000, 0xe01b4588, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.19",   0x0000006, 0x400000, 0x0feeda64, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.14",   0x1000000, 0x100000, 0x0560c6aa, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.16",   0x1000002, 0x100000, 0xae940f87, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.18",   0x1000004, 0x100000, 0x4bc3c8bc, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.20",   0x1000006, 0x100000, 0x39e674c0, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "sz2.01",   0x00000, 0x08000, 0x1bc323cf );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "sz2.02",   0x28000, 0x20000, 0xba6a5013 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "sz2.11",   0x000000, 0x200000, 0xaa47a601 )
		ROM_LOAD16_WORD_SWAP( "sz2.12",   0x200000, 0x200000, 0x2237bc53 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_sfz2aj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "szaj.03a", 0x000000, 0x80000, 0xa3802fe3 )
		ROM_LOAD16_WORD_SWAP( "szaj.04a", 0x080000, 0x80000, 0xe7ca87c7 )
		ROM_LOAD16_WORD_SWAP( "szaj.05a", 0x100000, 0x80000, 0xc88ebf88 )
		ROM_LOAD16_WORD_SWAP( "szaj.06a", 0x180000, 0x80000, 0x35ed5b7a )
		ROM_LOAD16_WORD_SWAP( "szaj.07a", 0x200000, 0x80000, 0x975dcb3e )
		ROM_LOAD16_WORD_SWAP( "szaj.08a", 0x280000, 0x80000, 0xdc73f2d7 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "szajx.03a", 0x000000, 0x80000, 0x6d3aa71e )
		ROM_LOAD16_WORD_SWAP( "szajx.04a", 0x080000, 0x80000, 0x006d5cb8 )
	
		ROM_REGION( 0x1400000, REGION_GFX1, 0 );
		ROMX_LOAD( "sz2.13",   0x0000000, 0x400000, 0x4d1f1f22, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.15",   0x0000002, 0x400000, 0x19cea680, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.17",   0x0000004, 0x400000, 0xe01b4588, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.19",   0x0000006, 0x400000, 0x0feeda64, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.14",   0x1000000, 0x100000, 0x0560c6aa, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.16",   0x1000002, 0x100000, 0xae940f87, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.18",   0x1000004, 0x100000, 0x4bc3c8bc, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.20",   0x1000006, 0x100000, 0x39e674c0, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "sz2.01",   0x00000, 0x08000, 0x1bc323cf );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "sz2.02",   0x28000, 0x20000, 0xba6a5013 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "sz2.11",   0x000000, 0x200000, 0xaa47a601 )
		ROM_LOAD16_WORD_SWAP( "sz2.12",   0x200000, 0x200000, 0x2237bc53 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_sfz2ah = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "szah.03", 0x000000, 0x80000, 0x06f93d1d )
		ROM_LOAD16_WORD_SWAP( "szah.04", 0x080000, 0x80000, 0xe62ee914 )
		ROM_LOAD16_WORD_SWAP( "szah.05", 0x100000, 0x80000, 0x2b7f4b20 )
		ROM_LOAD16_WORD_SWAP( "szah.06", 0x180000, 0x80000, 0x0abda2fc )
		ROM_LOAD16_WORD_SWAP( "szah.07", 0x200000, 0x80000, 0xe9430762 )
		ROM_LOAD16_WORD_SWAP( "szah.08", 0x280000, 0x80000, 0xb65711a9 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "szahx.03", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "szahx.04", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x1400000, REGION_GFX1, 0 );
		ROMX_LOAD( "sz2.13",   0x0000000, 0x400000, 0x4d1f1f22, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.15",   0x0000002, 0x400000, 0x19cea680, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.17",   0x0000004, 0x400000, 0xe01b4588, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.19",   0x0000006, 0x400000, 0x0feeda64, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.14",   0x1000000, 0x100000, 0x0560c6aa, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.16",   0x1000002, 0x100000, 0xae940f87, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.18",   0x1000004, 0x100000, 0x4bc3c8bc, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz2.20",   0x1000006, 0x100000, 0x39e674c0, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "sz2.01",   0x00000, 0x08000, 0x1bc323cf );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "sz2.02",   0x28000, 0x20000, 0xba6a5013 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "sz2.11",   0x000000, 0x200000, 0xaa47a601 )
		ROM_LOAD16_WORD_SWAP( "sz2.12",   0x200000, 0x200000, 0x2237bc53 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_sfa3 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "sz3u.03c", 0x000000, 0x80000, 0xe007da2e )
		ROM_LOAD16_WORD_SWAP( "sz3u.04c", 0x080000, 0x80000, 0x5f78f0e7 )
		ROM_LOAD16_WORD_SWAP( "sz3.05c",  0x100000, 0x80000, 0x57fd0a40 )
		ROM_LOAD16_WORD_SWAP( "sz3.06c",  0x180000, 0x80000, 0xf6305f8b )
		ROM_LOAD16_WORD_SWAP( "sz3.07c",  0x200000, 0x80000, 0x6eab0f6f )
		ROM_LOAD16_WORD_SWAP( "sz3.08c",  0x280000, 0x80000, 0x910c4a3b )
		ROM_LOAD16_WORD_SWAP( "sz3.09c",  0x300000, 0x80000, 0xb29e5199 )
		ROM_LOAD16_WORD_SWAP( "sz3.10b",  0x380000, 0x80000, 0xdeb2ff52 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "sz3ux.03c", 0x000000, 0x80000, 0x7091276b )
		ROM_LOAD16_WORD_SWAP( "sz3ux.04c", 0x080000, 0x80000, 0x83b213b1 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "sz3.13",   0x0000000, 0x400000, 0x0f7a60d9, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.15",   0x0000002, 0x400000, 0x8e933741, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.17",   0x0000004, 0x400000, 0xd6e98147, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.19",   0x0000006, 0x400000, 0xf31a728a, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.14",   0x1000000, 0x400000, 0x5ff98297, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.16",   0x1000002, 0x400000, 0x52b5bdee, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.18",   0x1000004, 0x400000, 0x40631ed5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.20",   0x1000006, 0x400000, 0x763409b4, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "sz3.01",   0x00000, 0x08000, 0xde810084 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "sz3.02",   0x28000, 0x20000, 0x72445dc4 );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "sz3.11",   0x000000, 0x400000, 0x1c89eed1 )
		ROM_LOAD16_WORD_SWAP( "sz3.12",   0x400000, 0x400000, 0xf392b13a )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_sfa3r1 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "sz3u.03", 0x000000, 0x80000, 0xb5984a19 )
		ROM_LOAD16_WORD_SWAP( "sz3u.04", 0x080000, 0x80000, 0x7e8158ba )
		ROM_LOAD16_WORD_SWAP( "sz3.05",  0x100000, 0x80000, 0x9b21518a )
		ROM_LOAD16_WORD_SWAP( "sz3.06",  0x180000, 0x80000, 0xe7a6c3a7 )
		ROM_LOAD16_WORD_SWAP( "sz3.07",  0x200000, 0x80000, 0xec4c0cfd )
		ROM_LOAD16_WORD_SWAP( "sz3.08",  0x280000, 0x80000, 0x5c7e7240 )
		ROM_LOAD16_WORD_SWAP( "sz3.09",  0x300000, 0x80000, 0xc5589553 )
		ROM_LOAD16_WORD_SWAP( "sz3.10",  0x380000, 0x80000, 0xa9717252 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "sz3ux.03", 0x000000, 0x80000, 0x42994875 )
		ROM_LOAD16_WORD_SWAP( "sz3ux.04", 0x080000, 0x80000, 0x86ee79e3 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "sz3.13",   0x0000000, 0x400000, 0x0f7a60d9, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.15",   0x0000002, 0x400000, 0x8e933741, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.17",   0x0000004, 0x400000, 0xd6e98147, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.19",   0x0000006, 0x400000, 0xf31a728a, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.14",   0x1000000, 0x400000, 0x5ff98297, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.16",   0x1000002, 0x400000, 0x52b5bdee, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.18",   0x1000004, 0x400000, 0x40631ed5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.20",   0x1000006, 0x400000, 0x763409b4, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "sz3.01",   0x00000, 0x08000, 0xde810084 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "sz3.02",   0x28000, 0x20000, 0x72445dc4 );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "sz3.11",   0x000000, 0x400000, 0x1c89eed1 )
		ROM_LOAD16_WORD_SWAP( "sz3.12",   0x400000, 0x400000, 0xf392b13a )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_sfz3j = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "sz3j.03a", 0x000000, 0x80000, 0x6ee0beae )
		ROM_LOAD16_WORD_SWAP( "sz3j.04a", 0x080000, 0x80000, 0xa6e2978d )
		ROM_LOAD16_WORD_SWAP( "sz3.05a",  0x100000, 0x80000, 0x05964b7d )
		ROM_LOAD16_WORD_SWAP( "sz3.06a",  0x180000, 0x80000, 0x78ce2179 )
		ROM_LOAD16_WORD_SWAP( "sz3.07a",  0x200000, 0x80000, 0x398bf52f )
		ROM_LOAD16_WORD_SWAP( "sz3.08a",  0x280000, 0x80000, 0x866d0588 )
		ROM_LOAD16_WORD_SWAP( "sz3.09a",  0x300000, 0x80000, 0x2180892c )
		ROM_LOAD16_WORD_SWAP( "sz3.10",   0x380000, 0x80000, 0xa9717252 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "sz3jx.03a", 0x000000, 0x80000, 0xb2f4046d )
		ROM_LOAD16_WORD_SWAP( "sz3jx.04a", 0x080000, 0x80000, 0x85c38642 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "sz3.13",   0x0000000, 0x400000, 0x0f7a60d9, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.15",   0x0000002, 0x400000, 0x8e933741, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.17",   0x0000004, 0x400000, 0xd6e98147, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.19",   0x0000006, 0x400000, 0xf31a728a, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.14",   0x1000000, 0x400000, 0x5ff98297, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.16",   0x1000002, 0x400000, 0x52b5bdee, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.18",   0x1000004, 0x400000, 0x40631ed5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.20",   0x1000006, 0x400000, 0x763409b4, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "sz3.01",   0x00000, 0x08000, 0xde810084 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "sz3.02",   0x28000, 0x20000, 0x72445dc4 );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "sz3.11",   0x000000, 0x400000, 0x1c89eed1 )
		ROM_LOAD16_WORD_SWAP( "sz3.12",   0x400000, 0x400000, 0xf392b13a )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_sfz3jr1 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "sz3j.03", 0x000000, 0x80000, 0xf7cb4b13 )
		ROM_LOAD16_WORD_SWAP( "sz3j.04", 0x080000, 0x80000, 0x0846c29d )
		ROM_LOAD16_WORD_SWAP( "sz3.05",  0x100000, 0x80000, 0x9b21518a )
		ROM_LOAD16_WORD_SWAP( "sz3.06",  0x180000, 0x80000, 0xe7a6c3a7 )
		ROM_LOAD16_WORD_SWAP( "sz3.07",  0x200000, 0x80000, 0xec4c0cfd )
		ROM_LOAD16_WORD_SWAP( "sz3.08",  0x280000, 0x80000, 0x5c7e7240 )
		ROM_LOAD16_WORD_SWAP( "sz3.09",  0x300000, 0x80000, 0xc5589553 )
		ROM_LOAD16_WORD_SWAP( "sz3.10",  0x380000, 0x80000, 0xa9717252 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "sz3jx.03", 0x000000, 0x80000, 0xacd88307 )
		ROM_LOAD16_WORD_SWAP( "sz3jx.04", 0x080000, 0x80000, 0x2c15655b )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "sz3.13",   0x0000000, 0x400000, 0x0f7a60d9, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.15",   0x0000002, 0x400000, 0x8e933741, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.17",   0x0000004, 0x400000, 0xd6e98147, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.19",   0x0000006, 0x400000, 0xf31a728a, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.14",   0x1000000, 0x400000, 0x5ff98297, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.16",   0x1000002, 0x400000, 0x52b5bdee, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.18",   0x1000004, 0x400000, 0x40631ed5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.20",   0x1000006, 0x400000, 0x763409b4, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "sz3.01",   0x00000, 0x08000, 0xde810084 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "sz3.02",   0x28000, 0x20000, 0x72445dc4 );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "sz3.11",   0x000000, 0x400000, 0x1c89eed1 )
		ROM_LOAD16_WORD_SWAP( "sz3.12",   0x400000, 0x400000, 0xf392b13a )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_sfz3a = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "sz3a.03a", 0x000000, 0x80000, 0x29c681fd )
		ROM_LOAD16_WORD_SWAP( "sz3a.04",  0x080000, 0x80000, 0x9ddd1484 )
		ROM_LOAD16_WORD_SWAP( "sz3.05",   0x100000, 0x80000, 0x9b21518a )
		ROM_LOAD16_WORD_SWAP( "sz3.06",   0x180000, 0x80000, 0xe7a6c3a7 )
		ROM_LOAD16_WORD_SWAP( "sz3.07",   0x200000, 0x80000, 0xec4c0cfd )
		ROM_LOAD16_WORD_SWAP( "sz3.08",   0x280000, 0x80000, 0x5c7e7240 )
		ROM_LOAD16_WORD_SWAP( "sz3.09",   0x300000, 0x80000, 0xc5589553 )
		ROM_LOAD16_WORD_SWAP( "sz3.10",   0x380000, 0x80000, 0xa9717252 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "sz3ax.03a", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "sz3ax.04", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "sz3.13",   0x0000000, 0x400000, 0x0f7a60d9, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.15",   0x0000002, 0x400000, 0x8e933741, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.17",   0x0000004, 0x400000, 0xd6e98147, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.19",   0x0000006, 0x400000, 0xf31a728a, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.14",   0x1000000, 0x400000, 0x5ff98297, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.16",   0x1000002, 0x400000, 0x52b5bdee, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.18",   0x1000004, 0x400000, 0x40631ed5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sz3.20",   0x1000006, 0x400000, 0x763409b4, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "sz3.01",   0x00000, 0x08000, 0xde810084 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "sz3.02",   0x28000, 0x20000, 0x72445dc4 );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "sz3.11",   0x000000, 0x400000, 0x1c89eed1 )
		ROM_LOAD16_WORD_SWAP( "sz3.12",   0x400000, 0x400000, 0xf392b13a )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_sgemf = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "pcfu.03", 0x000000, 0x80000, 0xac2e8566 )
		ROM_LOAD16_WORD_SWAP( "pcf.04",  0x080000, 0x80000, 0xf4314c96 )
		ROM_LOAD16_WORD_SWAP( "pcf.05",  0x100000, 0x80000, 0x215655f6 )
		ROM_LOAD16_WORD_SWAP( "pcf.06",  0x180000, 0x80000, 0xea6f13ea )
		ROM_LOAD16_WORD_SWAP( "pcf.07",  0x200000, 0x80000, 0x5ac6d5ea )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "pcfux.03", 0x000000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x1400000, REGION_GFX1, 0 );
		ROMX_LOAD( "pcf.13",   0x0000000, 0x400000, 0x22d72ab9, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pcf.15",   0x0000002, 0x400000, 0x16a4813c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pcf.17",   0x0000004, 0x400000, 0x1097e035, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pcf.19",   0x0000006, 0x400000, 0xd362d874, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pcf.14",   0x1000000, 0x100000, 0x0383897c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pcf.16",   0x1000002, 0x100000, 0x76f91084, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pcf.18",   0x1000004, 0x100000, 0x756c3754, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pcf.20",   0x1000006, 0x100000, 0x9ec9277d, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "pcf.01",   0x00000, 0x08000, 0x254e5f33 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "pcf.02",   0x28000, 0x20000, 0x6902f4f9 );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "pcf.11",   0x000000, 0x400000, 0xa5dea005 )
		ROM_LOAD16_WORD_SWAP( "pcf.12",   0x400000, 0x400000, 0x4ce235fe )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_pfghtj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "pcfj.03", 0x000000, 0x80000, 0x681da43e )
		ROM_LOAD16_WORD_SWAP( "pcf.04",  0x080000, 0x80000, 0xf4314c96 )
		ROM_LOAD16_WORD_SWAP( "pcf.05",  0x100000, 0x80000, 0x215655f6 )
		ROM_LOAD16_WORD_SWAP( "pcf.06",  0x180000, 0x80000, 0xea6f13ea )
		ROM_LOAD16_WORD_SWAP( "pcf.07",  0x200000, 0x80000, 0x5ac6d5ea )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "pcfjx.03", 0x000000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x1400000, REGION_GFX1, 0 );
		ROMX_LOAD( "pcf.13",   0x0000000, 0x400000, 0x22d72ab9, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pcf.15",   0x0000002, 0x400000, 0x16a4813c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pcf.17",   0x0000004, 0x400000, 0x1097e035, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pcf.19",   0x0000006, 0x400000, 0xd362d874, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pcf.14",   0x1000000, 0x100000, 0x0383897c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pcf.16",   0x1000002, 0x100000, 0x76f91084, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pcf.18",   0x1000004, 0x100000, 0x756c3754, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pcf.20",   0x1000006, 0x100000, 0x9ec9277d, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "pcf.01",   0x00000, 0x08000, 0x254e5f33 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "pcf.02",   0x28000, 0x20000, 0x6902f4f9 );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "pcf.11",   0x000000, 0x400000, 0xa5dea005 )
		ROM_LOAD16_WORD_SWAP( "pcf.12",   0x400000, 0x400000, 0x4ce235fe )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_sgemfh = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "pcfh.03", 0x000000, 0x80000, 0xe9103347 )
		ROM_LOAD16_WORD_SWAP( "pcf.04",  0x080000, 0x80000, 0xf4314c96 )
		ROM_LOAD16_WORD_SWAP( "pcf.05",  0x100000, 0x80000, 0x215655f6 )
		ROM_LOAD16_WORD_SWAP( "pcf.06",  0x180000, 0x80000, 0xea6f13ea )
		ROM_LOAD16_WORD_SWAP( "pcf.07",  0x200000, 0x80000, 0x5ac6d5ea )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "pcfhx.03", 0x000000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x1400000, REGION_GFX1, 0 );
		ROMX_LOAD( "pcf.13",   0x0000000, 0x400000, 0x22d72ab9, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pcf.15",   0x0000002, 0x400000, 0x16a4813c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pcf.17",   0x0000004, 0x400000, 0x1097e035, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pcf.19",   0x0000006, 0x400000, 0xd362d874, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pcf.14",   0x1000000, 0x100000, 0x0383897c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pcf.16",   0x1000002, 0x100000, 0x76f91084, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pcf.18",   0x1000004, 0x100000, 0x756c3754, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pcf.20",   0x1000006, 0x100000, 0x9ec9277d, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "pcf.01",   0x00000, 0x08000, 0x254e5f33 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "pcf.02",   0x28000, 0x20000, 0x6902f4f9 );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "pcf.11",   0x000000, 0x400000, 0xa5dea005 )
		ROM_LOAD16_WORD_SWAP( "pcf.12",   0x400000, 0x400000, 0x4ce235fe )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ringdest = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "smbe.03b", 0x000000, 0x80000, 0xb8016278 )
		ROM_LOAD16_WORD_SWAP( "smbe.04b", 0x080000, 0x80000, 0x18c4c447 )
		ROM_LOAD16_WORD_SWAP( "smbe.05b", 0x100000, 0x80000, 0x18ebda7f )
		ROM_LOAD16_WORD_SWAP( "smbe.06b", 0x180000, 0x80000, 0x89c80007 )
		ROM_LOAD16_WORD_SWAP( "smb.07",   0x200000, 0x80000, 0xb9a11577 )
		ROM_LOAD16_WORD_SWAP( "smb.08",   0x280000, 0x80000, 0xf931b76b )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "smbex.03b", 0x000000, 0x80000, 0x3b1457bd )
		ROM_LOAD16_WORD_SWAP( "smbex.04b", 0x080000, 0x80000, 0x6299eb4e )
		ROM_LOAD16_WORD_SWAP( "smbex.05b", 0x100000, 0x80000, 0xbe4a84d1 )
	
		ROM_REGION( 0x1200000, REGION_GFX1, 0 );
		ROMX_LOAD( "smb.13",   0x0000000, 0x200000, 0xd9b2d1de, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.15",   0x0000002, 0x200000, 0x9a766d92, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.17",   0x0000004, 0x200000, 0x51800f0f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.19",   0x0000006, 0x200000, 0x35757e96, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.14",   0x0800000, 0x200000, 0xe5bfd0e7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.16",   0x0800002, 0x200000, 0xc56c0866, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.18",   0x0800004, 0x200000, 0x4ded3910, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.20",   0x0800006, 0x200000, 0x26ea1ec5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.21",   0x1000000, 0x080000, 0x0a08c5fc, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.23",   0x1000002, 0x080000, 0x0911b6c4, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.25",   0x1000004, 0x080000, 0x82d6c4ec, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.27",   0x1000006, 0x080000, 0x9b48678b, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "smb.01",   0x00000, 0x08000, 0x0abc229a );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "smb.02",   0x28000, 0x20000, 0xd051679a );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "smb.11",   0x000000, 0x200000, 0xC56935f9 )
		ROM_LOAD16_WORD_SWAP( "smb.12",   0x200000, 0x200000, 0x955b0782 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_smbomb = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "smbj.03a", 0x000000, 0x80000, 0x1c5613de )
		ROM_LOAD16_WORD_SWAP( "smbj.04a", 0x080000, 0x80000, 0x29071ed7 )
		ROM_LOAD16_WORD_SWAP( "smbj.05a", 0x100000, 0x80000, 0xeb20bce4 )
		ROM_LOAD16_WORD_SWAP( "smbj.06a", 0x180000, 0x80000, 0x94b420cd )
		ROM_LOAD16_WORD_SWAP( "smb.07",  0x200000, 0x80000, 0xb9a11577 )
		ROM_LOAD16_WORD_SWAP( "smb.08",  0x280000, 0x80000, 0xf931b76b )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "smbjx.03a", 0x000000, 0x80000, 0x3600f8d8 )
		ROM_LOAD16_WORD_SWAP( "smbjx.04a", 0x080000, 0x80000, 0x6d0f1b81 )
		ROM_LOAD16_WORD_SWAP( "smbjx.05a", 0x100000, 0x80000, 0x97f5b4af )
	
		ROM_REGION( 0x1200000, REGION_GFX1, 0 );
		ROMX_LOAD( "smb.13",   0x0000000, 0x200000, 0xd9b2d1de, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.15",   0x0000002, 0x200000, 0x9a766d92, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.17",   0x0000004, 0x200000, 0x51800f0f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.19",   0x0000006, 0x200000, 0x35757e96, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.14",   0x0800000, 0x200000, 0xe5bfd0e7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.16",   0x0800002, 0x200000, 0xc56c0866, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.18",   0x0800004, 0x200000, 0x4ded3910, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.20",   0x0800006, 0x200000, 0x26ea1ec5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.21",   0x1000000, 0x080000, 0x0a08c5fc, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.23",   0x1000002, 0x080000, 0x0911b6c4, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.25",   0x1000004, 0x080000, 0x82d6c4ec, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.27",   0x1000006, 0x080000, 0x9b48678b, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "smb.01",   0x00000, 0x08000, 0x0abc229a );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "smb.02",   0x28000, 0x20000, 0xd051679a );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "smb.11",   0x000000, 0x200000, 0xC56935f9 )
		ROM_LOAD16_WORD_SWAP( "smb.12",   0x200000, 0x200000, 0x955b0782 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_smbombr1 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "smbj.03", 0x000000, 0x80000, 0x52eafb10 )
		ROM_LOAD16_WORD_SWAP( "smbj.04", 0x080000, 0x80000, 0xaa6e8078 )
		ROM_LOAD16_WORD_SWAP( "smbj.05", 0x100000, 0x80000, 0xb69e7d5f )
		ROM_LOAD16_WORD_SWAP( "smbj.06", 0x180000, 0x80000, 0x8d857b56 )
		ROM_LOAD16_WORD_SWAP( "smb.07",  0x200000, 0x80000, 0xb9a11577 )
		ROM_LOAD16_WORD_SWAP( "smb.08",  0x280000, 0x80000, 0xf931b76b )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "smbjx.03", 0x000000, 0x80000, 0xb0b67439 )
		ROM_LOAD16_WORD_SWAP( "smbjx.04", 0x080000, 0x80000, 0xa012f690 )
		ROM_LOAD16_WORD_SWAP( "smbjx.05", 0x100000, 0x80000, 0xf6e886d0 )
	
		ROM_REGION( 0x1200000, REGION_GFX1, 0 );
		ROMX_LOAD( "smb.13",   0x0000000, 0x200000, 0xd9b2d1de, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.15",   0x0000002, 0x200000, 0x9a766d92, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.17",   0x0000004, 0x200000, 0x51800f0f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.19",   0x0000006, 0x200000, 0x35757e96, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.14",   0x0800000, 0x200000, 0xe5bfd0e7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.16",   0x0800002, 0x200000, 0xc56c0866, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.18",   0x0800004, 0x200000, 0x4ded3910, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.20",   0x0800006, 0x200000, 0x26ea1ec5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.21",   0x1000000, 0x080000, 0x0a08c5fc, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.23",   0x1000002, 0x080000, 0x0911b6c4, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.25",   0x1000004, 0x080000, 0x82d6c4ec, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "smb.27",   0x1000006, 0x080000, 0x9b48678b, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "smb.01",   0x00000, 0x08000, 0x0abc229a );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "smb.02",   0x28000, 0x20000, 0xd051679a );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "smb.11",   0x000000, 0x200000, 0xC56935f9 )
		ROM_LOAD16_WORD_SWAP( "smb.12",   0x200000, 0x200000, 0x955b0782 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_spf2t = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION(CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "pzfu.03a", 0x000000, 0x80000, 0x346e62ef )
		ROM_LOAD16_WORD_SWAP( "pzf.04a",  0x080000, 0x80000, 0xb80649e2 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "pzfux.03a", 0x000000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0xC00000, REGION_GFX1, 0 );
		ROM_FILL(             0x000000, 0x800000, 0 );
		ROMX_LOAD( "pzf.14",  0x800000, 0x100000, 0x2d4881cb, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pzf.16",  0x800002, 0x100000, 0x4b0fd1be, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pzf.18",  0x800004, 0x100000, 0xe43aac33, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pzf.20",  0x800006, 0x100000, 0x7f536ff1, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION(QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "pzf.01",   0x00000, 0x08000, 0x600fb2a3 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "pzf.02",   0x28000, 0x20000, 0x496076e0 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "pzf.11",   0x000000, 0x200000, 0x78442743 )
		ROM_LOAD16_WORD_SWAP( "pzf.12",   0x200000, 0x200000, 0x399d2c7b )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_spf2xj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION(CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "pzfj.03a", 0x000000, 0x80000, 0x2070554a )
		ROM_LOAD16_WORD_SWAP( "pzf.04a",  0x080000, 0x80000, 0xb80649e2 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "pzfjx.03a", 0x000000, 0x80000, 0xc2e3f231 )
	
		ROM_REGION( 0xC00000, REGION_GFX1, 0 );
		ROM_FILL(             0x000000, 0x800000, 0 );
		ROMX_LOAD( "pzf.14",  0x800000, 0x100000, 0x2d4881cb, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pzf.16",  0x800002, 0x100000, 0x4b0fd1be, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pzf.18",  0x800004, 0x100000, 0xe43aac33, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "pzf.20",  0x800006, 0x100000, 0x7f536ff1, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION(QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "pzf.01",   0x00000, 0x08000, 0x600fb2a3 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "pzf.02",   0x28000, 0x20000, 0x496076e0 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "pzf.11",   0x000000, 0x200000, 0x78442743 )
		ROM_LOAD16_WORD_SWAP( "pzf.12",   0x200000, 0x200000, 0x399d2c7b )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ssf2 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "ssfu.03a", 0x000000, 0x80000, 0x72f29c33 )
		ROM_LOAD16_WORD_SWAP( "ssfu.04a", 0x080000, 0x80000, 0x935cea44 )
		ROM_LOAD16_WORD_SWAP( "ssfu.05",  0x100000, 0x80000, 0xa0acb28a )
		ROM_LOAD16_WORD_SWAP( "ssfu.06",  0x180000, 0x80000, 0x47413dcf )
		ROM_LOAD16_WORD_SWAP( "ssfu.07",  0x200000, 0x80000, 0xe6066077 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "ssfux.03a", 0x000000, 0x80000, 0xec278279 )
		ROM_LOAD16_WORD_SWAP( "ssfux.04a", 0x080000, 0x80000, 0x6194d896 )
		ROM_LOAD16_WORD_SWAP( "ssfux.05",  0x100000, 0x80000, 0xaa846b9f )
		ROM_LOAD16_WORD_SWAP( "ssfux.06",  0x180000, 0x80000, 0x235268c4 )
		ROM_LOAD16_WORD_SWAP( "ssfux.07",  0x200000, 0x80000, 0xe46e033c )
	
		ROM_REGION( 0xc00000, REGION_GFX1, 0 );
		ROMX_LOAD( "ssf.13",   0x000000, 0x200000, 0xcf94d275, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.15",   0x000002, 0x200000, 0x5eb703af, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.17",   0x000004, 0x200000, 0xffa60e0f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.19",   0x000006, 0x200000, 0x34e825c5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.14",   0x800000, 0x100000, 0xb7cc32e7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.16",   0x800002, 0x100000, 0x8376ad18, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.18",   0x800004, 0x100000, 0xf5b1b336, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.20",   0x800006, 0x100000, 0x459d5c6b, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "ssf.01",   0x00000, 0x08000, 0xeb247e8c );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD( "ssf.q01",  0x000000, 0x080000, 0xa6f9da5c );
		ROM_LOAD( "ssf.q02",  0x080000, 0x080000, 0x8c66ae26 );
		ROM_LOAD( "ssf.q03",  0x100000, 0x080000, 0x695cc2ca );
		ROM_LOAD( "ssf.q04",  0x180000, 0x080000, 0x9d9ebe32 );
		ROM_LOAD( "ssf.q05",  0x200000, 0x080000, 0x4770e7b7 );
		ROM_LOAD( "ssf.q06",  0x280000, 0x080000, 0x4e79c951 );
		ROM_LOAD( "ssf.q07",  0x300000, 0x080000, 0xcdd14313 );
		ROM_LOAD( "ssf.q08",  0x380000, 0x080000, 0x6f5a088c );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ssf2a = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "ssfa.03a", 0x000000, 0x80000, 0xd2a3c520 )
		ROM_LOAD16_WORD_SWAP( "ssfa.04a", 0x080000, 0x80000, 0x5d873642 )
		ROM_LOAD16_WORD_SWAP( "ssfa.05a", 0x100000, 0x80000, 0xf8fb4de2 )
		ROM_LOAD16_WORD_SWAP( "ssfa.06a", 0x180000, 0x80000, 0xaa8acee7 )
		ROM_LOAD16_WORD_SWAP( "ssfa.07a", 0x200000, 0x80000, 0x36e29217 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "ssfax.03a", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "ssfax.04a", 0x080000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "ssfax.05a", 0x100000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "ssfax.06a", 0x180000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "ssfax.07a", 0x200000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0xc00000, REGION_GFX1, 0 );
		ROMX_LOAD( "ssf.13",   0x000000, 0x200000, 0xcf94d275, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.15",   0x000002, 0x200000, 0x5eb703af, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.17",   0x000004, 0x200000, 0xffa60e0f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.19",   0x000006, 0x200000, 0x34e825c5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.14",   0x800000, 0x100000, 0xb7cc32e7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.16",   0x800002, 0x100000, 0x8376ad18, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.18",   0x800004, 0x100000, 0xf5b1b336, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.20",   0x800006, 0x100000, 0x459d5c6b, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "ssf.01",   0x00000, 0x08000, 0xeb247e8c );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD( "ssf.q01",  0x000000, 0x080000, 0xa6f9da5c );
		ROM_LOAD( "ssf.q02",  0x080000, 0x080000, 0x8c66ae26 );
		ROM_LOAD( "ssf.q03",  0x100000, 0x080000, 0x695cc2ca );
		ROM_LOAD( "ssf.q04",  0x180000, 0x080000, 0x9d9ebe32 );
		ROM_LOAD( "ssf.q05",  0x200000, 0x080000, 0x4770e7b7 );
		ROM_LOAD( "ssf.q06",  0x280000, 0x080000, 0x4e79c951 );
		ROM_LOAD( "ssf.q07",  0x300000, 0x080000, 0xcdd14313 );
		ROM_LOAD( "ssf.q08",  0x380000, 0x080000, 0x6f5a088c );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ssf2j = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "ssfj.03b", 0x000000, 0x80000, 0x5c2e356d )
		ROM_LOAD16_WORD_SWAP( "ssfj.04a", 0x080000, 0x80000, 0x013bd55c )
		ROM_LOAD16_WORD_SWAP( "ssfj.05",  0x100000, 0x80000, 0x0918d19a )
		ROM_LOAD16_WORD_SWAP( "ssfj.06b", 0x180000, 0x80000, 0x014e0c6d )
		ROM_LOAD16_WORD_SWAP( "ssfj.07",  0x200000, 0x80000, 0xeb6a9b1b )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "ssfjx.03b", 0x000000, 0x80000, 0x693985dd )
		ROM_LOAD16_WORD_SWAP( "ssfjx.04a", 0x080000, 0x80000, 0xf866d34a )
		ROM_LOAD16_WORD_SWAP( "ssfjx.05",  0x100000, 0x80000, 0x7282bb56 )
		ROM_LOAD16_WORD_SWAP( "ssfjx.06b", 0x180000, 0x80000, 0xc597bc4a )
		ROM_LOAD16_WORD_SWAP( "ssfjx.07",  0x200000, 0x80000, 0x2af7cab2 )
	
		ROM_REGION( 0xc00000, REGION_GFX1, 0 );
		ROMX_LOAD( "ssf.13",   0x000000, 0x200000, 0xcf94d275, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.15",   0x000002, 0x200000, 0x5eb703af, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.17",   0x000004, 0x200000, 0xffa60e0f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.19",   0x000006, 0x200000, 0x34e825c5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.14",   0x800000, 0x100000, 0xb7cc32e7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.16",   0x800002, 0x100000, 0x8376ad18, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.18",   0x800004, 0x100000, 0xf5b1b336, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.20",   0x800006, 0x100000, 0x459d5c6b, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "ssf.01",   0x00000, 0x08000, 0xeb247e8c );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD( "ssf.q01",  0x000000, 0x080000, 0xa6f9da5c );
		ROM_LOAD( "ssf.q02",  0x080000, 0x080000, 0x8c66ae26 );
		ROM_LOAD( "ssf.q03",  0x100000, 0x080000, 0x695cc2ca );
		ROM_LOAD( "ssf.q04",  0x180000, 0x080000, 0x9d9ebe32 );
		ROM_LOAD( "ssf.q05",  0x200000, 0x080000, 0x4770e7b7 );
		ROM_LOAD( "ssf.q06",  0x280000, 0x080000, 0x4e79c951 );
		ROM_LOAD( "ssf.q07",  0x300000, 0x080000, 0xcdd14313 );
		ROM_LOAD( "ssf.q08",  0x380000, 0x080000, 0x6f5a088c );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ssf2jr1 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "ssfj.03a", 0x000000, 0x80000, 0x0bbf1304 )
		ROM_LOAD16_WORD_SWAP( "ssfj.04a", 0x080000, 0x80000, 0x013bd55c )
		ROM_LOAD16_WORD_SWAP( "ssfj.05",  0x100000, 0x80000, 0x0918d19a )
		ROM_LOAD16_WORD_SWAP( "ssfj.06",  0x180000, 0x80000, 0xd808a6cd )
		ROM_LOAD16_WORD_SWAP( "ssfj.07",  0x200000, 0x80000, 0xeb6a9b1b )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "ssfjx.03a", 0x000000, 0x80000, 0xc1b1d0c1 )
		ROM_LOAD16_WORD_SWAP( "ssfjx.04a", 0x080000, 0x80000, 0xf866d34a )
		ROM_LOAD16_WORD_SWAP( "ssfjx.05",  0x100000, 0x80000, 0x7282bb56 )
		ROM_LOAD16_WORD_SWAP( "ssfjx.06",  0x180000, 0x80000, 0xcc027506 )
		ROM_LOAD16_WORD_SWAP( "ssfjx.07",  0x200000, 0x80000, 0x2af7cab2 )
	
		ROM_REGION( 0xc00000, REGION_GFX1, 0 );
		ROMX_LOAD( "ssf.13",   0x000000, 0x200000, 0xcf94d275, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.15",   0x000002, 0x200000, 0x5eb703af, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.17",   0x000004, 0x200000, 0xffa60e0f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.19",   0x000006, 0x200000, 0x34e825c5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.14",   0x800000, 0x100000, 0xb7cc32e7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.16",   0x800002, 0x100000, 0x8376ad18, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.18",   0x800004, 0x100000, 0xf5b1b336, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.20",   0x800006, 0x100000, 0x459d5c6b, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "ssf.01",   0x00000, 0x08000, 0xeb247e8c );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD( "ssf.q01",  0x000000, 0x080000, 0xa6f9da5c );
		ROM_LOAD( "ssf.q02",  0x080000, 0x080000, 0x8c66ae26 );
		ROM_LOAD( "ssf.q03",  0x100000, 0x080000, 0x695cc2ca );
		ROM_LOAD( "ssf.q04",  0x180000, 0x080000, 0x9d9ebe32 );
		ROM_LOAD( "ssf.q05",  0x200000, 0x080000, 0x4770e7b7 );
		ROM_LOAD( "ssf.q06",  0x280000, 0x080000, 0x4e79c951 );
		ROM_LOAD( "ssf.q07",  0x300000, 0x080000, 0xcdd14313 );
		ROM_LOAD( "ssf.q08",  0x380000, 0x080000, 0x6f5a088c );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ssf2jr2 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "ssfj.03", 0x000000, 0x80000, 0x7eb0efed )
		ROM_LOAD16_WORD_SWAP( "ssfj.04", 0x080000, 0x80000, 0xd7322164 )
		ROM_LOAD16_WORD_SWAP( "ssfj.05", 0x100000, 0x80000, 0x0918d19a )
		ROM_LOAD16_WORD_SWAP( "ssfj.06", 0x180000, 0x80000, 0xd808a6cd )
		ROM_LOAD16_WORD_SWAP( "ssfj.07", 0x200000, 0x80000, 0xeb6a9b1b )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "ssfjx.03", 0x000000, 0x80000, 0xc3eca34c )
		ROM_LOAD16_WORD_SWAP( "ssfjx.04", 0x080000, 0x80000, 0x4e1080c2 )
		ROM_LOAD16_WORD_SWAP( "ssfjx.05", 0x100000, 0x80000, 0x7282bb56 )
		ROM_LOAD16_WORD_SWAP( "ssfjx.06", 0x180000, 0x80000, 0xcc027506 )
		ROM_LOAD16_WORD_SWAP( "ssfjx.07", 0x200000, 0x80000, 0x2af7cab2 )
	
		ROM_REGION( 0xc00000, REGION_GFX1, 0 );
		ROMX_LOAD( "ssf.13",   0x000000, 0x200000, 0xcf94d275, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.15",   0x000002, 0x200000, 0x5eb703af, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.17",   0x000004, 0x200000, 0xffa60e0f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.19",   0x000006, 0x200000, 0x34e825c5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.14",   0x800000, 0x100000, 0xb7cc32e7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.16",   0x800002, 0x100000, 0x8376ad18, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.18",   0x800004, 0x100000, 0xf5b1b336, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.20",   0x800006, 0x100000, 0x459d5c6b, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "ssf.01",   0x00000, 0x08000, 0xeb247e8c );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD( "ssf.q01",  0x000000, 0x080000, 0xa6f9da5c );
		ROM_LOAD( "ssf.q02",  0x080000, 0x080000, 0x8c66ae26 );
		ROM_LOAD( "ssf.q03",  0x100000, 0x080000, 0x695cc2ca );
		ROM_LOAD( "ssf.q04",  0x180000, 0x080000, 0x9d9ebe32 );
		ROM_LOAD( "ssf.q05",  0x200000, 0x080000, 0x4770e7b7 );
		ROM_LOAD( "ssf.q06",  0x280000, 0x080000, 0x4e79c951 );
		ROM_LOAD( "ssf.q07",  0x300000, 0x080000, 0xcdd14313 );
		ROM_LOAD( "ssf.q08",  0x380000, 0x080000, 0x6f5a088c );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ssf2tb = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "ssfe.3tc", 0x000000, 0x80000, 0x496a8409 )
		ROM_LOAD16_WORD_SWAP( "ssfe.4tc", 0x080000, 0x80000, 0x4b45c18b )
		ROM_LOAD16_WORD_SWAP( "ssfe.5t",  0x100000, 0x80000, 0x6a9c6444 )
		ROM_LOAD16_WORD_SWAP( "ssfe.6tb", 0x180000, 0x80000, 0xe4944fc3 )
		ROM_LOAD16_WORD_SWAP( "ssfe.7t",  0x200000, 0x80000, 0x2c9f4782 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "ssfex.3tc", 0x000000, 0x80000, 0x9b2cda8c )
		ROM_LOAD16_WORD_SWAP( "ssfex.4tc", 0x080000, 0x80000, 0x62d26dc2 )
		ROM_LOAD16_WORD_SWAP( "ssfex.5t",  0x100000, 0x80000, 0x3ae42ff3 )
		ROM_LOAD16_WORD_SWAP( "ssfex.6tb", 0x180000, 0x80000, 0xf12e7228 )
		ROM_LOAD16_WORD_SWAP( "ssfex.7t",  0x200000, 0x80000, 0x4d51f760 )
	
		ROM_REGION( 0xc00000, REGION_GFX1, 0 );
		ROMX_LOAD( "ssf.13",   0x000000, 0x200000, 0xcf94d275, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.15",   0x000002, 0x200000, 0x5eb703af, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.17",   0x000004, 0x200000, 0xffa60e0f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.19",   0x000006, 0x200000, 0x34e825c5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.14",   0x800000, 0x100000, 0xb7cc32e7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.16",   0x800002, 0x100000, 0x8376ad18, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.18",   0x800004, 0x100000, 0xf5b1b336, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.20",   0x800006, 0x100000, 0x459d5c6b, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "ssf.01",   0x00000, 0x08000, 0xeb247e8c );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD( "ssf.q01",  0x000000, 0x080000, 0xa6f9da5c );
		ROM_LOAD( "ssf.q02",  0x080000, 0x080000, 0x8c66ae26 );
		ROM_LOAD( "ssf.q03",  0x100000, 0x080000, 0x695cc2ca );
		ROM_LOAD( "ssf.q04",  0x180000, 0x080000, 0x9d9ebe32 );
		ROM_LOAD( "ssf.q05",  0x200000, 0x080000, 0x4770e7b7 );
		ROM_LOAD( "ssf.q06",  0x280000, 0x080000, 0x4e79c951 );
		ROM_LOAD( "ssf.q07",  0x300000, 0x080000, 0xcdd14313 );
		ROM_LOAD( "ssf.q08",  0x380000, 0x080000, 0x6f5a088c );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ssf2tbj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "ssfj.3t", 0x000000, 0x80000, 0x980d4450 )
		ROM_LOAD16_WORD_SWAP( "ssfj.4t", 0x080000, 0x80000, 0xb4dc1906 )
		ROM_LOAD16_WORD_SWAP( "ssfj.5t", 0x100000, 0x80000, 0xa7e35fbc )
		ROM_LOAD16_WORD_SWAP( "ssfj.6t", 0x180000, 0x80000, 0xcb592b30 )
		ROM_LOAD16_WORD_SWAP( "ssfj.7t", 0x200000, 0x80000, 0x1f239515 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "ssfjx.3t", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "ssfjx.4t", 0x080000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "ssfjx.5t", 0x100000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "ssfjx.6t", 0x180000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "ssfjx.7t", 0x200000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0xc00000, REGION_GFX1, 0 );
		ROMX_LOAD( "ssf.13",   0x000000, 0x200000, 0xcf94d275, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.15",   0x000002, 0x200000, 0x5eb703af, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.17",   0x000004, 0x200000, 0xffa60e0f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.19",   0x000006, 0x200000, 0x34e825c5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.14",   0x800000, 0x100000, 0xb7cc32e7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.16",   0x800002, 0x100000, 0x8376ad18, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.18",   0x800004, 0x100000, 0xf5b1b336, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.20",   0x800006, 0x100000, 0x459d5c6b, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "ssf.01",   0x00000, 0x08000, 0xeb247e8c );
		ROM_CONTINUE(         0x10000, 0x18000 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD( "ssf.q01",  0x000000, 0x080000, 0xa6f9da5c );
		ROM_LOAD( "ssf.q02",  0x080000, 0x080000, 0x8c66ae26 );
		ROM_LOAD( "ssf.q03",  0x100000, 0x080000, 0x695cc2ca );
		ROM_LOAD( "ssf.q04",  0x180000, 0x080000, 0x9d9ebe32 );
		ROM_LOAD( "ssf.q05",  0x200000, 0x080000, 0x4770e7b7 );
		ROM_LOAD( "ssf.q06",  0x280000, 0x080000, 0x4e79c951 );
		ROM_LOAD( "ssf.q07",  0x300000, 0x080000, 0xcdd14313 );
		ROM_LOAD( "ssf.q08",  0x380000, 0x080000, 0x6f5a088c );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ssf2t = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "sfxe.03c", 0x000000, 0x80000, 0x2fa1f396 )
		ROM_LOAD16_WORD_SWAP( "sfxe.04a", 0x080000, 0x80000, 0xd0bc29c6 )
		ROM_LOAD16_WORD_SWAP( "sfxe.05",  0x100000, 0x80000, 0x65222964 )
		ROM_LOAD16_WORD_SWAP( "sfxe.06a", 0x180000, 0x80000, 0x8fe9f531 )
		ROM_LOAD16_WORD_SWAP( "sfxe.07",  0x200000, 0x80000, 0x8a7d0cb6 )
		ROM_LOAD16_WORD_SWAP( "sfxe.08",  0x280000, 0x80000, 0x74c24062 )
		ROM_LOAD16_WORD_SWAP( "sfx.09",   0x300000, 0x80000, 0x642fae3f )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "sfxex.03c", 0x000000, 0x80000, 0xa181b207 )
		ROM_LOAD16_WORD_SWAP( "sfxex.04a", 0x080000, 0x80000, 0xdf28bd00 )
		ROM_LOAD16_WORD_SWAP( "sfxex.05",  0x100000, 0x80000, 0x29b7bda4 )
		ROM_LOAD16_WORD_SWAP( "sfxex.06a", 0x180000, 0x80000, 0x6c8719b3 )
		ROM_LOAD16_WORD_SWAP( "sfxex.07",  0x200000, 0x80000, 0xdfc3b3cd )
		ROM_LOAD16_WORD_SWAP( "sfxex.08",  0x280000, 0x80000, 0xd7436ae9 )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROMX_LOAD( "ssf.13",   0x000000, 0x200000, 0xcf94d275, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.15",   0x000002, 0x200000, 0x5eb703af, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.17",   0x000004, 0x200000, 0xffa60e0f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.19",   0x000006, 0x200000, 0x34e825c5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.14",   0x800000, 0x100000, 0xb7cc32e7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.16",   0x800002, 0x100000, 0x8376ad18, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.18",   0x800004, 0x100000, 0xf5b1b336, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.20",   0x800006, 0x100000, 0x459d5c6b, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfx.21",   0xc00000, 0x100000, 0xe32854af, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfx.23",   0xc00002, 0x100000, 0x760f2927, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfx.25",   0xc00004, 0x100000, 0x1ee90208, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfx.27",   0xc00006, 0x100000, 0xf814400f, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "sfx.01",   0x00000, 0x08000, 0xb47b8835 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "sfx.02",   0x28000, 0x20000, 0x0022633f );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "sfx.11",   0x000000, 0x200000, 0x9bdbd476 )
		ROM_LOAD16_WORD_SWAP( "sfx.12",   0x200000, 0x200000, 0xa05e3aab )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ssf2tu = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "sfxu.03c", 0x000000, 0x80000, 0x86e4a335 )
		ROM_LOAD16_WORD_SWAP( "sfxu.04a", 0x080000, 0x80000, 0x532b5ffd )
		ROM_LOAD16_WORD_SWAP( "sfxu.05",  0x100000, 0x80000, 0xffa3c6de )
		ROM_LOAD16_WORD_SWAP( "sfxu.06a", 0x180000, 0x80000, 0xe4c04c99 )
		ROM_LOAD16_WORD_SWAP( "sfxu.07",  0x200000, 0x80000, 0xd8199e41 )
		ROM_LOAD16_WORD_SWAP( "sfxu.08",  0x280000, 0x80000, 0xb3c71810 )
		ROM_LOAD16_WORD_SWAP( "sfx.09",   0x300000, 0x80000, 0x642fae3f )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "sfxux.03c", 0x000000, 0x80000, 0x441df197 )
		ROM_LOAD16_WORD_SWAP( "sfxux.04a", 0x080000, 0x80000, 0x7390df1b )
		ROM_LOAD16_WORD_SWAP( "sfxux.05",  0x100000, 0x80000, 0x1d3310a0 )
		ROM_LOAD16_WORD_SWAP( "sfxux.06a", 0x180000, 0x80000, 0x6fc5efa6 )
		ROM_LOAD16_WORD_SWAP( "sfxux.07",  0x200000, 0x80000, 0x88455606 )
		ROM_LOAD16_WORD_SWAP( "sfxux.08",  0x280000, 0x80000, 0xfa2396a6 )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROMX_LOAD( "ssf.13",   0x000000, 0x200000, 0xcf94d275, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.15",   0x000002, 0x200000, 0x5eb703af, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.17",   0x000004, 0x200000, 0xffa60e0f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.19",   0x000006, 0x200000, 0x34e825c5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.14",   0x800000, 0x100000, 0xb7cc32e7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.16",   0x800002, 0x100000, 0x8376ad18, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.18",   0x800004, 0x100000, 0xf5b1b336, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.20",   0x800006, 0x100000, 0x459d5c6b, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfx.21",   0xc00000, 0x100000, 0xe32854af, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfx.23",   0xc00002, 0x100000, 0x760f2927, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfx.25",   0xc00004, 0x100000, 0x1ee90208, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfx.27",   0xc00006, 0x100000, 0xf814400f, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "sfx.01",   0x00000, 0x08000, 0xb47b8835 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "sfx.02",   0x28000, 0x20000, 0x0022633f );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "sfx.11",   0x000000, 0x200000, 0x9bdbd476 )
		ROM_LOAD16_WORD_SWAP( "sfx.12",   0x200000, 0x200000, 0xa05e3aab )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ssf2ta = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "sfxa.03c", 0x000000, 0x80000, 0x04b9ff34 )
		ROM_LOAD16_WORD_SWAP( "sfxa.04a", 0x080000, 0x80000, 0x16ea5f7a )
		ROM_LOAD16_WORD_SWAP( "sfxa.05",  0x100000, 0x80000, 0x53d61f0c )
		ROM_LOAD16_WORD_SWAP( "sfxa.06a", 0x180000, 0x80000, 0x066d09b5 )
		ROM_LOAD16_WORD_SWAP( "sfxa.07",  0x200000, 0x80000, 0xa428257b )
		ROM_LOAD16_WORD_SWAP( "sfxa.08",  0x280000, 0x80000, 0x39be596c )
		ROM_LOAD16_WORD_SWAP( "sfx.09",   0x300000, 0x80000, 0x642fae3f )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "sfxax.03c", 0x000000, 0x80000, 0xc8a664fa )
		ROM_LOAD16_WORD_SWAP( "sfxax.04a", 0x080000, 0x80000, 0x149d6352 )
		ROM_LOAD16_WORD_SWAP( "sfxax.05",  0x100000, 0x80000, 0xac169aa9 )
		ROM_LOAD16_WORD_SWAP( "sfxax.06a", 0x180000, 0x80000, 0xbb60394c )
		ROM_LOAD16_WORD_SWAP( "sfxax.07",  0x200000, 0x80000, 0xe62b1b69 )
		ROM_LOAD16_WORD_SWAP( "sfxax.08",  0x280000, 0x80000, 0x7c5fd202 )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROMX_LOAD( "ssf.13",   0x000000, 0x200000, 0xcf94d275, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.15",   0x000002, 0x200000, 0x5eb703af, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.17",   0x000004, 0x200000, 0xffa60e0f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.19",   0x000006, 0x200000, 0x34e825c5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.14",   0x800000, 0x100000, 0xb7cc32e7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.16",   0x800002, 0x100000, 0x8376ad18, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.18",   0x800004, 0x100000, 0xf5b1b336, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.20",   0x800006, 0x100000, 0x459d5c6b, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfx.21",   0xc00000, 0x100000, 0xe32854af, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfx.23",   0xc00002, 0x100000, 0x760f2927, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfx.25",   0xc00004, 0x100000, 0x1ee90208, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfx.27",   0xc00006, 0x100000, 0xf814400f, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "sfx.01",   0x00000, 0x08000, 0xb47b8835 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "sfx.02",   0x28000, 0x20000, 0x0022633f );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "sfx.11",   0x000000, 0x200000, 0x9bdbd476 )
		ROM_LOAD16_WORD_SWAP( "sfx.12",   0x200000, 0x200000, 0xa05e3aab )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ssf2xj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "sfxj.03c", 0x000000, 0x80000, 0xa7417b79 )
		ROM_LOAD16_WORD_SWAP( "sfxj.04a", 0x080000, 0x80000, 0xaf7767b4 )
		ROM_LOAD16_WORD_SWAP( "sfxj.05",  0x100000, 0x80000, 0xf4ff18f5 )
		ROM_LOAD16_WORD_SWAP( "sfxj.06a", 0x180000, 0x80000, 0x260d0370 )
		ROM_LOAD16_WORD_SWAP( "sfxj.07",  0x200000, 0x80000, 0x1324d02a )
		ROM_LOAD16_WORD_SWAP( "sfxj.08",  0x280000, 0x80000, 0x2de76f10 )
		ROM_LOAD16_WORD_SWAP( "sfx.09",   0x300000, 0x80000, 0x642fae3f )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "sfxjx.03c", 0x000000, 0x80000, 0x084e929f )
		ROM_LOAD16_WORD_SWAP( "sfxjx.04a", 0x080000, 0x80000, 0x9ea7a7c0 )
		ROM_LOAD16_WORD_SWAP( "sfxjx.05",  0x100000, 0x80000, 0x46184b32 )
		ROM_LOAD16_WORD_SWAP( "sfxjx.06a", 0x180000, 0x80000, 0x9877b7a4 )
		ROM_LOAD16_WORD_SWAP( "sfxjx.07",  0x200000, 0x80000, 0xeb8c5317 )
		ROM_LOAD16_WORD_SWAP( "sfxjx.08",  0x280000, 0x80000, 0x656a9858 )
	
		ROM_REGION( 0x1000000, REGION_GFX1, 0 );
		ROMX_LOAD( "ssf.13",   0x000000, 0x200000, 0xcf94d275, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.15",   0x000002, 0x200000, 0x5eb703af, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.17",   0x000004, 0x200000, 0xffa60e0f, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.19",   0x000006, 0x200000, 0x34e825c5, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.14",   0x800000, 0x100000, 0xb7cc32e7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.16",   0x800002, 0x100000, 0x8376ad18, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.18",   0x800004, 0x100000, 0xf5b1b336, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "ssf.20",   0x800006, 0x100000, 0x459d5c6b, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfx.21",   0xc00000, 0x100000, 0xe32854af, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfx.23",   0xc00002, 0x100000, 0x760f2927, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfx.25",   0xc00004, 0x100000, 0x1ee90208, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "sfx.27",   0xc00006, 0x100000, 0xf814400f, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "sfx.01",   0x00000, 0x08000, 0xb47b8835 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "sfx.02",   0x28000, 0x20000, 0x0022633f );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "sfx.11",   0x000000, 0x200000, 0x9bdbd476 )
		ROM_LOAD16_WORD_SWAP( "sfx.12",   0x200000, 0x200000, 0xa05e3aab )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_vhunt2 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "vh2j.03", 0x000000, 0x80000, 0x1a5feb13 )
		ROM_LOAD16_WORD_SWAP( "vh2j.04", 0x080000, 0x80000, 0x434611a5 )
		ROM_LOAD16_WORD_SWAP( "vh2j.05", 0x100000, 0x80000, 0xde34f624 )
		ROM_LOAD16_WORD_SWAP( "vh2j.06", 0x180000, 0x80000, 0x6a3b9897 )
		ROM_LOAD16_WORD_SWAP( "vh2j.07", 0x200000, 0x80000, 0xb021c029 )
		ROM_LOAD16_WORD_SWAP( "vh2j.08", 0x280000, 0x80000, 0xac873dff )
		ROM_LOAD16_WORD_SWAP( "vh2j.09", 0x300000, 0x80000, 0xeaefce9c )
		ROM_LOAD16_WORD_SWAP( "vh2j.10", 0x380000, 0x80000, 0x11730952 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "vh2jx.03", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "vh2jx.04", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "vh2.13",   0x0000000, 0x400000, 0x3b02ddaa, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vh2.15",   0x0000002, 0x400000, 0x4e40de66, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vh2.17",   0x0000004, 0x400000, 0xb31d00c9, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vh2.19",   0x0000006, 0x400000, 0x149be3ab, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vh2.14",   0x1000000, 0x400000, 0xcd09bd63, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vh2.16",   0x1000002, 0x400000, 0xe0182c15, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vh2.18",   0x1000004, 0x400000, 0x778dc4f6, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vh2.20",   0x1000006, 0x400000, 0x605d9d1d, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "vh2.01",  0x00000, 0x08000, 0x67b9f779 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "vh2.02",  0x28000, 0x20000, 0xaaf15fcb );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "vh2.11",  0x000000, 0x400000, 0x38922efd )
		ROM_LOAD16_WORD_SWAP( "vh2.12",  0x400000, 0x400000, 0x6e2430af )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_vsav = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "vm3u.03d", 0x000000, 0x80000, 0x1f295274 )
		ROM_LOAD16_WORD_SWAP( "vm3u.04d", 0x080000, 0x80000, 0xc46adf81 )
		ROM_LOAD16_WORD_SWAP( "vm3.05a",  0x100000, 0x80000, 0x4118e00f )
		ROM_LOAD16_WORD_SWAP( "vm3.06a",  0x180000, 0x80000, 0x2f4fd3a9 )
		ROM_LOAD16_WORD_SWAP( "vm3.07b",  0x200000, 0x80000, 0xcbda91b8 )
		ROM_LOAD16_WORD_SWAP( "vm3.08a",  0x280000, 0x80000, 0x6ca47259 )
		ROM_LOAD16_WORD_SWAP( "vm3.09b",  0x300000, 0x80000, 0xf4a339e3 )
		ROM_LOAD16_WORD_SWAP( "vm3.10b",  0x380000, 0x80000, 0xfffbb5b8 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "vm3ux.03d", 0x000000, 0x80000, 0xfb135627 )
		ROM_LOAD16_WORD_SWAP( "vm3ux.04d", 0x080000, 0x80000, 0xcf02f61d )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "vm3.13",   0x0000000, 0x400000, 0xfd8a11eb, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.15",   0x0000002, 0x400000, 0xdd1e7d4e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.17",   0x0000004, 0x400000, 0x6b89445e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.19",   0x0000006, 0x400000, 0x3830fdc7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.14",   0x1000000, 0x400000, 0xc1a28e6c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.16",   0x1000002, 0x400000, 0x194a7304, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.18",   0x1000004, 0x400000, 0xdf9a9f47, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.20",   0x1000006, 0x400000, 0xc22fc3d9, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "vm3.01",   0x00000, 0x08000, 0xf778769b );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "vm3.02",   0x28000, 0x20000, 0xcc09faa1 );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "vm3.11",   0x000000, 0x400000, 0xe80e956e )
		ROM_LOAD16_WORD_SWAP( "vm3.12",   0x400000, 0x400000, 0x9cd71557 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_vsavj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "vm3j.03d", 0x000000, 0x80000, 0x2a2e74a4 )
		ROM_LOAD16_WORD_SWAP( "vm3j.04d", 0x080000, 0x80000, 0x1c2427bc )
		ROM_LOAD16_WORD_SWAP( "vm3j.05a", 0x100000, 0x80000, 0x95ce88d5 )
		ROM_LOAD16_WORD_SWAP( "vm3j.06b", 0x180000, 0x80000, 0x2c4297e0 )
		ROM_LOAD16_WORD_SWAP( "vm3j.07b", 0x200000, 0x80000, 0xa38aaae7 )
		ROM_LOAD16_WORD_SWAP( "vm3j.08a", 0x280000, 0x80000, 0x5773e5c9 )
		ROM_LOAD16_WORD_SWAP( "vm3j.09b", 0x300000, 0x80000, 0xd064f8b9 )
		ROM_LOAD16_WORD_SWAP( "vm3j.10b", 0x380000, 0x80000, 0x434518e9 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "vm3jx.03d", 0x000000, 0x80000, 0xa9ab54df )
		ROM_LOAD16_WORD_SWAP( "vm3jx.04d", 0x080000, 0x80000, 0x20c4aa2d )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "vm3.13",   0x0000000, 0x400000, 0xfd8a11eb, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.15",   0x0000002, 0x400000, 0xdd1e7d4e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.17",   0x0000004, 0x400000, 0x6b89445e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.19",   0x0000006, 0x400000, 0x3830fdc7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.14",   0x1000000, 0x400000, 0xc1a28e6c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.16",   0x1000002, 0x400000, 0x194a7304, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.18",   0x1000004, 0x400000, 0xdf9a9f47, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.20",   0x1000006, 0x400000, 0xc22fc3d9, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "vm3.01",   0x00000, 0x08000, 0xf778769b );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "vm3.02",   0x28000, 0x20000, 0xcc09faa1 );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "vm3.11",   0x000000, 0x400000, 0xe80e956e )
		ROM_LOAD16_WORD_SWAP( "vm3.12",   0x400000, 0x400000, 0x9cd71557 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_vsava = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "vm3a.03d", 0x000000, 0x80000, 0x44c1198f )
		ROM_LOAD16_WORD_SWAP( "vm3a.04d", 0x080000, 0x80000, 0x2218b781 )
		ROM_LOAD16_WORD_SWAP( "vm3.05a",  0x100000, 0x80000, 0x4118e00f )
		ROM_LOAD16_WORD_SWAP( "vm3.06a",  0x180000, 0x80000, 0x2f4fd3a9 )
		ROM_LOAD16_WORD_SWAP( "vm3.07b",  0x200000, 0x80000, 0xcbda91b8 )
		ROM_LOAD16_WORD_SWAP( "vm3.08a",  0x280000, 0x80000, 0x6ca47259 )
		ROM_LOAD16_WORD_SWAP( "vm3.09b",  0x300000, 0x80000, 0xf4a339e3 )
		ROM_LOAD16_WORD_SWAP( "vm3.10b",  0x380000, 0x80000, 0xfffbb5b8 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "vm3ax.03d", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "vm3ax.04d", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "vm3.13",   0x0000000, 0x400000, 0xfd8a11eb, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.15",   0x0000002, 0x400000, 0xdd1e7d4e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.17",   0x0000004, 0x400000, 0x6b89445e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.19",   0x0000006, 0x400000, 0x3830fdc7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.14",   0x1000000, 0x400000, 0xc1a28e6c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.16",   0x1000002, 0x400000, 0x194a7304, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.18",   0x1000004, 0x400000, 0xdf9a9f47, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.20",   0x1000006, 0x400000, 0xc22fc3d9, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "vm3.01",   0x00000, 0x08000, 0xf778769b );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "vm3.02",   0x28000, 0x20000, 0xcc09faa1 );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "vm3.11",   0x000000, 0x400000, 0xe80e956e )
		ROM_LOAD16_WORD_SWAP( "vm3.12",   0x400000, 0x400000, 0x9cd71557 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_vsavh = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "vm3h.03a", 0x000000, 0x80000, 0x7cc62df8 )
		ROM_LOAD16_WORD_SWAP( "vm3h.04d", 0x080000, 0x80000, 0xd716f3b5 )
		ROM_LOAD16_WORD_SWAP( "vm3.05a",  0x100000, 0x80000, 0x4118e00f )
		ROM_LOAD16_WORD_SWAP( "vm3.06a",  0x180000, 0x80000, 0x2f4fd3a9 )
		ROM_LOAD16_WORD_SWAP( "vm3.07b",  0x200000, 0x80000, 0xcbda91b8 )
		ROM_LOAD16_WORD_SWAP( "vm3.08a",  0x280000, 0x80000, 0x6ca47259 )
		ROM_LOAD16_WORD_SWAP( "vm3.09b",  0x300000, 0x80000, 0xf4a339e3 )
		ROM_LOAD16_WORD_SWAP( "vm3.10b",  0x380000, 0x80000, 0xfffbb5b8 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "vm3hx.03a", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "vm3hx.04d", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "vm3.13",   0x0000000, 0x400000, 0xfd8a11eb, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.15",   0x0000002, 0x400000, 0xdd1e7d4e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.17",   0x0000004, 0x400000, 0x6b89445e, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.19",   0x0000006, 0x400000, 0x3830fdc7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.14",   0x1000000, 0x400000, 0xc1a28e6c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.16",   0x1000002, 0x400000, 0x194a7304, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.18",   0x1000004, 0x400000, 0xdf9a9f47, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vm3.20",   0x1000006, 0x400000, 0xc22fc3d9, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "vm3.01",   0x00000, 0x08000, 0xf778769b );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "vm3.02",   0x28000, 0x20000, 0xcc09faa1 );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "vm3.11",   0x000000, 0x400000, 0xe80e956e )
		ROM_LOAD16_WORD_SWAP( "vm3.12",   0x400000, 0x400000, 0x9cd71557 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_vsav2 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "vs2j.03", 0x000000, 0x80000, 0x89fd86b4 )
		ROM_LOAD16_WORD_SWAP( "vs2j.04", 0x080000, 0x80000, 0x107c091b )
		ROM_LOAD16_WORD_SWAP( "vs2j.05", 0x100000, 0x80000, 0x61979638 )
		ROM_LOAD16_WORD_SWAP( "vs2j.06", 0x180000, 0x80000, 0xf37c5bc2 )
		ROM_LOAD16_WORD_SWAP( "vs2j.07", 0x200000, 0x80000, 0x8f885809 )
		ROM_LOAD16_WORD_SWAP( "vs2j.08", 0x280000, 0x80000, 0x2018c120 )
		ROM_LOAD16_WORD_SWAP( "vs2j.09", 0x300000, 0x80000, 0xfac3c217 )
		ROM_LOAD16_WORD_SWAP( "vs2j.10", 0x380000, 0x80000, 0xeb490213 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "vs2jx.03", 0x000000, 0x80000, 0x8f83159a )
		ROM_LOAD16_WORD_SWAP( "vs2jx.04", 0x080000, 0x80000, 0xe9822de8 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "vs2.13",   0x0000000, 0x400000, 0x5c852f52, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vs2.15",   0x0000002, 0x400000, 0xa20f58af, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vs2.17",   0x0000004, 0x400000, 0x39db59ad, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vs2.19",   0x0000006, 0x400000, 0x00c763a7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vs2.14",   0x1000000, 0x400000, 0xcd09bd63, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vs2.16",   0x1000002, 0x400000, 0xe0182c15, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vs2.18",   0x1000004, 0x400000, 0x778dc4f6, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "vs2.20",   0x1000006, 0x400000, 0x605d9d1d, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "vs2.01",   0x00000, 0x08000, 0x35190139 );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "vs2.02",   0x28000, 0x20000, 0xc32dba09 );
	
		ROM_REGION( 0x800000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "vs2.11",   0x000000, 0x400000, 0xd67e47b7 )
		ROM_LOAD16_WORD_SWAP( "vs2.12",   0x400000, 0x400000, 0x6d020a14 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_xmcota = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "xmne.03e", 0x000000, 0x80000, 0xa9a09b09 )
		ROM_LOAD16_WORD_SWAP( "xmne.04e", 0x080000, 0x80000, 0x52fa2106 )
		ROM_LOAD16_WORD_SWAP( "xmnu.05a", 0x100000, 0x80000, 0xac0d7759 )	// not sure if it's the correct one
		ROM_LOAD16_WORD_SWAP( "xmn.06a",  0x180000, 0x80000, 0x1b86a328 )	// not sure if it's the correct one
		ROM_LOAD16_WORD_SWAP( "xmn.07a",  0x200000, 0x80000, 0x2c142a44 )	// not sure if it's the correct one
		ROM_LOAD16_WORD_SWAP( "xmn.08a",  0x280000, 0x80000, 0xf712d44f )	// not sure if it's the correct one
		ROM_LOAD16_WORD_SWAP( "xmn.09a",  0x300000, 0x80000, 0x9241cae8 )	// not sure if it's the correct one
		ROM_LOAD16_WORD_SWAP( "xmnu.10a", 0x380000, 0x80000, 0x53c0eab9 )	// not sure if it's the correct one
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "xmnex.03e", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "xmnex.04e", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "xmn.13",   0x0000000, 0x400000, 0xbf4df073, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.15",   0x0000002, 0x400000, 0x4d7e4cef, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.17",   0x0000004, 0x400000, 0x513eea17, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.19",   0x0000006, 0x400000, 0xd23897fc, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.14",   0x1000000, 0x400000, 0x778237b7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.16",   0x1000002, 0x400000, 0x67b36948, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.18",   0x1000004, 0x400000, 0x015a7c4c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.20",   0x1000006, 0x400000, 0x9dde2758, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "xmn.01a",  0x00000, 0x08000, 0x40f479ea );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "xmn.02a",  0x28000, 0x20000, 0x39d9b5ad );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "xmn.11",   0x000000, 0x200000, 0xc848a6bc )
		ROM_LOAD16_WORD_SWAP( "xmn.12",   0x200000, 0x200000, 0x729c188f )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_xmcotau = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "xmnu.03e", 0x000000, 0x80000, 0x0bafeb0e )
		ROM_LOAD16_WORD_SWAP( "xmnu.04e", 0x080000, 0x80000, 0xc29bdae3 )
		ROM_LOAD16_WORD_SWAP( "xmnu.05a", 0x100000, 0x80000, 0xac0d7759 )
		ROM_LOAD16_WORD_SWAP( "xmn.06a",  0x180000, 0x80000, 0x1b86a328 )
		ROM_LOAD16_WORD_SWAP( "xmn.07a",  0x200000, 0x80000, 0x2c142a44 )
		ROM_LOAD16_WORD_SWAP( "xmn.08a",  0x280000, 0x80000, 0xf712d44f )
		ROM_LOAD16_WORD_SWAP( "xmn.09a",  0x300000, 0x80000, 0x9241cae8 )
		ROM_LOAD16_WORD_SWAP( "xmnu.10a", 0x380000, 0x80000, 0x53c0eab9 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "xmnux.03e", 0x000000, 0x80000, 0x27636ac7 )
		ROM_LOAD16_WORD_SWAP( "xmnux.04e", 0x080000, 0x80000, 0x0aed300c )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "xmn.13",   0x0000000, 0x400000, 0xbf4df073, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.15",   0x0000002, 0x400000, 0x4d7e4cef, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.17",   0x0000004, 0x400000, 0x513eea17, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.19",   0x0000006, 0x400000, 0xd23897fc, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.14",   0x1000000, 0x400000, 0x778237b7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.16",   0x1000002, 0x400000, 0x67b36948, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.18",   0x1000004, 0x400000, 0x015a7c4c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.20",   0x1000006, 0x400000, 0x9dde2758, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "xmn.01a",  0x00000, 0x08000, 0x40f479ea );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "xmn.02a",  0x28000, 0x20000, 0x39d9b5ad );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "xmn.11",   0x000000, 0x200000, 0xc848a6bc )
		ROM_LOAD16_WORD_SWAP( "xmn.12",   0x200000, 0x200000, 0x729c188f )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_xmcotah = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "xmnh.03", 0x000000, 0x80000, 0xe4b85a90 )
		ROM_LOAD16_WORD_SWAP( "xmnh.04", 0x080000, 0x80000, 0x7dfe1406 )
		ROM_LOAD16_WORD_SWAP( "xmnh.05", 0x100000, 0x80000, 0x87b0ed0f )
		ROM_LOAD16_WORD_SWAP( "xmn.06a", 0x180000, 0x80000, 0x1b86a328 )
		ROM_LOAD16_WORD_SWAP( "xmn.07a", 0x200000, 0x80000, 0x2c142a44 )
		ROM_LOAD16_WORD_SWAP( "xmn.08a", 0x280000, 0x80000, 0xf712d44f )
		ROM_LOAD16_WORD_SWAP( "xmn.09a", 0x300000, 0x80000, 0x9241cae8 )
		ROM_LOAD16_WORD_SWAP( "xmnh.10", 0x380000, 0x80000, 0xcb36b0a4 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "xmnhx.03", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "xmnhx.04", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "xmn.13",   0x0000000, 0x400000, 0xbf4df073, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.15",   0x0000002, 0x400000, 0x4d7e4cef, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.17",   0x0000004, 0x400000, 0x513eea17, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.19",   0x0000006, 0x400000, 0xd23897fc, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.14",   0x1000000, 0x400000, 0x778237b7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.16",   0x1000002, 0x400000, 0x67b36948, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.18",   0x1000004, 0x400000, 0x015a7c4c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.20",   0x1000006, 0x400000, 0x9dde2758, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "xmn.01a",  0x00000, 0x08000, 0x40f479ea );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "xmn.02a",  0x28000, 0x20000, 0x39d9b5ad );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "xmn.11",   0x000000, 0x200000, 0xc848a6bc )
		ROM_LOAD16_WORD_SWAP( "xmn.12",   0x200000, 0x200000, 0x729c188f )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_xmcotaj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "xmnj.03b", 0x000000, 0x80000, 0xc8175fb3 )
		ROM_LOAD16_WORD_SWAP( "xmnj.04b", 0x080000, 0x80000, 0x54b3fba3 )
		ROM_LOAD16_WORD_SWAP( "xmnj.05",  0x100000, 0x80000, 0xc3ed62a2 )
		ROM_LOAD16_WORD_SWAP( "xmnj.06",  0x180000, 0x80000, 0xf03c52e1 )
		ROM_LOAD16_WORD_SWAP( "xmnj.07",  0x200000, 0x80000, 0x325626b1 )
		ROM_LOAD16_WORD_SWAP( "xmnj.08",  0x280000, 0x80000, 0x7194ea10 )
		ROM_LOAD16_WORD_SWAP( "xmnj.09",  0x300000, 0x80000, 0xae946df3 )
		ROM_LOAD16_WORD_SWAP( "xmnj.10",  0x380000, 0x80000, 0x32a6be1d )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "xmnjx.03b", 0x000000, 0x80000, 0x523c9589 )
		ROM_LOAD16_WORD_SWAP( "xmnjx.04b", 0x080000, 0x80000, 0x673765ba )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "xmn.13",   0x0000000, 0x400000, 0xbf4df073, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.15",   0x0000002, 0x400000, 0x4d7e4cef, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.17",   0x0000004, 0x400000, 0x513eea17, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.19",   0x0000006, 0x400000, 0xd23897fc, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.14",   0x1000000, 0x400000, 0x778237b7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.16",   0x1000002, 0x400000, 0x67b36948, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.18",   0x1000004, 0x400000, 0x015a7c4c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.20",   0x1000006, 0x400000, 0x9dde2758, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "xmn.01a",  0x00000, 0x08000, 0x40f479ea );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "xmn.02a",  0x28000, 0x20000, 0x39d9b5ad );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "xmn.11",   0x000000, 0x200000, 0xc848a6bc )
		ROM_LOAD16_WORD_SWAP( "xmn.12",   0x200000, 0x200000, 0x729c188f )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_xmcotaj1 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "xmnj.03a", 0x000000, 0x80000, 0x00761611 )
		ROM_LOAD16_WORD_SWAP( "xmnj.04a", 0x080000, 0x80000, 0x614d3f60 )
		ROM_LOAD16_WORD_SWAP( "xmnj.05",  0x100000, 0x80000, 0xc3ed62a2 )
		ROM_LOAD16_WORD_SWAP( "xmnj.06",  0x180000, 0x80000, 0xf03c52e1 )
		ROM_LOAD16_WORD_SWAP( "xmnj.07",  0x200000, 0x80000, 0x325626b1 )
		ROM_LOAD16_WORD_SWAP( "xmnj.08",  0x280000, 0x80000, 0x7194ea10 )
		ROM_LOAD16_WORD_SWAP( "xmnj.09",  0x300000, 0x80000, 0xae946df3 )
		ROM_LOAD16_WORD_SWAP( "xmnj.10",  0x380000, 0x80000, 0x32a6be1d )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "xmnjx.03a", 0x000000, 0x80000, 0x515b9bf9 )
		ROM_LOAD16_WORD_SWAP( "xmnjx.04a", 0x080000, 0x80000, 0x5419572b )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "xmn.13",   0x0000000, 0x400000, 0xbf4df073, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.15",   0x0000002, 0x400000, 0x4d7e4cef, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.17",   0x0000004, 0x400000, 0x513eea17, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.19",   0x0000006, 0x400000, 0xd23897fc, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.14",   0x1000000, 0x400000, 0x778237b7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.16",   0x1000002, 0x400000, 0x67b36948, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.18",   0x1000004, 0x400000, 0x015a7c4c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.20",   0x1000006, 0x400000, 0x9dde2758, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "xmn.01a",  0x00000, 0x08000, 0x40f479ea );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "xmn.02a",  0x28000, 0x20000, 0x39d9b5ad );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "xmn.11",   0x000000, 0x200000, 0xc848a6bc )
		ROM_LOAD16_WORD_SWAP( "xmn.12",   0x200000, 0x200000, 0x729c188f )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_xmcotajr = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "xmno.03a", 0x000000, 0x80000, 0x7ab19acf )
		ROM_LOAD16_WORD_SWAP( "xmno.04a", 0x080000, 0x80000, 0x7615dd21 )
		ROM_LOAD16_WORD_SWAP( "xmno.05a", 0x100000, 0x80000, 0x0303d672 )
		ROM_LOAD16_WORD_SWAP( "xmno.06a", 0x180000, 0x80000, 0x332839a5 )
		ROM_LOAD16_WORD_SWAP( "xmno.07",  0x200000, 0x80000, 0x6255e8d5 )
		ROM_LOAD16_WORD_SWAP( "xmno.08",  0x280000, 0x80000, 0xb8ebe77c )
		ROM_LOAD16_WORD_SWAP( "xmno.09",  0x300000, 0x80000, 0x5440d950 )
		ROM_LOAD16_WORD_SWAP( "xmno.10a", 0x380000, 0x80000, 0xb8296966 )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "xmnox.03a", 0x000000, 0x80000, 0xc2d26e40 )
		ROM_LOAD16_WORD_SWAP( "xmnox.04a", 0x080000, 0x80000, 0x9fb6b396 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "xmn.13",   0x0000000, 0x400000, 0xbf4df073, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.15",   0x0000002, 0x400000, 0x4d7e4cef, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.17",   0x0000004, 0x400000, 0x513eea17, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.19",   0x0000006, 0x400000, 0xd23897fc, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.14",   0x1000000, 0x400000, 0x778237b7, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.16",   0x1000002, 0x400000, 0x67b36948, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.18",   0x1000004, 0x400000, 0x015a7c4c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xmn.20",   0x1000006, 0x400000, 0x9dde2758, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "xmn.01",   0x00000, 0x08000, 0x7178336e );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "xmn.02",   0x28000, 0x20000, 0x0ec58501 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "xmn.11",   0x000000, 0x200000, 0xc848a6bc )
		ROM_LOAD16_WORD_SWAP( "xmn.12",   0x200000, 0x200000, 0x729c188f )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_xmvsf = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "xvse.03d", 0x000000, 0x80000, 0x5ae5bd3b )
		ROM_LOAD16_WORD_SWAP( "xvse.04d", 0x080000, 0x80000, 0x5eb9c02e )
		ROM_LOAD16_WORD_SWAP( "xvs.05a",  0x100000, 0x80000, 0x7db6025d )
		ROM_LOAD16_WORD_SWAP( "xvs.06a",  0x180000, 0x80000, 0xe8e2c75c )
		ROM_LOAD16_WORD_SWAP( "xvs.07",   0x200000, 0x80000, 0x08f0abed )
		ROM_LOAD16_WORD_SWAP( "xvs.08",   0x280000, 0x80000, 0x81929675 )
		ROM_LOAD16_WORD_SWAP( "xvs.09",   0x300000, 0x80000, 0x9641f36b )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "xvsex.03d", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "xvsex.04d", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "xvs.13",   0x0000000, 0x400000, 0xf6684efd, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.15",   0x0000002, 0x400000, 0x29109221, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.17",   0x0000004, 0x400000, 0x92db3474, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.19",   0x0000006, 0x400000, 0x3733473c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.14",   0x1000000, 0x400000, 0xbcac2e41, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.16",   0x1000002, 0x400000, 0xea04a272, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.18",   0x1000004, 0x400000, 0xb0def86a, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.20",   0x1000006, 0x400000, 0x4b40ff9f, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "xvs.01",   0x00000, 0x08000, 0x3999e93a );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "xvs.02",   0x28000, 0x20000, 0x101bdee9 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "xvs.11",   0x000000, 0x200000, 0x9cadcdbc )
		ROM_LOAD16_WORD_SWAP( "xvs.12",   0x200000, 0x200000, 0x7b11e460 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_xmvsfu = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "xvsu.03h", 0x000000, 0x80000, 0x5481155a )
		ROM_LOAD16_WORD_SWAP( "xvsu.04h", 0x080000, 0x80000, 0x1e236388 )
		ROM_LOAD16_WORD_SWAP( "xvs.05a",  0x100000, 0x80000, 0x7db6025d )
		ROM_LOAD16_WORD_SWAP( "xvs.06a",  0x180000, 0x80000, 0xe8e2c75c )
		ROM_LOAD16_WORD_SWAP( "xvs.07",   0x200000, 0x80000, 0x08f0abed )
		ROM_LOAD16_WORD_SWAP( "xvs.08",   0x280000, 0x80000, 0x81929675 )
		ROM_LOAD16_WORD_SWAP( "xvs.09",   0x300000, 0x80000, 0x9641f36b )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "xvsux.03h", 0x000000, 0x80000, 0x1539c639 )
		ROM_LOAD16_WORD_SWAP( "xvsux.04h", 0x080000, 0x80000, 0x68916b3f )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "xvs.13",   0x0000000, 0x400000, 0xf6684efd, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.15",   0x0000002, 0x400000, 0x29109221, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.17",   0x0000004, 0x400000, 0x92db3474, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.19",   0x0000006, 0x400000, 0x3733473c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.14",   0x1000000, 0x400000, 0xbcac2e41, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.16",   0x1000002, 0x400000, 0xea04a272, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.18",   0x1000004, 0x400000, 0xb0def86a, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.20",   0x1000006, 0x400000, 0x4b40ff9f, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "xvs.01",   0x00000, 0x08000, 0x3999e93a );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "xvs.02",   0x28000, 0x20000, 0x101bdee9 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "xvs.11",   0x000000, 0x200000, 0x9cadcdbc )
		ROM_LOAD16_WORD_SWAP( "xvs.12",   0x200000, 0x200000, 0x7b11e460 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_xmvsfj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "xvsj.03d", 0x000000, 0x80000, 0xbeb81de9 )
		ROM_LOAD16_WORD_SWAP( "xvsj.04d", 0x080000, 0x80000, 0x23d11271 )
		ROM_LOAD16_WORD_SWAP( "xvs.05a",  0x100000, 0x80000, 0x7db6025d )
		ROM_LOAD16_WORD_SWAP( "xvs.06a",  0x180000, 0x80000, 0xe8e2c75c )
		ROM_LOAD16_WORD_SWAP( "xvs.07",   0x200000, 0x80000, 0x08f0abed )
		ROM_LOAD16_WORD_SWAP( "xvs.08",   0x280000, 0x80000, 0x81929675 )
		ROM_LOAD16_WORD_SWAP( "xvs.09",   0x300000, 0x80000, 0x9641f36b )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "xvsjx.03d", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "xvsjx.04d", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "xvs.13",   0x0000000, 0x400000, 0xf6684efd, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.15",   0x0000002, 0x400000, 0x29109221, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.17",   0x0000004, 0x400000, 0x92db3474, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.19",   0x0000006, 0x400000, 0x3733473c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.14",   0x1000000, 0x400000, 0xbcac2e41, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.16",   0x1000002, 0x400000, 0xea04a272, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.18",   0x1000004, 0x400000, 0xb0def86a, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.20",   0x1000006, 0x400000, 0x4b40ff9f, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "xvs.01",   0x00000, 0x08000, 0x3999e93a );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "xvs.02",   0x28000, 0x20000, 0x101bdee9 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "xvs.11",   0x000000, 0x200000, 0x9cadcdbc )
		ROM_LOAD16_WORD_SWAP( "xvs.12",   0x200000, 0x200000, 0x7b11e460 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_xmvsfa = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "xvsa.03", 0x000000, 0x80000, 0xd0cca7a8 )
		ROM_LOAD16_WORD_SWAP( "xvsa.04", 0x080000, 0x80000, 0x8c8e76fd )
		ROM_LOAD16_WORD_SWAP( "xvs.05a", 0x100000, 0x80000, 0x7db6025d )
		ROM_LOAD16_WORD_SWAP( "xvs.06a", 0x180000, 0x80000, 0xe8e2c75c )
		ROM_LOAD16_WORD_SWAP( "xvs.07",  0x200000, 0x80000, 0x08f0abed )
		ROM_LOAD16_WORD_SWAP( "xvs.08",  0x280000, 0x80000, 0x81929675 )
		ROM_LOAD16_WORD_SWAP( "xvs.09",  0x300000, 0x80000, 0x9641f36b )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "xvsax.03", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "xvsax.04", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "xvs.13",   0x0000000, 0x400000, 0xf6684efd, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.15",   0x0000002, 0x400000, 0x29109221, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.17",   0x0000004, 0x400000, 0x92db3474, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.19",   0x0000006, 0x400000, 0x3733473c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.14",   0x1000000, 0x400000, 0xbcac2e41, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.16",   0x1000002, 0x400000, 0xea04a272, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.18",   0x1000004, 0x400000, 0xb0def86a, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.20",   0x1000006, 0x400000, 0x4b40ff9f, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "xvs.01",   0x00000, 0x08000, 0x3999e93a );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "xvs.02",   0x28000, 0x20000, 0x101bdee9 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "xvs.11",   0x000000, 0x200000, 0x9cadcdbc )
		ROM_LOAD16_WORD_SWAP( "xvs.12",   0x200000, 0x200000, 0x7b11e460 )
	ROM_END(); }}; 
	
	static RomLoadPtr rom_xmvsfh = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( CODE_SIZE, REGION_CPU1, 0 );     /* 68000 code */
		ROM_LOAD16_WORD_SWAP( "xvsh.03a", 0x000000, 0x80000, 0xd4fffb04 )
		ROM_LOAD16_WORD_SWAP( "xvsh.04a", 0x080000, 0x80000, 0x1b4ea638 )
		ROM_LOAD16_WORD_SWAP( "xvs.05a",  0x100000, 0x80000, 0x7db6025d )
		ROM_LOAD16_WORD_SWAP( "xvs.06a",  0x180000, 0x80000, 0xe8e2c75c )
		ROM_LOAD16_WORD_SWAP( "xvs.07",   0x200000, 0x80000, 0x08f0abed )
		ROM_LOAD16_WORD_SWAP( "xvs.08",   0x280000, 0x80000, 0x81929675 )
		ROM_LOAD16_WORD_SWAP( "xvs.09",   0x300000, 0x80000, 0x9641f36b )
	
		ROM_REGION16_BE( CODE_SIZE, REGION_USER1, 0 );
		ROM_LOAD16_WORD_SWAP( "xvshx.03a", 0x000000, 0x80000, 0x00000000 )
		ROM_LOAD16_WORD_SWAP( "xvshx.04a", 0x080000, 0x80000, 0x00000000 )
	
		ROM_REGION( 0x2000000, REGION_GFX1, 0 );
		ROMX_LOAD( "xvs.13",   0x0000000, 0x400000, 0xf6684efd, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.15",   0x0000002, 0x400000, 0x29109221, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.17",   0x0000004, 0x400000, 0x92db3474, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.19",   0x0000006, 0x400000, 0x3733473c, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.14",   0x1000000, 0x400000, 0xbcac2e41, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.16",   0x1000002, 0x400000, 0xea04a272, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.18",   0x1000004, 0x400000, 0xb0def86a, ROM_GROUPWORD | ROM_SKIP(6) )
		ROMX_LOAD( "xvs.20",   0x1000006, 0x400000, 0x4b40ff9f, ROM_GROUPWORD | ROM_SKIP(6) )
	
		ROM_REGION( QSOUND_SIZE, REGION_CPU2, 0 );/* 64k for the audio CPU (+banks) */
		ROM_LOAD( "xvs.01",   0x00000, 0x08000, 0x3999e93a );
		ROM_CONTINUE(         0x10000, 0x18000 );
		ROM_LOAD( "xvs.02",   0x28000, 0x20000, 0x101bdee9 );
	
		ROM_REGION( 0x400000, REGION_SOUND1, 0 );/* QSound samples */
		ROM_LOAD16_WORD_SWAP( "xvs.11",   0x000000, 0x200000, 0x9cadcdbc )
		ROM_LOAD16_WORD_SWAP( "xvs.12",   0x200000, 0x200000, 0x7b11e460 )
	ROM_END(); }}; 
	
	public static GameDriver driver_1944j	   = new GameDriver("2000"	,"1944j"	,"cps2.java"	,rom_1944j,null	,machine_driver_cps2	,input_ports_19xx	,init_cps2	,ROT270	,	"Capcom", "1944: The Loop Master (Japan 000620)", GAME_NOT_WORKING )
	public static GameDriver driver_19xx	   = new GameDriver("1996"	,"19xx"	,"cps2.java"	,rom_19xx,null	,machine_driver_cps2	,input_ports_19xx	,init_cps2	,ROT270	,	"Capcom", "19XX: The War Against Destiny (US 951207)" )
	public static GameDriver driver_19xxj	   = new GameDriver("1996"	,"19xxj"	,"cps2.java"	,rom_19xxj,driver_19xx	,machine_driver_cps2	,input_ports_19xx	,init_cps2	,ROT270	,	"Capcom", "19XX: The War Against Destiny (Japan 951225)", GAME_NOT_WORKING )
	public static GameDriver driver_19xxjr1	   = new GameDriver("1996"	,"19xxjr1"	,"cps2.java"	,rom_19xxjr1,driver_19xx	,machine_driver_cps2	,input_ports_19xx	,init_cps2	,ROT270	,	"Capcom", "19XX: The War Against Destiny (Japan 951207)", GAME_NOT_WORKING )
	public static GameDriver driver_19xxh	   = new GameDriver("1996"	,"19xxh"	,"cps2.java"	,rom_19xxh,driver_19xx	,machine_driver_cps2	,input_ports_19xx	,init_cps2	,ROT270	,	"Capcom", "19XX: The War Against Destiny (Hispanic 951218)" )
	public static GameDriver driver_armwar	   = new GameDriver("1994"	,"armwar"	,"cps2.java"	,rom_armwar,null	,machine_driver_cps2	,input_ports_avsp	,init_cps2	,ROT0	,	"Capcom", "Armored Warriors (Euro 941011)" )
	public static GameDriver driver_armwaru	   = new GameDriver("1994"	,"armwaru"	,"cps2.java"	,rom_armwaru,driver_armwar	,machine_driver_cps2	,input_ports_avsp	,init_cps2	,ROT0	,	"Capcom", "Armored Warriors (US 941024)" )
	public static GameDriver driver_pgear	   = new GameDriver("1994"	,"pgear"	,"cps2.java"	,rom_pgear,driver_armwar	,machine_driver_cps2	,input_ports_avsp	,init_cps2	,ROT0	,	"Capcom", "Powered Gear: Strategic Variant Armor Equipment (Japan 941024)" )
	public static GameDriver driver_pgearr1	   = new GameDriver("1994"	,"pgearr1"	,"cps2.java"	,rom_pgearr1,driver_armwar	,machine_driver_cps2	,input_ports_avsp	,init_cps2	,ROT0	,	"Capcom", "Powered Gear: Strategic Variant Armor Equipment (Japan 940916)" )
	public static GameDriver driver_armwara	   = new GameDriver("1994"	,"armwara"	,"cps2.java"	,rom_armwara,driver_armwar	,machine_driver_cps2	,input_ports_avsp	,init_cps2	,ROT0	,	"Capcom", "Armored Warriors (Asia 940920)", GAME_NOT_WORKING )
	public static GameDriver driver_avsp	   = new GameDriver("1994"	,"avsp"	,"cps2.java"	,rom_avsp,null	,machine_driver_cps2	,input_ports_avsp	,init_cps2	,ROT0	,	"Capcom", "Alien vs. Predator (Euro 940520)" )
	public static GameDriver driver_avspu	   = new GameDriver("1994"	,"avspu"	,"cps2.java"	,rom_avspu,driver_avsp	,machine_driver_cps2	,input_ports_avsp	,init_cps2	,ROT0	,	"Capcom", "Alien vs. Predator (US 940520)" )
	public static GameDriver driver_avspj	   = new GameDriver("1994"	,"avspj"	,"cps2.java"	,rom_avspj,driver_avsp	,machine_driver_cps2	,input_ports_avsp	,init_cps2	,ROT0	,	"Capcom", "Alien vs. Predator (Japan 940520)" )
	public static GameDriver driver_avspa	   = new GameDriver("1994"	,"avspa"	,"cps2.java"	,rom_avspa,driver_avsp	,machine_driver_cps2	,input_ports_avsp	,init_cps2	,ROT0	,	"Capcom", "Alien vs. Predator (Asia 940520)", GAME_NOT_WORKING )
	public static GameDriver driver_batcirj	   = new GameDriver("1997"	,"batcirj"	,"cps2.java"	,rom_batcirj,null	,machine_driver_cps2	,input_ports_batcir	,init_cps2	,ROT0	,	"Capcom", "Battle Circuit (Japan 970319)" )
	public static GameDriver driver_batcira	   = new GameDriver("1997"	,"batcira"	,"cps2.java"	,rom_batcira,driver_batcirj	,machine_driver_cps2	,input_ports_batcir	,init_cps2	,ROT0	,	"Capcom", "Battle Circuit (Asia 970319)", GAME_NOT_WORKING )
	public static GameDriver driver_csclubj	   = new GameDriver("1997"	,"csclubj"	,"cps2.java"	,rom_csclubj,null	,machine_driver_cps2	,input_ports_sgemf	,init_cps2	,ROT0	,	"Capcom", "Capcom Sports Club (Japan 970722)" )
	public static GameDriver driver_cscluba	   = new GameDriver("1997"	,"cscluba"	,"cps2.java"	,rom_cscluba,driver_csclubj	,machine_driver_cps2	,input_ports_sgemf	,init_cps2	,ROT0	,	"Capcom", "Capcom Sports Club (Asia 970722)" )
	public static GameDriver driver_cybotsj	   = new GameDriver("1995"	,"cybotsj"	,"cps2.java"	,rom_cybotsj,null	,machine_driver_cps2	,input_ports_cybotsj	,init_cps2	,ROT0	,	"Capcom", "Cyberbots: Fullmetal Madness (Japan 950420)" )
	public static GameDriver driver_ddtod	   = new GameDriver("1993"	,"ddtod"	,"cps2.java"	,rom_ddtod,null	,machine_driver_cps2	,input_ports_ddtod	,init_cps2	,ROT0	,	"Capcom", "Dungeons & Dragons: Tower of Doom (Euro 940412)", GAME_NOT_WORKING )
	public static GameDriver driver_ddtodu	   = new GameDriver("1993"	,"ddtodu"	,"cps2.java"	,rom_ddtodu,driver_ddtod	,machine_driver_cps2	,input_ports_ddtod	,init_cps2	,ROT0	,	"Capcom", "Dungeons & Dragons: Tower of Doom (US 940125)" )
	public static GameDriver driver_ddtodur1	   = new GameDriver("1993"	,"ddtodur1"	,"cps2.java"	,rom_ddtodur1,driver_ddtod	,machine_driver_cps2	,input_ports_ddtod	,init_cps2	,ROT0	,	"Capcom", "Dungeons & Dragons: Tower of Doom (US 940113)" )
	public static GameDriver driver_ddtodj	   = new GameDriver("1993"	,"ddtodj"	,"cps2.java"	,rom_ddtodj,driver_ddtod	,machine_driver_cps2	,input_ports_ddtod	,init_cps2	,ROT0	,	"Capcom", "Dungeons & Dragons: Tower of Doom (Japan 940113)" )
	public static GameDriver driver_ddtoda	   = new GameDriver("1993"	,"ddtoda"	,"cps2.java"	,rom_ddtoda,driver_ddtod	,machine_driver_cps2	,input_ports_ddtod	,init_cps2	,ROT0	,	"Capcom", "Dungeons & Dragons: Tower of Doom (Asia 940113)", GAME_NOT_WORKING )
	public static GameDriver driver_ddsom	   = new GameDriver("1996"	,"ddsom"	,"cps2.java"	,rom_ddsom,null	,machine_driver_cps2	,input_ports_ddtod	,init_cps2	,ROT0	,	"Capcom", "Dungeons & Dragons: Shadow over Mystara (Euro 960209)" )
	public static GameDriver driver_ddsomu	   = new GameDriver("1996"	,"ddsomu"	,"cps2.java"	,rom_ddsomu,driver_ddsom	,machine_driver_cps2	,input_ports_ddtod	,init_cps2	,ROT0	,	"Capcom", "Dungeons & Dragons: Shadow over Mystara (US 960209)" )
	public static GameDriver driver_ddsomj	   = new GameDriver("1996"	,"ddsomj"	,"cps2.java"	,rom_ddsomj,driver_ddsom	,machine_driver_cps2	,input_ports_ddtod	,init_cps2	,ROT0	,	"Capcom", "Dungeons & Dragons: Shadow over Mystara (Japan 960619)", GAME_NOT_WORKING )
	public static GameDriver driver_ddsomjr1	   = new GameDriver("1996"	,"ddsomjr1"	,"cps2.java"	,rom_ddsomjr1,driver_ddsom	,machine_driver_cps2	,input_ports_ddtod	,init_cps2	,ROT0	,	"Capcom", "Dungeons & Dragons: Shadow over Mystara (Japan 960206)" )
	public static GameDriver driver_ddsoma	   = new GameDriver("1996"	,"ddsoma"	,"cps2.java"	,rom_ddsoma,driver_ddsom	,machine_driver_cps2	,input_ports_ddtod	,init_cps2	,ROT0	,	"Capcom", "Dungeons & Dragons: Shadow over Mystara (Asia 960619)", GAME_NOT_WORKING )
	public static GameDriver driver_dimahoo	   = new GameDriver("2000"	,"dimahoo"	,"cps2.java"	,rom_dimahoo,null	,machine_driver_cps2	,input_ports_sgemf	,init_cps2	,ROT270	,	"Capcom", "Dimahoo (US 000121)", GAME_NOT_WORKING )
	public static GameDriver driver_gmahou	   = new GameDriver("2000"	,"gmahou"	,"cps2.java"	,rom_gmahou,driver_dimahoo	,machine_driver_cps2	,input_ports_sgemf	,init_cps2	,ROT270	,	"Capcom", "Great Mahou Daisakusen (Japan 000121)", GAME_NOT_WORKING )
	public static GameDriver driver_dstlk	   = new GameDriver("1994"	,"dstlk"	,"cps2.java"	,rom_dstlk,null	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Darkstalkers: The Night Warriors (US 940818)" )
	public static GameDriver driver_dstlkr1	   = new GameDriver("1994"	,"dstlkr1"	,"cps2.java"	,rom_dstlkr1,driver_dstlk	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Darkstalkers: The Night Warriors (US 940705)" )
	public static GameDriver driver_vampj	   = new GameDriver("1994"	,"vampj"	,"cps2.java"	,rom_vampj,driver_dstlk	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Vampire: The Night Warriors (Japan 940705)" )
	public static GameDriver driver_vampja	   = new GameDriver("1994"	,"vampja"	,"cps2.java"	,rom_vampja,driver_dstlk	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Vampire: The Night Warriors (Japan 940705 alt)" )
	public static GameDriver driver_vampjr1	   = new GameDriver("1994"	,"vampjr1"	,"cps2.java"	,rom_vampjr1,driver_dstlk	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Vampire: The Night Warriors (Japan 940630)" )
	public static GameDriver driver_vampa	   = new GameDriver("1994"	,"vampa"	,"cps2.java"	,rom_vampa,driver_dstlk	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Vampire: The Night Warriors (Asia 940705)", GAME_NOT_WORKING )
	public static GameDriver driver_ecofghtr	   = new GameDriver("1993"	,"ecofghtr"	,"cps2.java"	,rom_ecofghtr,null	,machine_driver_cps2	,input_ports_sgemf	,init_cps2	,ROT0	,	"Capcom", "Eco Fighters (World 931203)" )
	public static GameDriver driver_uecology	   = new GameDriver("1993"	,"uecology"	,"cps2.java"	,rom_uecology,driver_ecofghtr	,machine_driver_cps2	,input_ports_sgemf	,init_cps2	,ROT0	,	"Capcom", "Ultimate Ecology (Japan 931203)", GAME_NOT_WORKING )
	public static GameDriver driver_gwingj	   = new GameDriver("1999"	,"gwingj"	,"cps2.java"	,rom_gwingj,null	,machine_driver_cps2	,input_ports_cps2	,init_cps2	,ROT0	,	"Capcom", "Giga Wing (Japan 990223)", GAME_NOT_WORKING )
	public static GameDriver driver_mmatrixj	   = new GameDriver("2000"	,"mmatrixj"	,"cps2.java"	,rom_mmatrixj,null	,machine_driver_cps2	,input_ports_cps2	,init_cps2	,ROT0	,	"Capcom", "Mars Matrix: Hyper Solid Shooting (Japan 000412)", GAME_NOT_WORKING )
	public static GameDriver driver_msh	   = new GameDriver("1995"	,"msh"	,"cps2.java"	,rom_msh,null	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Marvel Super Heroes (US 951024)" )
	public static GameDriver driver_mshj	   = new GameDriver("1995"	,"mshj"	,"cps2.java"	,rom_mshj,driver_msh	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Marvel Super Heroes (Japan 951024)" )
	public static GameDriver driver_mshh	   = new GameDriver("1995"	,"mshh"	,"cps2.java"	,rom_mshh,driver_msh	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Marvel Super Heroes (Hispanic 951117)" )
	public static GameDriver driver_mshvsf	   = new GameDriver("1997"	,"mshvsf"	,"cps2.java"	,rom_mshvsf,null	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Marvel Super Heroes Vs. Street Fighter (US 970625)" )
	public static GameDriver driver_mshvsfj	   = new GameDriver("1997"	,"mshvsfj"	,"cps2.java"	,rom_mshvsfj,driver_mshvsf	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Marvel Super Heroes Vs. Street Fighter (Japan 970707)" )
	public static GameDriver driver_mshvsfj1	   = new GameDriver("1997"	,"mshvsfj1"	,"cps2.java"	,rom_mshvsfj1,driver_mshvsf	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Marvel Super Heroes Vs. Street Fighter (Japan 970702)" )
	public static GameDriver driver_mshvsfh	   = new GameDriver("1997"	,"mshvsfh"	,"cps2.java"	,rom_mshvsfh,driver_mshvsf	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Marvel Super Heroes Vs. Street Fighter (Hispanic 970625)", GAME_NOT_WORKING )
	public static GameDriver driver_mshvsfa	   = new GameDriver("1997"	,"mshvsfa"	,"cps2.java"	,rom_mshvsfa,driver_mshvsf	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Marvel Super Heroes Vs. Street Fighter (Asia 970625)", GAME_NOT_WORKING )
	public static GameDriver driver_mshvsfa1	   = new GameDriver("1997"	,"mshvsfa1"	,"cps2.java"	,rom_mshvsfa1,driver_mshvsf	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Marvel Super Heroes Vs. Street Fighter (Asia 970620)", GAME_NOT_WORKING )
	public static GameDriver driver_mvsc	   = new GameDriver("1998"	,"mvsc"	,"cps2.java"	,rom_mvsc,null	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Marvel Vs. Capcom: Clash of Super Heroes (US 980123)", GAME_NOT_WORKING )
	public static GameDriver driver_mvscj	   = new GameDriver("1998"	,"mvscj"	,"cps2.java"	,rom_mvscj,driver_mvsc	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Marvel Vs. Capcom: Clash of Super Heroes (Japan 980123)", GAME_NOT_WORKING )
	public static GameDriver driver_mvscjr1	   = new GameDriver("1998"	,"mvscjr1"	,"cps2.java"	,rom_mvscjr1,driver_mvsc	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Marvel Vs. Capcom: Clash of Super Heroes (Japan 980112)", GAME_NOT_WORKING )
	public static GameDriver driver_mvsca	   = new GameDriver("1998"	,"mvsca"	,"cps2.java"	,rom_mvsca,driver_mvsc	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Marvel Vs. Capcom: Clash of Super Heroes (Asia 980112)", GAME_NOT_WORKING )
	public static GameDriver driver_mvsch	   = new GameDriver("1998"	,"mvsch"	,"cps2.java"	,rom_mvsch,driver_mvsc	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Marvel Vs. Capcom: Clash of Super Heroes (Hispanic 980123)", GAME_NOT_WORKING )
	public static GameDriver driver_nwarr	   = new GameDriver("1995"	,"nwarr"	,"cps2.java"	,rom_nwarr,null	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Night Warriors: Darkstalkers Revenge (US 950406)", GAME_NOT_WORKING )
	public static GameDriver driver_vhuntj	   = new GameDriver("1995"	,"vhuntj"	,"cps2.java"	,rom_vhuntj,driver_nwarr	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Vampire Hunter: Darkstalkers Revenge (Japan 950316)", GAME_NOT_WORKING )
	public static GameDriver driver_vhuntjr1	   = new GameDriver("1995"	,"vhuntjr1"	,"cps2.java"	,rom_vhuntjr1,driver_nwarr	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Vampire Hunter: Darkstalkers Revenge (Japan 950302)", GAME_NOT_WORKING )
	public static GameDriver driver_nwarrh	   = new GameDriver("1995"	,"nwarrh"	,"cps2.java"	,rom_nwarrh,driver_nwarr	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Night Warriors: Darkstalkers Revenge (Hispanic xxxxxx)", GAME_NOT_WORKING )
	public static GameDriver driver_puzloop2	   = new GameDriver("2001"	,"puzloop2"	,"cps2.java"	,rom_puzloop2,null	,machine_driver_cps2	,input_ports_cps2	,init_cps2	,ROT0	,	"Capcom", "Puzz Loop 2 (Japan 010205)", GAME_NOT_WORKING )
	public static GameDriver driver_qndream	   = new GameDriver("1996"	,"qndream"	,"cps2.java"	,rom_qndream,null	,machine_driver_cps2	,input_ports_qndream	,init_cps2	,ROT0	,	"Capcom", "Quiz Nanairo Dreams: Nijiirotyou no Kiseki (Japan 960826)" )
	public static GameDriver driver_rckmanj	   = new GameDriver("1995"	,"rckmanj"	,"cps2.java"	,rom_rckmanj,driver_megaman	,machine_driver_cps2	,input_ports_cps2	,init_cps2	,ROT0	,	"Capcom", "Rockman: The Power Battle (Japan 950922)", GAME_NOT_WORKING )
	public static GameDriver driver_rckman2j	   = new GameDriver("1996"	,"rckman2j"	,"cps2.java"	,rom_rckman2j,null	,machine_driver_cps2	,input_ports_cps2	,init_cps2	,ROT0	,	"Capcom", "Rockman 2: The Power Fighters (Japan 960708)", GAME_NOT_WORKING )
	public static GameDriver driver_sfa	   = new GameDriver("1995"	,"sfa"	,"cps2.java"	,rom_sfa,null	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Street Fighter Alpha: Warriors' Dreams (Euro 950727)" )
	public static GameDriver driver_sfar1	   = new GameDriver("1995"	,"sfar1"	,"cps2.java"	,rom_sfar1,driver_sfa	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Street Fighter Alpha: Warriors' Dreams (Euro 950605)" )
	public static GameDriver driver_sfau	   = new GameDriver("1995"	,"sfau"	,"cps2.java"	,rom_sfau,driver_sfa	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Street Fighter Alpha: Warriors' Dreams (US 950627)" )
	public static GameDriver driver_sfzj	   = new GameDriver("1995"	,"sfzj"	,"cps2.java"	,rom_sfzj,driver_sfa	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Street Fighter Zero (Japan 950727)" )
	public static GameDriver driver_sfzjr1	   = new GameDriver("1995"	,"sfzjr1"	,"cps2.java"	,rom_sfzjr1,driver_sfa	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Street Fighter Zero (Japan 950627)" )
	public static GameDriver driver_sfzjr2	   = new GameDriver("1995"	,"sfzjr2"	,"cps2.java"	,rom_sfzjr2,driver_sfa	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Street Fighter Zero (Japan 950605)" )
	public static GameDriver driver_sfzh	   = new GameDriver("1995"	,"sfzh"	,"cps2.java"	,rom_sfzh,driver_sfa	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Street Fighter Zero (Hispanic 950627)", GAME_NOT_WORKING )
	public static GameDriver driver_sfa2	   = new GameDriver("1996"	,"sfa2"	,"cps2.java"	,rom_sfa2,null	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Street Fighter Alpha 2 (US 960306)" )
	public static GameDriver driver_sfz2j	   = new GameDriver("1996"	,"sfz2j"	,"cps2.java"	,rom_sfz2j,driver_sfa2	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Street Fighter Zero 2 (Japan 960227)" )
	public static GameDriver driver_sfz2aj	   = new GameDriver("1996"	,"sfz2aj"	,"cps2.java"	,rom_sfz2aj,driver_sfa2	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Street Fighter Zero 2 Alpha (Japan 960805)" )
	public static GameDriver driver_sfz2ah	   = new GameDriver("1996"	,"sfz2ah"	,"cps2.java"	,rom_sfz2ah,driver_sfa2	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Street Fighter Zero 2 Alpha (Hispanic 960813)", GAME_NOT_WORKING )
	public static GameDriver driver_sfa3	   = new GameDriver("1998"	,"sfa3"	,"cps2.java"	,rom_sfa3,null	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Street Fighter Alpha 3 (US 980904)" )
	public static GameDriver driver_sfa3r1	   = new GameDriver("1998"	,"sfa3r1"	,"cps2.java"	,rom_sfa3r1,driver_sfa3	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Street Fighter Alpha 3 (US 980629)" )
	public static GameDriver driver_sfz3j	   = new GameDriver("1998"	,"sfz3j"	,"cps2.java"	,rom_sfz3j,driver_sfa3	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Street Fighter Zero 3 (Japan 980727)" )
	public static GameDriver driver_sfz3jr1	   = new GameDriver("1998"	,"sfz3jr1"	,"cps2.java"	,rom_sfz3jr1,driver_sfa3	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Street Fighter Zero 3 (Japan 980629)" )
	public static GameDriver driver_sfz3a	   = new GameDriver("1998"	,"sfz3a"	,"cps2.java"	,rom_sfz3a,driver_sfa3	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Street Fighter Zero 3 (Asia 980701)", GAME_NOT_WORKING  )
	public static GameDriver driver_sgemf	   = new GameDriver("1997"	,"sgemf"	,"cps2.java"	,rom_sgemf,null	,machine_driver_cps2	,input_ports_sgemf	,init_cps2	,ROT0	,	"Capcom", "Super Gem Fighter Mini Mix (US 970904)", GAME_NOT_WORKING )
	public static GameDriver driver_pfghtj	   = new GameDriver("1997"	,"pfghtj"	,"cps2.java"	,rom_pfghtj,driver_sgemf	,machine_driver_cps2	,input_ports_sgemf	,init_cps2	,ROT0	,	"Capcom", "Pocket Fighter (Japan 970904)", GAME_NOT_WORKING )
	public static GameDriver driver_sgemfh	   = new GameDriver("1997"	,"sgemfh"	,"cps2.java"	,rom_sgemfh,driver_sgemf	,machine_driver_cps2	,input_ports_sgemf	,init_cps2	,ROT0	,	"Capcom", "Pocket Fighter (Hispanic 970904)", GAME_NOT_WORKING )
	public static GameDriver driver_ringdest	   = new GameDriver("1994"	,"ringdest"	,"cps2.java"	,rom_ringdest,null	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Ring of Destruction: Slammasters II (Euro 940902)" )
	public static GameDriver driver_smbomb	   = new GameDriver("1994"	,"smbomb"	,"cps2.java"	,rom_smbomb,driver_ringdest	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Super Muscle Bomber: The International Blowout (Japan 940831)" )
	public static GameDriver driver_smbombr1	   = new GameDriver("1994"	,"smbombr1"	,"cps2.java"	,rom_smbombr1,driver_ringdest	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Super Muscle Bomber: The International Blowout (Japan 940808)" )
	public static GameDriver driver_spf2t	   = new GameDriver("1996"	,"spf2t"	,"cps2.java"	,rom_spf2t,null	,machine_driver_cps2	,input_ports_19xx	,init_cps2	,ROT0	,	"Capcom", "Super Puzzle Fighter 2 Turbo (US 960620)", GAME_NOT_WORKING )
	public static GameDriver driver_spf2xj	   = new GameDriver("1996"	,"spf2xj"	,"cps2.java"	,rom_spf2xj,driver_spf2t	,machine_driver_cps2	,input_ports_19xx	,init_cps2	,ROT0	,	"Capcom", "Super Puzzle Fighter 2 X (Japan 960531)" )
	public static GameDriver driver_ssf2	   = new GameDriver("1993"	,"ssf2"	,"cps2.java"	,rom_ssf2,null	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Super Street Fighter 2: The New Challengers (US 930911)" )
	public static GameDriver driver_ssf2a	   = new GameDriver("1993"	,"ssf2a"	,"cps2.java"	,rom_ssf2a,driver_ssf2	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Super Street Fighter 2: The New Challengers (Asia 930911)", GAME_NOT_WORKING )
	public static GameDriver driver_ssf2j	   = new GameDriver("1993"	,"ssf2j"	,"cps2.java"	,rom_ssf2j,driver_ssf2	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Super Street Fighter 2: The New Challengers (Japan 931005)" )
	public static GameDriver driver_ssf2jr1	   = new GameDriver("1993"	,"ssf2jr1"	,"cps2.java"	,rom_ssf2jr1,driver_ssf2	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Super Street Fighter 2: The New Challengers (Japan 930911)" )
	public static GameDriver driver_ssf2jr2	   = new GameDriver("1993"	,"ssf2jr2"	,"cps2.java"	,rom_ssf2jr2,driver_ssf2	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Super Street Fighter 2: The New Challengers (Japan 930910)" )
	public static GameDriver driver_ssf2tb	   = new GameDriver("1993"	,"ssf2tb"	,"cps2.java"	,rom_ssf2tb,driver_ssf2	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Super Street Fighter 2: The Tournament Battle (World 931119)", GAME_NOT_WORKING )
	public static GameDriver driver_ssf2tbj	   = new GameDriver("1993"	,"ssf2tbj"	,"cps2.java"	,rom_ssf2tbj,driver_ssf2	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Super Street Fighter 2: The Tournament Battle (Japan 930910)", GAME_NOT_WORKING )
	public static GameDriver driver_ssf2t	   = new GameDriver("1994"	,"ssf2t"	,"cps2.java"	,rom_ssf2t,driver_ssf2	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Super Street Fighter 2 Turbo (World 940223)" )
	public static GameDriver driver_ssf2ta	   = new GameDriver("1994"	,"ssf2ta"	,"cps2.java"	,rom_ssf2ta,driver_ssf2	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Super Street Fighter 2 Turbo (Asia 940223)" )
	public static GameDriver driver_ssf2tu	   = new GameDriver("1994"	,"ssf2tu"	,"cps2.java"	,rom_ssf2tu,driver_ssf2	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Super Street Fighter 2 Turbo (US 940223)" )
	public static GameDriver driver_ssf2xj	   = new GameDriver("1994"	,"ssf2xj"	,"cps2.java"	,rom_ssf2xj,driver_ssf2	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Super Street Fighter 2 X: Grand Master Challenge (Japan 940223)" )
	public static GameDriver driver_vhunt2	   = new GameDriver("1997"	,"vhunt2"	,"cps2.java"	,rom_vhunt2,null	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Vampire Hunter 2: Darkstalkers Revenge (Japan 970913)", GAME_NOT_WORKING )
	public static GameDriver driver_vsav	   = new GameDriver("1997"	,"vsav"	,"cps2.java"	,rom_vsav,null	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Vampire Savior: The Lord of Vampire (US 970519)" )
	public static GameDriver driver_vsavj	   = new GameDriver("1997"	,"vsavj"	,"cps2.java"	,rom_vsavj,driver_vsav	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Vampire Savior: The Lord of Vampire (Japan 970519)" )
	public static GameDriver driver_vsava	   = new GameDriver("1997"	,"vsava"	,"cps2.java"	,rom_vsava,driver_vsav	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Vampire Savior: The Lord of Vampire (Asia 970519)", GAME_NOT_WORKING )
	public static GameDriver driver_vsavh	   = new GameDriver("1997"	,"vsavh"	,"cps2.java"	,rom_vsavh,driver_vsav	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Vampire Savior: The Lord of Vampire (Hispanic 970519)", GAME_NOT_WORKING )
	public static GameDriver driver_vsav2	   = new GameDriver("1997"	,"vsav2"	,"cps2.java"	,rom_vsav2,null	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "Vampire Savior 2: The Lord of Vampire (Japan 970913)" )
	public static GameDriver driver_xmcota	   = new GameDriver("1994"	,"xmcota"	,"cps2.java"	,rom_xmcota,null	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "X-Men: Children of the Atom (Euro xxxxxx)", GAME_NOT_WORKING )
	public static GameDriver driver_xmcotau	   = new GameDriver("1994"	,"xmcotau"	,"cps2.java"	,rom_xmcotau,driver_xmcota	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "X-Men: Children of the Atom (US 950105)" )
	public static GameDriver driver_xmcotah	   = new GameDriver("1994"	,"xmcotah"	,"cps2.java"	,rom_xmcotah,driver_xmcota	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "X-Men: Children of the Atom (Hispanic 950331)", GAME_NOT_WORKING )
	public static GameDriver driver_xmcotaj	   = new GameDriver("1994"	,"xmcotaj"	,"cps2.java"	,rom_xmcotaj,driver_xmcota	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "X-Men: Children of the Atom (Japan 941219)" )
	public static GameDriver driver_xmcotaj1	   = new GameDriver("1994"	,"xmcotaj1"	,"cps2.java"	,rom_xmcotaj1,driver_xmcota	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "X-Men: Children of the Atom (Japan 941217)" )
	public static GameDriver driver_xmcotajr	   = new GameDriver("1994"	,"xmcotajr"	,"cps2.java"	,rom_xmcotajr,driver_xmcota	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "X-Men: Children of the Atom (Japan 941208 rent version)" )
	public static GameDriver driver_xmvsf	   = new GameDriver("1996"	,"xmvsf"	,"cps2.java"	,rom_xmvsf,null	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "X-Men Vs. Street Fighter (Euro 960910)", GAME_NOT_WORKING )
	public static GameDriver driver_xmvsfu	   = new GameDriver("1996"	,"xmvsfu"	,"cps2.java"	,rom_xmvsfu,driver_xmvsf	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "X-Men Vs. Street Fighter (US 961004)" )
	public static GameDriver driver_xmvsfj	   = new GameDriver("1996"	,"xmvsfj"	,"cps2.java"	,rom_xmvsfj,driver_xmvsf	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "X-Men Vs. Street Fighter (Japan 960910)", GAME_NOT_WORKING )
	public static GameDriver driver_xmvsfa	   = new GameDriver("1996"	,"xmvsfa"	,"cps2.java"	,rom_xmvsfa,driver_xmvsf	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "X-Men Vs. Street Fighter (Asia 961023)", GAME_NOT_WORKING )
	public static GameDriver driver_xmvsfh	   = new GameDriver("1996"	,"xmvsfh"	,"cps2.java"	,rom_xmvsfh,driver_xmvsf	,machine_driver_cps2	,input_ports_ssf2	,init_cps2	,ROT0	,	"Capcom", "X-Men Vs. Street Fighter (Hispanic 961004)", GAME_NOT_WORKING )
}
