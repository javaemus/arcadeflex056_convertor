/***************************************************************************

  Funky Jet                               (c) 1992 Mitchell Corporation
  Sotsugyo Shousho	                      (c) 1995 Mitchell Corporation

  But actually a Data East pcb...  Hardware is pretty close to Super Burger
  Time but with a different graphics chip.

  Emulation by Bryan McPhail, mish@tendril.co.uk

TODO:
- The protection chip which isn't fully worked out yet, stage selection in funkyjet
  doesn't work.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package drivers;

public class funkyjet
{
	
	/******************************************************************************/
	
	static data16_t loopback[0x400];
	
	static WRITE16_HANDLER( funkyjet_protection16_w )
	{
		COMBINE_DATA(&loopback[offset]);
	
		if (offset != (0x500 >> 1) && offset != (0x70e >> 1) && offset != (0x70e >> 1) &&
			offset != (0x100 >> 1) && offset != (0x102 >> 1) && offset != (0x502 >> 1) &&
			offset != (0x702 >> 1) && offset != (0x50c >> 1) && offset != (0x700 >> 1) &&
			offset != (0x10a >> 1) && offset != (0x300 >> 1) && offset != (0x304 >> 1) &&
			offset != (0x58c >> 1) && offset != (0x18e >> 1) && offset != (0x304 >> 1) &&
			offset != (0x78e >> 1))
	
			logerror("CPU #0 PC %06x: warning - write unmapped control address %06x %04x\n",cpu_get_pc(),offset<<1,data);
	
		if (offset == (0x10a >> 1)) {
			soundlatch_w(0,data&0xff);
			cpu_cause_interrupt(1,H6280_INT_IRQ1);
		}
	}
	
	/* Protection/IO chip 74 */
	static READ16_HANDLER( funkyjet_protection16_r )
	{
		switch (offset)
		{
			case 0x11e >> 1:
				return loopback[0x500>>1];
	
			case 0x148 >> 1: /* EOR mask for joysticks */
				return loopback[0x70e>>1];
	
			case 0x1da >> 1:
				return loopback[0x100>>1];
			case 0x226 >> 1:
				return loopback[0x58c>>1];
			case 0x24c >> 1:
				return loopback[0x78e>>1];
			case 0x250 >> 1:
				return loopback[0x304>>1];
			case 0x2d4 >> 1:
				return loopback[0x102>>1];
	
			case 0x2d8 >> 1: /* EOR mask for credits */
				return loopback[0x502>>1];
	
			case 0x4e4 >> 1:
				return loopback[0x702>>1];
			case 0x562 >> 1:
				return loopback[0x18e>>1];
			case 0x56c >> 1:
				return loopback[0x50c>>1];
	
			case 0x688 >> 1: //corrupt?
				return loopback[0x300>>1];
			case 0x788 >> 1:
				return loopback[0x700>>1];
			case 0x7d4 >> 1: //unchecked?? nop in bootleg
				return loopback[0x7da>>1];
	
	
	//		case 0x5be >> 1: //guess
	//			return loopback[0x506>>1];
	//		case 0x5ca >> 1://guess
	//			return loopback[0x302>>1];
			case 0x5be >> 1: //guess
			case 0x5ca >> 1://guess
				return 1;
	
			case 0x00c >> 1: /* Player 1 & Player 2 joysticks & fire buttons */
				return (readinputport(0) + (readinputport(1) << 8));
			case 0x778 >> 1: /* Credits */
				return readinputport(2);
			case 0x382 >> 1: /* DIPS */
				return (readinputport(3) + (readinputport(4) << 8));
		}
	
	//	if (cpu_get_pc()!=0xc0ea)	logerror("CPU #0 PC %06x: warning - read unmapped control address %06x\n",cpu_get_pc(),offset<<1);
	
		return 0;
	}
	
	/******************************************************************************/
	
	static MEMORY_READ16_START( funkyjet_readmem )
		{ 0x000000, 0x07ffff, MRA16_ROM },
		{ 0x120000, 0x1207ff, MRA16_RAM },
		{ 0x140000, 0x143fff, MRA16_RAM },
		{ 0x160000, 0x1607ff, MRA16_RAM },
		{ 0x180000, 0x1807ff, funkyjet_protection16_r },
		{ 0x320000, 0x321fff, MRA16_RAM },
		{ 0x322000, 0x323fff, MRA16_RAM },
		{ 0x340000, 0x340bff, MRA16_RAM },
		{ 0x342000, 0x342bff, MRA16_RAM },
	MEMORY_END
	
	static MEMORY_WRITE16_START( funkyjet_writemem )
		{ 0x000000, 0x07ffff, MWA16_ROM },
		{ 0x120000, 0x1207ff, paletteram16_xxxxBBBBGGGGRRRR_word_w, &paletteram16 },
		{ 0x140000, 0x143fff, MWA16_RAM },
		{ 0x160000, 0x1607ff, MWA16_RAM, &spriteram16 },
		{ 0x180000, 0x1807ff, funkyjet_protection16_w },
		{ 0x184000, 0x184001, MWA16_NOP },
		{ 0x188000, 0x188001, MWA16_NOP },
		{ 0x300000, 0x30000f, funkyjet_control_0_w },
		{ 0x320000, 0x321fff, funkyjet_pf1_data_w, &funkyjet_pf1_data },
		{ 0x322000, 0x323fff, funkyjet_pf2_data_w, &funkyjet_pf2_data },
		{ 0x340000, 0x340bff, MWA16_RAM, &funkyjet_pf1_row },
		{ 0x342000, 0x342bff, MWA16_RAM }, /* pf2 rowscroll - unused? */
	MEMORY_END
	
	/******************************************************************************/
	
	public static WriteHandlerPtr YM2151_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		switch (offset)
		{
		case 0:
			YM2151_register_port_0_w(0,data);
			break;
		case 1:
			YM2151_data_port_0_w(0,data);
			break;
		}
	} };
	
	/* Physical memory map (21 bits) */
	public static Memory_ReadAddress sound_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x000000, 0x00ffff, MRA_ROM ),
		new Memory_ReadAddress( 0x100000, 0x100001, MRA_NOP ),
		new Memory_ReadAddress( 0x110000, 0x110001, YM2151_status_port_0_r ),
		new Memory_ReadAddress( 0x120000, 0x120001, OKIM6295_status_0_r ),
		new Memory_ReadAddress( 0x130000, 0x130001, MRA_NOP ), /* This board only has 1 oki chip */
		new Memory_ReadAddress( 0x140000, 0x140001, soundlatch_r ),
		new Memory_ReadAddress( 0x1f0000, 0x1f1fff, MRA_BANK8 ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress sound_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x000000, 0x00ffff, MWA_ROM ),
		new Memory_WriteAddress( 0x100000, 0x100001, MWA_NOP ), /* YM2203 - this board doesn't have one */
		new Memory_WriteAddress( 0x110000, 0x110001, YM2151_w ),
		new Memory_WriteAddress( 0x120000, 0x120001, OKIM6295_data_0_w ),
		new Memory_WriteAddress( 0x130000, 0x130001, MWA_NOP ),
		new Memory_WriteAddress( 0x1f0000, 0x1f1fff, MWA_BANK8 ),
		new Memory_WriteAddress( 0x1fec00, 0x1fec01, H6280_timer_w ),
		new Memory_WriteAddress( 0x1ff402, 0x1ff403, H6280_irq_status_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	/******************************************************************************/
	
	static InputPortPtr input_ports_funkyjet = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* Player 1 controls */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );/* button 3 - unused? */
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		PORT_START(); 	/* Player 2 controls */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_COCKTAIL );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );/* button 3 - unused? */
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		PORT_START(); 	/* Credits */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN3 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_VBLANK );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 	/* Dip switch bank 1 */
	
		/* Dips seem inverted with respect to other Deco games */
	
		/* Some of these coinage options may not be correct.. */
		PORT_DIPNAME( 0xe0, 0xe0, DEF_STR( "Coin_A") );
		PORT_DIPSETTING(    0x00, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x40, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0xe0, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x60, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0xa0, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x60, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0x80, DEF_STR( "1C_5C") );
		PORT_DIPSETTING(    0x20, DEF_STR( "1C_6C") );
		PORT_DIPNAME( 0x1c, 0x1c, DEF_STR( "Coin_B") );
		PORT_DIPSETTING(    0x00, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x04, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x1c, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x18, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x14, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x10, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0x0c, DEF_STR( "1C_5C") );
		PORT_DIPSETTING(    0x08, DEF_STR( "1C_6C") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		PORT_START(); 	/* Dip switch bank 2 */
		PORT_DIPNAME( 0xc0, 0xc0, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x80, "1" );
		PORT_DIPSETTING(    0x00, "2" );
		PORT_DIPSETTING(    0xc0, "3" );
		PORT_DIPSETTING(    0x40, "4" );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x10, "Easy" );
		PORT_DIPSETTING(    0x30, "Normal" );
		PORT_DIPSETTING(    0x20, "Hard" );
		PORT_DIPSETTING(    0x00, "Hardest" );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "No") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Yes") );
	  	PORT_DIPNAME( 0x01, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_sotsugyo = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* Player 1 controls */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		PORT_START(); 	/* Player 2 controls */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_COCKTAIL );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		PORT_START(); 	/* Credits */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN3 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_VBLANK );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 	/* Dip switch bank 1 */
		PORT_DIPNAME( 0xe0, 0xe0, DEF_STR( "Coin_A") );
		PORT_DIPSETTING(    0x00, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x40, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0xe0, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x60, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0xa0, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x60, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0x80, DEF_STR( "1C_5C") );
		PORT_DIPSETTING(    0x20, DEF_STR( "1C_6C") );
		PORT_DIPNAME( 0x1c, 0x1c, DEF_STR( "Coin_B") );
		PORT_DIPSETTING(    0x00, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x04, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x1c, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x18, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x14, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x10, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0x0c, DEF_STR( "1C_5C") );
		PORT_DIPSETTING(    0x08, DEF_STR( "1C_6C") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x01, IP_ACTIVE_LOW );
	
		PORT_START(); 	/* Dip switch bank 2 */
		PORT_DIPNAME( 0x80, 0x80, "Pause" );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Free_Play") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x20, "1" );
		PORT_DIPSETTING(    0x10, "2" );
		PORT_DIPSETTING(    0x30, "3" );
		PORT_DIPSETTING(    0x00, "4" );
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x08, "Easy" );
		PORT_DIPSETTING(    0x0c, "Normal" );
		PORT_DIPSETTING(    0x04, "Hard" );
		PORT_DIPSETTING(    0x00, "Hardest" );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x02, DEF_STR( "On") );
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	INPUT_PORTS_END(); }}; 
	
	/******************************************************************************/
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,
		RGN_FRAC(1,2),
		4,
		new int[] { RGN_FRAC(1,2)+8, RGN_FRAC(1,2)+0, 8, 0 },
		new int[] { 0, 1, 2, 3, 4, 5, 6, 7 },
		new int[] { 0*16, 1*16, 2*16, 3*16, 4*16, 5*16, 6*16, 7*16 },
		16*8
	);
	
	static GfxLayout tile_layout = new GfxLayout
	(
		16,16,
		RGN_FRAC(1,2),
		4,
		new int[] { RGN_FRAC(1,2)+8, RGN_FRAC(1,2)+0, 8, 0 },
		new int[] { 32*8+0, 32*8+1, 32*8+2, 32*8+3, 32*8+4, 32*8+5, 32*8+6, 32*8+7,
				0, 1, 2, 3, 4, 5, 6, 7 },
		new int[] { 0*16, 1*16, 2*16, 3*16, 4*16, 5*16, 6*16, 7*16,
				8*16, 9*16, 10*16, 11*16, 12*16, 13*16, 14*16, 15*16 },
		64*8
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, charlayout,  256, 16 ),	/* Characters 8x8 */
		new GfxDecodeInfo( REGION_GFX1, 0, tile_layout, 512, 16 ),	/* Tiles 16x16 */
		new GfxDecodeInfo( REGION_GFX2, 0, tile_layout,   0, 16 ),	/* Sprites 16x16 */
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	/******************************************************************************/
	
	static struct OKIM6295interface okim6295_interface =
	{
		1,          /* 1 chip */
		{ 7757 },	/* Frequency */
		{ REGION_SOUND1 },      /* memory region */
		{ 50 }
	};
	
	static void sound_irq(int state)
	{
		cpu_set_irq_line(1,1,state); /* IRQ 2 */
	}
	
	static struct YM2151interface ym2151_interface =
	{
		1,
		32220000/9,
		{ YM3012_VOL(45,MIXER_PAN_LEFT,45,MIXER_PAN_RIGHT) },
		{ sound_irq }
	};
	
	static MachineDriver machine_driver_funkyjet = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
		 	new MachineCPU(
				CPU_M68000,
				14000000, /* 28 MHz crystal */
				funkyjet_readmem,funkyjet_writemem,null,null,
				m68_level6_irq,1
			),
			new MachineCPU(
				CPU_H6280 | CPU_AUDIO_CPU, /* Custom chip 45 */
				32220000/8, /* Audio section crystal is 32.220 MHz */
				sound_readmem,sound_writemem,null,null,
				ignore_interrupt,0
			)
		},
		60, 529,
		1,
		null,
	
		/* video hardware */
		40*8, 32*8, new rectangle( 0*8, 40*8-1, 1*8, 31*8-1 ),
	
		gfxdecodeinfo,
		1024, null,
		0,
	
		VIDEO_TYPE_RASTER,
		null,
		funkyjet_vh_start,
		null,
		funkyjet_vh_screenrefresh,
	
		/* sound hardware */
		SOUND_SUPPORTS_STEREO,0,0,0,
	  	new MachineSound[] {
			new MachineSound(
				SOUND_YM2151,
				ym2151_interface
			),
			new MachineSound(
				SOUND_OKIM6295,
				okim6295_interface
			)
		}
	);
	
	static MachineDriver machine_driver_funkyjtb = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
		 	new MachineCPU(
				CPU_M68000,
				14000000, /* 28 MHz crystal */
				funkyjet_readmem,funkyjet_writemem,null,null,
				m68_level6_irq,1
			)
		},
		60, 529,
		1,
		null,
	
		/* video hardware */
		40*8, 32*8, new rectangle( 0*8, 40*8-1, 1*8, 31*8-1 ),
	
		gfxdecodeinfo,
		1024, null,
		0,
	
		VIDEO_TYPE_RASTER,
		null,
		funkyjet_vh_start,
		null,
		funkyjet_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
	  	new MachineSound[] {
			new MachineSound(
				SOUND_OKIM6295,
				okim6295_interface
			)
		}
	);
	
	/******************************************************************************/
	
	static RomLoadPtr rom_funkyjet = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x80000, REGION_CPU1, 0 );/* 68000 code */
		ROM_LOAD16_BYTE( "jk00.12f", 0x00000, 0x40000, 0x712089c1 );
		ROM_LOAD16_BYTE( "jk01.13f", 0x00001, 0x40000, 0xbe3920d7 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* Sound CPU */
		ROM_LOAD( "jk02.16f",    0x00000, 0x10000, 0x748c0bd8 );
	
		ROM_REGION( 0x080000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "mat02", 0x000000, 0x80000, 0xe4b94c7e );/* Encrypted chars */
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "mat01", 0x000000, 0x80000, 0x24093a8d );/* sprites */
	  	ROM_LOAD( "mat00", 0x080000, 0x80000, 0xfbda0228 );
	
		ROM_REGION( 0x20000, REGION_SOUND1, 0 );/* ADPCM samples */
	  	ROM_LOAD( "jk03.15h",    0x00000, 0x20000, 0x69a0eaf7 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_sotsugyo = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x80000, REGION_CPU1, 0 );/* 68000 code */
		ROM_LOAD16_BYTE( "03.12f", 0x00000, 0x40000, 0xd175dfd1 );
		ROM_LOAD16_BYTE( "04.13f", 0x00001, 0x40000, 0x2072477c );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* Sound CPU */
		ROM_LOAD( "sb020.16f",    0x00000, 0x10000, 0xbaf5ec93 );
	
		ROM_REGION( 0x080000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "02.2f", 0x000000, 0x80000, 0x337b1451 );/* chars */
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "01.4a", 0x000000, 0x80000, 0xfa10dd54 );/* sprites */
	  	ROM_LOAD( "00.2a", 0x080000, 0x80000, 0xd35a14ef );
	
		ROM_REGION( 0x20000, REGION_SOUND1, 0 );/* ADPCM samples */
	  	ROM_LOAD( "sb030.15h",    0x00000, 0x20000, 0x1ea43f48 );
	ROM_END(); }}; 
	
	
	public static InitDriverPtr init_funkyjet = new InitDriverPtr() { public void handler() (void)
	{
		deco74_decrypt();
	} };
	
	/******************************************************************************/
	
	public static GameDriver driver_funkyjet	   = new GameDriver("1992"	,"funkyjet"	,"funkyjet.java"	,rom_funkyjet,null	,machine_driver_funkyjet	,input_ports_funkyjet	,init_funkyjet	,ROT0	,	"[Data East] (Mitchell license)", "Funky Jet", GAME_UNEMULATED_PROTECTION )
	public static GameDriver driver_sotsugyo	   = new GameDriver("1995"	,"sotsugyo"	,"funkyjet.java"	,rom_sotsugyo,null	,machine_driver_funkyjet	,input_ports_sotsugyo	,init_funkyjet	,ROT0	,	"Mitchell Corporation (Atlus license)", "Sotsugyo Shousho", GAME_IMPERFECT_GRAPHICS )
}
