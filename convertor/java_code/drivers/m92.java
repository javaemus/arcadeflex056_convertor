/*****************************************************************************

	Irem M92 system games:

	Gunforce (World)				M92-A	(c) 1991 Irem Corp
	Gunforce (USA)					M92-A	(c) 1991 Irem America Corp
	Gunforce (Japan)				M92-A	(c) 1991 Irem Corp
	Blademaster	(World)						(c) 1991 Irem Corp
	Lethal Thunder (World)					(c) 1991 Irem Corp
	Thunder Blaster (Japan)					(c) 1991 Irem Corp
	Undercover Cops	(World)					(c) 1992 Irem Corp
	Undercover Cops	(Japan)					(c) 1992 Irem Corp
	Mystic Riders (World)					(c) 1992 Irem Corp
	Gun Hohki (Japan)						(c) 1992 Irem Corp
	Major Title 2 (World)			M92-F	(c) 1992 Irem Corp
	The Irem Skins Game (USA Set 1)	M92-F	(c) 1992 Irem America Corp
	The Irem Skins Game (USA Set 2)	M92-F	(c) 1992 Irem America Corp
	Hook (World)							(c) 1992 Irem Corp
	Hook (USA)								(c) 1992 Irem America Corp
	R-Type Leo (Japan)						(c) 1992 Irem Corp
	In The Hunt	(World)				M92-E	(c) 1993 Irem Corp
	In The Hunt	(USA)				M92-E	(c) 1993 Irem Corp
	Kaitei Daisensou (Japan)		M92-E	(c) 1993 Irem Corp
	Ninja Baseball Batman (USA)				(c) 1993 Irem America Corp
	Yakyuu Kakutou League-Man (Japan)		(c) 1993 Irem Corp
	Perfect Soldiers (Japan)		M92-G	(c) 1993 Irem Corp
	Dream Soccer 94 (Japan)			M92-G	(c) 1994 Irem Corp

System notes:
	Each game has an encrypted sound cpu (see irem_cpu.c), the sound cpu and
	the sprite chip are on the game board rather than the main board and
	can differ between games.

	Irem Skins Game has an eeprom and ticket payout(?).
	R-Type Leo & Lethal Thunder have a memory card.

	Many games use raster IRQ's for special video effects, eg,
		* Scrolling water in Undercover Cops
		* Score display in R-Type Leo

	These are slow to emulate, and can be turned on/off by pressing
	F1 - they are on by default.
	Todo:  Raster effects don't work in flipscreen mode.

Glitch list!

	Gunforce:
		Animated water sometimes doesn't appear on level 5 (but it
		always appears if you cheat and jump straight to the level).
		Almost certainly a core bug.

	Lethal Thunder:
		Gives 99 credits.

	Irem Skins:
		- Priority bug: you can't see the arrow on the top right map.
		- Gfx problems at the players information during attract mode in
		  Skins Game *only*, Major Title is fine (that part of attract mode
		  is different).
		- Eeprom load/save not yet implemented - when done, MT2EEP should
		  be removed from the ROM definition.

	Perfect Soliders:
		Shortly into the fight, the sound CPU enters a tight loop, conitnuously
		writing to the status port and with interrupts disabled. I don't see how
		it is supposed to get out of that loop. Maybe it's not supposed to enter
		it at all?

	LeagueMan:
		Raster effects don't work properly (not even cpu time per line?).

	Dream Soccer 94:
		Slight priority problems when goal scoring animation is played

	Emulation by Bryan McPhail, mish@tendril.co.uk
	Thanks to Chris Hardy and Olli Bergmann too!


Sound programs:

Game                          Year  ID string
----------------------------  ----  ------------
Gunforce					  1991  -
Blade Master				  1991  -
Lethal Thunder				  1991  -
Undercover Cops				  1992  Rev 3.40 M92
Mystic Riders				  1992  Rev 3.44 M92
Major Title 2				  1992  Rev 3.44 M92
Hook						  1992  Rev 3.45 M92
R-Type Leo					  1992  Rev 3.45 M92
In The Hunt					  1993  Rev 3.45 M92
Ninja Baseball Batman		  1993  Rev 3.50 M92
Perfect Soldiers			  1993  Rev 3.50 M92
Fire Barrel                   1993  Rev 3.52 M92
Dream Soccer '94              1994  Rev 3.53 M92

*****************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package drivers;

public class m92
{
	
	static int m92_irq_vectorbase;
	static unsigned char *m92_ram;
	
	#define M92_IRQ_0 ((m92_irq_vectorbase+0)/4)  /* VBL interrupt*/
	#define M92_IRQ_1 ((m92_irq_vectorbase+4)/4)  /* Sprite buffer complete interrupt */
	#define M92_IRQ_2 ((m92_irq_vectorbase+8)/4)  /* Raster interrupt */
	#define M92_IRQ_3 ((m92_irq_vectorbase+12)/4) /* Sound cpu interrupt */
	
	#define M92_SCANLINES	256
	
	/* From vidhrdw/m92.c */
	void m92_vh_raster_partial_refresh(struct mame_bitmap *bitmap,int start_line,int end_line);
	
	extern unsigned char *m92_vram_data,*m92_spritecontrol;
	
	
	/*****************************************************************************/
	
	public static ReadHandlerPtr m92_eeprom_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		unsigned char *RAM = memory_region(REGION_USER1);
	//	logerror("%05x: EEPROM RE %04x\n",cpu_get_pc(),offset);
		return RAM[offset/2];
	} };
	
	public static WriteHandlerPtr m92_eeprom_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		unsigned char *RAM = memory_region(REGION_USER1);
	//	logerror("%05x: EEPROM WR %04x\n",cpu_get_pc(),offset);
		RAM[offset/2]=data;
	} };
	
	public static WriteHandlerPtr m92_coincounter_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (offset==0) {
			coin_counter_w(0,data & 0x01);
			coin_counter_w(1,data & 0x02);
			/* Bit 0x8 is Motor(?!), used in Hook, In The Hunt, UCops */
			/* Bit 0x8 is Memcard related in RTypeLeo */
			/* Bit 0x40 set in Blade Master test mode input check */
		}
	} };
	
	public static WriteHandlerPtr m92_bankswitch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		unsigned char *RAM = memory_region(REGION_CPU1);
		if (offset==1) return; /* Unused top byte */
		cpu_setbank(1,&RAM[0x100000 + ((data&0x7)*0x10000)]);
	} };
	
	public static ReadHandlerPtr m92_port_4_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return readinputport(4) | m92_sprite_buffer_busy; /* Bit 7 low indicates busy */
	} };
	
	/*****************************************************************************/
	
	public static final int VECTOR_INIT = 0;
	public static final int YM2151_ASSERT = 1;
	public static final int YM2151_CLEAR = 2;
	public static final int V30_ASSERT = 3;
	public static final int V30_CLEAR = 4;
	
	
	static void setvector_callback(int param)
	{
		static int irqvector;
	
		switch(param)
		{
			case VECTOR_INIT:	irqvector = 0;		break;
			case YM2151_ASSERT:	irqvector |= 0x2;	break;
			case YM2151_CLEAR:	irqvector &= ~0x2;	break;
			case V30_ASSERT:	irqvector |= 0x1;	break;
			case V30_CLEAR:		irqvector &= ~0x1;	break;
		}
	
		if (irqvector & 0x2)		/* YM2151 has precedence */
			cpu_irq_line_vector_w(1,0,0x18);
		else if (irqvector & 0x1)	/* V30 */
			cpu_irq_line_vector_w(1,0,0x19);
	
		if (irqvector == 0)	/* no IRQs pending */
			cpu_set_irq_line(1,0,CLEAR_LINE);
		else	/* IRQ pending */
			cpu_set_irq_line(1,0,ASSERT_LINE);
	}
	
	public static WriteHandlerPtr m92_soundlatch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (offset==0)
		{
			timer_set(TIME_NOW,V30_ASSERT,setvector_callback);
			soundlatch_w(0,data);
	//		logerror("soundlatch_w %02x\n",data);
		}
	} };
	
	static int sound_status;
	
	public static ReadHandlerPtr m92_sound_status_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	//logerror("%06x: read sound status\n",cpu_get_pc());
		if (offset == 0)
			return sound_status&0xff;
		return sound_status>>8;
	} };
	
	public static ReadHandlerPtr m92_soundlatch_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if (offset == 0)
		{
			int res = soundlatch_r(offset);
	//		logerror("soundlatch_r %02x\n",res);
			return res;
		}
		else return 0xff;
	} };
	
	public static WriteHandlerPtr m92_sound_irq_ack_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (offset == 0)
			timer_set(TIME_NOW,V30_CLEAR,setvector_callback);
	} };
	
	public static WriteHandlerPtr m92_sound_status_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (offset == 0) {
			sound_status = data | (sound_status&0xff00);
			cpu_cause_interrupt(0,M92_IRQ_3);
		}
		else
			sound_status = (data<<8) | (sound_status&0xff);
	} };
	
	/*****************************************************************************/
	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x00000, 0x9ffff, MRA_ROM ),
		new Memory_ReadAddress( 0xa0000, 0xbffff, MRA_BANK1 ),
		new Memory_ReadAddress( 0xc0000, 0xcffff, MRA_BANK2 ), /* Mirror of rom:  Used by In The Hunt as protection */
		new Memory_ReadAddress( 0xd0000, 0xdffff, m92_vram_r ),
		new Memory_ReadAddress( 0xe0000, 0xeffff, MRA_RAM ),
		new Memory_ReadAddress( 0xf8000, 0xf87ff, MRA_RAM ),
		new Memory_ReadAddress( 0xf8800, 0xf8fff, m92_paletteram_r ),
		new Memory_ReadAddress( 0xffff0, 0xfffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x00000, 0xbffff, MWA_ROM ),
		new Memory_WriteAddress( 0xd0000, 0xdffff, m92_vram_w, m92_vram_data ),
		new Memory_WriteAddress( 0xe0000, 0xeffff, MWA_RAM, m92_ram ), /* System ram */
		new Memory_WriteAddress( 0xf8000, 0xf87ff, MWA_RAM, spriteram, spriteram_size ),
		new Memory_WriteAddress( 0xf8800, 0xf8fff, m92_paletteram_w ),
		new Memory_WriteAddress( 0xf9000, 0xf900f, m92_spritecontrol_w, m92_spritecontrol ),
		new Memory_WriteAddress( 0xf9800, 0xf9801, m92_videocontrol_w ),
		new Memory_WriteAddress( 0xffff0, 0xfffff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress lethalth_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),/* Same as above but with different VRAM addressing PAL */
		new Memory_ReadAddress( 0x00000, 0x7ffff, MRA_ROM ),
		new Memory_ReadAddress( 0x80000, 0x8ffff, m92_vram_r ),
		new Memory_ReadAddress( 0xe0000, 0xeffff, MRA_RAM ),
		new Memory_ReadAddress( 0xf8000, 0xf87ff, MRA_RAM ),
		new Memory_ReadAddress( 0xf8800, 0xf8fff, paletteram_r ),
		new Memory_ReadAddress( 0xffff0, 0xfffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress lethalth_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x00000, 0x7ffff, MWA_ROM ),
		new Memory_WriteAddress( 0x80000, 0x8ffff, m92_vram_w, m92_vram_data ),
		new Memory_WriteAddress( 0xe0000, 0xeffff, MWA_RAM, m92_ram ), /* System ram */
		new Memory_WriteAddress( 0xf8000, 0xf87ff, MWA_RAM, spriteram, spriteram_size ),
		new Memory_WriteAddress( 0xf8800, 0xf8fff, m92_paletteram_w ),
		new Memory_WriteAddress( 0xf9000, 0xf900f, m92_spritecontrol_w, m92_spritecontrol ),
		new Memory_WriteAddress( 0xf9800, 0xf9801, m92_videocontrol_w ),
		new Memory_WriteAddress( 0xffff0, 0xfffff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static IO_ReadPort readport[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_ReadPort( 0x00, 0x00, input_port_0_r ), /* Player 1 */
		new IO_ReadPort( 0x01, 0x01, input_port_1_r ), /* Player 2 */
		new IO_ReadPort( 0x02, 0x02, m92_port_4_r ),   /* Coins  VBL */
		new IO_ReadPort( 0x03, 0x03, input_port_7_r ), /* Dip 3 */
		new IO_ReadPort( 0x04, 0x04, input_port_6_r ), /* Dip 2 */
		new IO_ReadPort( 0x05, 0x05, input_port_5_r ), /* Dip 1 */
		new IO_ReadPort( 0x06, 0x06, input_port_2_r ), /* Player 3 */
		new IO_ReadPort( 0x07, 0x07, input_port_3_r ),	/* Player 4 */
		new IO_ReadPort( 0x08, 0x09, m92_sound_status_r ),	/* answer from sound CPU */
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	
	public static IO_WritePort writeport[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_WritePort( 0x00, 0x01, m92_soundlatch_w ),
		new IO_WritePort( 0x02, 0x03, m92_coincounter_w ),
		new IO_WritePort( 0x20, 0x21, m92_bankswitch_w ),
		new IO_WritePort( 0x40, 0x43, MWA_NOP ), /* Interrupt controller, only written to at bootup */
		new IO_WritePort( 0x80, 0x87, m92_pf1_control_w ),
		new IO_WritePort( 0x88, 0x8f, m92_pf2_control_w ),
		new IO_WritePort( 0x90, 0x97, m92_pf3_control_w ),
		new IO_WritePort( 0x98, 0x9f, m92_master_control_w ),
	//	new IO_WritePort( 0xc0, 0xc1, m92_unknown_w ),	// sound related?
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	/******************************************************************************/
	
	public static Memory_ReadAddress sound_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x00000, 0x1ffff, MRA_ROM ),
		new Memory_ReadAddress( 0xa0000, 0xa3fff, MRA_RAM ),
		new Memory_ReadAddress( 0xa8042, 0xa8043, YM2151_status_port_0_r ),
		new Memory_ReadAddress( 0xa8044, 0xa8045, m92_soundlatch_r ),
		new Memory_ReadAddress( 0xffff0, 0xfffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress sound_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x00000, 0x1ffff, MWA_ROM ),
		new Memory_WriteAddress( 0x9ff00, 0x9ffff, MWA_NOP ), /* Irq controller? */
		new Memory_WriteAddress( 0xa0000, 0xa3fff, MWA_RAM ),
		new Memory_WriteAddress( 0xa8000, 0xa803f, IremGA20_w ),
		new Memory_WriteAddress( 0xa8040, 0xa8041, YM2151_register_port_0_w ),
		new Memory_WriteAddress( 0xa8042, 0xa8043, YM2151_data_port_0_w ),
		new Memory_WriteAddress( 0xa8044, 0xa8045, m92_sound_irq_ack_w ),
		new Memory_WriteAddress( 0xa8046, 0xa8047, m92_sound_status_w ),
		new Memory_WriteAddress( 0xffff0, 0xfffff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	/******************************************************************************/
	
	static InputPortPtr input_ports_bmaster = new InputPortPtr(){ public void handler() { 
		IREM_JOYSTICK_1_2(1)
		IREM_JOYSTICK_1_2(2)
		PORT_UNUSED
		PORT_UNUSED
		IREM_COINS
		IREM_SYSTEM_DIPSWITCH
	
		PORT_START(); 	/* Dip switch bank 1 */
		PORT_DIPNAME( 0x03, 0x02, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "1" );
		PORT_DIPSETTING(    0x03, "2" );
		PORT_DIPSETTING(    0x02, "3" );
		PORT_DIPSETTING(    0x01, "4" );
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x00, "Very Easy" );
		PORT_DIPSETTING(    0x08, "Easy" );
		PORT_DIPSETTING(    0x0c, "Normal" );
		PORT_DIPSETTING(    0x04, "Hard" );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x10, "300k only" );
		PORT_DIPSETTING(    0x00, "None" );
		PORT_DIPNAME( 0x20, 0x20, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "No") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x80, IP_ACTIVE_LOW );
	
		PORT_START(); 	/* Dip switch bank 2 */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_gunforce = new InputPortPtr(){ public void handler() { 
		IREM_JOYSTICK_1_2(1)
		IREM_JOYSTICK_1_2(2)
		PORT_UNUSED
		PORT_UNUSED
		IREM_COINS
		IREM_SYSTEM_DIPSWITCH
	
		PORT_START(); 	/* Dip switch bank 1 */
		PORT_DIPNAME( 0x03, 0x03, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x02, "2" );
		PORT_DIPSETTING(    0x03, "3" );
		PORT_DIPSETTING(    0x01, "4" );
		PORT_DIPSETTING(    0x00, "5" );
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x00, "Very Easy" );
		PORT_DIPSETTING(    0x08, "Easy" );
		PORT_DIPSETTING(    0x0c, "Normal" );
		PORT_DIPSETTING(    0x04, "Hard" );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x00, "15000 35000 75000 120000" );
		PORT_DIPSETTING(    0x10, "20000 40000 90000 150000" );
		PORT_DIPNAME( 0x20, 0x20, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "No") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x80, IP_ACTIVE_LOW );
	
		PORT_UNUSED	/* Game manual only mentions 2 dips */
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_lethalth = new InputPortPtr(){ public void handler() { 
		IREM_JOYSTICK_1_2(1)
		IREM_JOYSTICK_1_2(2)
		PORT_UNUSED
		PORT_UNUSED
		IREM_COINS
		IREM_SYSTEM_DIPSWITCH
	
		PORT_START(); 	/* Dip switch bank 1 */
		PORT_DIPNAME( 0x03, 0x03, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x02, "2" );
		PORT_DIPSETTING(    0x03, "3" );
		PORT_DIPSETTING(    0x01, "4" );
		PORT_DIPSETTING(    0x00, "5" );
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x00, "Very Easy" );
		PORT_DIPSETTING(    0x08, "Easy" );
		PORT_DIPSETTING(    0x0c, "Normal" );
		PORT_DIPSETTING(    0x04, "Hard" );
		PORT_DIPNAME( 0x10, 0x10, "Continuous Play" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x10, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x20, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "No") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x80, IP_ACTIVE_LOW );
	
		PORT_START(); 	/* Dip switch bank 2 */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_hook = new InputPortPtr(){ public void handler() { 
		IREM_JOYSTICK_1_2(1)
		IREM_JOYSTICK_1_2(2)
		IREM_JOYSTICK_3_4(3)
		IREM_JOYSTICK_3_4(4)
		IREM_COINS
		IREM_SYSTEM_DIPSWITCH_4PLAYERS
	
		PORT_START(); 	/* Dip switch bank 1 */
		PORT_DIPNAME( 0x03, 0x03, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "1" );
		PORT_DIPSETTING(    0x03, "2" );
		PORT_DIPSETTING(    0x02, "3" );
		PORT_DIPSETTING(    0x01, "4" );
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x00, "Very Easy" );
		PORT_DIPSETTING(    0x08, "Easy" );
		PORT_DIPSETTING(    0x0c, "Normal" );
		PORT_DIPSETTING(    0x04, "Hard" );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x20, "Any Button to Start" );
		PORT_DIPSETTING(    0x00, DEF_STR( "No") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x80, IP_ACTIVE_LOW );
	
		PORT_START(); 	/* Dip switch bank 2 */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_majtitl2 = new InputPortPtr(){ public void handler() { 
		IREM_JOYSTICK_1_2(1)
		IREM_JOYSTICK_1_2(2)
		IREM_JOYSTICK_3_4(3)
		IREM_JOYSTICK_3_4(4)
		IREM_COINS
		IREM_SYSTEM_DIPSWITCH_4PLAYERS
	
		PORT_START(); 	/* Dip switch bank 1 */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") ); /* Probably difficulty */
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unknown") ); /* One of these is continue */
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x00, "Any Button to Start" );
		PORT_DIPSETTING(    0x20, DEF_STR( "No") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x80, IP_ACTIVE_LOW );
	
		PORT_START(); 	/* Dip switch bank 2 */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_mysticri = new InputPortPtr(){ public void handler() { 
		IREM_JOYSTICK_1_2(1)
		IREM_JOYSTICK_1_2(2)
		PORT_UNUSED
		PORT_UNUSED
		IREM_COINS
		IREM_SYSTEM_DIPSWITCH
	
		PORT_START(); 	/* Dip switch bank 1 */
		PORT_DIPNAME( 0x03, 0x03, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x02, "2" );
		PORT_DIPSETTING(    0x03, "3" );
		PORT_DIPSETTING(    0x01, "4" );
		PORT_DIPSETTING(    0x00, "5" );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") ); /* Probably difficulty */
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x20, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "No") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x80, IP_ACTIVE_LOW );
	
		PORT_START(); 	/* Dip switch bank 2 */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_uccops = new InputPortPtr(){ public void handler() { 
		IREM_JOYSTICK_1_2(1)
		IREM_JOYSTICK_1_2(2)
		IREM_JOYSTICK_3_4(3)
		PORT_UNUSED
		IREM_COINS
		IREM_SYSTEM_DIPSWITCH_3PLAYERS
	
		PORT_START(); 	/* Dip switch bank 1 */
		PORT_DIPNAME( 0x03, 0x02, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "1" );
		PORT_DIPSETTING(    0x03, "2" );
		PORT_DIPSETTING(    0x02, "3" );
		PORT_DIPSETTING(    0x01, "4" );
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x00, "Very Easy" );
		PORT_DIPSETTING(    0x08, "Easy" );
		PORT_DIPSETTING(    0x0c, "Normal" );
		PORT_DIPSETTING(    0x04, "Hard" );
		/* There is ALLWAYS a Bonus Life at 300K */
		/* It does not depends on the value of bit 0x10 */
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x20, "Any Button to Start" );
		PORT_DIPSETTING(    0x00, DEF_STR( "No") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x80, IP_ACTIVE_LOW );
	
		PORT_START(); 	/* Dip switch bank 2 */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_rtypeleo = new InputPortPtr(){ public void handler() { 
		IREM_JOYSTICK_1_2(1)
		IREM_JOYSTICK_1_2(2)
		PORT_UNUSED
		PORT_UNUSED
		IREM_COINS
		IREM_SYSTEM_DIPSWITCH
	
		PORT_START(); 	/* Dip switch bank 1 */
		PORT_DIPNAME( 0x03, 0x03, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x02, "2" );
		PORT_DIPSETTING(    0x03, "3" );
		PORT_DIPSETTING(    0x01, "4" );
		PORT_DIPSETTING(    0x00, "5" );
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x00, "Very Easy" );
		PORT_DIPSETTING(    0x08, "Easy" );
		PORT_DIPSETTING(    0x0c, "Normal" );
		PORT_DIPSETTING(    0x04, "Hard" );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unknown") ); /* Buy in/coin mode?? */
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x20, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "No") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x80, IP_ACTIVE_LOW );
	
		PORT_UNUSED	/* Game manual only mentions 2 dips */
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_inthunt = new InputPortPtr(){ public void handler() { 
		IREM_JOYSTICK_1_2(1)
		IREM_JOYSTICK_1_2(2)
		PORT_UNUSED
		PORT_UNUSED
		IREM_COINS
		IREM_SYSTEM_DIPSWITCH
	
		PORT_START(); 	/* Dip switch bank 1 */
		PORT_DIPNAME( 0x03, 0x03, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x02, "2" );
		PORT_DIPSETTING(    0x03, "3" );
		PORT_DIPSETTING(    0x01, "4" );
		PORT_DIPSETTING(    0x00, "5" );
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x00, "Very Easy" );
		PORT_DIPSETTING(    0x08, "Easy" );
		PORT_DIPSETTING(    0x0c, "Normal" );
		PORT_DIPSETTING(    0x04, "Hard" );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x80, IP_ACTIVE_LOW );
	
		PORT_START(); 	/* Dip switch bank 2 */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_nbbatman = new InputPortPtr(){ public void handler() { 
		IREM_JOYSTICK_1_2(1)
		IREM_JOYSTICK_1_2(2)
		IREM_JOYSTICK_3_4(3)
		IREM_JOYSTICK_3_4(4)
		IREM_COINS
		IREM_SYSTEM_DIPSWITCH_4PLAYERS
	
		PORT_START(); 	/* Dip switch bank 1 */
		PORT_DIPNAME( 0x03, 0x03, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "1" );
		PORT_DIPSETTING(    0x03, "2" );
		PORT_DIPSETTING(    0x02, "3" );
		PORT_DIPSETTING(    0x01, "4" );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") ); /* Probably difficulty */
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unknown") ); /* One of these is continue */
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x00, "Any Button to Start" );
		PORT_DIPSETTING(    0x20, DEF_STR( "No") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x80, IP_ACTIVE_LOW );
	
		PORT_START(); 	/* Dip switch bank 2 */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_psoldier = new InputPortPtr(){ public void handler() { 
		PORT_START(); 
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
	
		PORT_START(); 
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
	
		PORT_UNUSED
	
		PORT_START();  /* Extra connector for kick buttons */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_BUTTON6 | IPF_PLAYER1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER2 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_BUTTON6 | IPF_PLAYER2 );
	
		IREM_COINS
		IREM_SYSTEM_DIPSWITCH
	
		PORT_START(); 	/* Dip switch bank 1 */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") ); /* Probably difficulty */
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unknown") ); /* One of these is continue */
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x00, "Any Button to Start" );
		PORT_DIPSETTING(    0x20, DEF_STR( "No") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x80, IP_ACTIVE_LOW );
	
		PORT_START(); 	/* Dip switch bank 2 */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_dsccr94j = new InputPortPtr(){ public void handler() { 
		IREM_JOYSTICK_1_2(1)
		IREM_JOYSTICK_1_2(2)
		IREM_JOYSTICK_3_4(3)
		IREM_JOYSTICK_3_4(4)
		IREM_COINS
		IREM_SYSTEM_DIPSWITCH_4PLAYERS
	
		PORT_START(); 	/* Dip switch bank 1 */
		PORT_DIPNAME( 0x03, 0x03, "Time" );
		PORT_DIPSETTING(    0x00, "1:30" );
		PORT_DIPSETTING(    0x03, "2:00" );
		PORT_DIPSETTING(    0x02, "2:30" );
		PORT_DIPSETTING(    0x01, "3:00" );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x20, "Starting Button" );
		PORT_DIPSETTING(    0x00, "Button 1" );
		PORT_DIPSETTING(    0x20, "Start Button" );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x80, IP_ACTIVE_LOW );
	
		PORT_START(); 	/* Dip switch bank 2 */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	INPUT_PORTS_END(); }}; 
	
	/***************************************************************************/
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,	/* 8*8 characters */
		RGN_FRAC(1,4),
		4,	/* 4 bits per pixel */
		new int[] { RGN_FRAC(3,4), RGN_FRAC(2,4), RGN_FRAC(1,4), RGN_FRAC(0,4) },
		new int[] { 0, 1, 2, 3, 4, 5, 6, 7 },
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
		8*8	/* every char takes 8 consecutive bytes */
	);
	
	static GfxLayout spritelayout = new GfxLayout
	(
		16,16,
		RGN_FRAC(1,4),
		4,
		new int[] { RGN_FRAC(3,4), RGN_FRAC(2,4), RGN_FRAC(1,4), RGN_FRAC(0,4) },
		new int[] { 0, 1, 2, 3, 4, 5, 6, 7,
			16*8+0, 16*8+1, 16*8+2, 16*8+3, 16*8+4, 16*8+5, 16*8+6, 16*8+7 },
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8,
				8*8, 9*8, 10*8, 11*8, 12*8, 13*8, 14*8, 15*8 },
		32*8
	);
	
	static GfxLayout spritelayout2 = new GfxLayout
	(
		16,16,
		RGN_FRAC(1,4),
		4,
		new int[] { RGN_FRAC(3,4), RGN_FRAC(2,4), RGN_FRAC(1,4), RGN_FRAC(0,4) },
		new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 },
		new int[] { 0*16, 1*16, 2*16, 3*16, 4*16, 5*16, 6*16, 7*16,
				8*16, 9*16, 10*16, 11*16, 12*16, 13*16, 14*16, 15*16 },
		32*8
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, charlayout,   0, 128 ),
		new GfxDecodeInfo( REGION_GFX2, 0, spritelayout, 0, 128 ),
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	static GfxDecodeInfo gfxdecodeinfo2[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, charlayout,    0, 128 ),
		new GfxDecodeInfo( REGION_GFX2, 0, spritelayout2, 0, 128 ),
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	/***************************************************************************/
	
	static void sound_irq(int state)
	{
		if (state)
			timer_set(TIME_NOW,YM2151_ASSERT,setvector_callback);
		else
			timer_set(TIME_NOW,YM2151_CLEAR,setvector_callback);
	}
	
	static struct YM2151interface ym2151_interface =
	{
		1,
		14318180/4,
		{ YM3012_VOL(40,MIXER_PAN_LEFT,40,MIXER_PAN_RIGHT) },
		{ sound_irq }
	};
	
	static struct IremGA20_interface iremGA20_interface =
	{
		14318180/4,
		REGION_SOUND1,
		{ MIXER(100,MIXER_PAN_LEFT), MIXER(100,MIXER_PAN_RIGHT) },
	};
	
	/***************************************************************************/
	
	public static InterruptPtr m92_interrupt = new InterruptPtr() { public int handler() 
	{
		if (osd_skip_this_frame()==0)
			m92_vh_raster_partial_refresh(Machine->scrbitmap,0,249);
	
		return M92_IRQ_0; /* VBL */
	} };
	
	public static InterruptPtr m92_raster_interrupt = new InterruptPtr() { public int handler() 
	{
		static int last_line=0;
		int line = M92_SCANLINES - cpu_getiloops();
	
		/* Raster interrupt */
		if (m92_raster_enable && line==m92_raster_irq_position) {
			if (osd_skip_this_frame()==0)
				m92_vh_raster_partial_refresh(Machine->scrbitmap,last_line,line+1);
			last_line=line+1;
			return M92_IRQ_2;
		}
	
		/* Redraw screen, then set vblank and trigger the VBL interrupt */
		if (line==249) { /* 248 is last visible line */
			if (osd_skip_this_frame()==0)
				m92_vh_raster_partial_refresh(Machine->scrbitmap,last_line,249);
			last_line=249;
			return M92_IRQ_0;
		}
	
		/* End of vblank */
		if (line==M92_SCANLINES-1) {
			last_line=0;
			return ignore_interrupt();
		}
	
		return ignore_interrupt();
	} };
	
	void m92_sprite_interrupt(void)
	{
		cpu_cause_interrupt(0,M92_IRQ_1);
	}
	
	static struct MachineDriver machine_driver_raster =
	{
		/* basic machine hardware */
		{
			{
				CPU_V33,	/* NEC V33 */
				18000000,	/* 18 MHz clock */
				readmem,writemem,readport,writeport,
				m92_raster_interrupt,M92_SCANLINES /* First visible line 8? */
			},
			{
				CPU_V30, //| CPU_AUDIO_CPU,
				14318180,	/* 14.31818 MHz */
				sound_readmem,sound_writemem,0,0,
				ignore_interrupt,0
			}
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		1,	/* 1 CPU slice per frame - interleaving is forced when a sound command is written */
		0,
	
		/* video hardware */
		512, 512, { 80, 511-112, 128+8, 511-128-8 }, /* 320 x 240 */
	
		gfxdecodeinfo,
		2048, 0,
		0,
	
		VIDEO_TYPE_RASTER | VIDEO_BUFFERS_SPRITERAM,
		0,
		m92_vh_start,
		m92_vh_stop,
		m92_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		{
			{
				SOUND_YM2151,
				&ym2151_interface
			},
			{
				SOUND_IREMGA20,
				&iremGA20_interface
			}
		}
	};
	
	static struct MachineDriver machine_driver_nonraster =
	{
		/* basic machine hardware */
		{
			{
				CPU_V33,	/* NEC V33 */
				18000000,	/* 18 MHz clock */
				readmem,writemem,readport,writeport,
				m92_interrupt,1
			},
			{
				CPU_V30 | CPU_AUDIO_CPU,
				14318180,	/* 14.31818 MHz */
				sound_readmem,sound_writemem,0,0,
				ignore_interrupt,0
			}
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		1,	/* 1 CPU slice per frame - interleaving is forced when a sound command is written */
		0,
	
		/* video hardware */
		512, 512, { 80, 511-112, 128+8, 511-128-8 }, /* 320 x 240 */
	
		gfxdecodeinfo,
		2048, 0,
		0,
	
		VIDEO_TYPE_RASTER | VIDEO_BUFFERS_SPRITERAM,
		0,
		m92_vh_start,
		m92_vh_stop,
		m92_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		{
			{
				SOUND_YM2151,
				&ym2151_interface
			},
			{
				SOUND_IREMGA20,
				&iremGA20_interface
			}
		}
	};
	
	static struct MachineDriver machine_driver_lethalth =
	{
		/* basic machine hardware */
		{
			{
				CPU_V33,	/* NEC V33 */
				18000000,	/* 18 MHz clock */
				lethalth_readmem,lethalth_writemem,readport,writeport,
				m92_interrupt,1
			},
			{
				CPU_V30 | CPU_AUDIO_CPU,
				14318180,	/* 14.31818 MHz */
				sound_readmem,sound_writemem,0,0,
				ignore_interrupt,0
			}
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		1,	/* 1 CPU slice per frame - interleaving is forced when a sound command is written */
		0,
	
		/* video hardware */
		512, 512, { 80, 511-112, 128+8, 511-128-8 }, /* 320 x 240 */
	
		gfxdecodeinfo,
		2048, 0,
		0,
	
		VIDEO_TYPE_RASTER | VIDEO_BUFFERS_SPRITERAM,
		0,
		m92_vh_start,
		m92_vh_stop,
		m92_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		{
			{
				SOUND_YM2151,
				&ym2151_interface
			},
			{
				SOUND_IREMGA20,
				&iremGA20_interface
			}
		}
	};
	
	static struct MachineDriver machine_driver_psoldier =
	{
		/* basic machine hardware */
		{
			{
				CPU_V33,	/* NEC V33 */
				18000000,	/* 18 MHz clock */
				readmem,writemem,readport,writeport,
				m92_interrupt,1
			},
			{
				CPU_V30 | CPU_AUDIO_CPU,
				14318180,	/* 14.31818 MHz */
				sound_readmem,sound_writemem,0,0,
				ignore_interrupt,0
			}
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		1,	/* 1 CPU slice per frame - interleaving is forced when a sound command is written */
		0,
	
		/* video hardware */
		512, 512, { 80, 511-112, 128+8, 511-128-8 }, /* 320 x 240 */
	
		gfxdecodeinfo2,
		2048, 0,
		0,
	
		VIDEO_TYPE_RASTER | VIDEO_BUFFERS_SPRITERAM,
		0,
		m92_vh_start,
		m92_vh_stop,
		m92_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		{
			{
				SOUND_YM2151,
				&ym2151_interface
			},
			{
				SOUND_IREMGA20,
				&iremGA20_interface
			}
		}
	};
	
	/***************************************************************************/
	
	static RomLoadPtr rom_bmaster = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x100000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "bm_d-h0.rom",  0x000001, 0x40000, 0x49b257c7 );
		ROM_LOAD16_BYTE( "bm_d-l0.rom",  0x000000, 0x40000, 0xa873523e );
		ROM_LOAD16_BYTE( "bm_d-h1.rom",  0x080001, 0x10000, 0x082b7158 );
		ROM_LOAD16_BYTE( "bm_d-l1.rom",  0x080000, 0x10000, 0x6ff0c04e );
	
		ROM_REGION( 0x100000 * 2, REGION_CPU2, 0 );
		ROM_LOAD16_BYTE( "bm_d-sh0.rom",  0x000001, 0x10000, 0x9f7c075b );
		ROM_LOAD16_BYTE( "bm_d-sl0.rom",  0x000000, 0x10000, 0x1fa87c89 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* Tiles */
		ROM_LOAD( "bm_c0.rom",       0x000000, 0x40000, 0x2cc966b8 );
		ROM_LOAD( "bm_c1.rom",       0x040000, 0x40000, 0x46df773e );
		ROM_LOAD( "bm_c2.rom",       0x080000, 0x40000, 0x05b867bd );
		ROM_LOAD( "bm_c3.rom",       0x0c0000, 0x40000, 0x0a2227a4 );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "bm_000.rom",      0x000000, 0x80000, 0x339fc9f3 );
		ROM_LOAD( "bm_010.rom",      0x080000, 0x80000, 0x6a14377d );
		ROM_LOAD( "bm_020.rom",      0x100000, 0x80000, 0x31532198 );
		ROM_LOAD( "bm_030.rom",      0x180000, 0x80000, 0xd1a041d3 );
	
		ROM_REGION( 0x80000, REGION_SOUND1, ROMREGION_SOUNDONLY );
		ROM_LOAD( "bm_da.rom",       0x000000, 0x80000, 0x62ce5798 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_skingame = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x180000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "is-h0-d",  0x000001, 0x40000, 0x80940abb );
		ROM_LOAD16_BYTE( "is-l0-d",  0x000000, 0x40000, 0xb84beed6 );
		ROM_LOAD16_BYTE( "is-h1",    0x100001, 0x40000, 0x9ba8e1f2 );
		ROM_LOAD16_BYTE( "is-l1",    0x100000, 0x40000, 0xe4e00626 );
	
		ROM_REGION( 0x100000 * 2, REGION_CPU2, 0 );/* 64k for the audio CPU */
		ROM_LOAD16_BYTE( "mt2sh0",  0x000001, 0x10000, 0x1ecbea43 );
		ROM_LOAD16_BYTE( "mt2sl0",  0x000000, 0x10000, 0x8fd5b531 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* Tiles */
		ROM_LOAD( "c0",       0x000000, 0x40000, 0x7e61e4b5 );
		ROM_LOAD( "c1",       0x040000, 0x40000, 0x0a667564 );
		ROM_LOAD( "c2",       0x080000, 0x40000, 0x5eb44312 );
		ROM_LOAD( "c3",       0x0c0000, 0x40000, 0xf2866294 );
	
		ROM_REGION( 0x400000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "k30",      0x000000, 0x100000, 0x8c9a2678 );
		ROM_LOAD( "k31",      0x100000, 0x100000, 0x5455df78 );
		ROM_LOAD( "k32",      0x200000, 0x100000, 0x3a258c41 );
		ROM_LOAD( "k33",      0x300000, 0x100000, 0xc1e91a14 );
	
		ROM_REGION( 0x80000, REGION_SOUND1, ROMREGION_SOUNDONLY );
		ROM_LOAD( "da",        0x000000, 0x80000, 0x713b9e9f );
	
		ROM_REGION( 0x4000, REGION_USER1, 0 );/* EEPROM */
		ROM_LOAD( "mt2eep",       0x000000, 0x800, 0x208af971 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_majtitl2 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x180000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "mt2-ho-b.5m",0x000001, 0x40000, 0xb163b12e );
		ROM_LOAD16_BYTE( "mt2-lo-b.5f",0x000000, 0x40000, 0x6f3b5d9d );
		ROM_LOAD16_BYTE( "is-h1",      0x100001, 0x40000, 0x9ba8e1f2 );
		ROM_LOAD16_BYTE( "is-l1",      0x100000, 0x40000, 0xe4e00626 );
	
		ROM_REGION( 0x100000 * 2, REGION_CPU2, 0 );/* 64k for the audio CPU */
		ROM_LOAD16_BYTE( "mt2sh0",  0x000001, 0x10000, 0x1ecbea43 );
		ROM_LOAD16_BYTE( "mt2sl0",  0x000000, 0x10000, 0x8fd5b531 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* Tiles */
		ROM_LOAD( "c0",       0x000000, 0x40000, 0x7e61e4b5 );
		ROM_LOAD( "c1",       0x040000, 0x40000, 0x0a667564 );
		ROM_LOAD( "c2",       0x080000, 0x40000, 0x5eb44312 );
		ROM_LOAD( "c3",       0x0c0000, 0x40000, 0xf2866294 );
	
		ROM_REGION( 0x400000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "k30",      0x000000, 0x100000, 0x8c9a2678 );
		ROM_LOAD( "k31",      0x100000, 0x100000, 0x5455df78 );
		ROM_LOAD( "k32",      0x200000, 0x100000, 0x3a258c41 );
		ROM_LOAD( "k33",      0x300000, 0x100000, 0xc1e91a14 );
	
		ROM_REGION( 0x80000, REGION_SOUND1, ROMREGION_SOUNDONLY );
		ROM_LOAD( "da",        0x000000, 0x80000, 0x713b9e9f );
	
		ROM_REGION( 0x4000, REGION_USER1, 0 );/* EEPROM */
		ROM_LOAD( "mt2eep",       0x000000, 0x800, 0x208af971 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_skingam2 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x180000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "mt2h0a", 0x000001, 0x40000, 0x7c6dbbc7 );
		ROM_LOAD16_BYTE( "mt2l0a", 0x000000, 0x40000, 0x9de5f689 );
		ROM_LOAD16_BYTE( "is-h1",  0x100001, 0x40000, 0x9ba8e1f2 );
		ROM_LOAD16_BYTE( "is-l1",  0x100000, 0x40000, 0xe4e00626 );
	
		ROM_REGION( 0x100000 * 2, REGION_CPU2, 0 );
		ROM_LOAD16_BYTE( "mt2sh0",  0x000001, 0x10000, 0x1ecbea43 );
		ROM_LOAD16_BYTE( "mt2sl0",  0x000000, 0x10000, 0x8fd5b531 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* Tiles */
		ROM_LOAD( "c0",       0x000000, 0x40000, 0x7e61e4b5 );
		ROM_LOAD( "c1",       0x040000, 0x40000, 0x0a667564 );
		ROM_LOAD( "c2",       0x080000, 0x40000, 0x5eb44312 );
		ROM_LOAD( "c3",       0x0c0000, 0x40000, 0xf2866294 );
	
		ROM_REGION( 0x400000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "k30",      0x000000, 0x100000, 0x8c9a2678 );
		ROM_LOAD( "k31",      0x100000, 0x100000, 0x5455df78 );
		ROM_LOAD( "k32",      0x200000, 0x100000, 0x3a258c41 );
		ROM_LOAD( "k33",      0x300000, 0x100000, 0xc1e91a14 );
	
		ROM_REGION( 0x80000, REGION_SOUND1, ROMREGION_SOUNDONLY );
		ROM_LOAD( "da",        0x000000, 0x80000, 0x713b9e9f );
	
		ROM_REGION( 0x4000, REGION_USER1, 0 );/* EEPROM */
		ROM_LOAD( "mt2eep",       0x000000, 0x800, 0x208af971 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_gunforce = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x100000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "gf_h0-c.rom",  0x000001, 0x20000, 0xc09bb634 );
		ROM_LOAD16_BYTE( "gf_l0-c.rom",  0x000000, 0x20000, 0x1bef6f7d );
		ROM_LOAD16_BYTE( "gf_h1-c.rom",  0x040001, 0x20000, 0xc84188b7 );
		ROM_LOAD16_BYTE( "gf_l1-c.rom",  0x040000, 0x20000, 0xb189f72a );
	
		ROM_REGION( 0x100000 * 2, REGION_CPU2, 0 );/* 1MB for the audio CPU - encrypted V30 = NANAO custom D80001 (?) */
		ROM_LOAD16_BYTE( "gf_sh0.rom",0x000001, 0x010000, 0x3f8f16e0 );
		ROM_LOAD16_BYTE( "gf_sl0.rom",0x000000, 0x010000, 0xdb0b13a3 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* Tiles */
		ROM_LOAD( "gf_c0.rom",       0x000000, 0x40000, 0xb3b74979 );
		ROM_LOAD( "gf_c1.rom",       0x040000, 0x40000, 0xf5c8590a );
		ROM_LOAD( "gf_c2.rom",       0x080000, 0x40000, 0x30f9fb64 );
		ROM_LOAD( "gf_c3.rom",       0x0c0000, 0x40000, 0x87b3e621 );
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "gf_000.rom",      0x000000, 0x40000, 0x209e8e8d );
		ROM_LOAD( "gf_010.rom",      0x040000, 0x40000, 0x6e6e7808 );
		ROM_LOAD( "gf_020.rom",      0x080000, 0x40000, 0x6f5c3cb0 );
		ROM_LOAD( "gf_030.rom",      0x0c0000, 0x40000, 0x18978a9f );
	
		ROM_REGION( 0x20000, REGION_SOUND1, ROMREGION_SOUNDONLY );
		ROM_LOAD( "gf-da.rom",	 0x000000, 0x020000, 0x933ba935 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_gunforcj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x100000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "gfbh0e.bin",  0x000001, 0x20000, 0x43c36e0f );
		ROM_LOAD16_BYTE( "gfbl0e.bin",  0x000000, 0x20000, 0x24a558d8 );
		ROM_LOAD16_BYTE( "gfbh1e.bin",  0x040001, 0x20000, 0xd9744f5d );
		ROM_LOAD16_BYTE( "gfbl1e.bin",  0x040000, 0x20000, 0xa0f7b61b );
	
		ROM_REGION( 0x100000 * 2, REGION_CPU2, 0 );/* 1MB for the audio CPU - encrypted V30 = NANAO custom D80001 (?) */
		ROM_LOAD16_BYTE( "gf_sh0.rom",0x000001, 0x010000, 0x3f8f16e0 );
		ROM_LOAD16_BYTE( "gf_sl0.rom",0x000000, 0x010000, 0xdb0b13a3 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* Tiles */
		ROM_LOAD( "gf_c0.rom",       0x000000, 0x40000, 0xb3b74979 );
		ROM_LOAD( "gf_c1.rom",       0x040000, 0x40000, 0xf5c8590a );
		ROM_LOAD( "gf_c2.rom",       0x080000, 0x40000, 0x30f9fb64 );
		ROM_LOAD( "gf_c3.rom",       0x0c0000, 0x40000, 0x87b3e621 );
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "gf_000.rom",      0x000000, 0x40000, 0x209e8e8d );
		ROM_LOAD( "gf_010.rom",      0x040000, 0x40000, 0x6e6e7808 );
		ROM_LOAD( "gf_020.rom",      0x080000, 0x40000, 0x6f5c3cb0 );
		ROM_LOAD( "gf_030.rom",      0x0c0000, 0x40000, 0x18978a9f );
	
		ROM_REGION( 0x20000, REGION_SOUND1, ROMREGION_SOUNDONLY );
		ROM_LOAD( "gf-da.rom",	 0x000000, 0x020000, 0x933ba935 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_gunforcu = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x100000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "gf_h0-d.5m",  0x000001, 0x20000, 0xa6db7b5c );
		ROM_LOAD16_BYTE( "gf_l0-d.5f",  0x000000, 0x20000, 0x82cf55f6 );
		ROM_LOAD16_BYTE( "gf_h1-d.5l",  0x040001, 0x20000, 0x08a3736c );
		ROM_LOAD16_BYTE( "gf_l1-d.5j",  0x040000, 0x20000, 0x435f524f );
	
		ROM_REGION( 0x100000 * 2, REGION_CPU2, 0 );/* 1MB for the audio CPU - encrypted V30 = NANAO custom D80001 (?) */
		ROM_LOAD16_BYTE( "gf_sh0.rom",0x000001, 0x010000, 0x3f8f16e0 );
		ROM_LOAD16_BYTE( "gf_sl0.rom",0x000000, 0x010000, 0xdb0b13a3 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* Tiles */
		ROM_LOAD( "gf_c0.rom",       0x000000, 0x40000, 0xb3b74979 );
		ROM_LOAD( "gf_c1.rom",       0x040000, 0x40000, 0xf5c8590a );
		ROM_LOAD( "gf_c2.rom",       0x080000, 0x40000, 0x30f9fb64 );
		ROM_LOAD( "gf_c3.rom",       0x0c0000, 0x40000, 0x87b3e621 );
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "gf_000.rom",      0x000000, 0x40000, 0x209e8e8d );
		ROM_LOAD( "gf_010.rom",      0x040000, 0x40000, 0x6e6e7808 );
		ROM_LOAD( "gf_020.rom",      0x080000, 0x40000, 0x6f5c3cb0 );
		ROM_LOAD( "gf_030.rom",      0x0c0000, 0x40000, 0x18978a9f );
	
		ROM_REGION( 0x20000, REGION_SOUND1, ROMREGION_SOUNDONLY );
		ROM_LOAD( "gf-da.rom",	 0x000000, 0x020000, 0x933ba935 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_inthunt = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x100000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "ith-h0-d.rom",0x000001, 0x040000, 0x52f8e7a6 );
		ROM_LOAD16_BYTE( "ith-l0-d.rom",0x000000, 0x040000, 0x5db79eb7 );
		ROM_LOAD16_BYTE( "ith-h1-b.rom",0x080001, 0x020000, 0xfc2899df );
		ROM_LOAD16_BYTE( "ith-l1-b.rom",0x080000, 0x020000, 0x955a605a );
	
		ROM_REGION( 0x100000 * 2, REGION_CPU2, 0 );/* Irem D8000011A1 */
		ROM_LOAD16_BYTE( "ith-sh0.rom",0x000001, 0x010000, 0x209c8b7f );
		ROM_LOAD16_BYTE( "ith-sl0.rom",0x000000, 0x010000, 0x18472d65 );
	
		ROM_REGION( 0x200000, REGION_GFX1, ROMREGION_DISPOSE );/* Tiles */
		ROM_LOAD( "ith_ic26.rom",0x000000, 0x080000, 0x4c1818cf );
		ROM_LOAD( "ith_ic25.rom",0x080000, 0x080000, 0x91145bae );
		ROM_LOAD( "ith_ic24.rom",0x100000, 0x080000, 0xfc03fe3b );
		ROM_LOAD( "ith_ic23.rom",0x180000, 0x080000, 0xee156a0a );
	
		ROM_REGION( 0x400000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "ith_ic34.rom",0x000000, 0x100000, 0xa019766e );
		ROM_LOAD( "ith_ic35.rom",0x100000, 0x100000, 0x3fca3073 );
		ROM_LOAD( "ith_ic36.rom",0x200000, 0x100000, 0x20d1b28b );
		ROM_LOAD( "ith_ic37.rom",0x300000, 0x100000, 0x90b6fd4b );
	
		ROM_REGION( 0x80000, REGION_SOUND1, ROMREGION_SOUNDONLY );
		ROM_LOAD( "ith_ic9.rom" ,0x000000, 0x080000, 0x318ee71a );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_inthuntu = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x100000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "ithhoc.bin",0x000001, 0x040000, 0x563dcec0 );
		ROM_LOAD16_BYTE( "ithloc.bin",0x000000, 0x040000, 0x1638c705 );
		ROM_LOAD16_BYTE( "ithh1a.bin",0x080001, 0x020000, 0x0253065f );
		ROM_LOAD16_BYTE( "ithl1a.bin",0x080000, 0x020000, 0xa57d688d );
	
		ROM_REGION( 0x100000 * 2, REGION_CPU2, 0 );/* Irem D8000011A1 */
		ROM_LOAD16_BYTE( "ith-sh0.rom",0x000001, 0x010000, 0x209c8b7f );
		ROM_LOAD16_BYTE( "ith-sl0.rom",0x000000, 0x010000, 0x18472d65 );
	
		ROM_REGION( 0x200000, REGION_GFX1, ROMREGION_DISPOSE );/* Tiles */
		ROM_LOAD( "ith_ic26.rom",0x000000, 0x080000, 0x4c1818cf );
		ROM_LOAD( "ith_ic25.rom",0x080000, 0x080000, 0x91145bae );
		ROM_LOAD( "ith_ic24.rom",0x100000, 0x080000, 0xfc03fe3b );
		ROM_LOAD( "ith_ic23.rom",0x180000, 0x080000, 0xee156a0a );
	
		ROM_REGION( 0x400000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "ith_ic34.rom",0x000000, 0x100000, 0xa019766e );
		ROM_LOAD( "ith_ic35.rom",0x100000, 0x100000, 0x3fca3073 );
		ROM_LOAD( "ith_ic36.rom",0x200000, 0x100000, 0x20d1b28b );
		ROM_LOAD( "ith_ic37.rom",0x300000, 0x100000, 0x90b6fd4b );
	
		ROM_REGION( 0x80000, REGION_SOUND1, ROMREGION_SOUNDONLY );
		ROM_LOAD( "ith_ic9.rom" ,0x000000, 0x080000, 0x318ee71a );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_kaiteids = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x100000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "ith-h0j.bin",0x000001, 0x040000, 0xdc1dec36 );
		ROM_LOAD16_BYTE( "ith-l0j.bin",0x000000, 0x040000, 0x8835d704 );
		ROM_LOAD16_BYTE( "ith-h1j.bin",0x080001, 0x020000, 0x5a7b212d );
		ROM_LOAD16_BYTE( "ith-l1j.bin",0x080000, 0x020000, 0x4c084494 );
	
		ROM_REGION( 0x100000 * 2, REGION_CPU2, 0 );/* Irem D8000011A1 */
		ROM_LOAD16_BYTE( "ith-sh0.rom",0x000001, 0x010000, 0x209c8b7f );
		ROM_LOAD16_BYTE( "ith-sl0.rom",0x000000, 0x010000, 0x18472d65 );
	
		ROM_REGION( 0x200000, REGION_GFX1, ROMREGION_DISPOSE );/* Tiles */
		ROM_LOAD( "ith_ic26.rom",0x000000, 0x080000, 0x4c1818cf );
		ROM_LOAD( "ith_ic25.rom",0x080000, 0x080000, 0x91145bae );
		ROM_LOAD( "ith_ic24.rom",0x100000, 0x080000, 0xfc03fe3b );
		ROM_LOAD( "ith_ic23.rom",0x180000, 0x080000, 0xee156a0a );
	
		ROM_REGION( 0x400000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "ith_ic34.rom",0x000000, 0x100000, 0xa019766e );
		ROM_LOAD( "ith_ic35.rom",0x100000, 0x100000, 0x3fca3073 );
		ROM_LOAD( "ith_ic36.rom",0x200000, 0x100000, 0x20d1b28b );
		ROM_LOAD( "ith_ic37.rom",0x300000, 0x100000, 0x90b6fd4b );
	
		ROM_REGION( 0x80000, REGION_SOUND1, ROMREGION_SOUNDONLY );
		ROM_LOAD( "ith_ic9.rom" ,0x000000, 0x080000, 0x318ee71a );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_hook = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x100000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "h-h0-d.rom",0x000001, 0x040000, 0x40189ff6 );
		ROM_LOAD16_BYTE( "h-l0-d.rom",0x000000, 0x040000, 0x14567690 );
		ROM_LOAD16_BYTE( "h-h1.rom",  0x080001, 0x020000, 0x264ba1f0 );
		ROM_LOAD16_BYTE( "h-l1.rom",  0x080000, 0x020000, 0xf9913731 );
	
		ROM_REGION( 0x100000 * 2, REGION_CPU2, 0 );/* 1MB for the audio CPU - encrypted V30 = NANAO custom D80001 (?) */
		ROM_LOAD16_BYTE( "h-sh0.rom",0x000001, 0x010000, 0x86a4e56e );
		ROM_LOAD16_BYTE( "h-sl0.rom",0x000000, 0x010000, 0x10fd9676 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* Tiles */
		ROM_LOAD( "hook-c0.rom",0x000000, 0x040000, 0xdec63dcf );
		ROM_LOAD( "hook-c1.rom",0x040000, 0x040000, 0xe4eb0b92 );
		ROM_LOAD( "hook-c2.rom",0x080000, 0x040000, 0xa52b320b );
		ROM_LOAD( "hook-c3.rom",0x0c0000, 0x040000, 0x7ef67731 );
	
		ROM_REGION( 0x400000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "hook-000.rom",0x000000, 0x100000, 0xccceac30 );
		ROM_LOAD( "hook-010.rom",0x100000, 0x100000, 0x8ac8da67 );
		ROM_LOAD( "hook-020.rom",0x200000, 0x100000, 0x8847af9a );
		ROM_LOAD( "hook-030.rom",0x300000, 0x100000, 0x239e877e );
	
		ROM_REGION( 0x80000, REGION_SOUND1, ROMREGION_SOUNDONLY );
		ROM_LOAD( "hook-da.rom" ,0x000000, 0x080000, 0x88cd0212 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_hooku = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x100000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "h0-c.3h",0x000001, 0x040000, 0x84cc239e );
		ROM_LOAD16_BYTE( "l0-c.5h",0x000000, 0x040000, 0x45e194fe );
		ROM_LOAD16_BYTE( "h-h1.rom",  0x080001, 0x020000, 0x264ba1f0 );
		ROM_LOAD16_BYTE( "h-l1.rom",  0x080000, 0x020000, 0xf9913731 );
	
		ROM_REGION( 0x100000 * 2, REGION_CPU2, 0 );/* 1MB for the audio CPU - encrypted V30 = NANAO custom D80001 (?) */
		ROM_LOAD16_BYTE( "h-sh0.rom",0x000001, 0x010000, 0x86a4e56e );
		ROM_LOAD16_BYTE( "h-sl0.rom",0x000000, 0x010000, 0x10fd9676 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* Tiles */
		ROM_LOAD( "hook-c0.rom",0x000000, 0x040000, 0xdec63dcf );
		ROM_LOAD( "hook-c1.rom",0x040000, 0x040000, 0xe4eb0b92 );
		ROM_LOAD( "hook-c2.rom",0x080000, 0x040000, 0xa52b320b );
		ROM_LOAD( "hook-c3.rom",0x0c0000, 0x040000, 0x7ef67731 );
	
		ROM_REGION( 0x400000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "hook-000.rom",0x000000, 0x100000, 0xccceac30 );
		ROM_LOAD( "hook-010.rom",0x100000, 0x100000, 0x8ac8da67 );
		ROM_LOAD( "hook-020.rom",0x200000, 0x100000, 0x8847af9a );
		ROM_LOAD( "hook-030.rom",0x300000, 0x100000, 0x239e877e );
	
		ROM_REGION( 0x80000, REGION_SOUND1, ROMREGION_SOUNDONLY );
		ROM_LOAD( "hook-da.rom" ,0x000000, 0x080000, 0x88cd0212 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_rtypeleo = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x100000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "rtl-h0-d.bin", 0x000001, 0x040000, 0x3dbac89f );
		ROM_LOAD16_BYTE( "rtl-l0-d.bin", 0x000000, 0x040000, 0xf85a2537 );
		ROM_LOAD16_BYTE( "rtl-h1-d.bin", 0x080001, 0x020000, 0x352ff444 );
		ROM_LOAD16_BYTE( "rtl-l1-d.bin", 0x080000, 0x020000, 0xfd34ea46 );
	
		ROM_REGION( 0x100000 * 2, REGION_CPU2, 0 );/* 1MB for the audio CPU - encrypted V30 = NANAO custom D80001 (?) */
		ROM_LOAD16_BYTE( "rtl-sh0a.bin",0x000001, 0x010000, 0xe518b4e3 );
		ROM_LOAD16_BYTE( "rtl-sl0a.bin",0x000000, 0x010000, 0x896f0d36 );
	
		ROM_REGION( 0x200000, REGION_GFX1, ROMREGION_DISPOSE );/* Tiles */
		ROM_LOAD( "rtl-c0.bin", 0x000000, 0x080000, 0xfb588d7c );
		ROM_LOAD( "rtl-c1.bin", 0x080000, 0x080000, 0xe5541bff );
		ROM_LOAD( "rtl-c2.bin", 0x100000, 0x080000, 0xfaa9ae27 );
		ROM_LOAD( "rtl-c3.bin", 0x180000, 0x080000, 0x3a2343f6 );
	
		ROM_REGION( 0x400000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "rtl-000.bin",0x000000, 0x100000, 0x82a06870 );
		ROM_LOAD( "rtl-010.bin",0x100000, 0x100000, 0x417e7a56 );
		ROM_LOAD( "rtl-020.bin",0x200000, 0x100000, 0xf9a3f3a1 );
		ROM_LOAD( "rtl-030.bin",0x300000, 0x100000, 0x03528d95 );
	
		ROM_REGION( 0x80000, REGION_SOUND1, ROMREGION_SOUNDONLY );
		ROM_LOAD( "rtl-da.bin" ,0x000000, 0x080000, 0xdbebd1ff );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_mysticri = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x100000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "mr-h0-b.bin",  0x000001, 0x040000, 0xd529f887 );
		ROM_LOAD16_BYTE( "mr-l0-b.bin",  0x000000, 0x040000, 0xa457ab44 );
		ROM_LOAD16_BYTE( "mr-h1-b.bin",  0x080001, 0x010000, 0xe17649b9 );
		ROM_LOAD16_BYTE( "mr-l1-b.bin",  0x080000, 0x010000, 0xa87c62b4 );
	
		ROM_REGION( 0x100000 * 2, REGION_CPU2, 0 );/* 1MB for the audio CPU - encrypted V30 = NANAO custom D80001 (?) */
		ROM_LOAD16_BYTE( "mr-sh0.bin",0x000001, 0x010000, 0x50d335e4 );
		ROM_LOAD16_BYTE( "mr-sl0.bin",0x000000, 0x010000, 0x0fa32721 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* Tiles */
		ROM_LOAD( "mr-c0.bin", 0x000000, 0x040000, 0x872a8fad );
		ROM_LOAD( "mr-c1.bin", 0x040000, 0x040000, 0xd2ffb27a );
		ROM_LOAD( "mr-c2.bin", 0x080000, 0x040000, 0x62bff287 );
		ROM_LOAD( "mr-c3.bin", 0x0c0000, 0x040000, 0xd0da62ab );
	
		ROM_REGION( 0x400000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "mr-o00.bin", 0x000000, 0x080000, 0xa0f9ce16 );
		ROM_LOAD( "mr-o10.bin", 0x100000, 0x080000, 0x4e70a9e9 );
		ROM_LOAD( "mr-o20.bin", 0x200000, 0x080000, 0xb9c468fc );
		ROM_LOAD( "mr-o30.bin", 0x300000, 0x080000, 0xcc32433a );
	
		ROM_REGION( 0x40000, REGION_SOUND1, ROMREGION_SOUNDONLY );
		ROM_LOAD( "mr-da.bin" ,0x000000, 0x040000, 0x1a11fc59 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_gunhohki = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x100000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "mr-h0.bin",  0x000001, 0x040000, 0x83352270 );
		ROM_LOAD16_BYTE( "mr-l0.bin",  0x000000, 0x040000, 0x9db308ae );
		ROM_LOAD16_BYTE( "mr-h1.bin",  0x080001, 0x010000, 0xc9532b60 );
		ROM_LOAD16_BYTE( "mr-l1.bin",  0x080000, 0x010000, 0x6349b520 );
	
		ROM_REGION( 0x100000 * 2, REGION_CPU2, 0 );/* 1MB for the audio CPU - encrypted V30 = NANAO custom D80001 (?) */
		ROM_LOAD16_BYTE( "mr-sh0.bin",0x000001, 0x010000, 0x50d335e4 );
		ROM_LOAD16_BYTE( "mr-sl0.bin",0x000000, 0x010000, 0x0fa32721 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* Tiles */
		ROM_LOAD( "mr-c0.bin", 0x000000, 0x040000, 0x872a8fad );
		ROM_LOAD( "mr-c1.bin", 0x040000, 0x040000, 0xd2ffb27a );
		ROM_LOAD( "mr-c2.bin", 0x080000, 0x040000, 0x62bff287 );
		ROM_LOAD( "mr-c3.bin", 0x0c0000, 0x040000, 0xd0da62ab );
	
		ROM_REGION( 0x400000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "mr-o00.bin", 0x000000, 0x080000, 0xa0f9ce16 );
		ROM_LOAD( "mr-o10.bin", 0x100000, 0x080000, 0x4e70a9e9 );
		ROM_LOAD( "mr-o20.bin", 0x200000, 0x080000, 0xb9c468fc );
		ROM_LOAD( "mr-o30.bin", 0x300000, 0x080000, 0xcc32433a );
	
		ROM_REGION( 0x40000, REGION_SOUND1, ROMREGION_SOUNDONLY );
		ROM_LOAD( "mr-da.bin" ,0x000000, 0x040000, 0x1a11fc59 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_uccops = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x100000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "uc_h0.rom",  0x000001, 0x040000, 0x240aa5f7 );
		ROM_LOAD16_BYTE( "uc_l0.rom",  0x000000, 0x040000, 0xdf9a4826 );
		ROM_LOAD16_BYTE( "uc_h1.rom",  0x080001, 0x020000, 0x8d29bcd6 );
		ROM_LOAD16_BYTE( "uc_l1.rom",  0x080000, 0x020000, 0xa8a402d8 );
	
		ROM_REGION( 0x100000 * 2, REGION_CPU2, 0 );/* 1MB for the audio CPU - encrypted V30 = NANAO custom D80001 (?) */
		ROM_LOAD16_BYTE( "uc_sh0.rom", 0x000001, 0x010000, 0xdf90b198 );
		ROM_LOAD16_BYTE( "uc_sl0.rom", 0x000000, 0x010000, 0x96c11aac );
	
		ROM_REGION( 0x200000, REGION_GFX1, ROMREGION_DISPOSE );/* Tiles */
		ROM_LOAD( "uc_w38m.rom", 0x000000, 0x080000, 0x130a40e5 );
		ROM_LOAD( "uc_w39m.rom", 0x080000, 0x080000, 0xe42ca144 );
		ROM_LOAD( "uc_w40m.rom", 0x100000, 0x080000, 0xc2961648 );
		ROM_LOAD( "uc_w41m.rom", 0x180000, 0x080000, 0xf5334b80 );
	
		ROM_REGION( 0x400000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "uc_k16m.rom", 0x000000, 0x100000, 0x4a225f09 );
		ROM_LOAD( "uc_k17m.rom", 0x100000, 0x100000, 0xe4ed9a54 );
		ROM_LOAD( "uc_k18m.rom", 0x200000, 0x100000, 0xa626eb12 );
		ROM_LOAD( "uc_k19m.rom", 0x300000, 0x100000, 0x5df46549 );
	
		ROM_REGION( 0x80000, REGION_SOUND1, ROMREGION_SOUNDONLY );
		ROM_LOAD( "uc_w42.rom", 0x000000, 0x080000, 0xd17d3fd6 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_uccopsj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x100000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "uca-h0.bin", 0x000001, 0x040000, 0x9e17cada );
		ROM_LOAD16_BYTE( "uca-l0.bin", 0x000000, 0x040000, 0x4a4e3208 );
		ROM_LOAD16_BYTE( "uca-h1.bin", 0x080001, 0x020000, 0x83f78dea );
		ROM_LOAD16_BYTE( "uca-l1.bin", 0x080000, 0x020000, 0x19628280 );
	
		ROM_REGION( 0x100000 * 2, REGION_CPU2, 0 );/* 1MB for the audio CPU - encrypted V30 = NANAO custom D80001 (?) */
		ROM_LOAD16_BYTE( "uca-sh0.bin", 0x000001, 0x010000, 0xf0ca1b03 );
		ROM_LOAD16_BYTE( "uca-sl0.bin", 0x000000, 0x010000, 0xd1661723 );
	
		ROM_REGION( 0x200000, REGION_GFX1, ROMREGION_DISPOSE );/* Tiles */
		ROM_LOAD( "uca-c0.bin", 0x000000, 0x080000, 0x6a419a36 );
		ROM_LOAD( "uca-c1.bin", 0x080000, 0x080000, 0xd703ecc7 );
		ROM_LOAD( "uca-c2.bin", 0x100000, 0x080000, 0x96397ac6 );
		ROM_LOAD( "uca-c3.bin", 0x180000, 0x080000, 0x5d07d10d );
	
		ROM_REGION( 0x400000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "uca-o3.bin", 0x000000, 0x100000, 0x97f7775e );
		ROM_LOAD( "uca-o2.bin", 0x100000, 0x100000, 0x5e0b1d65 );
		ROM_LOAD( "uca-o1.bin", 0x200000, 0x100000, 0xbdc224b3 );
		ROM_LOAD( "uca-o0.bin", 0x300000, 0x100000, 0x7526daec );
	
		ROM_REGION( 0x80000, REGION_SOUND1, ROMREGION_SOUNDONLY );
		ROM_LOAD( "uca-da.bin", 0x000000, 0x080000, 0x0b2855e9 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_lethalth = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x100000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "lt_d-h0.rom",  0x000001, 0x020000, 0x20c68935 );
		ROM_LOAD16_BYTE( "lt_d-l0.rom",  0x000000, 0x020000, 0xe1432fb3 );
		ROM_LOAD16_BYTE( "lt_d-h1.rom",  0x040001, 0x020000, 0xd7dd3d48 );
		ROM_LOAD16_BYTE( "lt_d-l1.rom",  0x040000, 0x020000, 0xb94b3bd8 );
	
		ROM_REGION( 0x100000 * 2, REGION_CPU2, 0 );/* 1MB for the audio CPU */
		ROM_LOAD16_BYTE( "lt_d-sh0.rom",0x000001, 0x010000, 0xaf5b224f );
		ROM_LOAD16_BYTE( "lt_d-sl0.rom",0x000000, 0x010000, 0xcb3faac3 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* Tiles */
		ROM_LOAD( "lt_7a.rom", 0x000000, 0x040000, 0xada0fd50 );
		ROM_LOAD( "lt_7b.rom", 0x040000, 0x040000, 0xd2596883 );
		ROM_LOAD( "lt_7d.rom", 0x080000, 0x040000, 0x2de637ef );
		ROM_LOAD( "lt_7h.rom", 0x0c0000, 0x040000, 0x9f6585cd );
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "lt_7j.rom", 0x000000, 0x040000, 0xbaf8863e );
		ROM_LOAD( "lt_7l.rom", 0x040000, 0x040000, 0x40fd50af );
		ROM_LOAD( "lt_7s.rom", 0x080000, 0x040000, 0xc8e970df );
		ROM_LOAD( "lt_7y.rom", 0x0c0000, 0x040000, 0xf5436708 );
	
		ROM_REGION( 0x40000, REGION_SOUND1, ROMREGION_SOUNDONLY );
		ROM_LOAD( "lt_8a.rom" ,0x000000, 0x040000, 0x357762a2 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_thndblst = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x100000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "lt_d-h0j.rom", 0x000001, 0x020000, 0xdc218a18 );
		ROM_LOAD16_BYTE( "lt_d-l0j.rom", 0x000000, 0x020000, 0xae9a3f81 );
		ROM_LOAD16_BYTE( "lt_d-h1.rom",  0x040001, 0x020000, 0xd7dd3d48 );
		ROM_LOAD16_BYTE( "lt_d-l1.rom",  0x040000, 0x020000, 0xb94b3bd8 );
	
		ROM_REGION( 0x100000 * 2, REGION_CPU2, 0 );/* 1MB for the audio CPU */
		ROM_LOAD16_BYTE( "lt_d-sh0.rom",0x000001, 0x010000, 0xaf5b224f );
		ROM_LOAD16_BYTE( "lt_d-sl0.rom",0x000000, 0x010000, 0xcb3faac3 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* Tiles */
		ROM_LOAD( "lt_7a.rom", 0x000000, 0x040000, 0xada0fd50 );
		ROM_LOAD( "lt_7b.rom", 0x040000, 0x040000, 0xd2596883 );
		ROM_LOAD( "lt_7d.rom", 0x080000, 0x040000, 0x2de637ef );
		ROM_LOAD( "lt_7h.rom", 0x0c0000, 0x040000, 0x9f6585cd );
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "lt_7j.rom", 0x000000, 0x040000, 0xbaf8863e );
		ROM_LOAD( "lt_7l.rom", 0x040000, 0x040000, 0x40fd50af );
		ROM_LOAD( "lt_7s.rom", 0x080000, 0x040000, 0xc8e970df );
		ROM_LOAD( "lt_7y.rom", 0x0c0000, 0x040000, 0xf5436708 );
	
		ROM_REGION( 0x40000, REGION_SOUND1, ROMREGION_SOUNDONLY );
		ROM_LOAD( "lt_8a.rom" ,0x000000, 0x040000, 0x357762a2 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_nbbatman = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x180000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "a1-h0-a.34",  0x000001, 0x040000, 0x24a9b794 );
		ROM_LOAD16_BYTE( "a1-l0-a.31",  0x000000, 0x040000, 0x846d7716 );
		ROM_LOAD16_BYTE( "a1-h1-.33",   0x100001, 0x040000, 0x3ce2aab5 );
		ROM_LOAD16_BYTE( "a1-l1-.32",   0x100000, 0x040000, 0x116d9bcc );
	
		ROM_REGION( 0x100000 * 2, REGION_CPU2, 0 );/* 1MB for the audio CPU */
		ROM_LOAD16_BYTE( "a1-sh0-.14",0x000001, 0x010000, 0xb7fae3e6 );
		ROM_LOAD16_BYTE( "a1-sl0-.17",0x000000, 0x010000, 0xb26d54fc );
	
		ROM_REGION( 0x200000, REGION_GFX1, ROMREGION_DISPOSE );/* Tiles */
		ROM_LOAD( "lh534k0c.9",  0x000000, 0x080000, 0x314a0c6d );
		ROM_LOAD( "lh534k0e.10", 0x080000, 0x080000, 0xdc31675b );
		ROM_LOAD( "lh534k0f.11", 0x100000, 0x080000, 0xe15d8bfb );
		ROM_LOAD( "lh534k0g.12", 0x180000, 0x080000, 0x888d71a3 );
	
		ROM_REGION( 0x400000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "lh538393.42", 0x000000, 0x100000, 0x26cdd224 );
		ROM_LOAD( "lh538394.43", 0x100000, 0x100000, 0x4bbe94fa );
		ROM_LOAD( "lh538395.44", 0x200000, 0x100000, 0x2a533b5e );
		ROM_LOAD( "lh538396.45", 0x300000, 0x100000, 0x863a66fa );
	
		ROM_REGION( 0x80000, REGION_SOUND1, ROMREGION_SOUNDONLY );
		ROM_LOAD( "lh534k0k.8" ,0x000000, 0x080000, 0x735e6380 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_leaguemn = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x180000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "lma1-h0.34",  0x000001, 0x040000, 0x47c54204 );
		ROM_LOAD16_BYTE( "lma1-l0.31",  0x000000, 0x040000, 0x1d062c82 );
		ROM_LOAD16_BYTE( "a1-h1-.33",   0x100001, 0x040000, 0x3ce2aab5 );
		ROM_LOAD16_BYTE( "a1-l1-.32",   0x100000, 0x040000, 0x116d9bcc );
	
		ROM_REGION( 0x100000 * 2, REGION_CPU2, 0 );/* 1MB for the audio CPU */
		ROM_LOAD16_BYTE( "a1-sh0-.14",0x000001, 0x010000, 0xb7fae3e6 );
		ROM_LOAD16_BYTE( "a1-sl0-.17",0x000000, 0x010000, 0xb26d54fc );
	
		ROM_REGION( 0x200000, REGION_GFX1, ROMREGION_DISPOSE );/* Tiles */
		ROM_LOAD( "lh534k0c.9",  0x000000, 0x080000, 0x314a0c6d );
		ROM_LOAD( "lh534k0e.10", 0x080000, 0x080000, 0xdc31675b );
		ROM_LOAD( "lh534k0f.11", 0x100000, 0x080000, 0xe15d8bfb );
		ROM_LOAD( "lh534k0g.12", 0x180000, 0x080000, 0x888d71a3 );
	
		ROM_REGION( 0x400000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD( "lh538393.42", 0x000000, 0x100000, 0x26cdd224 );
		ROM_LOAD( "lh538394.43", 0x100000, 0x100000, 0x4bbe94fa );
		ROM_LOAD( "lh538395.44", 0x200000, 0x100000, 0x2a533b5e );
		ROM_LOAD( "lh538396.45", 0x300000, 0x100000, 0x863a66fa );
	
		ROM_REGION( 0x80000, REGION_SOUND1, ROMREGION_SOUNDONLY );
		ROM_LOAD( "lh534k0k.8" ,0x000000, 0x080000, 0x735e6380 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_psoldier = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x100000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE( "f3_h0d.h0",  0x000001, 0x040000, 0x38f131fd );
		ROM_LOAD16_BYTE( "f3_l0d.l0",  0x000000, 0x040000, 0x1662969c );
		ROM_LOAD16_BYTE( "f3_h1.h1",   0x080001, 0x040000, 0xc8d1947c );
		ROM_LOAD16_BYTE( "f3_l1.l1",   0x080000, 0x040000, 0x7b9492fc );
	
		ROM_REGION( 0x100000 * 2, REGION_CPU2, 0 );/* 1MB for the audio CPU */
		ROM_LOAD16_BYTE( "f3_sh0.sh0",0x000001, 0x010000, 0x90b55e5e );
		ROM_LOAD16_BYTE( "f3_sl0.sl0",0x000000, 0x010000, 0x77c16d57 );
	
		ROM_REGION( 0x200000, REGION_GFX1, ROMREGION_DISPOSE );/* Tiles */
		ROM_LOAD( "f3_w50.c0",  0x000000, 0x040000, 0x47e788ee );
		ROM_LOAD( "f3_w51.c1",  0x080000, 0x040000, 0x8e535e3f );
		ROM_LOAD( "f3_w52.c2",  0x100000, 0x040000, 0xa6eb2e56 );
		ROM_LOAD( "f3_w53.c3",  0x180000, 0x040000, 0x2f992807 );
	
		ROM_REGION( 0x800000, REGION_GFX2, ROMREGION_DISPOSE );/* Sprites */
		ROM_LOAD16_BYTE( "f3_w37.000", 0x000001, 0x100000, 0xfd4cda03 );
		ROM_LOAD16_BYTE( "f3_w38.001", 0x000000, 0x100000, 0x755bab10 );
		ROM_LOAD16_BYTE( "f3_w39.010", 0x200001, 0x100000, 0xb21ced92 );
		ROM_LOAD16_BYTE( "f3_w40.011", 0x200000, 0x100000, 0x2e906889 );
		ROM_LOAD16_BYTE( "f3_w41.020", 0x400001, 0x100000, 0x02455d10 );
		ROM_LOAD16_BYTE( "f3_w42.021", 0x400000, 0x100000, 0x124589b9 );
		ROM_LOAD16_BYTE( "f3_w43.030", 0x600001, 0x100000, 0xdae7327a );
		ROM_LOAD16_BYTE( "f3_w44.031", 0x600000, 0x100000, 0xd0fc84ac );
	
		ROM_REGION( 0x80000, REGION_SOUND1, ROMREGION_SOUNDONLY );
		ROM_LOAD( "f3_w95.da" ,0x000000, 0x080000, 0xf7ca432b );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_dsccr94j = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x180000, REGION_CPU1, 0 );
		ROM_LOAD16_BYTE("a3_-h0-e.bin", 0x000001, 0x040000, 0x8de1dbcd );
		ROM_LOAD16_BYTE("a3_-l0-e.bin", 0x000000, 0x040000, 0xd3df8bfd );
		ROM_LOAD16_BYTE("ds_h1-c.rom",  0x100001, 0x040000, 0x6109041b );
		ROM_LOAD16_BYTE("ds_l1-c.rom",  0x100000, 0x040000, 0x97a01f6b );
	
		ROM_REGION( 0x100000 * 2, REGION_CPU2, 0 );
		ROM_LOAD16_BYTE("ds_sh0.rom",   0x000001, 0x010000, 0x23fe6ffc );
		ROM_LOAD16_BYTE("ds_sl0.rom",   0x000000, 0x010000, 0x768132e5 );
	
		ROM_REGION( 0x400000, REGION_GFX1, ROMREGION_DISPOSE );/* chars */
		ROM_LOAD("c0.bin",   0x000000, 0x100000, 0x83ea8a47 );
		ROM_LOAD("c1.bin",   0x100000, 0x100000, 0x64063e6d );
		ROM_LOAD("c2.bin",   0x200000, 0x100000, 0xcc1f621a );
		ROM_LOAD("c3.bin",   0x300000, 0x100000, 0x515829e1 );
	
		ROM_REGION( 0x400000, REGION_GFX2, ROMREGION_DISPOSE );/* sprites */
		ROM_LOAD16_BYTE("a3-o00-w.bin",   0x000001, 0x80000, 0xb094e5ad );
		ROM_LOAD16_BYTE("a3-o01-w.bin",   0x000000, 0x80000, 0x91f34018 );
		ROM_LOAD16_BYTE("a3-o10-w.bin",   0x100001, 0x80000, 0xedddeef4 );
		ROM_LOAD16_BYTE("a3-o11-w.bin",   0x100000, 0x80000, 0x274a9526 );
		ROM_LOAD16_BYTE("a3-o20-w.bin",   0x200001, 0x80000, 0x32064393 );
		ROM_LOAD16_BYTE("a3-o21-w.bin",   0x200000, 0x80000, 0x57bae3d9 );
		ROM_LOAD16_BYTE("a3-o30-w.bin",   0x300001, 0x80000, 0xbe838e2f );
		ROM_LOAD16_BYTE("a3-o31-w.bin",   0x300000, 0x80000, 0xbf899f0d );
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );
		ROM_LOAD("ds_da0.rom" ,  0x000000, 0x100000, 0x67fc52fd );
	ROM_END(); }}; 
	
	/***************************************************************************/
	
	public static ReadHandlerPtr lethalth_cycle_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if (cpu_get_pc()==0x1f4 && m92_ram[0x1e]==2 && offset==0)
			cpu_spinuntil_int();
	
		return m92_ram[0x1e + offset];
	} };
	
	public static ReadHandlerPtr hook_cycle_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if (cpu_get_pc()==0x55ba && m92_ram[0x12]==0 && m92_ram[0x13]==0 && offset==0)
			cpu_spinuntil_int();
	
		return m92_ram[0x12 + offset];
	} };
	
	public static ReadHandlerPtr bmaster_cycle_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int d=cpu_geticount();
	
		/* If possible skip this cpu segment - idle loop */
		if (d>159 && d<0xf0000000) {
			if (cpu_get_pc()==0x410 && m92_ram[0x6fde]==0 && m92_ram[0x6fdf]==0 && offset==0) {
				/* Adjust in-game counter, based on cycles left to run */
				int old;
	
				old=m92_ram[0x74aa]+(m92_ram[0x74ab]<<8);
				old=(old+d/25)&0xffff; /* 25 cycles per increment */
				m92_ram[0x74aa]=old&0xff;
				m92_ram[0x74ab]=old>>8;
				cpu_spinuntil_int();
			}
		}
		return m92_ram[0x6fde + offset];
	} };
	
	public static ReadHandlerPtr psoldier_cycle_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int a=m92_ram[0]+(m92_ram[1]<<8);
		int b=m92_ram[0x1aec]+(m92_ram[0x1aed]<<8);
		int c=m92_ram[0x1aea]+(m92_ram[0x1aeb]<<8);
	
		if (cpu_get_pc()==0x2dae && b!=a && c!=a && offset==0)
			cpu_spinuntil_int();
	
		return m92_ram[0x1aec + offset];
	} };
	
	public static ReadHandlerPtr inthunt_cycle_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int d=cpu_geticount();
		int line = 256 - cpu_getiloops();
	
		/* If possible skip this cpu segment - idle loop */
		if (d>159 && d<0xf0000000 && line<247) {
			if (cpu_get_pc()==0x858 && m92_ram[0x25f]==0 && offset==1) {
				/* Adjust in-game counter, based on cycles left to run */
				int old;
	
				old=m92_ram[0xb892]+(m92_ram[0xb893]<<8);
				old=(old+d/82)&0xffff; /* 82 cycles per increment */
				m92_ram[0xb892]=old&0xff;
				m92_ram[0xb893]=old>>8;
	
				cpu_spinuntil_int();
			}
		}
	
		return m92_ram[0x25e + offset];
	} };
	
	public static ReadHandlerPtr uccops_cycle_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int a=m92_ram[0x3f28]+(m92_ram[0x3f29]<<8);
		int b=m92_ram[0x3a00]+(m92_ram[0x3a01]<<8);
		int c=m92_ram[0x3a02]+(m92_ram[0x3a03]<<8);
		int d=cpu_geticount();
		int line = 256 - cpu_getiloops();
	
		/* If possible skip this cpu segment - idle loop */
		if (d>159 && d<0xf0000000 && line<247) {
			if ((cpu_get_pc()==0x900ff || cpu_get_pc()==0x90103) && b==c && offset==1) {
				cpu_spinuntil_int();
				/* Update internal counter based on cycles left to run */
				a=(a+d/127)&0xffff; /* 127 cycles per loop increment */
				m92_ram[0x3f28]=a&0xff;
				m92_ram[0x3f29]=a>>8;
			}
		}
	
		return m92_ram[0x3a02 + offset];
	} };
	
	public static ReadHandlerPtr rtypeleo_cycle_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if (cpu_get_pc()==0x307a3 && offset==0 && m92_ram[0x32]==2 && m92_ram[0x33]==0)
			cpu_spinuntil_int();
	
		return m92_ram[0x32 + offset];
	} };
	
	public static ReadHandlerPtr gunforce_cycle_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int a=m92_ram[0x6542]+(m92_ram[0x6543]<<8);
		int b=m92_ram[0x61d0]+(m92_ram[0x61d1]<<8);
		int d=cpu_geticount();
		int line = 256 - cpu_getiloops();
	
		/* If possible skip this cpu segment - idle loop */
		if (d>159 && d<0xf0000000 && line<247) {
			if (cpu_get_pc()==0x40a && ((b&0x8000)==0) && offset==1) {
				cpu_spinuntil_int();
				/* Update internal counter based on cycles left to run */
				a=(a+d/80)&0xffff; /* 80 cycles per loop increment */
				m92_ram[0x6542]=a&0xff;
				m92_ram[0x6543]=a>>8;
			}
		}
	
		return m92_ram[0x61d0 + offset];
	} };
	
	public static ReadHandlerPtr dsccr94j_cycle_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int a=m92_ram[0x965a]+(m92_ram[0x965b]<<8);
		int d=cpu_geticount();
	
		if (cpu_get_pc()==0x988 && m92_ram[0x8636]==0 && offset==0) {
			cpu_spinuntil_int();
	
			/* Update internal counter based on cycles left to run */
			a=(a+d/56)&0xffff; /* 56 cycles per loop increment */
			m92_ram[0x965a]=a&0xff;
			m92_ram[0x965b]=a>>8;
		}
	
		return m92_ram[0x8636 + offset];
	} };
	
	/***************************************************************************/
	
	static void m92_startup(void)
	{
		unsigned char *RAM = memory_region(REGION_CPU1);
	
		memcpy(RAM+0xffff0,RAM+0x7fff0,0x10); /* Start vector */
		cpu_setbank(1,&RAM[0xa0000]); /* Initial bank */
	
		/* Mirror used by In The Hunt for protection */
		memcpy(RAM+0xc0000,RAM+0x00000,0x10000);
		cpu_setbank(2,&RAM[0xc0000]);
	
		RAM = memory_region(REGION_CPU2);
		memcpy(RAM+0xffff0,RAM+0x1fff0,0x10); /* Sound cpu Start vector */
	
		m92_game_kludge=0;
		m92_irq_vectorbase=0x80;
		m92_raster_enable=1;
		m92_sprite_buffer_busy=0x80;
	}
	
	static void init_m92(unsigned char *decryption_table)
	{
		m92_startup();
		setvector_callback(VECTOR_INIT);
		irem_cpu_decrypt(1,decryption_table);
	}
	
	static public static InitDriverPtr init_bmaster = new InitDriverPtr() { public void handler() 
	{
		install_mem_read_handler(0, 0xe6fde, 0xe6fdf, bmaster_cycle_r);
		init_m92(bomberman_decryption_table);
	} };
	
	static public static InitDriverPtr init_gunforce = new InitDriverPtr() { public void handler() 
	{
		install_mem_read_handler(0, 0xe61d0, 0xe61d1, gunforce_cycle_r);
		init_m92(gunforce_decryption_table);
	} };
	
	static public static InitDriverPtr init_hook = new InitDriverPtr() { public void handler() 
	{
		install_mem_read_handler(0, 0xe0012, 0xe0013, hook_cycle_r);
		init_m92(hook_decryption_table);
	} };
	
	static public static InitDriverPtr init_mysticri = new InitDriverPtr() { public void handler() 
	{
		init_m92(mysticri_decryption_table);
	} };
	
	static public static InitDriverPtr init_uccops = new InitDriverPtr() { public void handler() 
	{
		install_mem_read_handler(0, 0xe3a02, 0xe3a03, uccops_cycle_r);
		init_m92(dynablaster_decryption_table);
	} };
	
	static public static InitDriverPtr init_rtypeleo = new InitDriverPtr() { public void handler() 
	{
		install_mem_read_handler(0, 0xe0032, 0xe0033, rtypeleo_cycle_r);
		init_m92(rtypeleo_decryption_table);
		m92_irq_vectorbase=0x20;
		m92_game_kludge=1;
	} };
	
	static public static InitDriverPtr init_majtitl2 = new InitDriverPtr() { public void handler() 
	{
		init_m92(majtitl2_decryption_table);
	
		/* This game has an eprom on the game board */
		install_mem_read_handler(0, 0xf0000, 0xf3fff, m92_eeprom_r);
		install_mem_write_handler(0, 0xf0000, 0xf3fff, m92_eeprom_w);
	
		m92_game_kludge=2;
	} };
	
	static public static InitDriverPtr init_inthunt = new InitDriverPtr() { public void handler() 
	{
		install_mem_read_handler(0, 0xe025e, 0xe025f, inthunt_cycle_r);
		init_m92(inthunt_decryption_table);
	} };
	
	static public static InitDriverPtr init_lethalth = new InitDriverPtr() { public void handler() 
	{
		install_mem_read_handler(0, 0xe001e, 0xe001f, lethalth_cycle_r);
		init_m92(lethalth_decryption_table);
		m92_irq_vectorbase=0x20;
	
		/* This game sets the raster IRQ position, but the interrupt routine
			is just an iret, no need to emulate it */
		m92_raster_enable=0;
		m92_game_kludge=3; /* No upper palette bank? It could be a different motherboard */
	} };
	
	static public static InitDriverPtr init_nbbatman = new InitDriverPtr() { public void handler() 
	{
		unsigned char *RAM = memory_region(REGION_CPU1);
	
		init_m92(leagueman_decryption_table);
	
		memcpy(RAM+0x80000,RAM+0x100000,0x20000);
	} };
	
	static public static InitDriverPtr init_psoldier = new InitDriverPtr() { public void handler() 
	{
		install_mem_read_handler(0, 0xe1aec, 0xe1aed, psoldier_cycle_r);
		init_m92(psoldier_decryption_table);
		m92_irq_vectorbase=0x20;
		/* main CPU expects an answer even before writing the first command */
		sound_status = 0x80;
	} };
	
	static public static InitDriverPtr init_dsccr94j = new InitDriverPtr() { public void handler() 
	{
		install_mem_read_handler(0, 0xe8636, 0xe8637, dsccr94j_cycle_r);
		init_m92(dsoccr94_decryption_table);
	} };
	
	/***************************************************************************/
	
	/* The 'nonraster' machine is for games that don't use the raster interrupt feature - slightly faster to emulate */
	public static GameDriver driver_gunforce	   = new GameDriver("1991"	,"gunforce"	,"m92.java"	,rom_gunforce,null	,machine_driver_raster	,input_ports_gunforce	,init_gunforce	,ROT0	,	"Irem",         "Gunforce - Battle Fire Engulfed Terror Island (World)" )
	public static GameDriver driver_gunforcj	   = new GameDriver("1991"	,"gunforcj"	,"m92.java"	,rom_gunforcj,driver_gunforce	,machine_driver_raster	,input_ports_gunforce	,init_gunforce	,ROT0	,	"Irem",         "Gunforce - Battle Fire Engulfed Terror Island (Japan)" )
	public static GameDriver driver_gunforcu	   = new GameDriver("1991"	,"gunforcu"	,"m92.java"	,rom_gunforcu,driver_gunforce	,machine_driver_raster	,input_ports_gunforce	,init_gunforce	,ROT0	,	"Irem America", "Gunforce - Battle Fire Engulfed Terror Island (US)" )
	public static GameDriver driver_bmaster	   = new GameDriver("1991"	,"bmaster"	,"m92.java"	,rom_bmaster,null	,machine_driver_nonraster	,input_ports_bmaster	,init_bmaster	,ROT0	,	"Irem",         "Blade Master (World)" )
	public static GameDriver driver_lethalth	   = new GameDriver("1991"	,"lethalth"	,"m92.java"	,rom_lethalth,null	,machine_driver_lethalth	,input_ports_lethalth	,init_lethalth	,ROT270	,	"Irem",         "Lethal Thunder (World)" )
	public static GameDriver driver_thndblst	   = new GameDriver("1991"	,"thndblst"	,"m92.java"	,rom_thndblst,driver_lethalth	,machine_driver_lethalth	,input_ports_lethalth	,init_lethalth	,ROT270	,	"Irem",         "Thunder Blaster (Japan)" )
	public static GameDriver driver_uccops	   = new GameDriver("1992"	,"uccops"	,"m92.java"	,rom_uccops,null	,machine_driver_raster	,input_ports_uccops	,init_uccops	,ROT0	,	"Irem",         "Undercover Cops (World)" )
	public static GameDriver driver_uccopsj	   = new GameDriver("1992"	,"uccopsj"	,"m92.java"	,rom_uccopsj,driver_uccops	,machine_driver_raster	,input_ports_uccops	,init_uccops	,ROT0	,	"Irem",         "Undercover Cops (Japan)" )
	public static GameDriver driver_mysticri	   = new GameDriver("1992"	,"mysticri"	,"m92.java"	,rom_mysticri,null	,machine_driver_nonraster	,input_ports_mysticri	,init_mysticri	,ROT0	,	"Irem",         "Mystic Riders (World)" )
	public static GameDriver driver_gunhohki	   = new GameDriver("1992"	,"gunhohki"	,"m92.java"	,rom_gunhohki,driver_mysticri	,machine_driver_nonraster	,input_ports_mysticri	,init_mysticri	,ROT0	,	"Irem",         "Gun Hohki (Japan)" )
	public static GameDriver driver_majtitl2	   = new GameDriver("1992"	,"majtitl2"	,"m92.java"	,rom_majtitl2,null	,machine_driver_raster	,input_ports_majtitl2	,init_majtitl2	,ROT0	,	"Irem",         "Major Title 2 (World)", GAME_IMPERFECT_GRAPHICS )
	public static GameDriver driver_skingame	   = new GameDriver("1992"	,"skingame"	,"m92.java"	,rom_skingame,driver_majtitl2	,machine_driver_raster	,input_ports_majtitl2	,init_majtitl2	,ROT0	,	"Irem America", "The Irem Skins Game (US set 1)", GAME_IMPERFECT_GRAPHICS )
	public static GameDriver driver_skingam2	   = new GameDriver("1992"	,"skingam2"	,"m92.java"	,rom_skingam2,driver_majtitl2	,machine_driver_raster	,input_ports_majtitl2	,init_majtitl2	,ROT0	,	"Irem America", "The Irem Skins Game (US set 2)", GAME_IMPERFECT_GRAPHICS )
	public static GameDriver driver_hook	   = new GameDriver("1992"	,"hook"	,"m92.java"	,rom_hook,null	,machine_driver_nonraster	,input_ports_hook	,init_hook	,ROT0	,	"Irem",         "Hook (World)" )
	public static GameDriver driver_hooku	   = new GameDriver("1992"	,"hooku"	,"m92.java"	,rom_hooku,driver_hook	,machine_driver_nonraster	,input_ports_hook	,init_hook	,ROT0	,	"Irem America", "Hook (US)" )
	public static GameDriver driver_rtypeleo	   = new GameDriver("1992"	,"rtypeleo"	,"m92.java"	,rom_rtypeleo,null	,machine_driver_raster	,input_ports_rtypeleo	,init_rtypeleo	,ROT0	,	"Irem",         "R-Type Leo (Japan)" )
	public static GameDriver driver_inthunt	   = new GameDriver("1993"	,"inthunt"	,"m92.java"	,rom_inthunt,null	,machine_driver_raster	,input_ports_inthunt	,init_inthunt	,ROT0	,	"Irem",         "In The Hunt (World)" )
	public static GameDriver driver_inthuntu	   = new GameDriver("1993"	,"inthuntu"	,"m92.java"	,rom_inthuntu,driver_inthunt	,machine_driver_raster	,input_ports_inthunt	,init_inthunt	,ROT0	,	"Irem America", "In The Hunt (US)" )
	public static GameDriver driver_kaiteids	   = new GameDriver("1993"	,"kaiteids"	,"m92.java"	,rom_kaiteids,driver_inthunt	,machine_driver_raster	,input_ports_inthunt	,init_inthunt	,ROT0	,	"Irem",         "Kaitei Daisensou (Japan)" )
	public static GameDriver driver_nbbatman	   = new GameDriver("1993"	,"nbbatman"	,"m92.java"	,rom_nbbatman,null	,machine_driver_raster	,input_ports_nbbatman	,init_nbbatman	,ROT0	,	"Irem America", "Ninja Baseball Batman (US)", GAME_IMPERFECT_GRAPHICS )
	public static GameDriver driver_leaguemn	   = new GameDriver("1993"	,"leaguemn"	,"m92.java"	,rom_leaguemn,driver_nbbatman	,machine_driver_raster	,input_ports_nbbatman	,init_nbbatman	,ROT0	,	"Irem",         "Yakyuu Kakutou League-Man (Japan)", GAME_IMPERFECT_GRAPHICS )
	public static GameDriver driver_psoldier	   = new GameDriver("1993"	,"psoldier"	,"m92.java"	,rom_psoldier,null	,machine_driver_psoldier	,input_ports_psoldier	,init_psoldier	,ROT0	,	"Irem",         "Perfect Soldiers (Japan)", GAME_IMPERFECT_SOUND )
	public static GameDriver driver_dsccr94j	   = new GameDriver("1994"	,"dsccr94j"	,"m92.java"	,rom_dsccr94j,driver_dsoccr94	,machine_driver_psoldier	,input_ports_dsccr94j	,init_dsccr94j	,ROT0	,	"Irem",         "Dream Soccer '94 (Japan)" )
}
