/***************************************************************************

	Pipe Dream

    driver by Bryan McPhail & Aaron Giles

****************************************************************************

	Memory map

****************************************************************************

	========================================================================
	MAIN CPU
	========================================================================
	0000-7FFF   R     xxxxxxxx   Program ROM
	8000-9FFF   R/W   xxxxxxxx   Program RAM
	A000-BFFF   R     xxxxxxxx   Banked ROM
	C000-CBFF   R/W   xxxxxxxx   Palette RAM (1536 entries x 2 bytes)
	            R/W   ---xxxxx      (0: Blue)
	            R/W   xxx-----      (0: Green, 3 LSB)
	            R/W   ------xx      (1: Green, 2 MSB)
	            R/W   -xxxxx--      (1: Red)
	CC00-CFFF   R/W   xxxxxxxx   Sprite RAM (256 entries x 8 bytes)
	            R/W   xxxxxxxx      (0: Y position, 8 LSB)
	            R/W   -------x      (1: Y position, 1 MSB)
	            R/W   xxxx----      (1: Y zoom factor)
	            R/W   xxxxxxxx      (2: X position, 8 LSB)
	            R/W   -------x      (3: X position, 1 MSB)
	            R/W   xxxx----      (3: X zoom factor)
	            R/W   ---x----      (4: Priority)
	            R/W   ----xxxx      (4: Palette entry)
	            R/W   x-------      (5: Y flip)
	            R/W   -xxx----      (5: Number of Y tiles - 1)
	            R/W   ----x---      (5: X flip)
	            R/W   -----xxx      (5: Number of X tiles - 1)
	            R/W   xxxxxxxx      (6: Starting tile index, 8 LSB)
	            R/W   ----xxxx      (7: Starting tile index, 4 MSB)
	D000-DFFF   R/W   --xxxxxx   Background tile color
	E000-EFFF   R/W   xxxxxxxx   Background tile index, 8 MSB
	F000-FFFF   R/W   xxxxxxxx   Background tile index, 8 LSB
	========================================================================
    0020        R     xxxxxxxx   Player 1 controls
                R     --x-----      (Fast button)
                R     ---x----      (Place button)
                R     ----xxxx      (Joystick RLDU)
    0020          W   xxxxxxxx   Sound command
    0021        R     xxxxxxxx   Player 2 controls
                R     --x-----      (Fast button)
                R     ---x----      (Place button)
                R     ----xxxx      (Joystick RLDU)
    0021          W   -xxxxxxx   Bankswitch/video control
                  W   -x------      (Flip screen)
                  W   --x-----      (Background 2 X scroll, 1 MSB)
                  W   ---x----      (Background 1 X scroll, 1 MSB)
                  W   ----x---      (Background videoram select)
                  W   -----xxx      (Bank ROM select)
    0022        R     xxxxxxxx   Coinage DIP switch
                R     xxxx----      (Coin B)
                R     ----xxxx      (Coin A)
    0022          W   xxxxxxxx   Background 1 X scroll, 8 LSB
    0023        R     xxxxxxxx   Game options DIP switch
                R     x-------      (Test switch)
                R     -x------      (Training mode enable)
                R     --x-----      (Flip screen)
                R     ---x----      (Demo sounds)
                R     ----xx--      (Lives)
                R     ------xx      (Difficulty)
    0023          W   xxxxxxxx   Background 1 Y scroll
    0024        R     -x--xxxx   Coinage/start
                R     -x------      (Service coin)
                R     ----x---      (2 player start)
                R     -----x--      (1 player start)
                R     ------x-      (Coin B)
                R     -------x      (Coin A)
    0024          W   xxxxxxxx   Background 2 X scroll, 8 LSB
    0025        R     -------x   Sound command pending
    0025          W   xxxxxxxx   Background 2 Y scroll
	========================================================================
	Interrupts:
	   INT generated by CRTC VBLANK
	========================================================================


	========================================================================
	SOUND CPU
	========================================================================
	0000-77FF   R     xxxxxxxx   Program ROM
	7800-7FFF   R/W   xxxxxxxx   Program RAM
	8000-FFFF   R     xxxxxxxx   Banked ROM
	========================================================================
	0004          W   -------x   Bank ROM select
	0016        R     xxxxxxxx   Sound command read
	0017          W   --------   Sound command acknowledge
	0018-0019   R/W   xxxxxxxx   YM2610 port A
	001A-001B   R/W   xxxxxxxx   YM2610 port B
	========================================================================
	Interrupts:
	   INT generated by YM2610
	   NMI generated by command from main CPU
	========================================================================

***************************************************************************/


/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package drivers;

public class pipedrm
{
	
	
	static UINT8 pending_command;
	static UINT8 sound_command;
	
	
	
	/*************************************
	 *
	 *	Initialization & bankswitching
	 *
	 *************************************/
	
	static void init_machine(void)
	{
		UINT8 *ram;
	
		/* initialize main Z80 bank */
		ram = memory_region(REGION_CPU1);
		cpu_setbank(1, &ram[0x10000]);
	
		/* initialize sound bank */
		ram = memory_region(REGION_CPU2);
		cpu_setbank(2, &ram[0x10000]);
	}
	
	
	static WRITE_HANDLER( pipedrm_bankswitch_w )
	{
		/*
			Bit layout:
	
			D7 = unknown
			D6 = flip screen
			D5 = background 2 X scroll MSB
			D4 = background 1 X scroll MSB
			D3 = background videoram select
			D2-D0 = program ROM bank select
		*/
	
		/* set the memory bank on the Z80 using the low 3 bits */
		UINT8 *ram = memory_region(REGION_CPU1);
		cpu_setbank(1, &ram[0x10000 + (data & 0x7) * 0x2000]);
	
		/* map to the fromance gfx register */
		fromance_gfxreg_w(offset, ((data >> 6) & 0x01) | 	/* flipscreen */
								  ((~data >> 2) & 0x02));	/* videoram select */
	}
	
	
	static WRITE_HANDLER( sound_bankswitch_w )
	{
		UINT8 *ram = memory_region(REGION_CPU2);
		cpu_setbank(2, &ram[0x10000 + (data & 0x01) * 0x8000]);
	}
	
	
	
	/*************************************
	 *
	 *	Sound CPU I/O
	 *
	 *************************************/
	
	static void delayed_command_w(int data)
	{
		sound_command = data & 0xff;
		pending_command = 1;
	
		/* Hatris polls commands *and* listens to the NMI; this causes it to miss */
		/* sound commands. It's possible the NMI isn't really hooked up on the YM2608 */
		/* sound board. */
		if (data & 0x100)
			cpu_set_nmi_line(1, ASSERT_LINE);
	}
	
	
	static WRITE_HANDLER( sound_command_w )
	{
		timer_set(TIME_NOW, data | 0x100, delayed_command_w);
	}
	
	
	static WRITE_HANDLER( sound_command_nonmi_w )
	{
		timer_set(TIME_NOW, data, delayed_command_w);
	}
	
	
	static WRITE_HANDLER( pending_command_clear_w )
	{
		pending_command = 0;
		cpu_set_nmi_line(1, CLEAR_LINE);
	}
	
	
	static READ_HANDLER( pending_command_r )
	{
		return pending_command;
	}
	
	
	static READ_HANDLER( sound_command_r )
	{
		return sound_command;
	}
	
	
	
	/*************************************
	 *
	 *	Main CPU memory handlers
	 *
	 *************************************/
	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0x9fff, MRA_RAM ),
		new Memory_ReadAddress( 0xa000, 0xbfff, MRA_BANK1 ),
		new Memory_ReadAddress( 0xc000, 0xcfff, MRA_RAM ),
		new Memory_ReadAddress( 0xd000, 0xffff, fromance_videoram_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0x8000, 0x9fff, MWA_RAM ),
		new Memory_WriteAddress( 0xa000, 0xbfff, MWA_ROM ),
		new Memory_WriteAddress( 0xc000, 0xcfff, paletteram_xRRRRRGGGGGBBBBB_w, &paletteram ),
		new Memory_WriteAddress( 0xd000, 0xffff, fromance_videoram_w, &videoram, &videoram_size ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	public static IO_ReadPort readport[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_ReadPort( 0x20, 0x20, input_port_0_r ),
		new IO_ReadPort( 0x21, 0x21, input_port_1_r ),
		new IO_ReadPort( 0x22, 0x22, input_port_2_r ),
		new IO_ReadPort( 0x23, 0x23, input_port_3_r ),
		new IO_ReadPort( 0x24, 0x24, input_port_4_r ),
		new IO_ReadPort( 0x25, 0x25, pending_command_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	
	
	public static IO_WritePort writeport[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_WritePort( 0x20, 0x20, sound_command_w ),
		new IO_WritePort( 0x21, 0x21, pipedrm_bankswitch_w ),
		new IO_WritePort( 0x22, 0x25, fromance_scroll_w ),
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	
	
	/*************************************
	 *
	 *	Sound CPU memory handlers
	 *
	 *************************************/
	
	public static Memory_ReadAddress sound_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x77ff, MRA_ROM ),
		new Memory_ReadAddress( 0x7800, 0x7fff, MRA_RAM ),
		new Memory_ReadAddress( 0x8000, 0xffff, MRA_BANK2 ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	
	public static Memory_WriteAddress sound_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x77ff, MWA_ROM ),
		new Memory_WriteAddress( 0x7800, 0x7fff, MWA_RAM ),
		new Memory_WriteAddress( 0x8000, 0xffff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	public static IO_ReadPort sound_readport[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_ReadPort( 0x16, 0x16, sound_command_r ),
		new IO_ReadPort( 0x18, 0x18, YM2610_status_port_0_A_r ),
		new IO_ReadPort( 0x1a, 0x1a, YM2610_status_port_0_B_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	
	
	public static IO_WritePort sound_writeport[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_WritePort( 0x04, 0x04, sound_bankswitch_w ),
		new IO_WritePort( 0x17, 0x17, pending_command_clear_w ),
		new IO_WritePort( 0x18, 0x18, YM2610_control_port_0_A_w ),
		new IO_WritePort( 0x19, 0x19, YM2610_data_port_0_A_w ),
		new IO_WritePort( 0x1a, 0x1a, YM2610_control_port_0_B_w ),
		new IO_WritePort( 0x1b, 0x1b, YM2610_data_port_0_B_w ),
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	
	public static IO_ReadPort hatris_sound_readport[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_ReadPort( 0x04, 0x04, sound_command_r ),
		new IO_ReadPort( 0x05, 0x05, pending_command_r ),
		new IO_ReadPort( 0x08, 0x08, YM2608_status_port_0_A_r ),
		new IO_ReadPort( 0x0a, 0x0a, YM2608_status_port_0_B_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	
	
	public static IO_WritePort hatris_sound_writeport[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_WritePort( 0x02, 0x02, YM2608_control_port_0_B_w ),
		new IO_WritePort( 0x03, 0x03, YM2608_data_port_0_B_w ),
		new IO_WritePort( 0x05, 0x05, pending_command_clear_w ),
		new IO_WritePort( 0x08, 0x08, YM2608_control_port_0_A_w ),
		new IO_WritePort( 0x09, 0x09, YM2608_data_port_0_A_w ),
		new IO_WritePort( 0x0a, 0x0a, YM2608_control_port_0_B_w ),
		new IO_WritePort( 0x0b, 0x0b, YM2608_data_port_0_B_w ),
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	
	
	/*************************************
	 *
	 *	Port definitions
	 *
	 *************************************/
	
	INPUT_PORTS_START( pipedrm )
		PORT_START	/* $20 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER1 )
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER1 )
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER1 )
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER1 )
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 )
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN )
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN )
	
		PORT_START	/* $21 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 )
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 )
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 )
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 )
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 )
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN )
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN )
	
		PORT_START	/* $22 */
		PORT_DIPNAME( 0x0f, 0x0f, DEF_STR( Coin_A ))
		PORT_DIPSETTING(    0x06, DEF_STR( 5C_1C ))
		PORT_DIPSETTING(    0x07, DEF_STR( 4C_1C ))
		PORT_DIPSETTING(    0x08, DEF_STR( 3C_1C ))
		PORT_DIPSETTING(    0x09, DEF_STR( 2C_1C ))
		PORT_DIPSETTING(    0x04, "6 Coins/4 Credits" )
		PORT_DIPSETTING(    0x03, DEF_STR( 4C_3C ))
		PORT_DIPSETTING(    0x0f, DEF_STR( 1C_1C ))
		PORT_DIPSETTING(    0x02, "5 Coins/6 Credits" )
		PORT_DIPSETTING(    0x01, DEF_STR( 4C_5C ))
		PORT_DIPSETTING(    0x00, DEF_STR( 2C_3C ))
	//	PORT_DIPSETTING(    0x05, DEF_STR( 2C_3C ))
		PORT_DIPSETTING(    0x0e, DEF_STR( 1C_2C ))
		PORT_DIPSETTING(    0x0d, DEF_STR( 1C_3C ))
		PORT_DIPSETTING(    0x0c, DEF_STR( 1C_4C ))
		PORT_DIPSETTING(    0x0b, DEF_STR( 1C_5C ))
		PORT_DIPSETTING(    0x0a, DEF_STR( 1C_6C ))
		PORT_DIPNAME( 0xf0, 0xf0, DEF_STR( Coin_B ))
		PORT_DIPSETTING(    0x60, DEF_STR( 5C_1C ))
		PORT_DIPSETTING(    0x70, DEF_STR( 4C_1C ))
		PORT_DIPSETTING(    0x80, DEF_STR( 3C_1C ))
		PORT_DIPSETTING(    0x90, DEF_STR( 2C_1C ))
		PORT_DIPSETTING(    0x40, "6 Coins/4 Credits" )
		PORT_DIPSETTING(    0x30, DEF_STR( 4C_3C ))
		PORT_DIPSETTING(    0xf0, DEF_STR( 1C_1C ))
		PORT_DIPSETTING(    0x20, "5 Coins/6 Credits" )
		PORT_DIPSETTING(    0x10, DEF_STR( 4C_5C ))
		PORT_DIPSETTING(    0x00, DEF_STR( 2C_3C ))
	//	PORT_DIPSETTING(    0x50, DEF_STR( 2C_3C ))
		PORT_DIPSETTING(    0xe0, DEF_STR( 1C_2C ))
		PORT_DIPSETTING(    0xd0, DEF_STR( 1C_3C ))
		PORT_DIPSETTING(    0xc0, DEF_STR( 1C_4C ))
		PORT_DIPSETTING(    0xb0, DEF_STR( 1C_5C ))
		PORT_DIPSETTING(    0xa0, DEF_STR( 1C_6C ))
	
		PORT_START	/* $23 */
		PORT_DIPNAME( 0x03, 0x03, DEF_STR( Difficulty ))
		PORT_DIPSETTING(    0x02, "Easy" )
		PORT_DIPSETTING(    0x03, "Normal" )
		PORT_DIPSETTING(    0x01, "Hard" )
		PORT_DIPSETTING(    0x00, "Super" )
		PORT_DIPNAME( 0x0c, 0x04, DEF_STR( Lives ))
		PORT_DIPSETTING(    0x0c, "1" )
		PORT_DIPSETTING(    0x08, "2" )
		PORT_DIPSETTING(    0x04, "3" )
		PORT_DIPSETTING(    0x00, "4" )
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( Demo_Sounds ))
		PORT_DIPSETTING(    0x00, DEF_STR( Off ))
		PORT_DIPSETTING(    0x10, DEF_STR( On ))
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( Flip_Screen ))
		PORT_DIPSETTING(    0x20, DEF_STR( Off ))
		PORT_DIPSETTING(    0x00, DEF_STR( On ))
		PORT_DIPNAME( 0x40, 0x40, "Training Mode" )
		PORT_DIPSETTING(    0x00, DEF_STR( Off ))
		PORT_DIPSETTING(    0x40, DEF_STR( On ))
		PORT_SERVICE( 0x80, IP_ACTIVE_LOW )
	
		PORT_START	/* $24 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 )
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 )
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_START1 )
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START2 )
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN )
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_SERVICE1 )
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN )
	INPUT_PORTS_END
	
	
	INPUT_PORTS_START( hatris )
		PORT_START	/* $20 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER1 )
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER1 )
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER1 )
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER1 )
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 )
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 )
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN )
	
		PORT_START	/* $21 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 )
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 )
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 )
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 )
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 )
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 )
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN )
	
		PORT_START	/* $22 */
		PORT_DIPNAME( 0x0f, 0x00, DEF_STR( Coin_A ))
		PORT_DIPSETTING(    0x09, DEF_STR( 5C_1C ))
		PORT_DIPSETTING(    0x08, DEF_STR( 4C_1C ))
		PORT_DIPSETTING(    0x07, DEF_STR( 3C_1C ))
		PORT_DIPSETTING(    0x06, DEF_STR( 2C_1C ))
		PORT_DIPSETTING(    0x0b, "6 Coins/4 Credits" )
		PORT_DIPSETTING(    0x0c, DEF_STR( 4C_3C ))
		PORT_DIPSETTING(    0x00, DEF_STR( 1C_1C ))
		PORT_DIPSETTING(    0x0d, "5 Coins/6 Credits" )
		PORT_DIPSETTING(    0x0e, DEF_STR( 4C_5C ))
		PORT_DIPSETTING(    0x0f, DEF_STR( 2C_3C ))
	//	PORT_DIPSETTING(    0x0a, DEF_STR( 2C_3C ))
		PORT_DIPSETTING(    0x01, DEF_STR( 1C_2C ))
		PORT_DIPSETTING(    0x02, DEF_STR( 1C_3C ))
		PORT_DIPSETTING(    0x03, DEF_STR( 1C_4C ))
		PORT_DIPSETTING(    0x04, DEF_STR( 1C_5C ))
		PORT_DIPSETTING(    0x05, DEF_STR( 1C_6C ))
		PORT_DIPNAME( 0xf0, 0x00, DEF_STR( Coin_B ))
		PORT_DIPSETTING(    0x90, DEF_STR( 5C_1C ))
		PORT_DIPSETTING(    0x80, DEF_STR( 4C_1C ))
		PORT_DIPSETTING(    0x70, DEF_STR( 3C_1C ))
		PORT_DIPSETTING(    0x60, DEF_STR( 2C_1C ))
		PORT_DIPSETTING(    0xb0, "6 Coins/4 Credits" )
		PORT_DIPSETTING(    0xc0, DEF_STR( 4C_3C ))
		PORT_DIPSETTING(    0x00, DEF_STR( 1C_1C ))
		PORT_DIPSETTING(    0xd0, "5 Coins/6 Credits" )
		PORT_DIPSETTING(    0xe0, DEF_STR( 4C_5C ))
		PORT_DIPSETTING(    0xf0, DEF_STR( 2C_3C ))
	//	PORT_DIPSETTING(    0xa0, DEF_STR( 2C_3C ))
		PORT_DIPSETTING(    0x10, DEF_STR( 1C_2C ))
		PORT_DIPSETTING(    0x20, DEF_STR( 1C_3C ))
		PORT_DIPSETTING(    0x30, DEF_STR( 1C_4C ))
		PORT_DIPSETTING(    0x40, DEF_STR( 1C_5C ))
		PORT_DIPSETTING(    0x50, DEF_STR( 1C_6C ))
	
		PORT_START	/* $23 */
		PORT_DIPNAME( 0x03, 0x00, "Difficulty 1" )
		PORT_DIPSETTING(    0x01, "Easy" )
		PORT_DIPSETTING(    0x00, "Normal" )
		PORT_DIPSETTING(    0x02, "Hard" )
		PORT_DIPSETTING(    0x03, "Super" )
		PORT_DIPNAME( 0x0c, 0x00, "Difficulty 2" )
		PORT_DIPSETTING(    0x04, "Easy" )
		PORT_DIPSETTING(    0x00, "Normal" )
		PORT_DIPSETTING(    0x08, "Hard" )
		PORT_DIPSETTING(    0x0c, "Super" )
		PORT_SERVICE( 0x10, IP_ACTIVE_HIGH )
		PORT_DIPNAME( 0x20, 0x00, DEF_STR( Flip_Screen ))
		PORT_DIPSETTING(    0x00, DEF_STR( Off ))
		PORT_DIPSETTING(    0x20, DEF_STR( On ))
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( Demo_Sounds ))
		PORT_DIPSETTING(    0x40, DEF_STR( Off ))
		PORT_DIPSETTING(    0x00, DEF_STR( On ))
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED )
	
		PORT_START	/* $24 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 )
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 )
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_START1 )
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START2 )
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNUSED )
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNUSED )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_SERVICE1 )
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED )
	INPUT_PORTS_END
	
	
	
	/*************************************
	 *
	 *	Graphics definitions
	 *
	 *************************************/
	
	static struct GfxLayout bglayout =
	{
		8,4,
		RGN_FRAC(1,1),
		4,
		{ 0, 1, 2, 3 },
		{ 4, 0, 12, 8, 20, 16, 28, 24 },
		{ 0*32, 1*32, 2*32, 3*32 },
		8*16
	};
	
	
	static struct GfxLayout splayout =
	{
		16,16,
		RGN_FRAC(1,1),
		4,
		{ 0, 1, 2, 3 },
		{ 12, 8, 28, 24, 4, 0, 20, 16, 44, 40, 60, 56, 36, 32, 52, 48 },
		{ 0*64, 1*64, 2*64, 3*64, 4*64, 5*64, 6*64, 7*64,
				8*64, 9*64, 10*64, 11*64, 12*64, 13*64, 14*64, 15*64 },
		8*128
	};
	
	
	static struct GfxDecodeInfo gfxdecodeinfo[] =
	{
		{ REGION_GFX1, 0, &bglayout,    0, 64 },
		{ REGION_GFX2, 0, &bglayout,    0, 64 },
		{ REGION_GFX3, 0, &splayout, 1024, 32 },
		{ -1 } /* end of array */
	};
	
	
	static struct GfxDecodeInfo gfxdecodeinfo_hatris[] =
	{
		{ REGION_GFX1, 0, &bglayout,    0, 128 },
		{ REGION_GFX2, 0, &bglayout,    0, 128 },
		{ -1 } /* end of array */
	};
	
	
	
	/*************************************
	 *
	 *	Sound definitions
	 *
	 *************************************/
	
	static void irqhandler(int irq)
	{
		cpu_set_irq_line(1, 0, irq ? ASSERT_LINE : CLEAR_LINE);
	}
	
	
	static struct YM2608interface ym2608_interface =
	{
		1,
		8000000,	/* 8 MHz */
		{ 50 },
		{ 0 },
		{ 0 },
		{ 0 },
		{ 0 },
		{ irqhandler },
		{ REGION_SOUND1 },
		{ YM3012_VOL(50,MIXER_PAN_LEFT,50,MIXER_PAN_RIGHT) }
	};
	
	
	static struct YM2610interface ym2610_interface =
	{
		1,
		8000000,	/* 8 MHz */
		{ 50 },
		{ 0 },
		{ 0 },
		{ 0 },
		{ 0 },
		{ irqhandler },
		{ REGION_SOUND1 },
		{ REGION_SOUND2 },
		{ YM3012_VOL(100,MIXER_PAN_LEFT,100,MIXER_PAN_RIGHT) }
	};
	
	
	
	/*************************************
	 *
	 *	Machine driver
	 *
	 *************************************/
	
	static const struct MachineDriver machine_driver_pipedrm =
	{
		/* basic machine hardware */
		{
			{
				CPU_Z80,
				12000000/2,
				readmem,writemem,readport,writeport,
				interrupt,1
			},
			{
				CPU_Z80 | CPU_AUDIO_CPU,
				14318000/4,
				sound_readmem,sound_writemem,sound_readport,sound_writeport,
				ignore_interrupt,0
			}
		},
		60, DEFAULT_REAL_60HZ_VBLANK_DURATION,
		1,
		init_machine,
	
		/* video hardware */
	  	44*8, 30*8, { 0*8, 44*8-1, 0*8, 30*8-1 },
		gfxdecodeinfo,
		2048, 0,
		0,
	
		VIDEO_TYPE_RASTER,
		0,
		fromance_vh_start,
		fromance_vh_stop,
		pipedrm_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		{
			{ SOUND_YM2610, &ym2610_interface }
		}
	};
	
	
	static const struct MachineDriver machine_driver_hatris =
	{
		/* basic machine hardware */
		{
			{
				CPU_Z80,
				12000000/2,
				readmem,writemem,readport,writeport,
				interrupt,1
			},
			{
				CPU_Z80 | CPU_AUDIO_CPU,
				14318000/4,
				sound_readmem,sound_writemem,hatris_sound_readport,hatris_sound_writeport,
				ignore_interrupt,0
			}
		},
		60, DEFAULT_REAL_60HZ_VBLANK_DURATION,
		1,
		init_machine,
	
		/* video hardware */
	  	44*8, 30*8, { 0*8, 44*8-1, 0*8, 30*8-1 },
		gfxdecodeinfo_hatris,
		2048, 0,
		0,
	
		VIDEO_TYPE_RASTER,
		0,
		fromance_vh_start,
		fromance_vh_stop,
		fromance_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		{
			{ SOUND_YM2608, &ym2608_interface }
		}
	};
	
	
	
	/*************************************
	 *
	 *	ROM definitions
	 *
	 *************************************/
	
	ROM_START( pipedrm )
		ROM_REGION( 0x20000, REGION_CPU1, 0 )
		ROM_LOAD( "01.u12",	0x00000, 0x08000, 0x9fe261fb )
		ROM_LOAD( "02.u11",	0x10000, 0x10000, 0xc8209b67 )
	
		ROM_REGION( 0x20000, REGION_CPU2, 0 )
		ROM_LOAD( "4",	0x00000, 0x08000, 0x497fad4c )
		ROM_LOAD( "3",	0x10000, 0x10000, 0x4800322a )
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE )
		ROM_LOAD( "s73",    0x000000, 0x80000, 0x63f4e10c )
		ROM_LOAD( "s72",    0x080000, 0x80000, 0x4e669e97 )
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE )
		ROM_LOAD( "s71",    0x000000, 0x80000, 0x431485ee )
		ROM_COPY( REGION_GFX1, 0x080000, 0x080000, 0x80000 )
	
		ROM_REGION( 0x080000, REGION_GFX3, ROMREGION_DISPOSE )
		ROM_LOAD16_BYTE( "a30", 0x00000, 0x40000, 0x50bc5e98 )
		ROM_LOAD16_BYTE( "a29", 0x00001, 0x40000, 0xa240a448 )
	
		ROM_REGION( 0x80000, REGION_SOUND1, 0 )
		ROM_LOAD( "g72",     0x00000, 0x80000, 0xdc3d14be )
	
		ROM_REGION( 0x80000, REGION_SOUND2, 0 )
		ROM_LOAD( "g71",     0x00000, 0x80000, 0x488e2fd1 )
	ROM_END
	
	
	ROM_START( pipedrmj )
		ROM_REGION( 0x20000, REGION_CPU1, 0 )
		ROM_LOAD( "1",	0x00000, 0x08000, 0xdbfac46b )
		ROM_LOAD( "2",	0x10000, 0x10000, 0xb7adb99a )
	
		ROM_REGION( 0x20000, REGION_CPU2, 0 )
		ROM_LOAD( "4",	0x00000, 0x08000, 0x497fad4c )
		ROM_LOAD( "3",	0x10000, 0x10000, 0x4800322a )
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE )
		ROM_LOAD( "s73",    0x000000, 0x80000, 0x63f4e10c )
		ROM_LOAD( "s72",    0x080000, 0x80000, 0x4e669e97 )
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE )
		ROM_LOAD( "s71",    0x000000, 0x80000, 0x431485ee )
		ROM_COPY( REGION_GFX1, 0x080000, 0x080000, 0x80000 )
	
		ROM_REGION( 0x080000, REGION_GFX3, ROMREGION_DISPOSE )
		ROM_LOAD16_BYTE( "a30", 0x00000, 0x40000, 0x50bc5e98 )
		ROM_LOAD16_BYTE( "a29", 0x00001, 0x40000, 0xa240a448 )
	
		ROM_REGION( 0x80000, REGION_SOUND1, 0 )
		ROM_LOAD( "g72",     0x00000, 0x80000, 0xdc3d14be )
	
		ROM_REGION( 0x80000, REGION_SOUND2, 0 )
		ROM_LOAD( "g71",     0x00000, 0x80000, 0x488e2fd1 )
	ROM_END
	
	
	ROM_START( hatris )
		ROM_REGION( 0x10000, REGION_CPU1, 0 )
		ROM_LOAD( "2-ic79.bin",	0x00000, 0x08000, 0xbbcaddbf )
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 )
		ROM_LOAD( "1-ic81.bin",	0x00000, 0x08000, 0xdb25e166 )
	
		ROM_REGION( 0x80000, REGION_GFX1, ROMREGION_DISPOSE )
		ROM_LOAD( "b0-ic56.bin", 0x00000, 0x20000, 0x34f337a4 )
		ROM_FILL(                0x20000, 0x20000, 0 )
		ROM_LOAD( "b1-ic73.bin", 0x40000, 0x08000, 0x6351d0ba )
		ROM_FILL(                0x48000, 0x18000, 0 )
	
		ROM_REGION( 0x40000, REGION_GFX2, ROMREGION_DISPOSE )
		ROM_LOAD( "a0-ic55.bin", 0x00000, 0x20000, 0x7b7bc619 )
		ROM_LOAD( "a1-ic60.bin", 0x20000, 0x20000, 0xf74d4168 )
	
		ROM_REGION( 0x20000, REGION_SOUND1, 0 )
		ROM_LOAD( "pc-ic53.bin", 0x00000, 0x20000, 0x07147712 )
	ROM_END
	
	
	
	/*************************************
	 *
	 *	Driver initialization
	 *
	 *************************************/
	
	static void init_pipedrm(void)
	{
		/* sprite RAM lives at the end of palette RAM */
		spriteram = install_mem_read_handler(0, 0xcc00, 0xcfff, MRA_RAM);
		spriteram = install_mem_write_handler(0, 0xcc00, 0xcfff, MWA_RAM);
		spriteram_size = 0x400;
	}
	
	
	static void init_hatris(void)
	{
		install_port_write_handler(0, 0x20, 0x20, sound_command_nonmi_w);
		install_port_write_handler(0, 0x21, 0x21, fromance_gfxreg_w);
	}
	
	
	
	/*************************************
	 *
	 *	Game drivers
	 *
	 *************************************/
	
	GAME( 1990, pipedrm,  0,       pipedrm, pipedrm, pipedrm, ROT0, "Video System Co.", "Pipe Dream (US)" )
	GAME( 1990, pipedrmj, pipedrm, pipedrm, pipedrm, pipedrm, ROT0, "Video System Co.", "Pipe Dream (Japan)" )
	GAME( 1990, hatris,   0,       hatris,  hatris,  hatris,  ROT0, "Video System Co.", "Hatris (Japan)" )
}
