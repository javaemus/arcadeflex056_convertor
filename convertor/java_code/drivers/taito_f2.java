/***************************************************************************

Taito F2 System

driver by David Graves, Bryan McPhail, Brad Oliver, Andrew Prime, Brian
Troha, Nicola Salmoria with some initial help from Richard Bush

The Taito F2 system is a fairly flexible hardware platform. The main board
supports three 64x64 tiled scrolling background planes of 8x8 tiles, and a
powerful sprite engine capable of handling all the video chores by itself
(used in e.g. Super Space Invaders). The front tilemap has characters which
are generated in RAM for maximum versatility (fading effects etc.).
The expansion board can have additional gfx chip e.g. for a zooming/rotating
tilemap, or additional tilemap planes.

Sound is handled by a Z80 with a YM2610 connected to it.

The memory map for each of the games is similar but shuffled around.

Notes:
- Metal Black has secret command to select stage.
  Start the machine with holding service switch.
  Then push 1p start, 1p start, 1p start, service SW, 1p start
  while error message is displayed.

(This also works in many of the other games. Use in Don Doko Don to play
 an extra set of fifty levels.)


Custom chips
------------
The old version of the F2 main board (larger) has
TC0100SCN (tilemaps)
TC0200OBJ+TC0210FBC (sprites)
TC0140SYT (sound communication & other stuff)

The new version has
TC0100SCN (tilemaps)
TC0540OBN+TC0520TBC (sprites)
TC0530SYC (sound communication & other stuff)

            I/O    Priority / Palette      Additional gfx                 Other
         --------- ------------------- ----------------------- ----------------------------
finalb   TC0220IOC TC0110PCR TC0070RGB
dondokod TC0220IOC TC0360PRI TC0260DAR TC0280GRD(x2)(zoom/rot)
megab    TC0220IOC TC0360PRI TC0260DAR                         TC0030CMD(C-Chip protection)
thundfox TC0220IOC TC0360PRI TC0260DAR TC0100SCN (so it has two)
cameltry TC0220IOC TC0360PRI TC0260DAR TC0280GRD(x2)(zoom/rot)
qtorimon TC0220IOC TC0110PCR TC0070RGB
liquidk  TC0220IOC TC0360PRI TC0260DAR
quizhq   TMP82C265 TC0110PCR TC0070RGB
ssi      TC0510NIO 			 TC0260DAR
gunfront TC0510NIO TC0360PRI TC0260DAR
growl    TMP82C265 TC0360PRI TC0260DAR                         TC0190FMC(4 players input?sprite banking?)
mjnquest           TC0110PCR TC0070RGB
footchmp TE7750    TC0360PRI TC0260DAR TC0480SCP(tilemaps)     TC0190FMC(4 players input?sprite banking?)
koshien  TC0510NIO TC0360PRI TC0260DAR
yuyugogo TC0510NIO 			 TC0260DAR
ninjak   TE7750    TC0360PRI TC0260DAR                         TC0190FMC(4 players input?sprite banking?)
solfigtr ?         TC0360PRI TC0260DAR ?
qzquest  TC0510NIO 			 TC0260DAR
pulirula TC0510NIO TC0360PRI TC0260DAR TC0430GRW(zoom/rot)
metalb   TC0510NIO TC0360PRI TC0260DAR TC0480SCP(tilemaps)
qzchikyu TC0510NIO 			 TC0260DAR
yesnoj   TMP82C265           TC0260DAR                         TC8521AP(RTC?)
deadconx           TC0360PRI TC0260DAR TC0480SCP(tilemaps)     TC0190FMC(4 players input?sprite banking?)
dinorex  TC0510NIO TC0360PRI TC0260DAR
qjinsei  TC0510NIO TC0360PRI TC0260DAR
qcrayon  TC0510NIO TC0360PRI TC0260DAR
qcrayon2 TC0510NIO TC0360PRI TC0260DAR
driftout TC0510NIO TC0360PRI TC0260DAR TC0430GRW(zoom/rot)



F2 Game List
------------
. Final Blow                                                                       (1)
. Don Doko Don                                                                     (2)
. Mega Blast               http://www.taito.co.jp/game-history/80b/megabla.html    (3)
. Quiz Torimonochou        http://www.taito.co.jp/game-history/90a/qui_tori.html   (4)
. Quiz HQ                  http://www.taito.co.jp/game-history/90a/quiz_hq.html
. Thunder Fox              http://www.taito.co.jp/game-history/90a/thu_fox.html
. Liquid Kids              http://www.taito.co.jp/game-history/90a/miz_bak.html    (7)
. SSI / Majestic 12        http://www.taito.co.jp/game-history/90a/mj12.html       (8)
. Gun Frontier             http://www.taito.co.jp/game-history/90a/gunfro.html     (9)
. Growl / Runark           http://www.taito.co.jp/game-history/90a/runark.html    (10)
. Hat Trick Hero           http://www.taito.co.jp/game-history/90a/hthero.html    (11)
. Mahjong Quest            http://www.taito.co.jp/game-history/90a/mahque.html    (12)
. Yuu-yu no Quiz de Go!Go! http://www.taito.co.jp/game-history/90a/youyu.html     (13)
. Ah Eikou no Koshien      http://www.taito.co.jp/game-history/90a/koshien.html   (14)
. Ninja Kids               http://www.taito.co.jp/game-history/90a/ninjakids.html (15)
. Quiz Quest               http://www.taito.co.jp/game-history/90a/q_quest.html
. Metal Black              http://www.taito.co.jp/game-history/90a/metabla.html
. Quiz Chikyu Boueigun     http://www.taito.co.jp/game-history/90a/qui_tik.html
. Dinorex                  http://www.taito.co.jp/game-history/90a/dinorex.html
. Pulirula
. Dead Connection          http://www.taito.co.jp/game-history/90a/deadconn.html
. Quiz Jinsei Gekijou      http://www.taito.co.jp/game-history/90a/qui_jin.html
. Quiz Crayon Shinchan     http://www.taito.co.jp/game-history/90a/qcrashin.html
. Crayon Shinchan Orato Asobo


This list is translated version of
http://www.aianet.or.jp/~eisetu/rom/rom_tait.html
This page also contains info for other Taito boards.

F2 Motherboard ( Big ) K1100432A, J1100183A
               (Small) K1100608A, J1100242A

Apr.1989 Final Blow (B82, M4300123A, K1100433A)
Jul.1989 Don Doko Don (B95, M4300131A, K1100454A, J1100195A)
Oct.1989 Mega Blast (C11)
Feb.1990 Quiz Torimonochou (C41, K1100554A)
Apr.1990 Cameltry (C38, M4300167A, K1100556A)
Jul.1990 Quiz H.Q. (C53, K1100594A)
Aug.1990 Thunder Fox (C28, M4300181A, K1100580A) (exists in F1 version too)
Sep.1990 Liquid Kids/Mizubaku Daibouken (C49, K1100593A)
Nov.1990 MJ-12/Super Space Invaders (C64, M4300195A, K1100616A, J1100248A)
Jan.1991 Gun Frontier (C71, M4300199A, K1100625A, K1100629A(overseas))
Feb.1991 Growl/Runark (C74, M4300210A, K1100639A)
Mar.1991 Hat Trick Hero/Euro Football Championship (C80, K11J0646A)
Mar.1991 Yuu-yu no Quiz de Go!Go! (C83, K11J0652A)
Apr.1991 Ah Eikou no Koshien (C81, M43J0214A, K11J654A)
Apr.1991 Ninja Kids (C85, M43J0217A, K11J0659A)
May.1991 Mahjong Quest (C77, K1100637A, K1100637B)
Jul.1991 Quiz Quest (C92, K11J0678A)
Sep.1991 Metal Black (D12)
Oct.1991 Drift Out (Visco) (M43X0241A, K11X0695A)
Nov.1991 PuLiRuLa (C98, M43J0225A, K11J0672A)
Feb.1992 Quiz Chikyu Boueigun (D19, K11J0705A)
Jul.1992 Dead Connection (D28, K11J0715A)
Nov.1992 Dinorex (D39, K11J0254A)
Mar.1993 Quiz Jinsei Gekijou (D48, M43J0262A, K11J0742A)
Aug.1993 Quiz Crayon Shinchan (D55, K11J0758A)
Dec.1993 Crayon Shinchan Orato Asobo (D63, M43J0276A, K11J0779A)

Mar.1992 Yes.No. Shinri Tokimeki Chart (Fortune teller machine) (D20, K11J0706B)

Thunder Fox, Drift Out, "Quiz Crayon Shinchan", and "Crayon Shinchan
Orato Asobo" has "Not F2" version PCB.
Foreign version of Cameltry uses different hardware (B89's PLD,
K1100573A, K1100574A).




Sprite extension area types
===========================

These games need a special value for f2_spriteext:

Yuyugogo = 1
Pulirula = 2
Dinorex = 3
Quiz Crayon 1&2 = 3
Quiz Jinsei = 3
(all other games need it to be zero)

TODO Lists
==========

- The sprite system is still partly a mystery, and not an accurate emulation.
  A lot of sprite glitches are caused by data in sprite ram not being correct,
  part from one frame and part from the previous one. There has to be some
  buffering inside the chip but it's not clear how. See below the irq section
  for a long list of observations on sprite glitches.

  Other limitations include: misplaced tile of the zooming title in Qcrayon
  (the one on the yellow background in attract); sprites when you get a home
  run in Koshien are often out on x axis by 1 pixel.

- TC0480SCP emulation (footchmp, metalb, deadconx) has slight inaccuracies.
  Zoomed layers and zoomed pixel rows are not precisely positioned.

- DIPS, still many unknown

- Restored save states on some games tend to hang.

- All non-quiz games except Solfigtr have 2 country sets dumped: if 1 byte diff
  then create the third set.


Don Doko Don
------------

Roz layer is one pixel out vertically when screen flipped.


Cameltry (camltrua)
--------

Alt version with YM2203 sound missing ADPCM chip? Also sound tempo
may be fractionally too slow.


Gun Frontier
------------

There are mask sprites used on the waterfall in the first round
of attract demo, however it's not clear what they should mask since
there don't seem to be sprites below them. Shadow maybe?


Pulirula
--------

In level 3, the mask sprites used for the door are misaligned by one pixel to
the left.


Metal Black
-----------

Tilemap screenflip support has an issue: blue planet early in attract
should be 1 pixel left.

Sprite emulation issues may be responsible for minor glitches on the
"bolts" on round 4 boss ship: some sprite/tilemap lag creeps in.

Missing two blend effects: there's a sun sprite underneath tilemaps
in round 1; and the boss sprite crosses under the tilemaps at start
of round 5 finale.


Yesnoj
------

Input mapping incomplete. There's a 0x01 one which only seems to be
used in printer [printer test?] mode. It seems to be a printer status
input. With the value currently returned, it sounds an alarm and says
[Japanese trans.] "Error detected on the printer. Call machine operator."

The timer stays at 00:00. Missing RTC emulation?

[Coin lockout/ctr?]


Quiz Crayon 2
-------------

There should be a highlight circle around the player while it moves on the
map. This is done by a sprite which doesn't have priority over the
background. This is probably the same thing as the waterfall in Gun Frontier.


Driftout
--------

Sprites are 1 pixel too far right in screenflip.
Roz layer is around 4 pixels too far down in screenflip.


Driveout
--------

Sound

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package drivers;

public class taito_f2
{
	
	
	static int banknum = 0;
	static int mjnquest_input;
	static int yesnoj_dsw = 0;
	
	
	/**********************************************************
	                        GAME INPUTS
	**********************************************************/
	
	static WRITE16_HANDLER( taitof2_watchdog_w )
	{
		watchdog_reset_w(0,data);
	}
	
	static WRITE16_HANDLER( growl_coin_word_w )	/* what about coins 3&4 ?? */
	{
		if (ACCESSING_LSB)
		{
			coin_lockout_w(0, ~data & 0x01);
			coin_lockout_w(1, ~data & 0x02);
			coin_counter_w(0,  data & 0x04);
			coin_counter_w(1,  data & 0x08);
		}
	}
	
	static WRITE16_HANDLER( taitof2_4p_coin_word_w )
	{
		if (ACCESSING_LSB)
		{
			coin_lockout_w(0, ~data & 0x01);
			coin_lockout_w(1, ~data & 0x02);
			coin_lockout_w(2, ~data & 0x04);
			coin_lockout_w(3, ~data & 0x08);
			coin_counter_w(0,  data & 0x10);
			coin_counter_w(1,  data & 0x20);
			coin_counter_w(2,  data & 0x40);
			coin_counter_w(3,  data & 0x80);
		}
	}
	
	static WRITE16_HANDLER( ninjak_coin_word_w )
	{
		if (ACCESSING_MSB)
		{
			coin_lockout_w(0, ~data & 0x0100);
			coin_lockout_w(1, ~data & 0x0200);
			coin_lockout_w(2, ~data & 0x0400);
			coin_lockout_w(3, ~data & 0x0800);
			coin_counter_w(0,  data & 0x1000);
			coin_counter_w(1,  data & 0x2000);
			coin_counter_w(2,  data & 0x4000);
			coin_counter_w(3,  data & 0x8000);
		}
	}
	
	static READ16_HANDLER( growl_dsw_r )
	{
	    switch (offset)
	    {
	         case 0x00:
	              return input_port_3_word_r(0,mem_mask); /* DSW A */
	
	         case 0x01:
	              return input_port_4_word_r(0,mem_mask); /* DSW B */
	    }
	
	logerror("CPU #0 PC %06x: warning - read unmapped dsw_r offset %06x\n",cpu_get_pc(),offset);
	
		return 0xff;
	}
	
	static READ16_HANDLER( growl_input_r )
	{
	    switch (offset)
	    {
	         case 0x00:
	              return input_port_0_word_r(0,mem_mask); /* IN0 */
	
	         case 0x01:
	              return input_port_1_word_r(0,mem_mask); /* IN1 */
	
	         case 0x02:
	              return input_port_2_word_r(0,mem_mask); /* IN2 */
	
	    }
	
	logerror("CPU #0 PC %06x: warning - read unmapped input_r offset %06x\n",cpu_get_pc(),offset);
	
		return 0xff;
	}
	
	static READ16_HANDLER( footchmp_input_r )
	{
		switch (offset)
		{
			case 0x00:
				return input_port_3_word_r(0,mem_mask); /* DSW A */
	
			case 0x01:
				return input_port_4_word_r(0,mem_mask); /* DSW B */
	
			case 0x02:
				return input_port_2_word_r(0,mem_mask); /* IN2 */
	
	//		case 0x03:
	//			return (coin_word & ~mem_mask);
	
			case 0x05:
				return input_port_0_word_r(0,mem_mask); /* IN0 */
	
			case 0x06:
				return input_port_1_word_r(0,mem_mask); /* IN1 */
	
			case 0x07:
				return input_port_5_word_r(0,mem_mask); /* IN3 */
	
			case 0x08:
				return input_port_6_word_r(0,mem_mask); /* IN4 */
	    }
	
	logerror("CPU #0 PC %06x: warning - read unmapped input offset %06x\n",cpu_get_pc(),offset);
	
		return 0xff;
	}
	
	static READ16_HANDLER( ninjak_input_r )
	{
		switch (offset)
		{
			case 0x00:
				return (input_port_3_word_r(0,0) << 8); /* DSW A */
	
			case 0x01:
				return (input_port_4_word_r(0,0) << 8); /* DSW B */
	
			case 0x02:
				return (input_port_0_word_r(0,0) << 8); /* IN 0 */
	
			case 0x03:
				return (input_port_1_word_r(0,0) << 8); /* IN 1 */
	
			case 0x04:
				return (input_port_5_word_r(0,0) << 8); /* IN 3 */
	
			case 0x05:
				return (input_port_6_word_r(0,0) << 8); /* IN 4 */
	
			case 0x06:
				return (input_port_2_word_r(0,0) << 8); /* IN 2 */
	
	//		case 0x07:
	//			return (coin_word & ~mem_mask);
		}
	
	logerror("CPU #0 PC %06x: warning - read unmapped input offset %06x\n",cpu_get_pc(),offset);
	
		return 0xff;
	}
	
	static READ16_HANDLER( cameltry_paddle_r )
	{
		static int last[2];
		int curr,res = 0xff;
	
		switch (offset)
		{
			case 0x00:
				curr = input_port_5_word_r(0,0); /* Paddle A */
				res = curr - last[0];
				last[0] = curr;
				return res;
	
			case 0x02:
				curr = input_port_6_word_r(0,0); /* Paddle B */
				res = curr - last[1];
				last[1] = curr;
				return res;
		}
	
	logerror("CPU #0 PC %06x: warning - read unmapped paddle offset %06x\n",cpu_get_pc(),offset);
	
		return 0;
	}
	
	static READ16_HANDLER( driftout_paddle_r )
	{
	    switch (offset)
	    {
	         case 0x00:
	              return input_port_5_word_r(0,mem_mask); /* Paddle A */
	
	         case 0x01:
	              return input_port_6_word_r(0,mem_mask); /* Paddle B */
	    }
	
	logerror("CPU #0 PC %06x: warning - read unmapped paddle offset %06x\n",cpu_get_pc(),offset);
	
	        return 0xff;
	}
	
	static READ16_HANDLER( deadconx_input_r )
	{
		switch (offset)
		{
			case 0x00:
				return input_port_3_word_r(0,mem_mask); /* DSW A */
	
			case 0x01:
				return input_port_4_word_r(0,mem_mask); /* DSW B */
	
			case 0x02:
				return input_port_2_word_r(0,mem_mask); /* IN2 */
	
	//		case 0x03:
	//			return (coin_word & ~mem_mask);
	
			case 0x05:
				return input_port_0_word_r(0,mem_mask); /* IN0 */
	
			case 0x06:
				return input_port_1_word_r(0,mem_mask); /* IN1 */
	
			case 0x07:
				return input_port_5_word_r(0,mem_mask); /* IN3 */
	
			case 0x08:
				return input_port_6_word_r(0,mem_mask); /* IN4 */
	    }
	
	logerror("CPU #0 PC %06x: warning - read unmapped input offset %06x\n",cpu_get_pc(),offset);
	
		return 0xff;
	}
	
	static READ16_HANDLER( mjnquest_dsw_r )
	{
	    switch (offset)
	    {
	        case 0x00:
	        {
				return (input_port_5_word_r(0,0) << 8) + input_port_7_word_r(0,0); /* DSW A + coin */
	        }
	
	        case 0x01:
	        {
				return (input_port_6_word_r(0,0) << 8) + input_port_8_word_r(0,0); /* DSW B + coin */
	        }
	    }
	
	    logerror("CPU #0 PC %06x: warning - read unmapped dsw_r offset %06x\n",cpu_get_pc(),offset);
	
	    return 0xff;
	}
	
	static READ16_HANDLER( mjnquest_input_r )
	{
	    switch (mjnquest_input)
	    {
	         case 0x01:
	              return input_port_0_word_r(0,mem_mask); /* IN0 */
	
	         case 0x02:
	              return input_port_1_word_r(0,mem_mask); /* IN1 */
	
	         case 0x04:
	              return input_port_2_word_r(0,mem_mask); /* IN2 */
	
	         case 0x08:
	              return input_port_3_word_r(0,mem_mask); /* IN3 */
	
	         case 0x10:
	              return input_port_4_word_r(0,mem_mask); /* IN4 */
	
	    }
	
	logerror("CPU #0 mjnquest_input %06x: warning - read unknown input %06x\n",cpu_get_pc(),mjnquest_input);
	
		return 0xff;
	}
	
	static WRITE16_HANDLER( mjnquest_inputselect_w )
	{
	    mjnquest_input = (data >> 6);
	}
	
	static READ16_HANDLER( quizhq_input1_r )
	{
	    switch (offset)
	    {
	         case 0x00:
	              return input_port_4_word_r(0,mem_mask); /* DSW B */
	
	         case 0x01:
	              return input_port_0_word_r(0,mem_mask); /* IN0 */
	    }
	
	logerror("CPU #0 PC %06x: warning - read unmapped input_r offset %06x\n",cpu_get_pc(),offset);
	
		return 0xff;
	}
	
	static READ16_HANDLER( quizhq_input2_r )
	{
	    switch (offset)
	    {
	         case 0x00:
	              return input_port_3_word_r(0,mem_mask); /* DSW A */
	
	         case 0x01:
	              return input_port_1_word_r(0,mem_mask); /* IN1 */
	
	         case 0x02:
	              return input_port_2_word_r(0,mem_mask); /* IN2 */
	    }
	
	logerror("CPU #0 PC %06x: warning - read unmapped input_r offset %06x\n",cpu_get_pc(),offset);
	
		return 0xff;
	}
	
	static READ16_HANDLER( yesnoj_input_r )
	{
	    switch (offset)
	    {
	         case 0x00:
	              return input_port_0_word_r(0,mem_mask);	/* IN0 */
	
	/* case 0x01 only used if "printer" DSW is on, and appears to
	   be printer status byte */
	
	         case 0x02:
	              return input_port_1_word_r(0,mem_mask); /* IN1 */
	    }
	
	logerror("CPU #0 PC %06x: warning - read unmapped input_r offset %06x\n",cpu_get_pc(),offset);
	
		return 0x0;
	}
	
	static READ16_HANDLER( yesnoj_dsw_r )
	{
	#ifdef MAME_DEBUG
		logerror("CPU #0 PC = %06x: read yesnoj DSW %01x\n",cpu_get_pc(),yesnoj_dsw);
	#endif
	
		yesnoj_dsw = 1 - yesnoj_dsw;   /* game reads same word twice to get DSW A then B so we toggle */
	
		if (yesnoj_dsw)
		{
			return input_port_2_word_r(0,mem_mask);
		}
		else
		{
			return input_port_3_word_r(0,mem_mask);
		}
	}
	
	/******************************************************************
	                       INTERRUPTS (still a WIP)
	
	The are two interrupt request signals: VBL and DMA. DMA comes
	from the sprite generator (maybe when it has copied the data to
	a private buffer, or rendered the current frame, or who knows what
	else).
	The requests are mapped through a PAL so no hardwiring, but the PAL
	could be the same across all the games. All the games have just two
	valid vectors, IRQ5 and IRQ6.
	
	It seems that usually VBL maps to IRQ5 and DMA to IRQ6. However
	there are jumpers on the board allowing to swap the two interrupt
	request signals, so this could explain a need for certain games to
	have them in the opposite order.
	
	There are lots of sprite glitches in many games because the sprite ram
	is often updated in two out-of-sync chunks. I am almost sure there is
	some partial buffering going on in the sprite chip, and DMA has to
	play a part in it.
	
	
	             sprite ctrl regs   	  interrupts & sprites
	          0006 000a    8006 800a
	          ----------------------	-----------------------------------------------
	finalb    8000 0300    0000 0000	Needs partial buffering like dondokod to avoid glitches
	dondokod  8000 0000/8  0000 0000	IRQ6 just sets a flag. IRQ5 waits for that flag,
	                                	toggles ctrl register 0000<->0008, and copies bytes
										0 and 8 *ONLY* of sprite data (code, color, flip,
										ctrl). The other bytes of sprite data (coordinates
										and zoom) are updated by the main program.
										Caching sprite data and using bytes 0 and 8 from
										previous frame and the others from *TWO* frames
										before is enough to get glitch-free sprites that seem
										to be perfectly in sync with scrolling (check the tree
										mouths during level change).
	thundfox  8000 0000    0000 0000	IRQ6 copies bytes 0 and 8 *ONLY* of sprite data (code,
										color, flip, ctrl). The other bytes of sprite data
										(coordinates and zoom) are updated (I think) by the
										main program.
										The same sprite data caching that works for dondokod
										improves sprites, but there are still glitches related
										to zoom (check third round of attract mode). Those
										glitches can be fixed by buffering also the zoom ctrl
										byte.
										Moreover, sprites are not in perfect sync with the
										background (sometimes they are one frame behind, but
										not always).
	qtorimon  8000 0000    0000 0000    IRQ6 does some stuff but doesn't seem to deal with
										sprites. IRQ5 copies bytes 0, 8 *AND ALSO 2* of sprite
										data in one routine, and immediately after that the
										remaining bytes 4 and 6 in another routine, without
										doing, it seems, any waiting inbetween.
										Nevertheless, separated sprite data caching like in
										dondokod is still required to avoid glitches.
	liquidk   8000 0000/8  0000 0000	Same as dondokod. An important difference is that
										the sprite ctrl register doesn't toggle every frame
										(because the handler can't complete the frame in
										time?). This can be seen easily in the attract mode,
										where sprite glitches appear.
										Correctly handling the ctrl register and sprite data
										caching seems to be vital to avoid sprite glitches.
	quizhq    8000 0000    0000 0000	Both IRQ5 and IRQ6 do stuff, I haven't investigated.
										There is a very subtle sprite glitch if sprite data
										buffering is not used: the blinking INSERT COIN in
										the right window will get moved as garbage chars on
										the left window score and STOCK for one frame when
										INSERT COINS disappears from the right. This happens
										because bytes 0 and 8 of the sprite data are one
										frame behind and haven't been cleared yet.
	ssi       8000 0000    0000 0000	IRQ6 does nothing. IRQ5 copies bytes 0 and 8 *ONLY*
										of sprite data (code, color, flip, ctrl). The other
										bytes of sprite data (coordinates and zoom) are
										updated by the main program.
										The same sprite data caching that works for dondokod
										avoids major glitches, but I'm not sure it's working
										right when the big butterfly (time bonus) is on
										screen (it flickers on and off every frame).
	gunfront  8000 1000/1  8001 1000/1	The toggling bit in the control register selects the
										sprite bank used. It normally toggles every frame but
										sticks for two frame when lots of action is going on
										(see smart bombs in attract mode) and glitches will
										appear if it is not respected.
										IRQ6 writes the sprite ctrl registers, and also writes
										related data to the sprites at 9033e0/90b3e0. The
										active one gets 8000/8001 in byte 6 and 1001/1000 in
										byte 10, while the other gets 0. Note that the value
										in byte 10 is inverted from the active bank, as if it
										were a way to tell the sprite hardware "after this, go
										to the other bank".
										Note also that IRQ6 isn't the only one writing to the
										sprite ctrl registers, this is done also in the parts
										that actually change the sprite data (I think it's
										main program, not interrupt), so it's not clear who's
										"in charge". Actually it seems that what IRQ6 writes
										is soon overwritten so that what I outlined above
										regarding 9033e0/90b3e0 is no longer true, and they
										are no longer in sync with the ctrl registers, messing
										up smart bombs.
										There don't seem to be other glitches even without
										sprite data buffering.
	growl     8000 0000    8001 0001	IRQ6 just sets a flag. I haven't investigated who
										updates the sprite ram.
										This game uses sprite banks like gunfront, but unlike
										gunfront it doesn't change the ctrl registers. What it
										does is change the sprites at 903210/90b210; 8000/8001
										is always written in byte 6, while byte 10 receives
										the active bank (1000 or 1001). There are also end of
										list markers placed before that though, and those seem
										to always match what's stored in the ctrl registers
										(8000 1000 for the first bank and 8001 1001 for the
										second).
										There don't seem to be sprite glitches even without
										sprite data buffering, but sprites are not in sync with
										the background.
	mjnquest  8000 0800/8  0000 0000
	footchmp  8000 0000    8001 0001	IRQ6 just sets a flag (and writes to an unknown memory
										location).
										This games uses sprite banks as well, this time it
										writes markers at 2033e0/20b3e0, it always writes
										1000/1001 to byte 10, while it writes 8000 or 8001 to
										byte 6 depending on the active bank. This is the exact
										opposite of growl...
	hthero
	koshien   8000 0000    8001 0001	Another game using banks.The markers are again at
										9033e0/90b3e0 but this time byte 6 receives 9000/9001.
										Byte 10 is 1000 or 1001 depending on the active bank.
	yuyugogo  8000 0800/8  0000 0000
	ninjak    8000 0000    8001 0001	uses banks
	solfigtr  8000 0000    8001 0001	uses banks
	qzquest   8000 0000    0000 0000	Separated sprite data caching like in dondokod is
										required to avoid glitches.
	pulirula  8000 0000    8001 0001	uses banks
	qzchikyu  8000 0000    0000 0000	With this game there are glitches and the sprite data
										caching done in dondokod does NOT fix them.
	deadconx 8/9000 0000/1 8/9001 0000/1 I guess it's not a surprise that this game uses banks
										in yet another different way.
	dinorex   8000 0000    8001 0001	uses banks
	driftout  8000 0000/8  0000 0000	The first control changes from 8000 to 0000 at the end
										of the attract demo and seems to stay that way.
	
	
	******************************************************************/
	
	void taitof2_interrupt6(int x)
	{
		cpu_cause_interrupt(0,MC68000_IRQ_6);
	}
	
	public static InterruptPtr taitof2_interrupt = new InterruptPtr() { public int handler() 
	{
		timer_set(TIME_IN_CYCLES(500,0),0, taitof2_interrupt6);
		return MC68000_IRQ_5;
	} };
	
	
	/****************************************************************
	                            SOUND
	****************************************************************/
	
	static void reset_sound_region(void)
	{
		cpu_setbank( 2, memory_region(REGION_CPU2) + (banknum * 0x4000) + 0x10000 );
	}
	
	public static WriteHandlerPtr sound_bankswitch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		banknum = (data - 1) & 7;
		reset_sound_region();
	
	#ifdef MAME_DEBUG
		if (banknum>2) logerror("CPU #1 switch to ROM bank %06x: should only happen if Z80 prg rom is 128K!\n",banknum);
	#endif
	} };
	
	
	READ16_HANDLER( taitof2_sound_r )
	{
		if (offset == 1)
			return (taitosound_comm16_lsb_r(0,mem_mask));
		else return 0;
	}
	
	READ16_HANDLER( taitof2_msb_sound_r )
	{
		if (offset == 1)
			return (taitosound_comm16_msb_r(0,mem_mask));
		else return 0;
	}
	
	
	static int driveout_sound_latch = 0;
	
	
	public static ReadHandlerPtr driveout_sound_command_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		cpu_set_irq_line(1,0,CLEAR_LINE);
	//	logerror("sound IRQ OFF (sound command=%02x)\n",driveout_sound_latch);
		return driveout_sound_latch;
	} };
	
	static int oki_bank = 0;
	
	static void reset_driveout_sound_region(void)
	{
		OKIM6295_set_bank_base(0, oki_bank*0x40000);
	}
	
	static WRITE_HANDLER (oki_bank_w)
	{
		if ((data&4) && (oki_bank!=(data&3)) )
		{
			oki_bank = (data&3);
		}
		reset_driveout_sound_region();
	}
	
	static WRITE16_HANDLER ( driveout_sound_command_w )
	{
		static int nibble = 0;
	
		if (ACCESSING_MSB)
		{
			data >>= 8;
			if (offset==0)
			{
				nibble = data & 1;
			}
			else
			{
				if (nibble==0)
				{
					driveout_sound_latch = (data & 0x0f) | (driveout_sound_latch & 0xf0);
				}
				else
				{
					driveout_sound_latch = ((data<<4) & 0xf0) | (driveout_sound_latch & 0x0f);
					cpu_set_irq_line (1, 0, ASSERT_LINE);
				}
			}
		}
	}
	
	
	/***********************************************************
	                     MEMORY STRUCTURES
	***********************************************************/
	
	static MEMORY_READ16_START( finalb_readmem )
		{ 0x000000, 0x03ffff, MRA16_ROM },
		{ 0x100000, 0x10ffff, MRA16_RAM },
		{ 0x200000, 0x200007, TC0110PCR_word_r },	/* palette */
		{ 0x300000, 0x30000f, TC0220IOC_halfword_r },	/* I/O */
		{ 0x320000, 0x320003, taitof2_sound_r },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_r },
		{ 0x900000, 0x90ffff, MRA16_RAM },
	MEMORY_END
	
	static MEMORY_WRITE16_START( finalb_writemem )
		{ 0x000000, 0x03ffff, MWA16_ROM },
		{ 0x100000, 0x10ffff, MWA16_RAM },
		{ 0x200000, 0x200007, TC0110PCR_word_w },	/* palette */
		{ 0x300000, 0x30000f, TC0220IOC_halfword_w },	/* I/O */
		{ 0x320000, 0x320001, taitosound_port16_lsb_w },
		{ 0x320002, 0x320003, taitosound_comm16_lsb_w },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x810000, 0x81ffff, MWA16_NOP },   /* error in game init code ? */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_w },
		{ 0x900000, 0x90ffff, MWA16_RAM, &spriteram16, &spriteram_size  },
		{ 0xb00002, 0xb00003, MWA16_NOP },   /* ?? */
	MEMORY_END
	
	static MEMORY_READ16_START( dondokod_readmem )
		{ 0x000000, 0x07ffff, MRA16_ROM },
		{ 0x100000, 0x10ffff, MRA16_RAM },
		{ 0x200000, 0x201fff, MRA16_RAM },
		{ 0x300000, 0x30000f, TC0220IOC_halfword_r },	/* I/O */
		{ 0x320000, 0x320003, taitof2_msb_sound_r },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_r },
		{ 0x900000, 0x90ffff, MRA16_RAM },
		{ 0xa00000, 0xa01fff, TC0280GRD_word_r },
	MEMORY_END
	
	static MEMORY_WRITE16_START( dondokod_writemem )
		{ 0x000000, 0x07ffff, MWA16_ROM },
		{ 0x100000, 0x10ffff, MWA16_RAM },
		{ 0x200000, 0x201fff, paletteram16_RRRRGGGGBBBBxxxx_word_w, &paletteram16 },
		{ 0x300000, 0x30000f, TC0220IOC_halfword_w },	/* I/O */
		{ 0x320000, 0x320001, taitosound_port16_msb_w },
		{ 0x320002, 0x320003, taitosound_comm16_msb_w },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_w },
		{ 0x900000, 0x90ffff, MWA16_RAM, &spriteram16, &spriteram_size  },
		{ 0xa00000, 0xa01fff, TC0280GRD_word_w },	/* ROZ tilemap */
		{ 0xa02000, 0xa0200f, TC0280GRD_ctrl_word_w },
		{ 0xb00000, 0xb0001f, TC0360PRI_halfword_w },	/* ?? */
	MEMORY_END
	
	static MEMORY_READ16_START( megab_readmem )
		{ 0x000000, 0x07ffff, MRA16_ROM },
		{ 0x100000, 0x100003, taitof2_msb_sound_r },
		{ 0x120000, 0x12000f, TC0220IOC_halfword_r },	/* I/O */
		{ 0x180000, 0x180fff, cchip2_word_r },
		{ 0x200000, 0x20ffff, MRA16_RAM },
		{ 0x300000, 0x301fff, MRA16_RAM },
		{ 0x600000, 0x60ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x610000, 0x61ffff, MRA16_RAM }, /* unused? */
		{ 0x620000, 0x62000f, TC0100SCN_ctrl_word_0_r },
		{ 0x800000, 0x80ffff, MRA16_RAM },
	MEMORY_END
	
	static MEMORY_WRITE16_START( megab_writemem )
		{ 0x000000, 0x07ffff, MWA16_ROM },
		{ 0x100000, 0x100001, taitosound_port16_msb_w },
		{ 0x100002, 0x100003, taitosound_comm16_msb_w },
		{ 0x120000, 0x12000f, TC0220IOC_halfword_w },	/* I/O */
		{ 0x180000, 0x180fff, cchip2_word_w, &cchip_ram },
		{ 0x200000, 0x20ffff, MWA16_RAM },
		{ 0x300000, 0x301fff, paletteram16_RRRRGGGGBBBBxxxx_word_w, &paletteram16 },
		{ 0x400000, 0x40001f, TC0360PRI_halfword_w },	/* ?? */
		{ 0x600000, 0x60ffff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x610000, 0x61ffff, MWA16_RAM },   /* unused? */
		{ 0x620000, 0x62000f, TC0100SCN_ctrl_word_0_w },
		{ 0x800000, 0x80ffff, MWA16_RAM, &spriteram16, &spriteram_size  },
	MEMORY_END
	
	static MEMORY_READ16_START( thundfox_readmem )
		{ 0x000000, 0x07ffff, MRA16_ROM },
		{ 0x100000, 0x101fff, MRA16_RAM },
		{ 0x200000, 0x20000f, TC0220IOC_halfword_r },	/* I/O */
		{ 0x220000, 0x220003, taitof2_msb_sound_r },
		{ 0x300000, 0x30ffff, MRA16_RAM },
		{ 0x400000, 0x40ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x420000, 0x42000f, TC0100SCN_ctrl_word_0_r },
		{ 0x500000, 0x50ffff, TC0100SCN_word_1_r },	/* tilemaps */
		{ 0x520000, 0x52000f, TC0100SCN_ctrl_word_1_r },
		{ 0x600000, 0x60ffff, MRA16_RAM },
	MEMORY_END
	
	static MEMORY_WRITE16_START( thundfox_writemem )
		{ 0x000000, 0x07ffff, MWA16_ROM },
		{ 0x100000, 0x101fff, paletteram16_RRRRGGGGBBBBxxxx_word_w, &paletteram16 },
		{ 0x200000, 0x20000f, TC0220IOC_halfword_w },	/* I/O */
		{ 0x220000, 0x220001, taitosound_port16_msb_w },
		{ 0x220002, 0x220003, taitosound_comm16_msb_w },
		{ 0x300000, 0x30ffff, MWA16_RAM },
		{ 0x400000, 0x40ffff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x420000, 0x42000f, TC0100SCN_ctrl_word_0_w },
		{ 0x500000, 0x50ffff, TC0100SCN_word_1_w },	/* tilemaps */
		{ 0x520000, 0x52000f, TC0100SCN_ctrl_word_1_w },
		{ 0x600000, 0x60ffff, MWA16_RAM, &spriteram16, &spriteram_size },
		{ 0x800000, 0x80001f, TC0360PRI_halfword_swap_w },
	MEMORY_END
	
	static MEMORY_READ16_START( cameltry_readmem )
		{ 0x000000, 0x03ffff, MRA16_ROM },
		{ 0x100000, 0x10ffff, MRA16_RAM },
		{ 0x200000, 0x201fff, MRA16_RAM },
		{ 0x300000, 0x30000f, TC0220IOC_halfword_r },	/* I/O */
		{ 0x300018, 0x30001f, cameltry_paddle_r },
		{ 0x320000, 0x320003, taitof2_msb_sound_r },
		{ 0x800000, 0x813fff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_r },
		{ 0x900000, 0x90ffff, MRA16_RAM },
		{ 0xa00000, 0xa01fff, TC0280GRD_word_r },
	MEMORY_END
	
	static MEMORY_WRITE16_START( cameltry_writemem )
		{ 0x000000, 0x03ffff, MWA16_ROM },
		{ 0x100000, 0x10ffff, MWA16_RAM },
		{ 0x200000, 0x201fff, paletteram16_RRRRGGGGBBBBxxxx_word_w, &paletteram16 },
		{ 0x300000, 0x30000f, TC0220IOC_halfword_w },	/* I/O */
		{ 0x320000, 0x320001, taitosound_port16_msb_w },
		{ 0x320002, 0x320003, taitosound_comm16_msb_w },
		{ 0x800000, 0x813fff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_w },
		{ 0x900000, 0x90ffff, MWA16_RAM, &spriteram16, &spriteram_size  },
		{ 0xa00000, 0xa01fff, TC0280GRD_word_w },	/* ROZ tilemap */
		{ 0xa02000, 0xa0200f, TC0280GRD_ctrl_word_w },
		{ 0xd00000, 0xd0001f, TC0360PRI_halfword_w },	/* ?? */
	MEMORY_END
	
	static MEMORY_READ16_START( qtorimon_readmem )
		{ 0x000000, 0x07ffff, MRA16_ROM },
		{ 0x100000, 0x10ffff, MRA16_RAM },
		{ 0x200000, 0x200007, TC0110PCR_word_r },	/* palette */
		{ 0x500000, 0x50000f, TC0220IOC_halfword_r },	/* I/O */
		{ 0x600000, 0x600003, taitof2_msb_sound_r },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_r },
		{ 0x900000, 0x90ffff, MRA16_RAM },
	MEMORY_END
	
	static MEMORY_WRITE16_START( qtorimon_writemem )
		{ 0x000000, 0x03ffff, MWA16_ROM },
		{ 0x100000, 0x10ffff, MWA16_RAM },
		{ 0x200000, 0x200007, TC0110PCR_word_w },	/* palette */
		{ 0x500000, 0x50000f, TC0220IOC_halfword_w },	/* I/O */
		{ 0x600000, 0x600001, taitosound_port16_msb_w },
		{ 0x600002, 0x600003, taitosound_comm16_msb_w },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_w },
		{ 0x900000, 0x90ffff, MWA16_RAM, &spriteram16, &spriteram_size  },
		{ 0x910000, 0x9120ff, MWA16_NOP },   /* error in init code ? */
	MEMORY_END
	
	static MEMORY_READ16_START( liquidk_readmem )
		{ 0x000000, 0x07ffff, MRA16_ROM },
		{ 0x100000, 0x10ffff, MRA16_RAM },
		{ 0x200000, 0x201fff, MRA16_RAM },
		{ 0x300000, 0x30000f, TC0220IOC_halfword_r },	/* I/O */
		{ 0x320000, 0x320003, taitof2_sound_r },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_r },
		{ 0x900000, 0x90ffff, MRA16_RAM },
	MEMORY_END
	
	static MEMORY_WRITE16_START( liquidk_writemem )
		{ 0x000000, 0x07ffff, MWA16_ROM },
		{ 0x100000, 0x10ffff, MWA16_RAM },
		{ 0x200000, 0x201fff, paletteram16_RRRRGGGGBBBBxxxx_word_w, &paletteram16 },
		{ 0x300000, 0x30000f, TC0220IOC_halfword_w },	/* I/O */
		{ 0x320000, 0x320001, taitosound_port16_lsb_w },
		{ 0x320002, 0x320003, taitosound_comm16_lsb_w },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_w },
		{ 0x900000, 0x90ffff, MWA16_RAM, &spriteram16, &spriteram_size  },
		{ 0xb00000, 0xb0001f, TC0360PRI_halfword_w },	/* ?? */
	MEMORY_END
	
	static MEMORY_READ16_START( quizhq_readmem )
		{ 0x000000, 0x0bffff, MRA16_ROM },
		{ 0x100000, 0x10ffff, MRA16_RAM },
		{ 0x200000, 0x200007, TC0110PCR_word_r },	/* palette */
		{ 0x500000, 0x50000f, quizhq_input1_r },
		{ 0x580000, 0x58000f, quizhq_input2_r },
		{ 0x600000, 0x600003, taitof2_sound_r },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_r },
		{ 0x900000, 0x90ffff, MRA16_RAM },
	MEMORY_END
	
	static MEMORY_WRITE16_START( quizhq_writemem )
		{ 0x000000, 0x03ffff, MWA16_ROM },
		{ 0x100000, 0x10ffff, MWA16_RAM },
		{ 0x200000, 0x200007, TC0110PCR_word_w },	/* palette */
		{ 0x500004, 0x500005, growl_coin_word_w },
		{ 0x580000, 0x580001, taitof2_watchdog_w },	/* ??? */
		{ 0x580006, 0x580007, MWA16_NOP },   /* ??? */
		{ 0x600000, 0x600001, taitosound_port16_lsb_w },
		{ 0x600002, 0x600003, taitosound_comm16_lsb_w },
		{ 0x680000, 0x680001, MWA16_NOP },   /* ??? */
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x810000, 0x81ffff, MWA16_NOP },   /* error in init code ? */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_w },
		{ 0x900000, 0x90ffff, MWA16_RAM, &spriteram16, &spriteram_size  },
	MEMORY_END
	
	static MEMORY_READ16_START( ssi_readmem )
		{ 0x000000, 0x07ffff, MRA16_ROM },
		{ 0x100000, 0x10000f, TC0510NIO_halfword_r },
		{ 0x200000, 0x20ffff, MRA16_RAM },
		{ 0x300000, 0x301fff, MRA16_RAM },
		{ 0x400000, 0x400003, taitof2_msb_sound_r },
		{ 0x600000, 0x60ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x620000, 0x62000f, TC0100SCN_ctrl_word_0_r },
		{ 0x800000, 0x80ffff, MRA16_RAM },
	MEMORY_END
	
	static MEMORY_WRITE16_START( ssi_writemem )
		{ 0x000000, 0x07ffff, MWA16_ROM },
		{ 0x100000, 0x10000f, TC0510NIO_halfword_w },
		{ 0x200000, 0x20ffff, MWA16_RAM },
		{ 0x300000, 0x301fff, paletteram16_RRRRGGGGBBBBxxxx_word_w, &paletteram16 },
		{ 0x400000, 0x400001, taitosound_port16_msb_w },
		{ 0x400002, 0x400003, taitosound_comm16_msb_w },
	//	{ 0x500000, 0x500001, MWA16_NOP },   /* ?? */
		{ 0x600000, 0x60ffff, TC0100SCN_word_0_w },	/* tilemaps (not used) */
		{ 0x620000, 0x62000f, TC0100SCN_ctrl_word_0_w },
		{ 0x800000, 0x80ffff, MWA16_RAM, &spriteram16, &spriteram_size },   /* sprite ram */
	MEMORY_END
	
	static MEMORY_READ16_START( gunfront_readmem )
		{ 0x000000, 0x0bffff, MRA16_ROM },
		{ 0x100000, 0x10ffff, MRA16_RAM },
		{ 0x200000, 0x201fff, MRA16_RAM },
		{ 0x300000, 0x30000f, TC0510NIO_halfword_wordswap_r },
		{ 0x320000, 0x320003, taitof2_msb_sound_r },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_r },
		{ 0x900000, 0x90ffff, MRA16_RAM },
	MEMORY_END
	
	static MEMORY_WRITE16_START( gunfront_writemem )
		{ 0x000000, 0x0bffff, MWA16_ROM },
		{ 0x100000, 0x10ffff, MWA16_RAM },
		{ 0x200000, 0x201fff, paletteram16_RRRRGGGGBBBBxxxx_word_w, &paletteram16 },
		{ 0x300000, 0x30000f, TC0510NIO_halfword_wordswap_w },
		{ 0x320000, 0x320001, taitosound_port16_msb_w },
		{ 0x320002, 0x320003, taitosound_comm16_msb_w },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_w },
		{ 0x900000, 0x90ffff, MWA16_RAM, &spriteram16, &spriteram_size },
	//	{ 0xa00000, 0xa00001, MWA16_NOP },   /* ?? */
		{ 0xb00000, 0xb0001f, TC0360PRI_halfword_w },	/* ?? */
	MEMORY_END
	
	static MEMORY_READ16_START( growl_readmem )
		{ 0x000000, 0x0fffff, MRA16_ROM },
		{ 0x100000, 0x10ffff, MRA16_RAM },
		{ 0x200000, 0x201fff, MRA16_RAM },
		{ 0x300000, 0x30000f, growl_dsw_r },
		{ 0x320000, 0x32000f, growl_input_r },
		{ 0x400000, 0x400003, taitof2_msb_sound_r },
		{ 0x508000, 0x50800f, input_port_5_word_r },   /* IN3 */
		{ 0x50c000, 0x50c00f, input_port_6_word_r },   /* IN4 */
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_r },
		{ 0x900000, 0x90ffff, MRA16_RAM },
	MEMORY_END
	
	static MEMORY_WRITE16_START( growl_writemem )
		{ 0x000000, 0x0fffff, MWA16_ROM },
		{ 0x100000, 0x10ffff, MWA16_RAM },
		{ 0x200000, 0x201fff, paletteram16_RRRRGGGGBBBBxxxx_word_w, &paletteram16 },
		{ 0x300004, 0x300005, growl_coin_word_w },
		{ 0x340000, 0x340001, taitof2_watchdog_w },
		{ 0x400000, 0x400001, taitosound_port16_msb_w },
		{ 0x400002, 0x400003, taitosound_comm16_msb_w },
		{ 0x500000, 0x50000f, taitof2_spritebank_w },
		{ 0x504000, 0x504001, MWA16_NOP },	/* unknown... various values */
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_w },
		{ 0x900000, 0x90ffff, MWA16_RAM, &spriteram16, &spriteram_size },
		{ 0xb00000, 0xb0001f, TC0360PRI_halfword_w },	/* ?? */
	MEMORY_END
	
	static MEMORY_READ16_START( mjnquest_readmem )
		{ 0x000000, 0x0fffff, MRA16_ROM },
		{ 0x110000, 0x11ffff, MRA16_RAM },   /* "sram" */
		{ 0x120000, 0x12ffff, MRA16_RAM },
		{ 0x200000, 0x200007, TC0110PCR_word_r },	/* palette */
		{ 0x300000, 0x30000f, mjnquest_dsw_r },
		{ 0x310000, 0x310001, mjnquest_input_r },
		{ 0x360000, 0x360003, taitof2_msb_sound_r },
		{ 0x400000, 0x40ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x420000, 0x42000f, TC0100SCN_ctrl_word_0_r },
		{ 0x500000, 0x50ffff, MRA16_RAM },
	MEMORY_END
	
	static MEMORY_WRITE16_START( mjnquest_writemem )
		{ 0x000000, 0x03ffff, MWA16_ROM },
		{ 0x110000, 0x11ffff, MWA16_RAM },   /* "sram" */
		{ 0x120000, 0x12ffff, MWA16_RAM },
		{ 0x200000, 0x200007, TC0110PCR_word_w },	/* palette */
		{ 0x320000, 0x320001, mjnquest_inputselect_w },
		{ 0x330000, 0x330001, MWA16_NOP },   /* watchdog ? */
		{ 0x350000, 0x350001, MWA16_NOP },   /* watchdog ? */
		{ 0x360000, 0x360001, taitosound_port16_msb_w },
		{ 0x360002, 0x360003, taitosound_comm16_msb_w },
		{ 0x380000, 0x380001, TC0100SCN_gfxbank_w },	/* scr gfx bank select */
		{ 0x400000, 0x40ffff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x420000, 0x42000f, TC0100SCN_ctrl_word_0_w },
		{ 0x500000, 0x50ffff, MWA16_RAM, &spriteram16, &spriteram_size },
	MEMORY_END
	
	static MEMORY_READ16_START( footchmp_readmem )
		{ 0x000000, 0x07ffff, MRA16_ROM },
		{ 0x100000, 0x10ffff, MRA16_RAM },
		{ 0x200000, 0x20ffff, MRA16_RAM },
		{ 0x400000, 0x40ffff, TC0480SCP_word_r },   /* tilemaps */
		{ 0x430000, 0x43002f, TC0480SCP_ctrl_word_r },
		{ 0x600000, 0x601fff, MRA16_RAM },
		{ 0x700000, 0x70001f, footchmp_input_r },
		{ 0xa00000, 0xa00003, taitof2_sound_r },
	MEMORY_END
	
	static MEMORY_WRITE16_START( footchmp_writemem )
		{ 0x000000, 0x07ffff, MWA16_ROM },
		{ 0x100000, 0x10ffff, MWA16_RAM },
		{ 0x200000, 0x20ffff, MWA16_RAM, &spriteram16, &spriteram_size },
		{ 0x300000, 0x30000f, taitof2_spritebank_w },	/* updated at $a6e, off irq5 */
		{ 0x400000, 0x40ffff, TC0480SCP_word_w },	  /* tilemaps */
		{ 0x430000, 0x43002f, TC0480SCP_ctrl_word_w },
		{ 0x500000, 0x50001f, TC0360PRI_halfword_w },	/* 500002 written like a watchdog?! */
		{ 0x600000, 0x601fff, paletteram16_RRRRGGGGBBBBxxxx_word_w, &paletteram16 },
		{ 0x700006, 0x700007, taitof2_4p_coin_word_w },
		{ 0x800000, 0x800001, taitof2_watchdog_w },   /* ??? */
		{ 0xa00000, 0xa00001, taitosound_port16_lsb_w },
		{ 0xa00002, 0xa00003, taitosound_comm16_lsb_w },
	MEMORY_END
	
	static MEMORY_READ16_START( koshien_readmem )
		{ 0x000000, 0x0fffff, MRA16_ROM },
		{ 0x100000, 0x10ffff, MRA16_RAM },
		{ 0x200000, 0x201fff, MRA16_RAM },
		{ 0x300000, 0x30000f, TC0510NIO_halfword_r },
		{ 0x320000, 0x320003, taitof2_msb_sound_r },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_r },
		{ 0x900000, 0x90ffff, MRA16_RAM },
	//	{ 0xa20000, 0xa20001, koshien_spritebank_r },   /* for debugging spritebank */
	MEMORY_END
	
	static MEMORY_WRITE16_START( koshien_writemem )
		{ 0x000000, 0x03ffff, MWA16_ROM },
		{ 0x100000, 0x10ffff, MWA16_RAM },
		{ 0x200000, 0x201fff, paletteram16_RRRRGGGGBBBBxxxx_word_w, &paletteram16 },
		{ 0x300000, 0x30000f, TC0510NIO_halfword_w },
		{ 0x320000, 0x320001, taitosound_port16_msb_w },
		{ 0x320002, 0x320003, taitosound_comm16_msb_w },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_w },
		{ 0x900000, 0x90ffff, MWA16_RAM, &spriteram16, &spriteram_size  },
		{ 0xa20000, 0xa20001, koshien_spritebank_w },
		{ 0xb00000, 0xb0001f, TC0360PRI_halfword_swap_w },
	MEMORY_END
	
	static MEMORY_READ16_START( yuyugogo_readmem )
		{ 0x000000, 0x03ffff, MRA16_ROM },
		{ 0x200000, 0x20000f, TC0510NIO_halfword_r },
		{ 0x400000, 0x400003, taitof2_msb_sound_r },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_r },
		{ 0x900000, 0x90ffff, MRA16_RAM },
		{ 0xa00000, 0xa01fff, MRA16_RAM },
		{ 0xb00000, 0xb10fff, MRA16_RAM },
		{ 0xd00000, 0xdfffff, MRA16_BANK1 },   /* extra data rom */
	MEMORY_END
	
	static MEMORY_WRITE16_START( yuyugogo_writemem )
		{ 0x000000, 0x03ffff, MWA16_ROM },
		{ 0x200000, 0x20000f, TC0510NIO_halfword_w },
		{ 0x400000, 0x400001, taitosound_port16_msb_w },
		{ 0x400002, 0x400003, taitosound_comm16_msb_w },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_w },
		{ 0x900000, 0x90ffff, MWA16_RAM, &spriteram16, &spriteram_size  },
		{ 0xa00000, 0xa01fff, paletteram16_RRRRGGGGBBBBxxxx_word_w, &paletteram16 },
		{ 0xb00000, 0xb10fff, MWA16_RAM },   /* deliberate writes to $b10xxx, I think */
		{ 0xc00000, 0xc01fff, taitof2_sprite_extension_w, &f2_sprite_extension, &f2_spriteext_size },
		{ 0xd00000, 0xdfffff, MWA16_ROM },
	MEMORY_END
	
	static MEMORY_READ16_START( ninjak_readmem )
		{ 0x000000, 0x07ffff, MRA16_ROM },
		{ 0x100000, 0x10ffff, MRA16_RAM },
		{ 0x200000, 0x201fff, MRA16_RAM },
		{ 0x300000, 0x30000f, ninjak_input_r },
		{ 0x400000, 0x400003, taitof2_msb_sound_r },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_r },
		{ 0x900000, 0x90ffff, MRA16_RAM },
	MEMORY_END
	
	static MEMORY_WRITE16_START( ninjak_writemem )
		{ 0x000000, 0x07ffff, MWA16_ROM },
		{ 0x100000, 0x10ffff, MWA16_RAM },
		{ 0x200000, 0x201fff, paletteram16_RRRRGGGGBBBBxxxx_word_w, &paletteram16 },
		{ 0x30000e, 0x30000f, ninjak_coin_word_w },
		{ 0x380000, 0x380001, taitof2_watchdog_w },	/* ??? */
		{ 0x400000, 0x400001, taitosound_port16_msb_w },
		{ 0x400002, 0x400003, taitosound_comm16_msb_w },
		{ 0x600000, 0x60000f, taitof2_spritebank_w },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_w },
		{ 0x900000, 0x90ffff, MWA16_RAM, &spriteram16, &spriteram_size },
		{ 0xb00000, 0xb0001f, TC0360PRI_halfword_w },	/* b00002 written like a watchdog?! */
	MEMORY_END
	
	static MEMORY_READ16_START( solfigtr_readmem )
		{ 0x000000, 0x07ffff, MRA16_ROM },
		{ 0x100000, 0x10ffff, MRA16_RAM },
		{ 0x200000, 0x201fff, MRA16_RAM },
		{ 0x300000, 0x30000f, growl_dsw_r },
		{ 0x320000, 0x32000f, growl_input_r },
		{ 0x400000, 0x400003, taitof2_msb_sound_r },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_r },
		{ 0x900000, 0x90ffff, MRA16_RAM },
	MEMORY_END
	
	static MEMORY_WRITE16_START( solfigtr_writemem )
		{ 0x000000, 0x07ffff, MWA16_ROM },
		{ 0x100000, 0x10ffff, MWA16_RAM },
		{ 0x200000, 0x201fff, paletteram16_RRRRGGGGBBBBxxxx_word_w, &paletteram16 },
		{ 0x300004, 0x300005, growl_coin_word_w },	/* NOT VERIFIED */
		{ 0x340000, 0x340001, taitof2_watchdog_w },	/* NOT VERIFIED */
		{ 0x400000, 0x400001, taitosound_port16_msb_w },
		{ 0x400002, 0x400003, taitosound_comm16_msb_w },
		{ 0x500000, 0x50000f, taitof2_spritebank_w },
		{ 0x504000, 0x504001, MWA16_NOP },	/* unknown... various values */
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_w },
		{ 0x900000, 0x90ffff, MWA16_RAM, &spriteram16, &spriteram_size },
		{ 0xb00000, 0xb0001f, TC0360PRI_halfword_w },	/* ?? */
	MEMORY_END
	
	static MEMORY_READ16_START( qzquest_readmem )
		{ 0x000000, 0x17ffff, MRA16_ROM },
		{ 0x200000, 0x20000f, TC0510NIO_halfword_r },
		{ 0x300000, 0x300003, taitof2_sound_r },
		{ 0x400000, 0x401fff, MRA16_RAM },
		{ 0x500000, 0x50ffff, MRA16_RAM },
		{ 0x600000, 0x60ffff, MRA16_RAM },
		{ 0x700000, 0x70ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x720000, 0x72000f, TC0100SCN_ctrl_word_0_r },
	MEMORY_END
	
	static MEMORY_WRITE16_START( qzquest_writemem )
		{ 0x000000, 0x03ffff, MWA16_ROM },
		{ 0x200000, 0x20000f, TC0510NIO_halfword_w },
		{ 0x300000, 0x300001, taitosound_port16_lsb_w },
		{ 0x300002, 0x300003, taitosound_comm16_lsb_w },
		{ 0x400000, 0x401fff, paletteram16_xRRRRRGGGGGBBBBB_word_w, &paletteram16 },
		{ 0x500000, 0x50ffff, MWA16_RAM },
		{ 0x600000, 0x60ffff, MWA16_RAM, &spriteram16, &spriteram_size  },
		{ 0x700000, 0x70ffff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x720000, 0x72000f, TC0100SCN_ctrl_word_0_w },
	MEMORY_END
	
	static MEMORY_READ16_START( pulirula_readmem )
		{ 0x000000, 0x0bffff, MRA16_ROM },
		{ 0x200000, 0x200003, taitof2_msb_sound_r },
		{ 0x300000, 0x30ffff, MRA16_RAM },
		{ 0x400000, 0x401fff, TC0430GRW_word_r },
		{ 0x700000, 0x701fff, MRA16_RAM },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_r },
		{ 0x900000, 0x90ffff, MRA16_RAM },
		{ 0xb00000, 0xb0000f, TC0510NIO_halfword_r },
	MEMORY_END
	
	static MEMORY_WRITE16_START( pulirula_writemem )
		{ 0x000000, 0x0bffff, MWA16_ROM },
		{ 0x200000, 0x200001, taitosound_port16_msb_w },
		{ 0x200002, 0x200003, taitosound_comm16_msb_w },
		{ 0x300000, 0x30ffff, MWA16_RAM },
		{ 0x400000, 0x401fff, TC0430GRW_word_w },	/* ROZ tilemap */
		{ 0x402000, 0x40200f, TC0430GRW_ctrl_word_w },
	//	{ 0x500000, 0x500001, MWA16_NOP },   /* ??? */
		{ 0x600000, 0x603fff, taitof2_sprite_extension_w, &f2_sprite_extension, &f2_spriteext_size },
		{ 0x700000, 0x701fff, paletteram16_xRRRRRGGGGGBBBBB_word_w, &paletteram16 },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_w },
		{ 0x900000, 0x90ffff, MWA16_RAM, &spriteram16, &spriteram_size  },
		{ 0xa00000, 0xa0001f, TC0360PRI_halfword_swap_w },
		{ 0xb00000, 0xb0000f, TC0510NIO_halfword_w },
	MEMORY_END
	
	static MEMORY_READ16_START( metalb_readmem )
		{ 0x000000, 0x0bffff, MRA16_ROM },
		{ 0x100000, 0x10ffff, MRA16_RAM },
		{ 0x300000, 0x30ffff, MRA16_RAM },
		{ 0x500000, 0x50ffff, TC0480SCP_word_r },   /* tilemaps */
		{ 0x530000, 0x53002f, TC0480SCP_ctrl_word_r },
		{ 0x700000, 0x703fff, MRA16_RAM },
		{ 0x800000, 0x80000f, TC0510NIO_halfword_wordswap_r },
		{ 0x900000, 0x900003, taitof2_msb_sound_r },
	MEMORY_END
	
	static MEMORY_WRITE16_START( metalb_writemem )
		{ 0x000000, 0x0bffff, MWA16_ROM },
		{ 0x100000, 0x10ffff, MWA16_RAM },
		{ 0x300000, 0x30ffff, MWA16_RAM, &spriteram16, &spriteram_size },
	//	{ 0x42000c, 0x42000f, MWA16_NOP },   /* zeroed */
		{ 0x500000, 0x50ffff, TC0480SCP_word_w },	  /* tilemaps */
		{ 0x530000, 0x53002f, TC0480SCP_ctrl_word_w },
		{ 0x600000, 0x60001f, TC0360PRI_halfword_w },
		{ 0x700000, 0x703fff, paletteram16_RRRRGGGGBBBBxxxx_word_w, &paletteram16 },
		{ 0x800000, 0x80000f, TC0510NIO_halfword_wordswap_w },
		{ 0x900000, 0x900001, taitosound_port16_msb_w },
		{ 0x900002, 0x900003, taitosound_comm16_msb_w },
	//	{ 0xa00000, 0xa00001, MWA16_NOP },   /* ??? */
	MEMORY_END
	
	static MEMORY_READ16_START( qzchikyu_readmem )
		{ 0x000000, 0x17ffff, MRA16_ROM },
		{ 0x200000, 0x20000f, TC0510NIO_halfword_r },
		{ 0x300000, 0x300003, taitof2_sound_r },
		{ 0x400000, 0x401fff, MRA16_RAM },
		{ 0x500000, 0x50ffff, MRA16_RAM },
		{ 0x600000, 0x60ffff, MRA16_RAM },
		{ 0x700000, 0x70ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x720000, 0x72000f, TC0100SCN_ctrl_word_0_r },
	MEMORY_END
	
	static MEMORY_WRITE16_START( qzchikyu_writemem )
		{ 0x000000, 0x03ffff, MWA16_ROM },
		{ 0x200000, 0x20000f, TC0510NIO_halfword_w },
		{ 0x300000, 0x300001, taitosound_port16_lsb_w },
		{ 0x300002, 0x300003, taitosound_comm16_lsb_w },
		{ 0x400000, 0x401fff, paletteram16_xRRRRRGGGGGBBBBB_word_w, &paletteram16 },
		{ 0x500000, 0x50ffff, MWA16_RAM },
		{ 0x600000, 0x60ffff, MWA16_RAM, &spriteram16, &spriteram_size  },
		{ 0x700000, 0x70ffff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x720000, 0x72000f, TC0100SCN_ctrl_word_0_w },
	MEMORY_END
	
	static MEMORY_READ16_START( yesnoj_readmem )
		{ 0x000000, 0x07ffff, MRA16_ROM },
		{ 0x200000, 0x20ffff, MRA16_RAM },
		{ 0x400000, 0x40ffff, MRA16_RAM },
		{ 0x500000, 0x50ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x520000, 0x52000f, TC0100SCN_ctrl_word_0_r },
		{ 0x600000, 0x601fff, MRA16_RAM },
	//	{ 0x700000, 0x70000b, yesnoj_unknown_r },   /* what's this? */
		{ 0x800000, 0x800003, taitof2_msb_sound_r },
		{ 0xa00000, 0xa0000f, yesnoj_input_r },
		{ 0xb00000, 0xb00001, yesnoj_dsw_r },   /* ?? (reads this twice in init) */
	MEMORY_END
	
	static MEMORY_WRITE16_START( yesnoj_writemem )
		{ 0x000000, 0x07ffff, MWA16_ROM },
		{ 0x200000, 0x20ffff, MWA16_RAM },
		{ 0x400000, 0x40ffff, MWA16_RAM, &spriteram16, &spriteram_size  },
		{ 0x500000, 0x50ffff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x520000, 0x52000f, TC0100SCN_ctrl_word_0_w },
		{ 0x600000, 0x601fff, paletteram16_RRRRGGGGBBBBxxxx_word_w, &paletteram16 },
		{ 0x800000, 0x800001, taitosound_port16_msb_w },
		{ 0x800002, 0x800003, taitosound_comm16_msb_w },
		{ 0x900002, 0x900003, MWA16_NOP },   /* lots of similar writes */
		{ 0xc00000, 0xc00001, MWA16_NOP },   /* watchdog ?? */
		{ 0xd00000, 0xd00001, MWA16_NOP },   /* lots of similar writes */
	MEMORY_END
	
	static MEMORY_READ16_START( deadconx_readmem )
		{ 0x000000, 0x0fffff, MRA16_ROM },
		{ 0x100000, 0x10ffff, MRA16_RAM },
		{ 0x200000, 0x20ffff, MRA16_RAM },
		{ 0x400000, 0x40ffff, TC0480SCP_word_r },   /* tilemaps */
		{ 0x430000, 0x43002f, TC0480SCP_ctrl_word_r },
		{ 0x600000, 0x601fff, MRA16_RAM },
		{ 0x700000, 0x70001f, deadconx_input_r },
		{ 0xa00000, 0xa00003, taitof2_msb_sound_r },
	MEMORY_END
	
	static MEMORY_WRITE16_START( deadconx_writemem )
		{ 0x000000, 0x0fffff, MWA16_ROM },
		{ 0x100000, 0x10ffff, MWA16_RAM },
		{ 0x200000, 0x20ffff, MWA16_RAM, &spriteram16, &spriteram_size },
		{ 0x300000, 0x30000f, taitof2_spritebank_w },
		{ 0x400000, 0x40ffff, TC0480SCP_word_w },	  /* tilemaps */
	//	{ 0x42000c, 0x42000f, MWA16_NOP },   /* zeroed */
		{ 0x430000, 0x43002f, TC0480SCP_ctrl_word_w },
		{ 0x500000, 0x50001f, TC0360PRI_halfword_w },	/* uses 500002 like a watchdog !? */
		{ 0x600000, 0x601fff, paletteram16_RRRRGGGGBBBBxxxx_word_w, &paletteram16 },
		{ 0x700006, 0x700007, taitof2_4p_coin_word_w },
		{ 0x800000, 0x800001, taitof2_watchdog_w },   /* ??? */
		{ 0xa00000, 0xa00001, taitosound_port16_msb_w },
		{ 0xa00002, 0xa00003, taitosound_comm16_msb_w },
	MEMORY_END
	
	static MEMORY_READ16_START( dinorex_readmem )
		{ 0x000000, 0x2fffff, MRA16_ROM },
		{ 0x300000, 0x30000f, TC0510NIO_halfword_r },
		{ 0x500000, 0x501fff, MRA16_RAM },
		{ 0x600000, 0x60ffff, MRA16_RAM },
		{ 0x800000, 0x80ffff, MRA16_RAM },
		{ 0x900000, 0x90ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x920000, 0x92000f, TC0100SCN_ctrl_word_0_r },
		{ 0xa00000, 0xa00003, taitof2_msb_sound_r },
	MEMORY_END
	
	static MEMORY_WRITE16_START( dinorex_writemem )
		{ 0x000000, 0x2fffff, MWA16_ROM },
		{ 0x300000, 0x30000f, TC0510NIO_halfword_w },
		{ 0x400000, 0x400fff, taitof2_sprite_extension_w, &f2_sprite_extension, &f2_spriteext_size },
		{ 0x500000, 0x501fff, paletteram16_RRRRGGGGBBBBxxxx_word_w, &paletteram16 },
		{ 0x600000, 0x60ffff, MWA16_RAM },
		{ 0x700000, 0x70001f, TC0360PRI_halfword_w },	/* ?? */
		{ 0x800000, 0x80ffff, MWA16_RAM, &spriteram16, &spriteram_size },
		{ 0x900000, 0x90ffff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x920000, 0x92000f, TC0100SCN_ctrl_word_0_w },
		{ 0xa00000, 0xa00001, taitosound_port16_msb_w },
		{ 0xa00002, 0xa00003, taitosound_comm16_msb_w },
		{ 0xb00000, 0xb00001, MWA16_NOP },   /* watchdog? */
	MEMORY_END
	
	static MEMORY_READ16_START( qjinsei_readmem )
		{ 0x000000, 0x1fffff, MRA16_ROM },
		{ 0x200000, 0x200003, taitof2_msb_sound_r },
		{ 0x300000, 0x30ffff, MRA16_RAM },
		{ 0x700000, 0x701fff, MRA16_RAM },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_r },
		{ 0x900000, 0x90ffff, MRA16_RAM },
		{ 0xb00000, 0xb0000f, TC0510NIO_halfword_r },
	MEMORY_END
	
	static MEMORY_WRITE16_START( qjinsei_writemem )
		{ 0x000000, 0x07ffff, MWA16_ROM },
		{ 0x200000, 0x200001, taitosound_port16_msb_w },
		{ 0x200002, 0x200003, taitosound_comm16_msb_w },
		{ 0x300000, 0x30ffff, MWA16_RAM },
		{ 0x500000, 0x500001, MWA16_NOP },   /* watchdog ? */
		{ 0x600000, 0x603fff, taitof2_sprite_extension_w, &f2_sprite_extension, &f2_spriteext_size },
		{ 0x700000, 0x701fff, paletteram16_RRRRGGGGBBBBxxxx_word_w, &paletteram16 },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_w },
		{ 0x900000, 0x90ffff, MWA16_RAM, &spriteram16, &spriteram_size  },
		{ 0xa00000, 0xa0001f, TC0360PRI_halfword_w },	/* ?? */
		{ 0xb00000, 0xb0000f, TC0510NIO_halfword_w },
	MEMORY_END
	
	static MEMORY_READ16_START( qcrayon_readmem )
		{ 0x000000, 0x07ffff, MRA16_ROM },
		{ 0x100000, 0x10ffff, MRA16_RAM },
		{ 0x300000, 0x3fffff, MRA16_BANK1 },   /* extra data rom */
		{ 0x500000, 0x500003, taitof2_msb_sound_r },
		{ 0x700000, 0x701fff, MRA16_RAM },
		{ 0x800000, 0x80ffff, MRA16_RAM },
		{ 0x900000, 0x90ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x920000, 0x92000f, TC0100SCN_ctrl_word_0_r },
		{ 0xa00000, 0xa0000f, TC0510NIO_halfword_r },
	MEMORY_END
	
	static MEMORY_WRITE16_START( qcrayon_writemem )
		{ 0x000000, 0x07ffff, MWA16_ROM },
		{ 0x100000, 0x10ffff, MWA16_RAM },
	//	{ 0x200000, 0x200001, MWA16_NOP },   /* unknown */
		{ 0x300000, 0x3fffff, MWA16_ROM },
		{ 0x500000, 0x500001, taitosound_port16_msb_w },
		{ 0x500002, 0x500003, taitosound_comm16_msb_w },
		{ 0x600000, 0x603fff, taitof2_sprite_extension_w, &f2_sprite_extension, &f2_spriteext_size },
		{ 0x700000, 0x701fff, paletteram16_RRRRGGGGBBBBxxxx_word_w, &paletteram16 },
		{ 0x800000, 0x80ffff, MWA16_RAM, &spriteram16, &spriteram_size  },
		{ 0x900000, 0x90ffff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x920000, 0x92000f, TC0100SCN_ctrl_word_0_w },
		{ 0xa00000, 0xa0000f, TC0510NIO_halfword_w },
		{ 0xb00000, 0xb0001f, TC0360PRI_halfword_w },	/* ?? */
	MEMORY_END
	
	static MEMORY_READ16_START( qcrayon2_readmem )
		{ 0x000000, 0x07ffff, MRA16_ROM },
		{ 0x200000, 0x20ffff, MRA16_RAM },
		{ 0x300000, 0x301fff, MRA16_RAM },
		{ 0x400000, 0x40ffff, MRA16_RAM },
		{ 0x500000, 0x50ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x520000, 0x52000f, TC0100SCN_ctrl_word_0_r },
		{ 0x600000, 0x67ffff, MRA16_BANK1 },   /* extra data rom */
		{ 0x700000, 0x70000f, TC0510NIO_halfword_r },
		{ 0xa00000, 0xa00003, taitof2_msb_sound_r },
	MEMORY_END
	
	static MEMORY_WRITE16_START( qcrayon2_writemem )
		{ 0x000000, 0x07ffff, MWA16_ROM },
		{ 0x200000, 0x20ffff, MWA16_RAM },
		{ 0x300000, 0x301fff, paletteram16_RRRRGGGGBBBBxxxx_word_w, &paletteram16 },
		{ 0x400000, 0x40ffff, MWA16_RAM, &spriteram16, &spriteram_size  },
		{ 0x500000, 0x50ffff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x520000, 0x52000f, TC0100SCN_ctrl_word_0_w },
		{ 0x600000, 0x67ffff, MWA16_ROM },
		{ 0x700000, 0x70000f, TC0510NIO_halfword_w },
		{ 0x900000, 0x90001f, TC0360PRI_halfword_w },	/* ?? */
		{ 0xa00000, 0xa00001, taitosound_port16_msb_w },
		{ 0xa00002, 0xa00003, taitosound_comm16_msb_w },
		{ 0xb00000, 0xb017ff, taitof2_sprite_extension_w, &f2_sprite_extension, &f2_spriteext_size },
	MEMORY_END
	
	static MEMORY_READ16_START( driftout_readmem )
		{ 0x000000, 0x0fffff, MRA16_ROM },
		{ 0x200000, 0x200003, taitof2_msb_sound_r },
		{ 0x300000, 0x30ffff, MRA16_RAM },
		{ 0x400000, 0x401fff, TC0430GRW_word_r },
		{ 0x700000, 0x701fff, MRA16_RAM },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_r },
		{ 0x900000, 0x90ffff, MRA16_RAM },
		{ 0xb00000, 0xb0000f, TC0510NIO_halfword_r },
		{ 0xb00018, 0xb0001f, driftout_paddle_r },
	MEMORY_END
	
	static MEMORY_WRITE16_START( driftout_writemem )
		{ 0x000000, 0x0fffff, MWA16_ROM },
		{ 0x200000, 0x200001, taitosound_port16_msb_w },
		{ 0x200002, 0x200003, taitosound_comm16_msb_w },
		{ 0x300000, 0x30ffff, MWA16_RAM },
		{ 0x400000, 0x401fff, TC0430GRW_word_w },	/* ROZ tilemap */
		{ 0x402000, 0x40200f, TC0430GRW_ctrl_word_w },
		{ 0x700000, 0x701fff, paletteram16_xRRRRRGGGGGBBBBB_word_w, &paletteram16 },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_w },
		{ 0x900000, 0x90ffff, MWA16_RAM, &spriteram16, &spriteram_size },
		{ 0xa00000, 0xa0001f, TC0360PRI_halfword_swap_w },
		{ 0xb00000, 0xb0000f, TC0510NIO_halfword_w },
	MEMORY_END
	
	/* same as driftout, except for sound address 0x200000 */
	static MEMORY_READ16_START( driveout_readmem )
		{ 0x000000, 0x0fffff, MRA16_ROM },
		{ 0x200000, 0x200003, MRA16_NOP },
		{ 0x300000, 0x30ffff, MRA16_RAM },
		{ 0x400000, 0x401fff, TC0430GRW_word_r },
		{ 0x700000, 0x701fff, MRA16_RAM },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_r },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_r },
		{ 0x900000, 0x90ffff, MRA16_RAM },
		{ 0xb00000, 0xb0000f, TC0510NIO_halfword_r },
		{ 0xb00018, 0xb0001f, driftout_paddle_r },
	MEMORY_END
	
	static MEMORY_WRITE16_START( driveout_writemem )
		{ 0x000000, 0x0fffff, MWA16_ROM },
		{ 0x200000, 0x200003, driveout_sound_command_w },
		{ 0x300000, 0x30ffff, MWA16_RAM },
		{ 0x400000, 0x401fff, TC0430GRW_word_w },	/* ROZ tilemap */
		{ 0x402000, 0x40200f, TC0430GRW_ctrl_word_w },
		{ 0x700000, 0x701fff, paletteram16_xRRRRRGGGGGBBBBB_word_w, &paletteram16 },
		{ 0x800000, 0x80ffff, TC0100SCN_word_0_w },	/* tilemaps */
		{ 0x820000, 0x82000f, TC0100SCN_ctrl_word_0_w },
		{ 0x900000, 0x90ffff, MWA16_RAM, &spriteram16, &spriteram_size },
		{ 0xa00000, 0xa0001f, TC0360PRI_halfword_swap_w },
		{ 0xb00000, 0xb0000f, TC0510NIO_halfword_w },
	MEMORY_END
	
	
	/***************************************************************************/
	
	public static Memory_ReadAddress sound_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x3fff, MRA_ROM ),
		new Memory_ReadAddress( 0x4000, 0x7fff, MRA_BANK2 ),
		new Memory_ReadAddress( 0xc000, 0xdfff, MRA_RAM ),
		new Memory_ReadAddress( 0xe000, 0xe000, YM2610_status_port_0_A_r ),
		new Memory_ReadAddress( 0xe001, 0xe001, YM2610_read_port_0_r ),
		new Memory_ReadAddress( 0xe002, 0xe002, YM2610_status_port_0_B_r ),
		new Memory_ReadAddress( 0xe200, 0xe200, MRA_NOP ),
		new Memory_ReadAddress( 0xe201, 0xe201, taitosound_slave_comm_r ),
		new Memory_ReadAddress( 0xea00, 0xea00, MRA_NOP ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress sound_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0xc000, 0xdfff, MWA_RAM ),
		new Memory_WriteAddress( 0xe000, 0xe000, YM2610_control_port_0_A_w ),
		new Memory_WriteAddress( 0xe001, 0xe001, YM2610_data_port_0_A_w ),
		new Memory_WriteAddress( 0xe002, 0xe002, YM2610_control_port_0_B_w ),
		new Memory_WriteAddress( 0xe003, 0xe003, YM2610_data_port_0_B_w ),
		new Memory_WriteAddress( 0xe200, 0xe200, taitosound_slave_port_w ),
		new Memory_WriteAddress( 0xe201, 0xe201, taitosound_slave_comm_w ),
		new Memory_WriteAddress( 0xe400, 0xe403, MWA_NOP ), /* pan */
		new Memory_WriteAddress( 0xee00, 0xee00, MWA_NOP ), /* ? */
		new Memory_WriteAddress( 0xf000, 0xf000, MWA_NOP ), /* ? */
		new Memory_WriteAddress( 0xf200, 0xf200, sound_bankswitch_w ),	/* ?? */
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	/* Alt US version of Cameltry, YM2203 sound, missing ADPCM ? */
	
	public static Memory_ReadAddress camltrua_sound_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),	// I can't see a bank control, but there ARE some bytes past 0x8000
	//	new Memory_ReadAddress( 0x4000, 0x7fff, MRA_BANK1 ),
		new Memory_ReadAddress( 0x8000, 0x8fff, MRA_RAM ),
		new Memory_ReadAddress( 0x9000, 0x9000, YM2203_status_port_0_r ),
		new Memory_ReadAddress( 0xa001, 0xa001, taitosound_slave_comm_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress camltrua_sound_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0x8000, 0x8fff, MWA_RAM ),
		new Memory_WriteAddress( 0x9000, 0x9000, YM2203_control_port_0_w ),
		new Memory_WriteAddress( 0x9001, 0x9001, YM2203_write_port_0_w ),
		new Memory_WriteAddress( 0xa000, 0xa000, taitosound_slave_port_w ),
		new Memory_WriteAddress( 0xa001, 0xa001, taitosound_slave_comm_w ),
	//	new Memory_WriteAddress( 0xb000, 0xb000, unknown_w ),	// probably controlling sample player?
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	public static Memory_ReadAddress driveout_sound_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0x87ff, MRA_RAM ),
		new Memory_ReadAddress( 0x9800, 0x9800, OKIM6295_status_0_r ),
		new Memory_ReadAddress( 0xa000, 0xa000, driveout_sound_command_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress driveout_sound_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0x8000, 0x87ff, MWA_RAM ),
		new Memory_WriteAddress( 0x9000, 0x9000, oki_bank_w ),
		new Memory_WriteAddress( 0x9800, 0x9800, OKIM6295_data_0_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	/***********************************************************
	                     INPUT PORTS, DIPs
	***********************************************************/
	
	#define TAITO_COINAGE_WORLD_8 \
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Coin_A") ); \
		PORT_DIPSETTING(    0x00, DEF_STR( "4C_1C") ); \
		PORT_DIPSETTING(    0x10, DEF_STR( "3C_1C") ); \
		PORT_DIPSETTING(    0x20, DEF_STR( "2C_1C") ); \
		PORT_DIPSETTING(    0x30, DEF_STR( "1C_1C") ); \
		PORT_DIPNAME( 0xc0, 0xc0, DEF_STR( "Coin_B") ); \
		PORT_DIPSETTING(    0xc0, DEF_STR( "1C_2C") ); \
		PORT_DIPSETTING(    0x80, DEF_STR( "1C_3C") ); \
		PORT_DIPSETTING(    0x40, DEF_STR( "1C_4C") ); \
		PORT_DIPSETTING(    0x00, DEF_STR( "1C_6C") );
	
	#define TAITO_COINAGE_JAPAN_8 \
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Coin_A") ); \
		PORT_DIPSETTING(    0x10, DEF_STR( "2C_1C") ); \
		PORT_DIPSETTING(    0x30, DEF_STR( "1C_1C") ); \
		PORT_DIPSETTING(    0x00, DEF_STR( "2C_3C") ); \
		PORT_DIPSETTING(    0x20, DEF_STR( "1C_2C") ); \
		PORT_DIPNAME( 0xc0, 0xc0, DEF_STR( "Coin_B") ); \
		PORT_DIPSETTING(    0x40, DEF_STR( "2C_1C") ); \
		PORT_DIPSETTING(    0xc0, DEF_STR( "1C_1C") ); \
		PORT_DIPSETTING(    0x00, DEF_STR( "2C_3C") ); \
		PORT_DIPSETTING(    0x80, DEF_STR( "1C_2C") );
	
	/* This is a new Japanese coinage used in later TAITO games */
	#define TAITO_COINAGE_JAPAN_NEW_8 \
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Coin_A") ); \
		PORT_DIPSETTING(    0x00, DEF_STR( "3C_1C") ); \
		PORT_DIPSETTING(    0x10, DEF_STR( "2C_1C") ); \
		PORT_DIPSETTING(    0x30, DEF_STR( "1C_1C") ); \
		PORT_DIPSETTING(    0x20, DEF_STR( "1C_2C") ); \
		PORT_DIPNAME( 0xc0, 0xc0, DEF_STR( "Coin_B") ); \
		PORT_DIPSETTING(    0x00, DEF_STR( "3C_1C") ); \
		PORT_DIPSETTING(    0x40, DEF_STR( "2C_1C") ); \
		PORT_DIPSETTING(    0xc0, DEF_STR( "1C_1C") ); \
		PORT_DIPSETTING(    0x80, DEF_STR( "1C_2C") );
	
	#define TAITO_COINAGE_US_8 \
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Coinage") ); \
		PORT_DIPSETTING(    0x00, DEF_STR( "4C_1C") ); \
		PORT_DIPSETTING(    0x10, DEF_STR( "3C_1C") ); \
		PORT_DIPSETTING(    0x20, DEF_STR( "2C_1C") ); \
		PORT_DIPSETTING(    0x30, DEF_STR( "1C_1C") ); \
		PORT_DIPNAME( 0xc0, 0xc0, "Price to Continue" );\
		PORT_DIPSETTING(    0x00, DEF_STR( "3C_1C") ); \
		PORT_DIPSETTING(    0x40, DEF_STR( "2C_1C") ); \
		PORT_DIPSETTING(    0x80, DEF_STR( "1C_1C") ); \
		PORT_DIPSETTING(    0xc0, "Same as Start" );
	
	#define TAITO_DIFFICULTY_8 \
		PORT_DIPNAME( 0x03, 0x03, DEF_STR( "Difficulty") ); \
		PORT_DIPSETTING(    0x02, "Easy" );\
		PORT_DIPSETTING(    0x03, "Medium" );\
		PORT_DIPSETTING(    0x01, "Hard" );\
		PORT_DIPSETTING(    0x00, "Hardest" );
	
	#define TAITO_F2_PLAYERS_INPUT( player ) \
		PORT_START();  \
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | player );\
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | player );\
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | player );\
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | player );\
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | player );\
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | player );
	/* 0x40 and 0x80 are not always the same in all games, so they are not included here */
	
	#define TAITO_F2_SYSTEM_INPUT \
		PORT_START();  \
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_TILT );\
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_SERVICE1 );\
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN1 );\
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_COIN2 );
	/* The other bits vary from one game to another, so they are not included here */
	
	
	static InputPortPtr input_ports_finalb = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		/* Not sure how to handle alternate controls */
		PORT_DIPNAME( 0x01, 0x01, "Alternate Controls" );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_WORLD_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
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
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT	/* controls below are DIP selectable */
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER1 );/* 1P sen.sw.? */
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER1 );/* 1P ducking? */
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER2 );/* 2P sen.sw.? */
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER2 );/* 2P ducking? */
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_finalbj = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		/* Not sure how to handle alternate controls */
		PORT_DIPNAME( 0x01, 0x01, "Alternate Controls" );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_JAPAN_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
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
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT	/* controls below are DIP selectable */
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER1 );/* 1P sen.sw.? */
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER1 );/* 1P ducking? */
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER2 );/* 2P sen.sw.? */
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER2 );/* 2P ducking? */
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_dondokod = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_WORLD_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x0c, "10k and 100k" );
		PORT_DIPSETTING(    0x08, "10k and 150k" );
		PORT_DIPSETTING(    0x04, "10k and 250k" );
		PORT_DIPSETTING(    0x00, "10k and 350k" );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x20, "2" );
		PORT_DIPSETTING(    0x30, "3" );
		PORT_DIPSETTING(    0x00, "4" );
		PORT_DIPSETTING(    0x10, "5" );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_dondokdu = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_US_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x0c, "10k and 100k" );
		PORT_DIPSETTING(    0x08, "10k and 150k" );
		PORT_DIPSETTING(    0x04, "10k and 250k" );
		PORT_DIPSETTING(    0x00, "10k and 350k" );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x20, "2" );
		PORT_DIPSETTING(    0x30, "3" );
		PORT_DIPSETTING(    0x00, "4" );
		PORT_DIPSETTING(    0x10, "5" );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_dondokdj = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_JAPAN_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x0c, "10k and 100k" );
		PORT_DIPSETTING(    0x08, "10k and 150k" );
		PORT_DIPSETTING(    0x04, "10k and 250k" );
		PORT_DIPSETTING(    0x00, "10k and 350k" );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x20, "2" );
		PORT_DIPSETTING(    0x30, "3" );
		PORT_DIPSETTING(    0x00, "4" );
		PORT_DIPSETTING(    0x10, "5" );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_megab = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_WORLD_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x0c, "100k only" );
		PORT_DIPSETTING(    0x04, "150k only" );
		PORT_DIPSETTING(    0x08, "200k only" );
		PORT_DIPSETTING(    0x00, "None" );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x10, "1" );
		PORT_DIPSETTING(    0x00, "2" );
		PORT_DIPSETTING(    0x30, "3" );
		PORT_DIPSETTING(    0x20, "4" );
		PORT_DIPNAME( 0x40, 0x40, "Upright Controls" );/* ie single or two players at once */
		PORT_DIPSETTING(    0x00, "Single" );
		PORT_DIPSETTING(    0x40, "Dual" );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_megabj = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_JAPAN_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x0c, "100k only" );
		PORT_DIPSETTING(    0x04, "150k only" );
		PORT_DIPSETTING(    0x08, "200k only" );
		PORT_DIPSETTING(    0x00, "None" );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x10, "1" );
		PORT_DIPSETTING(    0x00, "2" );
		PORT_DIPSETTING(    0x30, "3" );
		PORT_DIPSETTING(    0x20, "4" );
		PORT_DIPNAME( 0x40, 0x40, "Upright Controls" );/* ie single or two players at once */
		PORT_DIPSETTING(    0x00, "Single" );
		PORT_DIPSETTING(    0x40, "Dual" );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_thundfox = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unused") );  // all 2 in manual
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_WORLD_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x04, 0x04, "Timer" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x20, "2" );
		PORT_DIPSETTING(    0x30, "3" );
		PORT_DIPSETTING(    0x10, "4" );
		PORT_DIPSETTING(    0x00, "5" );
		PORT_DIPNAME( 0x40, 0x40, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, "Upright Controls" );/* ie single or two players at once */
		PORT_DIPSETTING(    0x00, "Single" );
		PORT_DIPSETTING(    0x80, "Dual" );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_thndfoxu = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unused") );  // all 2 in manual
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_US_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x04, 0x04, "Timer" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x20, "2" );
		PORT_DIPSETTING(    0x30, "3" );
		PORT_DIPSETTING(    0x10, "4" );
		PORT_DIPSETTING(    0x00, "5" );
		PORT_DIPNAME( 0x40, 0x40, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, "Upright Controls" );/* ie single or two players at once */
		PORT_DIPSETTING(    0x00, "Single" );
		PORT_DIPSETTING(    0x80, "Dual" );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_thndfoxj = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unused") );  // all 2 in manual
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_JAPAN_NEW_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x04, 0x04, "Timer" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x20, "2" );
		PORT_DIPSETTING(    0x30, "3" );
		PORT_DIPSETTING(    0x10, "4" );
		PORT_DIPSETTING(    0x00, "5" );
		PORT_DIPNAME( 0x40, 0x40, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, "Upright Controls" );/* ie single or two players at once */
		PORT_DIPSETTING(    0x00, "Single" );
		PORT_DIPSETTING(    0x80, "Dual" );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_cameltry = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_US_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, "Start remain time" );
		PORT_DIPSETTING(    0x00, "35" );
		PORT_DIPSETTING(    0x04, "40" );
		PORT_DIPSETTING(    0x0c, "50" );
		PORT_DIPSETTING(    0x08, "60" );
		PORT_DIPNAME( 0x30, 0x30, "Continue play time" );
		PORT_DIPSETTING(    0x00, "+20" );
		PORT_DIPSETTING(    0x10, "+25" );
		PORT_DIPSETTING(    0x30, "+30" );
		PORT_DIPSETTING(    0x20, "+40" );
		PORT_DIPNAME( 0x40, 0x40, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, "Upright Controls" );/* ie single or two players at once */
		PORT_DIPSETTING(    0x80, "Single" );
		PORT_DIPSETTING(    0x00, "Dual" );
	
		PORT_START();       /* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		PORT_START();       /* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START();   /* Paddle A */
		PORT_ANALOG( 0xff, 0x00, IPT_DIAL | IPF_PLAYER1, 100, 20, 0, 0 );
	
		PORT_START();   /* Paddle B */
		PORT_ANALOG( 0xff, 0x00, IPT_DIAL | IPF_PLAYER2, 100, 20, 0, 0 );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_cameltrj = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_JAPAN_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, "Start remain time" );
		PORT_DIPSETTING(    0x00, "35" );
		PORT_DIPSETTING(    0x04, "40" );
		PORT_DIPSETTING(    0x0c, "50" );
		PORT_DIPSETTING(    0x08, "60" );
		PORT_DIPNAME( 0x30, 0x30, "Continue play time" );
		PORT_DIPSETTING(    0x00, "+20" );
		PORT_DIPSETTING(    0x10, "+25" );
		PORT_DIPSETTING(    0x30, "+30" );
		PORT_DIPSETTING(    0x20, "+40" );
		PORT_DIPNAME( 0x40, 0x40, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, "Upright Controls" );/* ie single or two players at once */
		PORT_DIPSETTING(    0x80, "Single" );
		PORT_DIPSETTING(    0x00, "Dual" );
	
		PORT_START();       /* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		PORT_START();       /* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START();   /* Paddle A */
		PORT_ANALOG( 0xff, 0x00, IPT_DIAL | IPF_PLAYER1, 100, 20, 0, 0 );
	
		PORT_START();   /* Paddle B */
		PORT_ANALOG( 0xff, 0x00, IPT_DIAL | IPF_PLAYER2, 100, 20, 0, 0 );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_qtorimon = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unused") ); // all 5 in manual
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		TAITO_COINAGE_JAPAN_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x10, "1" );
		PORT_DIPSETTING(    0x30, "3" );
		PORT_DIPSETTING(    0x20, "2" );
		PORT_DIPSETTING(    0x00, "4" );
		PORT_DIPNAME( 0x40, 0x40, "Show Correct Answer" );
		PORT_DIPSETTING(    0x40, DEF_STR( "No") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		PORT_START();       /* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		PORT_START();       /* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_liquidk = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_WORLD_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x0c, "30k and 100k" );
		PORT_DIPSETTING(    0x08, "30k and 150k" );
		PORT_DIPSETTING(    0x04, "50k and 250k" );
		PORT_DIPSETTING(    0x00, "50k and 350k" );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x20, "2" );
		PORT_DIPSETTING(    0x30, "3" );
		PORT_DIPSETTING(    0x00, "4" );
		PORT_DIPSETTING(    0x10, "5" );
		PORT_DIPNAME( 0x40, 0x40, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, "Upright Controls" );/* ie single or two players at once */
		PORT_DIPSETTING(    0x80, "Single" );
		PORT_DIPSETTING(    0x00, "Dual" );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_liquidku = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_US_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x0c, "30k and 100k" );
		PORT_DIPSETTING(    0x08, "30k and 150k" );
		PORT_DIPSETTING(    0x04, "50k and 250k" );
		PORT_DIPSETTING(    0x00, "50k and 350k" );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x20, "2" );
		PORT_DIPSETTING(    0x30, "3" );
		PORT_DIPSETTING(    0x00, "4" );
		PORT_DIPSETTING(    0x10, "5" );
		PORT_DIPNAME( 0x40, 0x40, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, "Upright Controls" );/* ie single or two players at once */
		PORT_DIPSETTING(    0x80, "Single" );
		PORT_DIPSETTING(    0x00, "Dual" );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_mizubaku = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_JAPAN_NEW_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x0c, "30k and 100k" );
		PORT_DIPSETTING(    0x08, "30k and 150k" );
		PORT_DIPSETTING(    0x04, "50k and 250k" );
		PORT_DIPSETTING(    0x00, "50k and 350k" );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x20, "2" );
		PORT_DIPSETTING(    0x30, "3" );
		PORT_DIPSETTING(    0x00, "4" );
		PORT_DIPSETTING(    0x10, "5" );
		PORT_DIPNAME( 0x40, 0x40, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, "Upright Controls" );/* ie single or two players at once */
		PORT_DIPSETTING(    0x80, "Single" );
		PORT_DIPSETTING(    0x00, "Dual" );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_ssi = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_WORLD_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, "Shields" );
		PORT_DIPSETTING(    0x00, "None");
		PORT_DIPSETTING(    0x0c, "1");
		PORT_DIPSETTING(    0x04, "2");
		PORT_DIPSETTING(    0x08, "3");
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "2");
		PORT_DIPSETTING(    0x10, "3");
		PORT_DIPNAME( 0xa0, 0xa0, "2 Players Mode" );
		PORT_DIPSETTING(    0xa0, "Simultaneous");
		PORT_DIPSETTING(    0x80, "Alternate, Single");
		PORT_DIPSETTING(    0x00, "Alternate, Dual");
		PORT_DIPSETTING(    0x20, "Not Allowed");
		PORT_DIPNAME( 0x40, 0x40, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_majest12 = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_JAPAN_NEW_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, "Shields" );
		PORT_DIPSETTING(    0x00, "None");
		PORT_DIPSETTING(    0x0c, "1");
		PORT_DIPSETTING(    0x04, "2");
		PORT_DIPSETTING(    0x08, "3");
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "2");
		PORT_DIPSETTING(    0x10, "3");
		PORT_DIPNAME( 0xa0, 0xa0, "2 Players Mode" );
		PORT_DIPSETTING(    0xa0, "Simultaneous");
		PORT_DIPSETTING(    0x80, "Alternate, Single Controls");
		PORT_DIPSETTING(    0x00, "Alternate, Dual Controls");
		PORT_DIPSETTING(    0x20, "Not Allowed");
		PORT_DIPNAME( 0x40, 0x40, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_growl = new InputPortPtr(){ public void handler() { 
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unused") );  //are "unused" verified from manual?
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_WORLD_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x30, 0x30, "Cabinet Type" );
		PORT_DIPSETTING(    0x30, "2 Players" );
		PORT_DIPSETTING(    0x20, "4 Players / 4 Coin Slots" );// Push Player button A to start
		PORT_DIPSETTING(    0x10, "4 Players / 2 cabinets combined" );
		PORT_DIPSETTING(    0x00, "4 Players / 2 Coin Slots" );
		PORT_DIPNAME( 0x40, 0x40, "Final Boss Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "No") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		PORT_START();       /* IN3 */
		PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER3 );
		PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER3 );
		PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER3 );
		PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER3 );
		PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER3 );
		PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER3 );
		PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER3 );
		PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_START3 );
		PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER4 );
		PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER4 );
		PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER4 );
		PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER4 );
		PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER4 );
		PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER4 );
		PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER4 );
		PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_START4 );
	
		PORT_START();       /* IN4 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN3 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN4 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_SERVICE2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_growlu = new InputPortPtr(){ public void handler() { 
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unused") );  //are "unused" verified from manual?
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_US_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x30, 0x30, "Cabinet Type" );
		PORT_DIPSETTING(    0x30, "2 Players" );
		PORT_DIPSETTING(    0x20, "4 Players / 4 Coin Slots" );// Push Player button A to start
		PORT_DIPSETTING(    0x10, "4 Players / 2 cabinets combined" );
		PORT_DIPSETTING(    0x00, "4 Players / 2 Coin Slots" );
		PORT_DIPNAME( 0x40, 0x40, "Final Boss Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "No") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		PORT_START();       /* IN3 */
		PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER3 );
		PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER3 );
		PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER3 );
		PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER3 );
		PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER3 );
		PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER3 );
		PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER3 );
		PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_START3 );
		PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER4 );
		PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER4 );
		PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER4 );
		PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER4 );
		PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER4 );
		PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER4 );
		PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER4 );
		PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_START4 );
	
		PORT_START();       /* IN4 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN3 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN4 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_SERVICE2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_runark = new InputPortPtr(){ public void handler() { 
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unused") );  //are "unused" verified from manual?
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_JAPAN_NEW_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x30, 0x30, "Cabinet Type" );
		PORT_DIPSETTING(    0x30, "2 Players" );
		PORT_DIPSETTING(    0x20, "4 Players / 4 Coin Slots" );// Push Player button A to start
		PORT_DIPSETTING(    0x10, "4 Players / 2 cabinets combined" );
		PORT_DIPSETTING(    0x00, "4 Players / 2 Coin Slots" );
		PORT_DIPNAME( 0x40, 0x40, "Final Boss Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "No") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		PORT_START();       /* IN3 */
		PORT_BIT( 0x0001, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER3 );
		PORT_BIT( 0x0002, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER3 );
		PORT_BIT( 0x0004, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER3 );
		PORT_BIT( 0x0008, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER3 );
		PORT_BIT( 0x0010, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER3 );
		PORT_BIT( 0x0020, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER3 );
		PORT_BIT( 0x0040, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER3 );
		PORT_BIT( 0x0080, IP_ACTIVE_LOW, IPT_START3 );
		PORT_BIT( 0x0100, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER4 );
		PORT_BIT( 0x0200, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER4 );
		PORT_BIT( 0x0400, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER4 );
		PORT_BIT( 0x0800, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER4 );
		PORT_BIT( 0x1000, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER4 );
		PORT_BIT( 0x2000, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER4 );
		PORT_BIT( 0x4000, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER4 );
		PORT_BIT( 0x8000, IP_ACTIVE_LOW, IPT_START4 );
	
		PORT_START();       /* IN4 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN3 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN4 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_SERVICE2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_pulirula = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_WORLD_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, "Magic" );
		PORT_DIPSETTING(    0x0c, "3" );
		PORT_DIPSETTING(    0x08, "4" );
		PORT_DIPSETTING(    0x04, "5" );
	//	PORT_DIPSETTING(    0x00, "5" );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x20, "2" );
		PORT_DIPSETTING(    0x30, "3" );
		PORT_DIPSETTING(    0x10, "4" );
		PORT_DIPSETTING(    0x00, "5" );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, "Upright Controls" );/* ie single or two players at once */
		PORT_DIPSETTING(    0x00, "Single" );
		PORT_DIPSETTING(    0x80, "Dual" );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_pulirulj = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_JAPAN_NEW_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, "Magic" );
		PORT_DIPSETTING(    0x0c, "3" );
		PORT_DIPSETTING(    0x08, "4" );
		PORT_DIPSETTING(    0x04, "5" );
	//	PORT_DIPSETTING(    0x00, "5" );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x20, "2" );
		PORT_DIPSETTING(    0x30, "3" );
		PORT_DIPSETTING(    0x10, "4" );
		PORT_DIPSETTING(    0x00, "5" );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, "Upright Controls" );/* ie single or two players at once */
		PORT_DIPSETTING(    0x00, "Single" );
		PORT_DIPSETTING(    0x80, "Dual" );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_qzquest = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_JAPAN_NEW_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8  //??
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
	
		PORT_START();       /* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		PORT_START();       /* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_qzchikyu = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_JAPAN_NEW_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8  //??
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
	
		PORT_START();       /* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		PORT_START();       /* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_footchmp = new InputPortPtr(){ public void handler() { 
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		PORT_START();       /* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN3 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_COIN4 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_SERVICE2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_SERVICE3 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_TILT );
	
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x00, "Game Over Type" );// 2p simultaneous play
		PORT_DIPSETTING(    0x01, "Both Teams' Games Over" );
		PORT_DIPSETTING(    0x00, "Losing Team's Game is Over" );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_WORLD_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, "Game Time" );
		PORT_DIPSETTING(    0x00, "1.5 Minutes" );
		PORT_DIPSETTING(    0x0c, " 2  Minutes" );
		PORT_DIPSETTING(    0x04, "2.5 Minutes" );
		PORT_DIPSETTING(    0x08, " 3  Minutes" );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x30, "2 Players" );
		PORT_DIPSETTING(    0x20, "4 Players / 4 Coin Slots" );// Push Player button A to start
		PORT_DIPSETTING(    0x10, "4 Players / 2 cabinets combined" );
		PORT_DIPSETTING(    0x00, "4 Players / 2 Coin Slots" );
		PORT_DIPNAME( 0x40, 0x40, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, "Game Version" );// Not used for Hat Trick Hero / Euro Champ '92
		PORT_DIPSETTING(    0x00, "Normal" );
		PORT_DIPSETTING(    0x80, "European" );
	
		/* IN3 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER3 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START3 );
	
		/* IN4 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER4 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START4 );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_hthero = new InputPortPtr(){ public void handler() { 
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		PORT_START();       /* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN3 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_COIN4 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_SERVICE2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_SERVICE3 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_TILT );
	
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x80, 0x00, "Game Over Type" );// 2p simultaneous play
		PORT_DIPSETTING(    0x80, "Both Teams' Games Over" );
		PORT_DIPSETTING(    0x00, "Losing Team's Game is Over" );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x20, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x10, DEF_STR( "On") );
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Coin_A") );
		PORT_DIPSETTING(    0x00, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x08, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x0c, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x04, DEF_STR( "1C_2C") );
		PORT_DIPNAME( 0x03, 0x03, DEF_STR( "Coin_B") );
		PORT_DIPSETTING(    0x00, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x02, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x03, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x01, DEF_STR( "1C_2C") );
	
		PORT_START();  /* DSW B */
		PORT_DIPNAME( 0xc0, 0xc0, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x40, "Easy" );
		PORT_DIPSETTING(    0xc0, "Medium" );
		PORT_DIPSETTING(    0x80, "Hard" );
		PORT_DIPSETTING(    0x00, "Hardest" );
		PORT_DIPNAME( 0x30, 0x30, "Game Time" );
		PORT_DIPSETTING(    0x00, "1.5 Minutes" );
		PORT_DIPSETTING(    0x30, " 2  Minutes" );
		PORT_DIPSETTING(    0x20, "2.5 Minutes" );
		PORT_DIPSETTING(    0x10, " 3  Minutes" );
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x0c, "2 Players" );
		PORT_DIPSETTING(    0x04, "4 Players / 4 Coin Slots" );// Push Player button A to start
		PORT_DIPSETTING(    0x08, "4 Players / 2 cabinets combined" );
		PORT_DIPSETTING(    0x00, "4 Players / 2 Coin Slots" );
		PORT_DIPNAME( 0x02, 0x02, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x02, DEF_STR( "On") );
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		/* IN3 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER3 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START3 );
	
		/* IN4 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER4 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START4 );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_ninjak = new InputPortPtr(){ public void handler() { 
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		PORT_START();       /* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_TILT );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_SERVICE2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_COIN3 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN4 );
	
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unused") );  //is this verified from manual?
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_WORLD_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, "Cabinet Type" );
		PORT_DIPSETTING(    0x0c, "2 players" );
		PORT_DIPSETTING(    0x08, "TROG (4 players / 2 coin slots); )
		PORT_DIPSETTING(    0x04, "MTX2 (4 players / 2 cabinets combined); )
		PORT_DIPSETTING(    0x00, "TMNT (4 players / 4 coin slots); )
		PORT_DIPNAME( 0x30, 0x10, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x20, "2" );
		PORT_DIPSETTING(    0x30, "3" );
		PORT_DIPSETTING(    0x10, "4" );
		PORT_DIPSETTING(    0x00, "5" );
		PORT_DIPNAME( 0x40, 0x40, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, "Game Type" );
		PORT_DIPSETTING(    0x00, "1 Player only" );
		PORT_DIPSETTING(    0x80, "Multiplayer" );
	
		/* IN3 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER3 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START3 );
	
		/* IN4 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER4 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START4 );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_ninjakj = new InputPortPtr(){ public void handler() { 
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		PORT_START();       /* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_TILT );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_SERVICE2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_COIN3 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN4 );
	
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unused") );  //is this verified from manual?
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_JAPAN_NEW_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, "Cabinet Type" );
		PORT_DIPSETTING(    0x0c, "2 players" );
		PORT_DIPSETTING(    0x08, "TROG (4 players / 2 coin slots); )
		PORT_DIPSETTING(    0x04, "MTX2 (4 players / 2 cabinets combined); )
		PORT_DIPSETTING(    0x00, "TMNT (4 players / 4 coin slots); )
		PORT_DIPNAME( 0x30, 0x10, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x20, "2" );
		PORT_DIPSETTING(    0x30, "3" );
		PORT_DIPSETTING(    0x10, "4" );
		PORT_DIPSETTING(    0x00, "5" );
		PORT_DIPNAME( 0x40, 0x40, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, "Game Type" );
		PORT_DIPSETTING(    0x00, "1 Player only" );
		PORT_DIPSETTING(    0x80, "Multiplayer" );
	
		/* IN3 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER3 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START3 );
	
		/* IN4 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER4 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START4 );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_driftout = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unused") );  // all 5 in Service Mode
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_JAPAN_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, "Control" );  /* correct acc. to service mode */
		PORT_DIPSETTING(    0x0c, "Joystick" );
		PORT_DIPSETTING(    0x08, "Paddle" );
		PORT_DIPSETTING(    0x04, "Joystick" );
		PORT_DIPSETTING(    0x00, "Steering wheel" );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		PORT_START();       /* IN1 */
		PORT_BIT( 0xff, IP_ACTIVE_LOW, IPT_UNKNOWN );/* 2P not used? */
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START();   /* Paddle A */
		PORT_ANALOG( 0xff, 0x00, IPT_DIAL | IPF_PLAYER1, 50, 10, 0, 0 );
	
		PORT_START();   /* Paddle B */
		PORT_ANALOG( 0xff, 0x00, IPT_DIAL | IPF_PLAYER2, 50, 10, 0, 0 );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_gunfront = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_WORLD_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x08, "10k and every 80k" );
		PORT_DIPSETTING(    0x0c, "20k and every 80k" );
		PORT_DIPSETTING(    0x04, "30k and every 80k" );
		PORT_DIPSETTING(    0x00, "60k and every 80k" );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x20, "1" );
		PORT_DIPSETTING(    0x10, "2" );
		PORT_DIPSETTING(    0x30, "3" );
		PORT_DIPSETTING(    0x00, "5" );
		PORT_DIPNAME( 0x40, 0x40, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, "Upright Controls" );/* ie single or two players at once */
		PORT_DIPSETTING(    0x00, "Single" );
		PORT_DIPSETTING(    0x80, "Dual" );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_gunfronj = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_JAPAN_NEW_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x08, "10k and every 80k" );
		PORT_DIPSETTING(    0x0c, "20k and every 80k" );
		PORT_DIPSETTING(    0x04, "30k and every 80k" );
		PORT_DIPSETTING(    0x00, "60k and every 80k" );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x20, "1" );
		PORT_DIPSETTING(    0x10, "2" );
		PORT_DIPSETTING(    0x30, "3" );
		PORT_DIPSETTING(    0x00, "5" );
		PORT_DIPNAME( 0x40, 0x40, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, "Upright Controls" );/* ie single or two players at once */
		PORT_DIPSETTING(    0x00, "Single" );
		PORT_DIPSETTING(    0x80, "Dual" );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_metalb = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_WORLD_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x04, "70k and every 150k" );
		PORT_DIPSETTING(    0x0c, "80k 150k 240k 400k" );
		PORT_DIPSETTING(    0x00, "80k 170k 290k 410k" );
		PORT_DIPSETTING(    0x08, "100k and every 200k" );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "1" );
		PORT_DIPSETTING(    0x10, "2" );
		PORT_DIPSETTING(    0x30, "3" );
		PORT_DIPSETTING(    0x20, "5" );
		PORT_DIPNAME( 0x40, 0x40, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, "Upright Controls" );/* ie single or two players at once */
		PORT_DIPSETTING(    0x00, "Single" );
		PORT_DIPSETTING(    0x80, "Dual" );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_metalbj = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_JAPAN_NEW_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x04, "70k and every 150k" );
		PORT_DIPSETTING(    0x0c, "80k 150k 240k 400k" );
		PORT_DIPSETTING(    0x00, "80k 170k 290k 410k" );
		PORT_DIPSETTING(    0x08, "100k and every 200k" );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "1" );
		PORT_DIPSETTING(    0x10, "2" );
		PORT_DIPSETTING(    0x30, "3" );
		PORT_DIPSETTING(    0x20, "5" );
		PORT_DIPNAME( 0x40, 0x40, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, "Upright Controls" );/* ie single or two players at once */
		PORT_DIPSETTING(    0x00, "Single" );
		PORT_DIPSETTING(    0x80, "Dual" );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_deadconx = new InputPortPtr(){ public void handler() { 
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		PORT_START();       /* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN3 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_COIN4 );
		PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_SERVICE, "Service A", KEYCODE_9, IP_JOY_NONE );
		PORT_BITX(0x20, IP_ACTIVE_LOW, IPT_SERVICE, "Service B", KEYCODE_0, IP_JOY_NONE );
		PORT_BITX(0x40, IP_ACTIVE_LOW, IPT_SERVICE, "Service C", KEYCODE_MINUS, IP_JOY_NONE );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_TILT );
	
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_WORLD_8
	
		PORT_START();  /* DSW B, missing a timer speed maybe? */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On"));
		PORT_DIPNAME( 0x18, 0x18, "Damage" );
		PORT_DIPSETTING(    0x10, "Small" );	/* Hero can take 12 gun shots */
		PORT_DIPSETTING(    0x18, "Normal" );/* Hero can take 10 gun shots */
		PORT_DIPSETTING(    0x08, "Big" );	/* Hero can take 8 gun shots */
		PORT_DIPSETTING(    0x00, "Biggest" );/* Hero can take 5 gun shots */
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On"));
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, "Game Type" );
		PORT_DIPSETTING(    0x00, "1 Player only" );
		PORT_DIPSETTING(    0x80, "Multiplayer" );
	
		/* IN3 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER3 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START3 );
	
		/* IN4 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER4 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START4 );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_deadconj = new InputPortPtr(){ public void handler() { 
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		PORT_START();       /* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN3 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_COIN4 );
		PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_SERVICE, "Service A", KEYCODE_9, IP_JOY_NONE );
		PORT_BITX(0x20, IP_ACTIVE_LOW, IPT_SERVICE, "Service B", KEYCODE_0, IP_JOY_NONE );
		PORT_BITX(0x40, IP_ACTIVE_LOW, IPT_SERVICE, "Service C", KEYCODE_MINUS, IP_JOY_NONE );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_TILT );
	
		/* hthero and deadconj have the dips in inverted order */
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x20, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x10, DEF_STR( "On") );
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Coin_A") );
		PORT_DIPSETTING(    0x00, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x08, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x0c, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x04, DEF_STR( "1C_2C") );
		PORT_DIPNAME( 0x03, 0x03, DEF_STR( "Coin_B") );
		PORT_DIPSETTING(    0x00, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x02, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x03, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x01, DEF_STR( "1C_2C") );
	
		PORT_START();  /* DSW B */
		PORT_DIPNAME( 0xc0, 0xc0, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x40, "Easy" );
		PORT_DIPSETTING(    0xc0, "Medium" );
		PORT_DIPSETTING(    0x80, "Hard" );
		PORT_DIPSETTING(    0x00, "Hardest" );
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On"));
		PORT_DIPNAME( 0x18, 0x18, "Damage" );
		PORT_DIPSETTING(    0x08, "Small" );	/* Hero can take 12 gun shots */
		PORT_DIPSETTING(    0x18, "Normal" );/* Hero can take 10 gun shots */
		PORT_DIPSETTING(    0x10, "Big" );	/* Hero can take 8 gun shots */
		PORT_DIPSETTING(    0x00, "Biggest" );/* Hero can take 5 gun shots */
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On"));
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x01, 0x01, "Game Type" );
		PORT_DIPSETTING(    0x00, "1 Player only" );
		PORT_DIPSETTING(    0x01, "Multiplayer" );
	
		/* IN3 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER3 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START3 );
	
		/* IN4 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER4 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START4 );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_dinorex = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unused") );  //are "unused" verified from manual?
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_WORLD_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, "Damage" );
		PORT_DIPSETTING(    0x08, "Small" );
		PORT_DIPSETTING(    0x0c, "Normal" );
		PORT_DIPSETTING(    0x04, "Big" );
		PORT_DIPSETTING(    0x00, "Biggest" );
		PORT_DIPNAME( 0x10, 0x10, "Timer Speed" ); // Appears to make little difference
		PORT_DIPSETTING(    0x10, "Normal" );
		PORT_DIPSETTING(    0x00, "Fast" );
		PORT_DIPNAME( 0x20, 0x20, "Match Type" );
		PORT_DIPSETTING(    0x20, "Best of 3" );
		PORT_DIPSETTING(    0x00, "Single" );
		PORT_DIPNAME( 0x40, 0x40, "2 Player Mode" );// actually this seems to be unknown
		PORT_DIPSETTING(    0x40, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x80, 0x80, "Upright Controls" );/* ie single or two players at once */
		PORT_DIPSETTING(    0x00, "Single" );
		PORT_DIPSETTING(    0x80, "Dual" );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_dinorexj = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unused") );  //are "unused" verified from manual?
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_JAPAN_NEW_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, "Damage" );
		PORT_DIPSETTING(    0x08, "Small" );
		PORT_DIPSETTING(    0x0c, "Normal" );
		PORT_DIPSETTING(    0x04, "Big" );
		PORT_DIPSETTING(    0x00, "Biggest" );
		PORT_DIPNAME( 0x10, 0x10, "Timer Speed" ); // Appears to make little difference
		PORT_DIPSETTING(    0x10, "Normal" );
		PORT_DIPSETTING(    0x00, "Fast" );
		PORT_DIPNAME( 0x20, 0x20, "Match Type" );
		PORT_DIPSETTING(    0x20, "Best of 3" );
		PORT_DIPSETTING(    0x00, "Single" );
		PORT_DIPNAME( 0x40, 0x40, "2 Player Mode" );// actually this seems to be unknown
		PORT_DIPSETTING(    0x40, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x80, 0x80, "Upright Controls" );/* ie single or two players at once */
		PORT_DIPSETTING(    0x00, "Single" );
		PORT_DIPSETTING(    0x80, "Dual" );
	
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_solfigtr = new InputPortPtr(){ public void handler() { 
		/* IN0 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER1 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		/* IN1 */
		TAITO_F2_PLAYERS_INPUT( IPF_PLAYER2 )
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_WORLD_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
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
	
	static InputPortPtr input_ports_koshien = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A, one lets you control fielders ? */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_JAPAN_NEW_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x04, 0x04, "Timer" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x20, "2" );
		PORT_DIPSETTING(    0x30, "3" );
		PORT_DIPSETTING(    0x10, "4" );
		PORT_DIPSETTING(    0x00, "5" );
		PORT_DIPNAME( 0x40, 0x40, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, "Upright Controls" );/* ie single or two players at once */
		PORT_DIPSETTING(    0x00, "Single" );
		PORT_DIPSETTING(    0x80, "Dual" );
	
		PORT_START();       /* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START();       /* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_quizhq = new InputPortPtr(){ public void handler() { 
		PORT_START();       /* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER1 );// ??
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		PORT_START();       /* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER2 );// ??
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_JAPAN_NEW_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, "Time" );
		PORT_DIPSETTING(    0x0c, "5 seconds" );
		PORT_DIPSETTING(    0x08, "10 seconds" );
		PORT_DIPSETTING(    0x04, "15 seconds" );
		PORT_DIPSETTING(    0x00, "20 seconds" );
		PORT_DIPNAME( 0x30, 0x30, "Stock" ); // Lives
		PORT_DIPSETTING(    0x20, "1" );
		PORT_DIPSETTING(    0x30, "2" );
		PORT_DIPSETTING(    0x10, "3" );
		PORT_DIPSETTING(    0x00, "4" );
		PORT_DIPNAME( 0x40, 0x40, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, "Upright Controls" );/* ie single or two players at once */
		PORT_DIPSETTING(    0x00, "Single" );
		PORT_DIPSETTING(    0x80, "Dual" );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_qjinsei = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_JAPAN_NEW_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
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
	
		PORT_START();       /* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER1 );// ??
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		PORT_START();       /* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER2 );// ??
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_qcrayon = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_JAPAN_NEW_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
		PORT_DIPNAME( 0x0c, 0x0c, "Time" );
		PORT_DIPSETTING(    0x00, "6 seconds" );
		PORT_DIPSETTING(    0x04, "7 seconds" );
		PORT_DIPSETTING(    0x08, "8 seconds" );
		PORT_DIPSETTING(    0x0c, "10 seconds" );
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
	
		PORT_START();       /* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER1 );// ??
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		PORT_START();       /* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER2 );// ??
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_qcrayon2 = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_JAPAN_NEW_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
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
		PORT_DIPNAME( 0x80, 0x80, "Game Control" );
		PORT_DIPSETTING(    0x80, "Joystick" );
		PORT_DIPSETTING(    0x00, "4 Buttons" );
	
		PORT_START();       /* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		PORT_START();       /* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_yuyugogo = new InputPortPtr(){ public void handler() { 
		PORT_START();  /* DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_JAPAN_NEW_8
	
		PORT_START();  /* DSW B */
		TAITO_DIFFICULTY_8
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
		PORT_DIPNAME( 0x40, 0x40, "Allow Continue" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		PORT_START();       /* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER1 );// ??
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		PORT_START();       /* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON5 | IPF_PLAYER2 );// ??
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		/* IN2 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_mjnquest = new InputPortPtr(){ public void handler() { 
		PORT_START();       /* IN0 */
		PORT_BITX(0x01, IP_ACTIVE_LOW, 0, "P1 A", KEYCODE_A, IP_JOY_NONE );
		PORT_BITX(0x02, IP_ACTIVE_LOW, 0, "P1 E", KEYCODE_E, IP_JOY_NONE );
		PORT_BITX(0x04, IP_ACTIVE_LOW, 0, "P1 I", KEYCODE_I, IP_JOY_NONE );
		PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P1 M", KEYCODE_M, IP_JOY_NONE );
		PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P1 Kan", KEYCODE_LCONTROL, IP_JOY_NONE );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START();       /* IN1 */
		PORT_BITX(0x01, IP_ACTIVE_LOW, 0, "P1 B", KEYCODE_B, IP_JOY_NONE );
		PORT_BITX(0x02, IP_ACTIVE_LOW, 0, "P1 F", KEYCODE_F, IP_JOY_NONE );
		PORT_BITX(0x04, IP_ACTIVE_LOW, 0, "P1 J", KEYCODE_J, IP_JOY_NONE );
		PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P1 N", KEYCODE_N, IP_JOY_NONE );
		PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P1 Reach", KEYCODE_LSHIFT, IP_JOY_NONE );
		PORT_BIT( 0xe0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START();       /* IN2 */
		PORT_BITX(0x01, IP_ACTIVE_LOW, 0, "P1 C", KEYCODE_C, IP_JOY_NONE );
		PORT_BITX(0x02, IP_ACTIVE_LOW, 0, "P1 G", KEYCODE_G, IP_JOY_NONE );
		PORT_BITX(0x04, IP_ACTIVE_LOW, 0, "P1 K", KEYCODE_K, IP_JOY_NONE );
		PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P1 Chi", KEYCODE_SPACE, IP_JOY_NONE );
		PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P1 Ron", KEYCODE_Z, IP_JOY_NONE );
		PORT_BIT( 0xe0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START();       /* IN3 */
		PORT_BITX(0x01, IP_ACTIVE_LOW, 0, "P1 D", KEYCODE_D, IP_JOY_NONE );
		PORT_BITX(0x02, IP_ACTIVE_LOW, 0, "P1 H", KEYCODE_H, IP_JOY_NONE );
		PORT_BITX(0x04, IP_ACTIVE_LOW, 0, "P1 L", KEYCODE_L, IP_JOY_NONE );
		PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P1 Pon", KEYCODE_LALT, IP_JOY_NONE );
		PORT_BIT( 0xf0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START();       /* IN4 */
		PORT_BIT( 0xff, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START();       /* IN5 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_TILT );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );	// ?
		PORT_BIT( 0xfc, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START();       /* IN6 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0xfc, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START();       /* IN7:DSW A */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		TAITO_COINAGE_JAPAN_NEW_8
	
		PORT_START();       /* IN8:DSW B */
		TAITO_DIFFICULTY_8
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
	
	static InputPortPtr input_ports_yesnoj = new InputPortPtr(){ public void handler() {    // apparently no test mode, though text in rom suggests printer test
		PORT_START();       /* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0xf0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		/* IN1 */
		TAITO_F2_SYSTEM_INPUT
		PORT_BIT( 0xf0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START();  /* DSW A ??? */
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
	
		PORT_START();  /* DSW B ? */
		PORT_DIPNAME( 0x01, 0x00, "Results Printer" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x01, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x02, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Coinage") );   // same for both slots
		PORT_DIPSETTING(    0x00, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x10, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x20, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x30, DEF_STR( "1C_1C") );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	INPUT_PORTS_END(); }}; 
	
	
	/***********************************************************
	                        GFX DECODING
	***********************************************************/
	
	static GfxLayout finalb_tilelayout = new GfxLayout
	(
		16,16,	/* 16*16 sprites */
		RGN_FRAC(1,2),
		6,	/* 6 bits per pixel */
		new int[] { RGN_FRAC(1,2)+0, RGN_FRAC(1,2)+1, 0, 1, 2, 3 },
		new int[] { 3*4, 2*4, 1*4, 0*4, 7*4, 6*4, 5*4, 4*4,
				11*4, 10*4, 9*4, 8*4, 15*4, 14*4, 13*4, 12*4 },
		new int[] { 0*64, 1*64, 2*64, 3*64, 4*64, 5*64, 6*64, 7*64,
				8*64, 9*64, 10*64, 11*64, 12*64, 13*64, 14*64, 15*64 },
		128*8	/* every sprite takes 128 consecutive bytes */
	);
	
	static GfxLayout tilelayout = new GfxLayout
	(
		16,16,	/* 16*16 sprites */
		RGN_FRAC(1,1),
		4,	/* 4 bits per pixel */
		new int[] { 0, 1, 2, 3 },
		new int[] { 1*4, 0*4, 3*4, 2*4, 5*4, 4*4, 7*4, 6*4, 9*4, 8*4, 11*4, 10*4, 13*4, 12*4, 15*4, 14*4 },
		new int[] { 0*64, 1*64, 2*64, 3*64, 4*64, 5*64, 6*64, 7*64, 8*64, 9*64, 10*64, 11*64, 12*64, 13*64, 14*64, 15*64 },
		128*8	/* every sprite takes 128 consecutive bytes */
	);
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,	/* 8*8 characters */
		RGN_FRAC(1,1),
		4,	/* 4 bits per pixel */
		new int[] { 0, 1, 2, 3 },
		new int[] { 2*4, 3*4, 0*4, 1*4, 6*4, 7*4, 4*4, 5*4 },
		new int[] { 0*32, 1*32, 2*32, 3*32, 4*32, 5*32, 6*32, 7*32 },
		32*8	/* every sprite takes 32 consecutive bytes */
	);
	
	static GfxLayout yuyugogo_charlayout = new GfxLayout
	(
		8,8,	/* 8*8 characters */
		RGN_FRAC(1,1),
		1,	/* 1 bit per pixel */
		new int[] { 0 },
		new int[] { 0, 1, 2, 3, 4, 5, 6, 7 },
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
		8*8	/* every sprite takes 8 consecutive bytes */
	);
	
	static GfxLayout pivotlayout = new GfxLayout
	(
		8,8,	/* 8*8 characters */
		RGN_FRAC(1,1),
		4,	/* 4 bits per pixel */
		new int[] { 0, 1, 2, 3 },
		new int[] { 0*4, 1*4, 2*4, 3*4, 4*4, 5*4, 6*4, 7*4 },
		new int[] { 0*32, 1*32, 2*32, 3*32, 4*32, 5*32, 6*32, 7*32 },
		32*8	/* every sprite takes 32 consecutive bytes */
	);
	
	static GfxDecodeInfo finalb_gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX2, 0, finalb_tilelayout,  0, 64 ),	/* sprites  playfield, 6-bit deep */
		new GfxDecodeInfo( REGION_GFX1, 0, charlayout,  0, 256 ),	/* sprites  playfield */
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	static GfxDecodeInfo taitof2_gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX2, 0, tilelayout,  0, 256 ),	/* sprites  playfield */
		new GfxDecodeInfo( REGION_GFX1, 0, charlayout,  0, 256 ),	/* sprites  playfield */
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	static GfxDecodeInfo pivot_gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX2, 0, tilelayout,  0, 256 ),	/* sprites  playfield */
		new GfxDecodeInfo( REGION_GFX1, 0, charlayout,  0, 256 ),	/* sprites  playfield */
		new GfxDecodeInfo( REGION_GFX3, 0, pivotlayout, 0, 256 ),	/* sprites  playfield */
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	static GfxDecodeInfo yuyugogo_gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX2, 0, tilelayout,  0, 256 ),	/* sprites  playfield */
		new GfxDecodeInfo( REGION_GFX1, 0, yuyugogo_charlayout,  0, 256 ),	/* sprites  playfield */
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	static GfxDecodeInfo thundfox_gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX2, 0, tilelayout,  0, 256 ),	/* sprites  playfield */
		new GfxDecodeInfo( REGION_GFX1, 0, charlayout,  0, 256 ),	/* TC0100SCN #1 */
		new GfxDecodeInfo( REGION_GFX3, 0, charlayout,  0, 256 ),	/* TC0100SCN #2 */
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	static GfxLayout deadconx_charlayout = new GfxLayout
	(
		16,16,    /* 16*16 characters */
		RGN_FRAC(1,1),
		4,        /* 4 bits per pixel */
		new int[] { 0, 1, 2, 3 },
		new int[] { 1*4, 0*4, 5*4, 4*4, 3*4, 2*4, 7*4, 6*4, 9*4, 8*4, 13*4, 12*4, 11*4, 10*4, 15*4, 14*4 },
		new int[] { 0*64, 1*64, 2*64, 3*64, 4*64, 5*64, 6*64, 7*64, 8*64, 9*64, 10*64, 11*64, 12*64, 13*64, 14*64, 15*64 },
		128*8     /* every sprite takes 128 consecutive bytes */
	);
	
	static GfxDecodeInfo deadconx_gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX2, 0, tilelayout,  0, 256 ),	/* sprites  playfield */
		new GfxDecodeInfo( REGION_GFX1, 0, deadconx_charlayout,  0, 256 ),	/* sprites  playfield */
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	/* handler called by the YM2610 emulator when the internal timers cause an IRQ */
	static void irq_handler(int irq)
	{
		cpu_set_irq_line(1,0,irq ? ASSERT_LINE : CLEAR_LINE);
	}
	
	static struct YM2610interface ym2610_interface =
	{
		1,	/* 1 chip */
		16000000/2,	/* 8 MHz */
		{ 25 },
		{ 0 },
		{ 0 },
		{ 0 },
		{ 0 },
		{ irq_handler },
		{ REGION_SOUND2 },	/* Delta-T */
		{ REGION_SOUND1 },	/* ADPCM */
		{ YM3012_VOL(100,MIXER_PAN_LEFT,100,MIXER_PAN_RIGHT) }
	};
	
	
	public static WriteHandlerPtr camltrua_porta_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		// Implement //
	} };
	
	static struct YM2203interface ym2203_interface =
	{
		1,		/* 1 chip ??? */
		3000000,	/* 3 MHz ??? (tempo much too fast @4) */
		{ YM2203_VOL(60,20) },
		{ 0 },	/* portA read */
		{ 0 },
		{ camltrua_porta_w },	/* portA write - not implemented */
		{ 0 },	/* portB write */
		{ irq_handler }
	};
	
	
	static struct OKIM6295interface okim6295_interface =
	{
		1,
		{ 8000 },			/* Hz ?? */
		{ REGION_SOUND1 },	/* memory region */
		{ 100 }			/* volume ?? */
	};
	
	
	/***********************************************************
	                      MACHINE DRIVERS
	***********************************************************/
	
	static void init_machine_qcrayon(void)
	{
		/* point to the extra ROM */
		cpu_setbank(1,memory_region(REGION_USER1));
	}
	
	#define init_machine_0 0
	
	#define MACHINE_DRIVER(NAME,INIT,MAXCOLS,GFX,VHSTART,VHREFRESH,EOF)							\
	static MachineDriver machine_driver_##NAME = new MachineDriver\
	(																					\
		/* basic machine hardware */													\
		new MachineCPU[] {																				\
			new MachineCPU(																			\
				CPU_M68000,																\
				24000000/2,	/* 12 MHz */												\
				NAME##_readmem, NAME##_writemem,null,null,									\
				taitof2_interrupt,1														\
			),																			\
			new MachineCPU(																			\
				CPU_Z80 | CPU_AUDIO_CPU,												\
				16000000/4,	/* 4 MHz */													\
				sound_readmem, sound_writemem,null,null,										\
				ignore_interrupt,0	/* IRQs are triggered by the YM2610 */				\
			)																			\
		},																				\
		60, DEFAULT_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */	\
		1,																				\
		init_machine_##INIT,															\
																						\
		/* video hardware */															\
		40*8, 32*8, new rectangle( 0*8, 40*8-1, 2*8, 30*8-1 ),										\
																						\
		GFX##_gfxdecodeinfo,															\
		MAXCOLS, 0,																		\
		0,																				\
																						\
		VIDEO_TYPE_RASTER,										\
		taitof2_##EOF##_eof_callback,													\
		taitof2_##VHSTART##_vh_start,													\
		taitof2_vh_stop,																\
		VHREFRESH##_vh_screenrefresh,													\
																						\
		/* sound hardware */															\
		SOUND_SUPPORTS_STEREO,0,0,0,													\
		new MachineSound[] {																				\
			new MachineSound(																			\
				SOUND_YM2610,															\
				ym2610_interface														\
			)																			\
		}																				\
	);
	
	#define hthero_readmem		footchmp_readmem
	#define hthero_writemem		footchmp_writemem
	#define deadconj_readmem	deadconx_readmem
	#define deadconj_writemem	deadconx_writemem
	
	/*              NAME      INIT     MAXCOLS	GFX       VHSTART   VHREFRESH        EOF*/
	MACHINE_DRIVER( finalb,   0,       4096,	finalb,   finalb,   taitof2,         partial_buffer_delayed )
	MACHINE_DRIVER( dondokod, 0,       4096,	pivot,    dondokod, taitof2_pri_roz, partial_buffer_delayed )
	MACHINE_DRIVER( megab,    0,       4096,	taitof2,  megab,    taitof2_pri,     no_buffer )
	MACHINE_DRIVER( thundfox, 0,       4096,	thundfox, thundfox, thundfox,        partial_buffer_delayed_thundfox )
	MACHINE_DRIVER( cameltry, 0,       4096,	pivot,    dondokod, taitof2_pri_roz, no_buffer )
	MACHINE_DRIVER( qtorimon, 0,       4096,	yuyugogo, default,  taitof2,         partial_buffer_delayed )
	MACHINE_DRIVER( liquidk,  0,       4096,	taitof2,  megab,    taitof2_pri,     partial_buffer_delayed )
	MACHINE_DRIVER( quizhq,   0,       4096,	yuyugogo, default,  taitof2,         partial_buffer_delayed )
	MACHINE_DRIVER( ssi,      0,       4096,	taitof2,  ssi,      ssi,             partial_buffer_delayed )
	MACHINE_DRIVER( gunfront, 0,       4096,	taitof2,  gunfront, taitof2_pri,     no_buffer )
	MACHINE_DRIVER( growl,    0,       4096,	taitof2,  growl,    taitof2_pri,     no_buffer )
	MACHINE_DRIVER( mjnquest, 0,       4096,	taitof2,  mjnquest, taitof2,         no_buffer )
	MACHINE_DRIVER( footchmp, 0,       4096,	deadconx, footchmp, deadconx,        full_buffer_delayed )
	MACHINE_DRIVER( hthero,   0,       4096,	deadconx, hthero,   deadconx,        full_buffer_delayed )
	MACHINE_DRIVER( koshien,  0,       4096,	taitof2,  koshien,  taitof2_pri,     no_buffer )
	MACHINE_DRIVER( yuyugogo, qcrayon, 4096,	yuyugogo, yuyugogo, yesnoj,          no_buffer )
	MACHINE_DRIVER( ninjak,   0,       4096,	taitof2,  ninjak,   taitof2_pri,     no_buffer )
	MACHINE_DRIVER( solfigtr, 0,       4096,	taitof2,  solfigtr, taitof2_pri,     no_buffer )
	MACHINE_DRIVER( qzquest,  0,       4096,	taitof2,  default,  taitof2,         partial_buffer_delayed )
	MACHINE_DRIVER( pulirula, 0,       4096,	pivot,    pulirula, taitof2_pri_roz, no_buffer )
	MACHINE_DRIVER( metalb,   0,       8192,	deadconx, metalb,   metalb,          no_buffer )
	MACHINE_DRIVER( qzchikyu, 0,       4096,	taitof2,  qzchikyu, taitof2,         partial_buffer_delayed_qzchikyu )
	MACHINE_DRIVER( yesnoj,   0,       4096,	yuyugogo, yesnoj,   yesnoj,          no_buffer )
	MACHINE_DRIVER( deadconx, 0,       4096,	deadconx, deadconx, deadconx,        no_buffer )
	MACHINE_DRIVER( deadconj, 0,       4096,	deadconx, deadconj, deadconx,        no_buffer )
	MACHINE_DRIVER( dinorex,  0,       4096,	taitof2,  dinorex,  taitof2_pri,     no_buffer )
	MACHINE_DRIVER( qjinsei,  0,       4096,	taitof2,  quiz,     taitof2_pri,     no_buffer )
	MACHINE_DRIVER( qcrayon,  qcrayon, 4096,	taitof2,  quiz,     taitof2_pri,     no_buffer )
	MACHINE_DRIVER( qcrayon2, qcrayon, 4096,	taitof2,  quiz,     taitof2_pri,     no_buffer )
	MACHINE_DRIVER( driftout, 0,       4096,	pivot,    driftout, taitof2_pri_roz, no_buffer )
	
	
	static MachineDriver machine_driver_camltrua = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_M68000,
				24000000/2,	/* 12 MHz */
				cameltry_readmem, cameltry_writemem,null,null,
				taitof2_interrupt,1
			),
			new MachineCPU(
				CPU_Z80 | CPU_AUDIO_CPU,
				16000000/4,	/* 4 MHz ??? */
				camltrua_sound_readmem, camltrua_sound_writemem,null,null,
				ignore_interrupt,1	/* IRQs are triggered by the main CPU */
			)
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		1,
		null,
	
		/* video hardware */
		40*8, 32*8, new rectangle( 0*8, 40*8-1, 2*8, 30*8-1 ),
	
		pivot_gfxdecodeinfo,
		4096, 0,
		0,
	
		VIDEO_TYPE_RASTER,
		taitof2_no_buffer_eof_callback,
		taitof2_dondokod_vh_start,
		taitof2_vh_stop,
		taitof2_pri_roz_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_YM2203,
				ym2203_interface
			)
		}
	);
	
	static MachineDriver machine_driver_driveout = new MachineDriver\
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_M68000,
				24000000/2,	/* 12 MHz */
				driveout_readmem, driveout_writemem,null,null,
				taitof2_interrupt,1
			),
			new MachineCPU(
				CPU_Z80 | CPU_AUDIO_CPU,
				16000000/4,	/* 4 MHz */
				driveout_sound_readmem, driveout_sound_writemem,null,null,
				ignore_interrupt,0	/* IRQs are triggered by the main CPU */
			)
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		1,
		null,
	
		/* video hardware */
		40*8, 32*8, new rectangle( 0*8, 40*8-1, 2*8, 30*8-1 ),
	
		pivot_gfxdecodeinfo,
		4096, 0,
		0,
	
		VIDEO_TYPE_RASTER,
		taitof2_no_buffer_eof_callback,
		taitof2_driftout_vh_start,
		taitof2_vh_stop,
		taitof2_pri_roz_vh_screenrefresh,
	
		/* sound hardware */
		SOUND_SUPPORTS_STEREO   /* does it ? */,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_OKIM6295,
				okim6295_interface
			)
		}
	);
	
	
	/***************************************************************************
	                                  DRIVERS
	***************************************************************************/
	
	static RomLoadPtr rom_finalb = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x40000, REGION_CPU1, 0 );    /* 256k for 68000 code */
		ROM_LOAD16_BYTE( "b82_09.10",  0x00000, 0x20000, 0x632f1ecd );
		ROM_LOAD16_BYTE( "b82_17.11",  0x00001, 0x20000, 0xe91b2ec9 );
	
		ROM_REGION( 0x040000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD16_BYTE( "b82-06.19",  0x00000, 0x20000, 0xfc450a25 );
		ROM_LOAD16_BYTE( "b82-07.18",  0x00001, 0x20000, 0xec3df577 );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD16_BYTE( "b82-04.4",   0x000000, 0x80000, 0x6346f98e );/* sprites 4-bit format*/
		ROM_LOAD16_BYTE( "b82-03.5",   0x000001, 0x80000, 0xdaa11561 );/* sprites 4-bit format*/
	
		/* Note: this is intentional to load at 0x180000, not at 0x100000
		   because finalb_driver_init will move some bits around before data
		   will be 'gfxdecoded'. The whole thing is because this data is 2bits-
		   while above is 4bits-packed format, for a total of 6 bits per pixel. */
	
		ROM_LOAD( "b82-05.3",    0x180000, 0x80000, 0xaa90b93a );/* sprites 2-bit format */
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "b82_10.16",   0x00000, 0x04000, 0xa38aaaed );
		ROM_CONTINUE(            0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x80000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "b82-02.1",    0x00000, 0x80000, 0x5dd06bdd );
	
		ROM_REGION( 0x80000, REGION_SOUND2, 0 );/* Delta-T samples */
		ROM_LOAD( "b82-01.2",    0x00000, 0x80000, 0xf0eb6846 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_finalbj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x40000, REGION_CPU1, 0 );    /* 256k for 68000 code */
		ROM_LOAD16_BYTE( "b82_09.10",  0x00000, 0x20000, 0x632f1ecd );
		ROM_LOAD16_BYTE( "b82_08.11",  0x00001, 0x20000, 0x07154fe5 );
	
		ROM_REGION( 0x040000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD16_BYTE( "b82-06.19",  0x00000, 0x20000, 0xfc450a25 );
		ROM_LOAD16_BYTE( "b82-07.18",  0x00001, 0x20000, 0xec3df577 );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD16_BYTE( "b82-04.4",   0x000000, 0x80000, 0x6346f98e );/* sprites 4-bit format*/
		ROM_LOAD16_BYTE( "b82-03.5",   0x000001, 0x80000, 0xdaa11561 );/* sprites 4-bit format*/
	
		/* Note: this is intentional to load at 0x180000, not at 0x100000
		   because finalb_driver_init will move some bits around before data
		   will be 'gfxdecoded'. The whole thing is because this data is 2bits-
		   while above is 4bits-packed format, for a total of 6 bits per pixel. */
	
		ROM_LOAD( "b82-05.3",    0x180000, 0x80000, 0xaa90b93a );/* sprites 2-bit format */
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "b82_10.16",   0x00000, 0x04000, 0xa38aaaed );
		ROM_CONTINUE(            0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x80000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "b82-02.1",    0x00000, 0x80000, 0x5dd06bdd );
	
		ROM_REGION( 0x80000, REGION_SOUND2, 0 );/* Delta-T samples */
		ROM_LOAD( "b82-01.2",    0x00000, 0x80000, 0xf0eb6846 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_dondokod = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x80000, REGION_CPU1, 0 );    /* 512k for 68000 code */
		ROM_LOAD16_BYTE( "b95-12.bin",   0x00000, 0x20000, 0xd0fce87a );
		ROM_LOAD16_BYTE( "b95-11-1.bin", 0x00001, 0x20000, 0xdad40cd3 );
		ROM_LOAD16_BYTE( "b95-10.bin",   0x40000, 0x20000, 0xa46e1f0b );
		ROM_LOAD16_BYTE( "b95-wrld.7",   0x40001, 0x20000, 0x6e4e1351 );// needs correct name
	
		ROM_REGION( 0x080000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "b95-02.bin", 0x00000, 0x80000, 0x67b4e979 );
	
		ROM_REGION( 0x080000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "b95-01.bin", 0x00000, 0x80000, 0x51c176ce );
	
		ROM_REGION( 0x080000, REGION_GFX3, ROMREGION_DISPOSE );/* pivot gfx */
		ROM_LOAD( "b95-03.bin", 0x00000, 0x80000, 0x543aa0d1 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "b95-08.bin",  0x00000, 0x04000, 0xb5aa49e1 );
		ROM_CONTINUE(            0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x80000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "b95-04.bin",  0x00000, 0x80000, 0xac4c1716 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_dondokdu = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x80000, REGION_CPU1, 0 );    /* 512k for 68000 code */
		ROM_LOAD16_BYTE( "b95-12.bin",   0x00000, 0x20000, 0xd0fce87a );
		ROM_LOAD16_BYTE( "b95-11-1.bin", 0x00001, 0x20000, 0xdad40cd3 );
		ROM_LOAD16_BYTE( "b95-10.bin",   0x40000, 0x20000, 0xa46e1f0b );
		ROM_LOAD16_BYTE( "b95-us.7",     0x40001, 0x20000, 0x350d2c65 );// needs correct name
	
		ROM_REGION( 0x080000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "b95-02.bin", 0x00000, 0x80000, 0x67b4e979 );
	
		ROM_REGION( 0x080000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "b95-01.bin", 0x00000, 0x80000, 0x51c176ce );
	
		ROM_REGION( 0x080000, REGION_GFX3, ROMREGION_DISPOSE );/* pivot gfx */
		ROM_LOAD( "b95-03.bin", 0x00000, 0x80000, 0x543aa0d1 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "b95-08.bin",  0x00000, 0x04000, 0xb5aa49e1 );
		ROM_CONTINUE(            0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x80000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "b95-04.bin",  0x00000, 0x80000, 0xac4c1716 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_dondokdj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x80000, REGION_CPU1, 0 );    /* 512k for 68000 code */
		ROM_LOAD16_BYTE( "b95-12.bin",   0x00000, 0x20000, 0xd0fce87a );
		ROM_LOAD16_BYTE( "b95-11-1.bin", 0x00001, 0x20000, 0xdad40cd3 );
		ROM_LOAD16_BYTE( "b95-10.bin",   0x40000, 0x20000, 0xa46e1f0b );
		ROM_LOAD16_BYTE( "b95-09.bin",   0x40001, 0x20000, 0xd8c86d39 );
	
		ROM_REGION( 0x080000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "b95-02.bin", 0x00000, 0x80000, 0x67b4e979 );
	
		ROM_REGION( 0x080000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "b95-01.bin", 0x00000, 0x80000, 0x51c176ce );
	
		ROM_REGION( 0x080000, REGION_GFX3, ROMREGION_DISPOSE );/* pivot gfx */
		ROM_LOAD( "b95-03.bin", 0x00000, 0x80000, 0x543aa0d1 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "b95-08.bin",  0x00000, 0x04000, 0xb5aa49e1 );
		ROM_CONTINUE(            0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x80000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "b95-04.bin",  0x00000, 0x80000, 0xac4c1716 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_megab = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x80000, REGION_CPU1, 0 );    /* 256k for 68000 code */
		ROM_LOAD16_BYTE( "c11-07.55",  0x00000, 0x20000, 0x11d228b6 );
		ROM_LOAD16_BYTE( "c11-08.39",  0x00001, 0x20000, 0xa79d4dca );
		ROM_LOAD16_BYTE( "c11-06.54",  0x40000, 0x20000, 0x7c249894 );
		ROM_LOAD16_BYTE( "c11-11.38",  0x40001, 0x20000, 0x263ecbf9 );
	
		ROM_REGION( 0x080000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "c11-05.58", 0x00000, 0x80000, 0x733e6d8e );
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD16_BYTE( "c11-03.32", 0x00000, 0x80000, 0x46718c7a );
		ROM_LOAD16_BYTE( "c11-04.31", 0x00001, 0x80000, 0x663f33cc );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );/* sound cpu */
		ROM_LOAD( "c11-12.3", 0x00000, 0x04000, 0xb11094f1 );
		ROM_CONTINUE(       0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x80000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c11-01.29", 0x00000, 0x80000, 0xfd1ea532 );
	
		ROM_REGION( 0x80000, REGION_SOUND2, 0 );/* Delta-T samples */
		ROM_LOAD( "c11-02.30", 0x00000, 0x80000, 0x451cc187 );
	
	//Pals  b89-01.8  b89-02.28  b89-04.27  c11-13.13  c11-14.23
	ROM_END(); }}; 
	
	static RomLoadPtr rom_megabj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x80000, REGION_CPU1, 0 );    /* 256k for 68000 code */
		ROM_LOAD16_BYTE( "c11-07.55",  0x00000, 0x20000, 0x11d228b6 );// c11-07.17
		ROM_LOAD16_BYTE( "c11-08.39",  0x00001, 0x20000, 0xa79d4dca );// c11-08.19
		ROM_LOAD16_BYTE( "c11-06.54",  0x40000, 0x20000, 0x7c249894 );// c11-06.16
		ROM_LOAD16_BYTE( "c11-09.18",  0x40001, 0x20000, 0xc830aad5 );// c11-09.18
	
		ROM_REGION( 0x080000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "c11-05.58", 0x00000, 0x80000, 0x733e6d8e );
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD16_BYTE( "c11-03.32", 0x00000, 0x80000, 0x46718c7a );
		ROM_LOAD16_BYTE( "c11-04.31", 0x00001, 0x80000, 0x663f33cc );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );/* sound cpu */
		ROM_LOAD( "c11-12.3", 0x00000, 0x04000, 0xb11094f1 );
		ROM_CONTINUE(       0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x80000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c11-01.29", 0x00000, 0x80000, 0xfd1ea532 );
	
		ROM_REGION( 0x80000, REGION_SOUND2, 0 );/* Delta-T samples */
		ROM_LOAD( "c11-02.30", 0x00000, 0x80000, 0x451cc187 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_thundfox = new RomLoadPtr(){ public void handler(){ 		/* Thunder Fox */
		ROM_REGION( 0x80000, REGION_CPU1, 0 );    /* 512k for 68000 code */
		ROM_LOAD16_BYTE( "c28-13-1.51",  0x00000, 0x20000, 0xacb07013 );
		ROM_LOAD16_BYTE( "c28-16-1.40",  0x00001, 0x20000, 0x1e43d55b );
		ROM_LOAD16_BYTE( "c28-08.50",    0x40000, 0x20000, 0x38e038f1 );
		ROM_LOAD16_BYTE( "c28-07.39",    0x40001, 0x20000, 0x24419abb );
	
		ROM_REGION( 0x080000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "c28-02.61", 0x000000, 0x80000, 0x6230a09d );/* TC0100SCN #1 */
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD16_BYTE( "c28-03.29", 0x00000, 0x80000, 0x51bdc7af );
		ROM_LOAD16_BYTE( "c28-04.28", 0x00001, 0x80000, 0xba7ed535 );
	
		ROM_REGION( 0x080000, REGION_GFX3, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "c28-01.63", 0x000000, 0x80000, 0x44552b25 );/* TC0100SCN #2 */
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "c28-14.3",  0x00000, 0x04000, 0x45ef3616 );
		ROM_CONTINUE(          0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x80000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c28-06.41", 0x00000, 0x80000, 0xdb6983db );
	
		ROM_REGION( 0x80000, REGION_SOUND2, 0 );/* Delta-T samples */
		ROM_LOAD( "c28-05.42", 0x00000, 0x80000, 0xd3b238fa );
	
	// Pals: c28-09.25  c28-10.26  c28-11.35  b89-01.19  b89-03.37  b89-04.33
	ROM_END(); }}; 
	
	static RomLoadPtr rom_thndfoxu = new RomLoadPtr(){ public void handler(){ 		/* Thunder Fox */
		ROM_REGION( 0x80000, REGION_CPU1, 0 );    /* 512k for 68000 code */
		ROM_LOAD16_BYTE( "c28-13-1.51",  0x00000, 0x20000, 0xacb07013 );
		ROM_LOAD16_BYTE( "c28-15-1.40",  0x00001, 0x20000, 0x874a84e1 );
		ROM_LOAD16_BYTE( "c28-08.50",    0x40000, 0x20000, 0x38e038f1 );
		ROM_LOAD16_BYTE( "c28-07.39",    0x40001, 0x20000, 0x24419abb );
	
		ROM_REGION( 0x080000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "c28-02.61", 0x000000, 0x80000, 0x6230a09d );/* TC0100SCN #1 */
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD16_BYTE( "c28-03.29", 0x00000, 0x80000, 0x51bdc7af );
		ROM_LOAD16_BYTE( "c28-04.28", 0x00001, 0x80000, 0xba7ed535 );
	
		ROM_REGION( 0x080000, REGION_GFX3, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "c28-01.63", 0x000000, 0x80000, 0x44552b25 );/* TC0100SCN #2 */
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "c28-14.3",  0x00000, 0x04000, 0x45ef3616 );
		ROM_CONTINUE(          0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x80000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c28-06.41", 0x00000, 0x80000, 0xdb6983db );
	
		ROM_REGION( 0x80000, REGION_SOUND2, 0 );/* Delta-T samples */
		ROM_LOAD( "c28-05.42", 0x00000, 0x80000, 0xd3b238fa );
	
	// Pals: c28-09.25  c28-10.26  c28-11.35  b89-01.19  b89-03.37  b89-04.33
	ROM_END(); }}; 
	
	static RomLoadPtr rom_thndfoxj = new RomLoadPtr(){ public void handler(){ 		/* Thunder Fox */
		ROM_REGION( 0x80000, REGION_CPU1, 0 );    /* 512k for 68000 code */
		ROM_LOAD16_BYTE( "c28-13-1.51",  0x00000, 0x20000, 0xacb07013 );
		ROM_LOAD16_BYTE( "c28-12-1.40",  0x00001, 0x20000, 0xf04db477 );
		ROM_LOAD16_BYTE( "c28-08.50",    0x40000, 0x20000, 0x38e038f1 );
		ROM_LOAD16_BYTE( "c28-07.39",    0x40001, 0x20000, 0x24419abb );
	
		ROM_REGION( 0x080000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "c28-02.61", 0x000000, 0x80000, 0x6230a09d );/* TC0100SCN #1 */
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD16_BYTE( "c28-03.29", 0x00000, 0x80000, 0x51bdc7af );
		ROM_LOAD16_BYTE( "c28-04.28", 0x00001, 0x80000, 0xba7ed535 );
	
		ROM_REGION( 0x080000, REGION_GFX3, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "c28-01.63", 0x000000, 0x80000, 0x44552b25 );/* TC0100SCN #2 */
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "c28-14.3",  0x00000, 0x04000, 0x45ef3616 );
		ROM_CONTINUE(          0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x80000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c28-06.41", 0x00000, 0x80000, 0xdb6983db );
	
		ROM_REGION( 0x80000, REGION_SOUND2, 0 );/* Delta-T samples */
		ROM_LOAD( "c28-05.42", 0x00000, 0x80000, 0xd3b238fa );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_cameltry = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x40000, REGION_CPU1, 0 );    /* 256k for 68000 code */
		ROM_LOAD16_BYTE( "c38-11", 0x00000, 0x20000, 0xbe172da0 );
		ROM_LOAD16_BYTE( "c38-14", 0x00001, 0x20000, 0xffa430de );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );
		/* empty! */
	
		ROM_REGION( 0x080000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "c38-01.bin", 0x00000, 0x80000, 0xc170ff36 );
	
		ROM_REGION( 0x080000, REGION_GFX3, ROMREGION_DISPOSE );/* pivot gfx */
		ROM_LOAD( "c38-02.bin", 0x00000, 0x20000, 0x1a11714b );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "c38-08.bin", 0x00000, 0x04000, 0x7ff78873 );
		ROM_CONTINUE(           0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c38-03.bin", 0x000000, 0x020000, 0x59fa59a7 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_camltrua = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x40000, REGION_CPU1, 0 );    /* 256k for 68000 code */
		ROM_LOAD16_BYTE( "c38-11", 0x00000, 0x20000, 0xbe172da0 );
		ROM_LOAD16_BYTE( "c38-14", 0x00001, 0x20000, 0xffa430de );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );
		/* empty! */
	
		ROM_REGION( 0x080000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "c38-01.bin", 0x00000, 0x80000, 0xc170ff36 );
	
		ROM_REGION( 0x080000, REGION_GFX3, ROMREGION_DISPOSE );/* pivot gfx */
		ROM_LOAD( "c38-02.bin", 0x00000, 0x20000, 0x1a11714b );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );     /* sound cpu (revised prog!?) */
		ROM_LOAD( "c38-us.15", 0x00000, 0x10000, 0x0e60faac );
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c38-03.bin", 0x000000, 0x020000, 0x59fa59a7 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_cameltrj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x40000, REGION_CPU1, 0 );    /* 256k for 68000 code */
		ROM_LOAD16_BYTE( "c38-09.bin", 0x00000, 0x20000, 0x2ae01120 );
		ROM_LOAD16_BYTE( "c38-10.bin", 0x00001, 0x20000, 0x48d8ff56 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );
		/* empty! */
	
		ROM_REGION( 0x080000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "c38-01.bin", 0x00000, 0x80000, 0xc170ff36 );
	
		ROM_REGION( 0x080000, REGION_GFX3, ROMREGION_DISPOSE );/* pivot gfx */
		ROM_LOAD( "c38-02.bin", 0x00000, 0x20000, 0x1a11714b );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "c38-08.bin", 0x00000, 0x04000, 0x7ff78873 );
		ROM_CONTINUE(           0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c38-03.bin", 0x000000, 0x020000, 0x59fa59a7 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_qtorimon = new RomLoadPtr(){ public void handler(){ 	/* Quiz Torimonochou */
		ROM_REGION( 0x80000, REGION_CPU1, 0 );    /* 256k for 68000 code */
		ROM_LOAD16_BYTE( "c41-04.bin",  0x00000, 0x20000, 0x0fbf5223 );
		ROM_LOAD16_BYTE( "c41-05.bin",  0x00001, 0x20000, 0x174bd5db );
		ROM_LOAD16_BYTE( "mask-51.bin", 0x40000, 0x20000, 0x12e14aca );/* char defs, read by cpu */
		ROM_LOAD16_BYTE( "mask-52.bin", 0x40001, 0X20000, 0xb3ef66f3 );/* char defs, read by cpu */
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );
		/* empty! */
	
		ROM_REGION( 0x040000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD16_BYTE( "c41-02.bin",  0x00000, 0x20000, 0x05dcd36d );
		ROM_LOAD16_BYTE( "c41-01.bin",  0x00001, 0x20000, 0x39ff043c );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "c41-06.bin",    0x00000, 0x04000, 0x753a98d8 );
		ROM_CONTINUE(          0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x080000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c41-03.bin",  0x000000, 0x020000, 0xb2c18e89 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_liquidk = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x80000, REGION_CPU1, 0 );    /* 512k for 68000 code */
		ROM_LOAD16_BYTE( "c49_09.12",  0x00000, 0x20000, 0x6ae09eb9 );
		ROM_LOAD16_BYTE( "c49_11.14",  0x00001, 0x20000, 0x42d2be6e );
		ROM_LOAD16_BYTE( "c49_10.13",  0x40000, 0x20000, 0x50bef2e0 );
		ROM_LOAD16_BYTE( "c49_12.15",  0x40001, 0x20000, 0xcb16bad5 );
	
		ROM_REGION( 0x080000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "lk_scr.bin",  0x00000, 0x80000, 0xc3364f9b );
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "lk_obj0.bin", 0x00000, 0x80000, 0x67cc3163 );
		ROM_LOAD( "lk_obj1.bin", 0x80000, 0x80000, 0xd2400710 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );	/* sound cpu */
		ROM_LOAD( "c49_08.9",    0x00000, 0x04000, 0x413c310c );
		ROM_CONTINUE(            0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x80000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "lk_snd.bin",  0x00000, 0x80000, 0x474d45a4 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_liquidku = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x80000, REGION_CPU1, 0 );    /* 512k for 68000 code */
		ROM_LOAD16_BYTE( "c49_09.12",  0x00000, 0x20000, 0x6ae09eb9 );
		ROM_LOAD16_BYTE( "c49_11.14",  0x00001, 0x20000, 0x42d2be6e );
		ROM_LOAD16_BYTE( "c49_10.13",  0x40000, 0x20000, 0x50bef2e0 );
		ROM_LOAD16_BYTE( "c49_14.15",  0x40001, 0x20000, 0xbc118a43 );
	
		ROM_REGION( 0x080000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "lk_scr.bin",  0x00000, 0x80000, 0xc3364f9b );
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "lk_obj0.bin", 0x00000, 0x80000, 0x67cc3163 );
		ROM_LOAD( "lk_obj1.bin", 0x80000, 0x80000, 0xd2400710 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );	/* sound cpu */
		ROM_LOAD( "c49_08.9",    0x00000, 0x04000, 0x413c310c );
		ROM_CONTINUE(            0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x80000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "lk_snd.bin",  0x00000, 0x80000, 0x474d45a4 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_mizubaku = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x80000, REGION_CPU1, 0 );    /* 512k for 68000 code */
		ROM_LOAD16_BYTE( "c49_09.12",  0x00000, 0x20000, 0x6ae09eb9 );
		ROM_LOAD16_BYTE( "c49_11.14",  0x00001, 0x20000, 0x42d2be6e );
		ROM_LOAD16_BYTE( "c49_10.13",  0x40000, 0x20000, 0x50bef2e0 );
		ROM_LOAD16_BYTE( "c49_13.15",  0x40001, 0x20000, 0x2518dbf9 );
	
		ROM_REGION( 0x080000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "lk_scr.bin",  0x00000, 0x80000, 0xc3364f9b );
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "lk_obj0.bin", 0x00000, 0x80000, 0x67cc3163 );
		ROM_LOAD( "lk_obj1.bin", 0x80000, 0x80000, 0xd2400710 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );	/* sound cpu */
		ROM_LOAD( "c49_08.9",    0x00000, 0x04000, 0x413c310c );
		ROM_CONTINUE(            0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x80000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "lk_snd.bin",  0x00000, 0x80000, 0x474d45a4 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_quizhq = new RomLoadPtr(){ public void handler(){ 	/* Quiz HQ */
		ROM_REGION( 0xc0000, REGION_CPU1, 0 );    /* 256k for 68000 code */
		ROM_LOAD16_BYTE( "c53-05.bin",  0x00000, 0x20000, 0xc798fc20 );
		ROM_LOAD16_BYTE( "c53-01.bin",  0x00001, 0x20000, 0xbf44c93e );
		ROM_LOAD16_BYTE( "c53-52.bin",  0x80000, 0x20000, 0x12e14aca );/* char defs, read by cpu */
		ROM_LOAD16_BYTE( "c53-51.bin",  0x80001, 0X20000, 0xb3ef66f3 );/* char defs, read by cpu */
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );
		/* empty */
	
		ROM_REGION( 0x080000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD16_BYTE( "c53-03.bin",  0x00000, 0x20000, 0x47596e70 );
		ROM_LOAD16_BYTE( "c53-07.bin",  0x00001, 0x20000, 0x4f9fa82f );
		ROM_LOAD16_BYTE( "c53-02.bin",  0x40000, 0x20000, 0xd704c6f4 );
		ROM_LOAD16_BYTE( "c53-06.bin",  0x40001, 0x20000, 0xf77f63fc );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 ); /* sound cpu */
		ROM_LOAD( "c53-08.bin",    0x00000, 0x04000, 0x25187e81 );
		ROM_CONTINUE(          0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x080000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c53-04.bin",  0x000000, 0x020000, 0x99890ad4 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ssi = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x80000, REGION_CPU1, 0 );    /* 512k for 68000 code */
		ROM_LOAD16_BYTE( "c64_15-1.bin", 0x00000, 0x40000, 0xce9308a6 );
		ROM_LOAD16_BYTE( "c64_16-1.bin", 0x00001, 0x40000, 0x470a483a );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );
		/* empty! */
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "c64-01.1",     0x000000, 0x100000, 0xa1b4f486 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );	/* sound cpu */
		ROM_LOAD( "c64_09.13",    0x00000, 0x04000, 0x88d7f65c );
		ROM_CONTINUE(             0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x20000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c64-02.2",     0x00000, 0x20000, 0x3cb0b907 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_majest12 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x80000, REGION_CPU1, 0 );    /* 512k for 68000 code */
		ROM_LOAD16_BYTE( "c64_07.10", 0x00000, 0x20000, 0xf29ed5c9 );
		ROM_LOAD16_BYTE( "c64_06.4",  0x40000, 0x20000, 0x18dc71ac );
		ROM_LOAD16_BYTE( "c64_08.11", 0x00001, 0x20000, 0xddfd33d5 );
		ROM_LOAD16_BYTE( "c64_05.5",  0x40001, 0x20000, 0xb61866c0 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );
		/* empty! */
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "c64-01.1",     0x000000, 0x100000, 0xa1b4f486 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );	/* sound cpu */
		ROM_LOAD( "c64_09.13",    0x00000, 0x04000, 0x88d7f65c );
		ROM_CONTINUE(             0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x20000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c64-02.2",     0x00000, 0x20000, 0x3cb0b907 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_gunfront = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0xc0000, REGION_CPU1, 0 );    /* 768k for 68000 code */
		ROM_LOAD16_BYTE( "c71-09.42",  0x00000, 0x20000, 0x10a544a2 );
		ROM_LOAD16_BYTE( "c71-08.41",  0x00001, 0x20000, 0xc17dc0a0 );
		ROM_LOAD16_BYTE( "c71-10.40",  0x40000, 0x20000, 0xf39c0a06 );
		ROM_LOAD16_BYTE( "c71-14.39",  0x40001, 0x20000, 0x312da036 );
		ROM_LOAD16_BYTE( "c71-16.38",  0x80000, 0x20000, 0x1bbcc2d4 );
		ROM_LOAD16_BYTE( "c71-15.37",  0x80001, 0x20000, 0xdf3e00bb );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "c71-02.59", 0x000000, 0x100000, 0x2a600c92 );
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "c71-03.19", 0x000000, 0x100000, 0x9133c605 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 ); /* sound cpu */
		ROM_LOAD( "c71-12.49", 0x00000, 0x04000, 0x0038c7f8 );
		ROM_CONTINUE(          0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c71-01.29", 0x000000, 0x100000, 0x0e73105a );
	
		/* no Delta-T samples */
	
	// Pals c71-16.28  c71-07.27
	ROM_END(); }}; 
	
	static RomLoadPtr rom_gunfronj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0xc0000, REGION_CPU1, 0 );    /* 768k for 68000 code */
		ROM_LOAD16_BYTE( "c71-09.42",  0x00000, 0x20000, 0x10a544a2 );// c71-09.9
		ROM_LOAD16_BYTE( "c71-08.41",  0x00001, 0x20000, 0xc17dc0a0 );// c71-08.2
		ROM_LOAD16_BYTE( "c71-10.40",  0x40000, 0x20000, 0xf39c0a06 );// c71-10.10
		ROM_LOAD16_BYTE( "c71-11.3",   0x40001, 0x20000, 0xdf23c11a );
		ROM_LOAD16_BYTE( "c71-16.38",  0x80000, 0x20000, 0x1bbcc2d4 );// c71-05.11
		ROM_LOAD16_BYTE( "c71-15.37",  0x80001, 0x20000, 0xdf3e00bb );// c71-04.4
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "c71-02.59", 0x000000, 0x100000, 0x2a600c92 );// c71-02.7
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "c71-03.19", 0x000000, 0x100000, 0x9133c605 );// c71-03.8
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 ); /* sound cpu */
		ROM_LOAD( "c71-12.49", 0x00000, 0x04000, 0x0038c7f8 );// c71-12.5
		ROM_CONTINUE(          0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c71-01.29", 0x000000, 0x100000, 0x0e73105a );// c71-01.1
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_growl = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x100000, REGION_CPU1, 0 );    /* 1024k for 68000 code */
		ROM_LOAD16_BYTE( "c74-10.59",  0x00000, 0x40000, 0xca81a20b );
		ROM_LOAD16_BYTE( "c74-08.61",  0x00001, 0x40000, 0xaa35dd9e );
		ROM_LOAD16_BYTE( "c74-11.58",  0x80000, 0x40000, 0xee3bd6d5 );
		ROM_LOAD16_BYTE( "c74-14.60",  0x80001, 0x40000, 0xb6c24ec7 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "c74-01.34",   0x000000, 0x100000, 0x3434ce80 );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "c74-03.12",   0x000000, 0x100000, 0x1a0d8951 );
		ROM_LOAD( "c74-02.11",   0x100000, 0x100000, 0x15a21506 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "c74-12.62",   0x00000, 0x04000, 0xbb6ed668 );
		ROM_CONTINUE(            0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c74-04.28",   0x000000, 0x100000, 0x2d97edf2 );
	
		ROM_REGION( 0x080000, REGION_SOUND2, 0 );/* Delta-T samples */
		ROM_LOAD( "c74-05.29",   0x000000, 0x080000, 0xe29c0828 );
	
	//Pals c74-06.48  c74-07.47
	ROM_END(); }}; 
	
	static RomLoadPtr rom_growlu = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x100000, REGION_CPU1, 0 );    /* 1024k for 68000 code */
		ROM_LOAD16_BYTE( "c74-10.59",  0x00000, 0x40000, 0xca81a20b );
		ROM_LOAD16_BYTE( "c74-08.61",  0x00001, 0x40000, 0xaa35dd9e );
		ROM_LOAD16_BYTE( "c74-11.58",  0x80000, 0x40000, 0xee3bd6d5 );
		ROM_LOAD16_BYTE( "c74-13.60",  0x80001, 0x40000, 0xc1c57e51 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "c74-01.34",   0x000000, 0x100000, 0x3434ce80 );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "c74-03.12",   0x000000, 0x100000, 0x1a0d8951 );
		ROM_LOAD( "c74-02.11",   0x100000, 0x100000, 0x15a21506 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "c74-12.62",   0x00000, 0x04000, 0xbb6ed668 );
		ROM_CONTINUE(            0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c74-04.28",   0x000000, 0x100000, 0x2d97edf2 );
	
		ROM_REGION( 0x080000, REGION_SOUND2, 0 );/* Delta-T samples */
		ROM_LOAD( "c74-05.29",   0x000000, 0x080000, 0xe29c0828 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_runark = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x100000, REGION_CPU1, 0 );    /* 1024k for 68000 code */
		ROM_LOAD16_BYTE( "c74-10.59",  0x00000, 0x40000, 0xca81a20b );// c74-10.15
		ROM_LOAD16_BYTE( "c74-08.61",  0x00001, 0x40000, 0xaa35dd9e );// c74-08.13
		ROM_LOAD16_BYTE( "c74-11.58",  0x80000, 0x40000, 0xee3bd6d5 );// c74-11.16
		ROM_LOAD16_BYTE( "c74-09.14",  0x80001, 0x40000, 0x58cc2feb );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "c74-01.34",   0x000000, 0x100000, 0x3434ce80 );// c74-01.1
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "c74-03.12",   0x000000, 0x100000, 0x1a0d8951 );// c74-03.9
		ROM_LOAD( "c74-02.11",   0x100000, 0x100000, 0x15a21506 );// c74-02.8
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );/* sound cpu */
		ROM_LOAD( "c74-12.62",   0x00000, 0x04000, 0xbb6ed668 );// c74-12.3
		ROM_CONTINUE(            0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c74-04.28",   0x000000, 0x100000, 0x2d97edf2 );// c74-04.10
	
		ROM_REGION( 0x080000, REGION_SOUND2, 0 );/* Delta-T samples */
		ROM_LOAD( "c74-05.29",   0x000000, 0x080000, 0xe29c0828 );// c74-05.2
	ROM_END(); }}; 
	
	static RomLoadPtr rom_mjnquest = new RomLoadPtr(){ public void handler(){ 	/* Mahjong Quest */
		ROM_REGION( 0x100000, REGION_CPU1, 0 );    /* 256k for 68000 code */
		ROM_LOAD16_BYTE( "c77-09",  0x000000, 0x020000, 0x0a005d01 );
		ROM_LOAD16_BYTE( "c77-08",  0x000001, 0x020000, 0x4244f775 );
		ROM_LOAD16_WORD_SWAP( "c77-04",  0x080000, 0x080000, 0xc2e7e038 )	/* data rom */
	
		ROM_REGION( 0x200000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "c77-01", 0x000000, 0x100000, 0x5ba51205 );
		ROM_LOAD( "c77-02", 0x100000, 0x100000, 0x6a6f3040 );
	
		ROM_REGION( 0x080000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "c77-05", 0x00000, 0x80000, 0xc5a54678 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );/* sound cpu */
		ROM_LOAD( "c77-10",    0x00000, 0x04000, 0xf16b2c1e );
		ROM_CONTINUE(          0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x080000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c77-03",  0x000000, 0x080000, 0x312f17b1 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_mjnquesb = new RomLoadPtr(){ public void handler(){ 	/* Mahjong Quest (No Nudity) */
		ROM_REGION( 0x100000, REGION_CPU1, 0 );    /* 256k for 68000 code */
		ROM_LOAD16_BYTE( "c77-09a", 0x000000, 0x020000, 0xfc17f1c2 );
		ROM_LOAD16_BYTE( "c77-08",  0x000001, 0x020000, 0x4244f775 );
		ROM_LOAD16_WORD_SWAP( "c77-04",  0x080000, 0x080000, 0xc2e7e038 )	/* data rom */
	
		ROM_REGION( 0x200000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "c77-01", 0x000000, 0x100000, 0x5ba51205 );
		ROM_LOAD( "c77-02", 0x100000, 0x100000, 0x6a6f3040 );
	
		ROM_REGION( 0x080000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "c77-05", 0x00000, 0x80000, 0xc5a54678 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );/* sound cpu */
		ROM_LOAD( "c77-10",    0x00000, 0x04000, 0xf16b2c1e );
		ROM_CONTINUE(          0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x080000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c77-03",  0x000000, 0x080000, 0x312f17b1 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_footchmp = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x80000, REGION_CPU1, 0 );    /* 512k for 68000 code */
		ROM_LOAD16_BYTE( "c80-11.6",     0x00000, 0x20000, 0xf78630fb );
		ROM_LOAD16_BYTE( "c80-10.4",     0x00001, 0x20000, 0x32c109cb );
		ROM_LOAD16_BYTE( "c80-12.7",     0x40000, 0x20000, 0x80d46fef );
		ROM_LOAD16_BYTE( "c80-14.5",     0x40001, 0x20000, 0x40ac4828 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD16_BYTE( "c80-04.1", 0x00000, 0x80000, 0x9a17fe8c );
		ROM_LOAD16_BYTE( "c80-05.2", 0x00001, 0x80000, 0xacde7071 );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "c80-01.9",  0x000000, 0x100000, 0xf43782e6 );
		ROM_LOAD( "c80-02.10", 0x100000, 0x100000, 0x060a8b61 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );/* 64k for Z80 code */
		ROM_LOAD( "c80-15.70", 0x00000, 0x04000, 0x05aa7fd7 );
		ROM_CONTINUE(          0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );    /* YM2610 samples */
		ROM_LOAD( "c80-03.57", 0x000000, 0x100000, 0x609938d5 );
	
		/* no Delta-T samples */
	
	// Pals c80-08.45  c80-09.46
	ROM_END(); }}; 
	
	static RomLoadPtr rom_hthero = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x80000, REGION_CPU1, 0 );    /* 512k for 68000 code */
		ROM_LOAD16_BYTE( "c80-16.6",  0x00000, 0x20000, 0x4e795b52 );
		ROM_LOAD16_BYTE( "c80-17.4",  0x00001, 0x20000, 0x42c0a838 );
		ROM_LOAD16_BYTE( "c80-12.7",  0x40000, 0x20000, 0x80d46fef );
		ROM_LOAD16_BYTE( "c80-18.5",  0x40001, 0x20000, 0xaea22904 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD16_BYTE( "c80-04.1", 0x00000, 0x80000, 0x9a17fe8c );
		ROM_LOAD16_BYTE( "c80-05.2", 0x00001, 0x80000, 0xacde7071 );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "c80-01.9",  0x000000, 0x100000, 0xf43782e6 );
		ROM_LOAD( "c80-02.10", 0x100000, 0x100000, 0x060a8b61 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );/* sound cpu */
		ROM_LOAD( "c80-15.70", 0x00000, 0x04000, 0x05aa7fd7 );
		ROM_CONTINUE(          0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c80-03.57", 0x000000, 0x100000, 0x609938d5 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_euroch92 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x80000, REGION_CPU1, 0 );    /* 512k for 68000 code */
		ROM_LOAD16_BYTE( "ec92_25.rom", 0x00000, 0x20000, 0x98482202 );
		ROM_LOAD16_BYTE( "ec92_23.rom", 0x00001, 0x20000, 0xae5e75e9 );
		ROM_LOAD16_BYTE( "ec92_26.rom", 0x40000, 0x20000, 0xb986ccb2 );
		ROM_LOAD16_BYTE( "ec92_24.rom", 0x40001, 0x20000, 0xb31d94ac );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD16_BYTE( "ec92_21.rom", 0x00000, 0x80000, 0x5759ed37 );
		ROM_LOAD16_BYTE( "ec92_22.rom", 0x00001, 0x80000, 0xd9a0d38e );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "ec92_19.rom", 0x000000, 0x100000, 0x219141a5 );
		ROM_LOAD( "c80-02.10",   0x100000, 0x100000, 0x060a8b61 );// ec92_20.rom
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );/* 64k for Z80 code */
		ROM_LOAD( "ec92_27.rom", 0x00000, 0x04000, 0x2db48e65 );
		ROM_CONTINUE(            0x10000, 0x0c000 );
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* YM2610 samples */
		ROM_LOAD( "c80-03.57", 0x000000, 0x100000, 0x609938d5 );// ec92_03.rom
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_koshien = new RomLoadPtr(){ public void handler(){ 	/* Ah Eikou no Koshien */
		ROM_REGION( 0x100000, REGION_CPU1, 0 );    /* 256k for 68000 code */
		ROM_LOAD16_BYTE( "c81-11.bin", 0x000000, 0x020000, 0xb44ea8c9 );
		ROM_LOAD16_BYTE( "c81-10.bin", 0x000001, 0x020000, 0x8f98c40a );
		ROM_LOAD16_WORD_SWAP( "c81-04.bin", 0x080000, 0x080000, 0x1592b460 )	/* data rom */
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "c81-03.bin", 0x000000, 0x100000, 0x29bbf492 );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "c81-01.bin", 0x000000, 0x100000, 0x64b15d2a );
		ROM_LOAD( "c81-02.bin", 0x100000, 0x100000, 0x962461e8 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "c81-12.bin", 0x00000, 0x04000, 0x6e8625b6 );
		ROM_CONTINUE(           0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x080000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c81-05.bin",  0x00000, 0x80000, 0x9c3d71be );
	
		ROM_REGION( 0x080000, REGION_SOUND2, 0 );/* Delta-T samples */
		ROM_LOAD( "c81-06.bin",  0x00000, 0x80000, 0x927833b4 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_yuyugogo = new RomLoadPtr(){ public void handler(){ 	/* Yuuyu no QUIZ de GO!GO! */
		ROM_REGION( 0x40000, REGION_CPU1, 0 );    /* 256k for 68000 code */
		ROM_LOAD16_BYTE( "c83_10.3",  0x00000,  0x20000, 0x4d185d03 );
		ROM_LOAD16_BYTE( "c83_09.2",  0x00001,  0x20000, 0xf9892792 );
	
		ROM_REGION16_BE( 0x100000, REGION_USER1, 0 );
		/* extra ROM mapped at d00000 */
		ROM_LOAD16_WORD_SWAP( "c83-03.10", 0x000000, 0x100000, 0xeed9acc2 )	/* data rom */
	
		ROM_REGION( 0x020000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "c83-05.21", 0x00000, 0x20000, 0xeca57fb1 );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD16_BYTE( "c83-01.12",  0x000000, 0x100000, 0x8bf0d416 );
		ROM_LOAD16_BYTE( "c83-02.11",  0x000001, 0x100000, 0x20bb1c15 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 ); /* sound cpu */
		ROM_LOAD( "c83_11.1"  , 0x00000, 0x04000, 0x461e702a );
		ROM_CONTINUE(           0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c83-04.5",  0x000000, 0x100000, 0x2600093a );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ninjak = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x80000, REGION_CPU1, 0 );    /* 512k for 68000 code */
		ROM_LOAD16_BYTE( "c85_10x.19",  0x00000, 0x20000, 0xba7e6e74 );
		ROM_LOAD16_BYTE( "c85_xx.5",    0x00001, 0x20000, 0x0ac2cba2 );
		ROM_LOAD16_BYTE( "c85_07.18",   0x40000, 0x20000, 0x3eccfd0a );
		ROM_LOAD16_BYTE( "c85_06.4",    0x40001, 0x20000, 0xd126ded1 );
	
		ROM_REGION( 0x080000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "c85-03.10",    0x00000, 0x80000, 0x4cc7b9df );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "c85-01.16",    0x000000, 0x100000, 0xa711977c );
		ROM_LOAD( "c85-02.15",    0x100000, 0x100000, 0xa6ad0f3d );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "c85-14.6",     0x00000, 0x04000, 0xf2a52a51 );
		ROM_CONTINUE(             0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x80000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c85-04.1",     0x00000, 0x80000, 0x5afb747e );
	
		ROM_REGION( 0x80000, REGION_SOUND2, 0 );/* Delta-T samples */
		ROM_LOAD( "c85-05.11",    0x00000, 0x80000, 0x3c1b0ed0 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ninjakj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x80000, REGION_CPU1, 0 );    /* 512k for 68000 code */
		ROM_LOAD16_BYTE( "c85_10x.19",  0x00000, 0x20000, 0xba7e6e74 );
		ROM_LOAD16_BYTE( "c85-11x.5",   0x00001, 0x20000, 0xe4ccaa8e );
		ROM_LOAD16_BYTE( "c85_07.18",   0x40000, 0x20000, 0x3eccfd0a );
		ROM_LOAD16_BYTE( "c85_06.4",    0x40001, 0x20000, 0xd126ded1 );
	
		ROM_REGION( 0x080000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "c85-03.10",    0x00000, 0x80000, 0x4cc7b9df );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "c85-01.16",    0x000000, 0x100000, 0xa711977c );
		ROM_LOAD( "c85-02.15",    0x100000, 0x100000, 0xa6ad0f3d );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "c85-14.6",     0x00000, 0x04000, 0xf2a52a51 );
		ROM_CONTINUE(             0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x80000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c85-04.1",     0x00000, 0x80000, 0x5afb747e );
	
		ROM_REGION( 0x80000, REGION_SOUND2, 0 );/* Delta-T samples */
		ROM_LOAD( "c85-05.11",    0x00000, 0x80000, 0x3c1b0ed0 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_solfigtr = new RomLoadPtr(){ public void handler(){ 	/* Solitary Fighter */
		ROM_REGION( 0x80000, REGION_CPU1, 0 );    /* 512k for 68000 code */
		ROM_LOAD16_BYTE( "c91-05.59",  0x00000, 0x40000, 0xc1260e7c );
		ROM_LOAD16_BYTE( "c91-09.61",  0x00001, 0x40000, 0xd82b5266 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "c91-03.34", 0x000000, 0x100000, 0x8965da12 );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "c91-01.12", 0x000000, 0x100000, 0x0f3f4e00 );
		ROM_LOAD( "c91-02.11", 0x100000, 0x100000, 0xe14ab98e );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "c91-07.62", 0x00000, 0x04000, 0xe471a05a );
		ROM_CONTINUE(          0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x80000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c91-04.28", 0x00000, 0x80000, 0x390b1065 );/* Channel A */
	
		/* no Delta-T samples */
	
	//Pals c74-06.48
	ROM_END(); }}; 
	
	static RomLoadPtr rom_qzquest = new RomLoadPtr(){ public void handler(){ 	/* Quiz Quest */
		ROM_REGION( 0x180000, REGION_CPU1, 0 );    /* 256k for 68000 code */
		ROM_LOAD16_BYTE( "c92_06.8", 0x000000, 0x020000, 0x424be722 );
		ROM_LOAD16_BYTE( "c92_05.7", 0x000001, 0x020000, 0xda470f93 );
		ROM_LOAD16_WORD_SWAP( "c92-03.6", 0x100000, 0x080000, 0x1d697606 )	/* data rom */
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "c92-02.10", 0x000000, 0x100000, 0x2daccecf );
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "c92-01.21", 0x000000, 0x100000, 0x9976a285 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "c92_07.5",  0x00000, 0x04000, 0x3e313db9 );
		ROM_CONTINUE(          0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x080000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c92-04.9",  0x000000, 0x080000, 0xe421bb43 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_pulirula = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0xc0000, REGION_CPU1, 0 );    /* 768k for 68000 code */
		ROM_LOAD16_BYTE( "c98-12.rom", 0x00000, 0x40000, 0x816d6cde );
		ROM_LOAD16_BYTE( "c98-16.rom", 0x00001, 0x40000, 0x59df5c77 );
		ROM_LOAD16_BYTE( "c98-06.rom", 0x80000, 0x20000, 0x64a71b45 );
		ROM_LOAD16_BYTE( "c98-07.rom", 0x80001, 0x20000, 0x90195bc0 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "c98-04.rom", 0x000000, 0x100000, 0x0e1fe3b2 );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "c98-02.rom", 0x000000, 0x100000, 0x4a2ad2b3 );
		ROM_LOAD( "c98-03.rom", 0x100000, 0x100000, 0x589a678f );
	
		ROM_REGION( 0x080000, REGION_GFX3, ROMREGION_DISPOSE );/* pivot gfx */
		ROM_LOAD( "c98-05.rom", 0x000000, 0x080000, 0x9ddd9c39 );
	
		ROM_REGION( 0x2c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "c98-14.rom", 0x00000, 0x04000, 0xa858e17c );
		ROM_CONTINUE(           0x10000, 0x1c000 );/* banked stuff */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c98-01.rom", 0x000000, 0x100000, 0x197f66f5 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_pulirulj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0xc0000, REGION_CPU1, 0 );    /* 768k for 68000 code */
		ROM_LOAD16_BYTE( "c98-12.rom", 0x00000, 0x40000, 0x816d6cde );
		ROM_LOAD16_BYTE( "c98-13",     0x00001, 0x40000, 0xb7d13d5b );
		ROM_LOAD16_BYTE( "c98-06.rom", 0x80000, 0x20000, 0x64a71b45 );
		ROM_LOAD16_BYTE( "c98-07.rom", 0x80001, 0x20000, 0x90195bc0 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "c98-04.rom", 0x000000, 0x100000, 0x0e1fe3b2 );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "c98-02.rom", 0x000000, 0x100000, 0x4a2ad2b3 );
		ROM_LOAD( "c98-03.rom", 0x100000, 0x100000, 0x589a678f );
	
		ROM_REGION( 0x080000, REGION_GFX3, ROMREGION_DISPOSE );/* pivot gfx */
		ROM_LOAD( "c98-05.rom", 0x000000, 0x080000, 0x9ddd9c39 );
	
		ROM_REGION( 0x2c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "c98-14.rom", 0x00000, 0x04000, 0xa858e17c );
		ROM_CONTINUE(           0x10000, 0x1c000 );/* banked stuff */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "c98-01.rom", 0x000000, 0x100000, 0x197f66f5 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_metalb = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0xc0000, REGION_CPU1, 0 );    /* 768k for 68000 code */
		ROM_LOAD16_BYTE( "d16-16.8",   0x00000, 0x40000, 0x3150be61 );
		ROM_LOAD16_BYTE( "d16-18.7",   0x00001, 0x40000, 0x5216d092 );
		ROM_LOAD16_BYTE( "d12-07.9",   0x80000, 0x20000, 0xe07f5136 );
		ROM_LOAD16_BYTE( "d12-06.6",   0x80001, 0x20000, 0x131df731 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD16_BYTE( "d12-03.14",  0x00000, 0x80000, 0x46b498c0 );
		ROM_LOAD16_BYTE( "d12-04.13",  0x00001, 0x80000, 0xab66d141 );
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "d12-01.20", 0x000000, 0x100000, 0xb81523b9 );
	
		ROM_REGION( 0x2c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "d12-13.5", 0x00000, 0x04000, 0xbcca2649 );
		ROM_CONTINUE(         0x10000, 0x1c000 );/* banked stuff */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "d12-02.10", 0x000000, 0x100000, 0x79263e74 );
	
		ROM_REGION( 0x080000, REGION_SOUND2, 0 );  /* Delta-T samples */
		ROM_LOAD( "d12-05.16", 0x000000, 0x080000, 0x7fd036c5 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_metalbj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0xc0000, REGION_CPU1, 0 );    /* 768k for 68000 code */
		ROM_LOAD16_BYTE( "d12-12.8",   0x00000, 0x40000, 0x556f82b2 );
		ROM_LOAD16_BYTE( "d12-11.7",   0x00001, 0x40000, 0xaf9ee28d );
		ROM_LOAD16_BYTE( "d12-07.9",   0x80000, 0x20000, 0xe07f5136 );
		ROM_LOAD16_BYTE( "d12-06.6",   0x80001, 0x20000, 0x131df731 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD16_BYTE( "d12-03.14",  0x00000, 0x80000, 0x46b498c0 );
		ROM_LOAD16_BYTE( "d12-04.13",  0x00001, 0x80000, 0xab66d141 );
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "d12-01.20", 0x000000, 0x100000, 0xb81523b9 );
	
		ROM_REGION( 0x2c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "d12-13.5", 0x00000, 0x04000, 0xbcca2649 );
		ROM_CONTINUE(         0x10000, 0x1c000 );/* banked stuff */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "d12-02.10", 0x000000, 0x100000, 0x79263e74 );
	
		ROM_REGION( 0x080000, REGION_SOUND2, 0 );  /* Delta-T samples */
		ROM_LOAD( "d12-05.16", 0x000000, 0x080000, 0x7fd036c5 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_qzchikyu = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x180000, REGION_CPU1, 0 );    /* 256k for 68000 code */
		ROM_LOAD16_BYTE( "d19_06.8",	0x000000, 0x020000, 0xde8c8e55 );
		ROM_LOAD16_BYTE( "d19_05.7",	0x000001, 0x020000, 0xc6d099d0 );
		ROM_LOAD16_WORD_SWAP( "d19-03.6",	0x100000, 0x080000, 0x5c1b92c0 )	/* data rom */
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "d19-02.10",	0x000000, 0x100000, 0xf2dce2f2 );
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "d19-01.21",	0x000000, 0x100000, 0x6c4342d0 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "d19_07.5",	0x00000, 0x04000, 0xa8935f84 );
		ROM_CONTINUE(			0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "d19-04.9",	0x000000, 0x080000, 0xd3c44905 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_yesnoj = new RomLoadPtr(){ public void handler(){ 	/* Yes/No Sinri Tokimeki Chart */
		ROM_REGION( 0x80000, REGION_CPU1, 0 );    /* 512k for 68000 code */
		ROM_LOAD16_BYTE( "d20-05-2.2",  0x00000, 0x40000, 0x68adb929 );
		ROM_LOAD16_BYTE( "d20-04-2.4",  0x00001, 0x40000, 0xa84762f8 );
	
		ROM_REGION( 0x080000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "d20-01.11", 0x00000, 0x80000, 0x9d8a4d57 );
	
		ROM_REGION( 0x100000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD16_BYTE( "d20-02.12",  0x00000, 0x80000, 0xe71a8e40 );
		ROM_LOAD16_BYTE( "d20-03.13",  0x00001, 0x80000, 0x6a51a1b4 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "d20-06.5",  0x00000, 0x04000, 0x3eb537dc );
		ROM_CONTINUE(          0x10000, 0x0c000 );/* banked stuff */
	
		/* no ADPCM samples */
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_deadconx = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x100000, REGION_CPU1, 0 );    /* 1024k for 68000 code */
		ROM_LOAD16_BYTE( "d28_06.3",  0x00000, 0x40000, 0x5b4bff51 );
		ROM_LOAD16_BYTE( "d28_12.5",  0x00001, 0x40000, 0x9b74e631 );
		ROM_LOAD16_BYTE( "d28_09.2",  0x80000, 0x40000, 0x143a0cc1 );
		ROM_LOAD16_BYTE( "d28_08.4",  0x80001, 0x40000, 0x4c872bd9 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD16_BYTE( "d28_04.16", 0x00000, 0x80000, 0xdcabc26b );
		ROM_LOAD16_BYTE( "d28_05.17", 0x00001, 0x80000, 0x862f9665 );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "d28_01.8", 0x000000, 0x100000, 0x181d7b69 );
		ROM_LOAD( "d28_02.9", 0x100000, 0x100000, 0xd301771c );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 ); /* sound cpu */
		ROM_LOAD( "d28_10.6", 0x00000, 0x04000, 0x40805d74 );
		ROM_CONTINUE(         0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "d28_03.10", 0x000000, 0x100000, 0xa1804b52 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_deadconj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x100000, REGION_CPU1, 0 );    /* 1024k for 68000 code */
		ROM_LOAD16_BYTE( "d28_06.3",  0x00000, 0x40000, 0x5b4bff51 );
		ROM_LOAD16_BYTE( "d28_07.5",  0x00001, 0x40000, 0x3fb8954c );
		ROM_LOAD16_BYTE( "d28_09.2",  0x80000, 0x40000, 0x143a0cc1 );
		ROM_LOAD16_BYTE( "d28_08.4",  0x80001, 0x40000, 0x4c872bd9 );
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD16_BYTE( "d28_04.16", 0x00000, 0x80000, 0xdcabc26b );
		ROM_LOAD16_BYTE( "d28_05.17", 0x00001, 0x80000, 0x862f9665 );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "d28_01.8", 0x000000, 0x100000, 0x181d7b69 );
		ROM_LOAD( "d28_02.9", 0x100000, 0x100000, 0xd301771c );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 ); /* sound cpu */
		ROM_LOAD( "d28_10.6", 0x00000, 0x04000, 0x40805d74 );
		ROM_CONTINUE(         0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "d28_03.10", 0x000000, 0x100000, 0xa1804b52 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_dinorex = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x300000, REGION_CPU1, 0 );    /* 1Mb for 68000 code */
		ROM_LOAD16_BYTE( "d39_14.9",	0x000000, 0x080000, 0xe6aafdac );
		ROM_LOAD16_BYTE( "d39_16.8",	0x000001, 0x080000, 0xcedc8537 );
		ROM_LOAD16_WORD_SWAP( "d39-04.6",	0x100000, 0x100000, 0x3800506d )	/* data rom */
		ROM_LOAD16_WORD_SWAP( "d39-05.7",	0x200000, 0x100000, 0xe2ec3b5d )	/* data rom */
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "d39-06.2",	0x000000, 0x100000, 0x52f62835 );
	
		ROM_REGION( 0x600000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "d39-01.29",	0x000000, 0x200000, 0xd10e9c7d );
		ROM_LOAD( "d39-02.28",	0x200000, 0x200000, 0x6c304403 );
		ROM_LOAD( "d39-03.27",	0x400000, 0x200000, 0xfc9cdab4 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );	 /* sound cpu */
		ROM_LOAD( "d39_12.5",	0x00000, 0x04000, 0x8292c7c1 );
		ROM_CONTINUE(             0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "d39-07.10",	0x000000, 0x100000, 0x28262816 );
	
		ROM_REGION( 0x080000, REGION_SOUND2, 0 );/* Delta-T samples */
		ROM_LOAD( "d39-08.4",	0x000000, 0x080000, 0x377b8b7b );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_dinorexj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x300000, REGION_CPU1, 0 );    /* 1Mb for 68000 code */
		ROM_LOAD16_BYTE( "d39_14.9",	0x000000, 0x080000, 0xe6aafdac );
		ROM_LOAD16_BYTE( "d39_13.8",	0x000001, 0x080000, 0xae496b2f );
		ROM_LOAD16_WORD_SWAP( "d39-04.6",	0x100000, 0x100000, 0x3800506d )	/* data rom */
		ROM_LOAD16_WORD_SWAP( "d39-05.7",	0x200000, 0x100000, 0xe2ec3b5d )	/* data rom */
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "d39-06.2",	0x000000, 0x100000, 0x52f62835 );
	
		ROM_REGION( 0x600000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "d39-01.29",	0x000000, 0x200000, 0xd10e9c7d );
		ROM_LOAD( "d39-02.28",	0x200000, 0x200000, 0x6c304403 );
		ROM_LOAD( "d39-03.27",	0x400000, 0x200000, 0xfc9cdab4 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );	 /* sound cpu */
		ROM_LOAD( "d39_12.5",	0x00000, 0x04000, 0x8292c7c1 );
		ROM_CONTINUE(             0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "d39-07.10",	0x000000, 0x100000, 0x28262816 );
	
		ROM_REGION( 0x080000, REGION_SOUND2, 0 );/* Delta-T samples */
		ROM_LOAD( "d39-08.4",	0x000000, 0x080000, 0x377b8b7b );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_dinorexu = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x300000, REGION_CPU1, 0 );/* 1Mb for 68000 code */
		ROM_LOAD16_BYTE( "d39_14.9",	0x000000, 0x080000, 0xe6aafdac );
		ROM_LOAD16_BYTE( "d39_15.8",	0x000001, 0x080000, 0xfe96723b );
		ROM_LOAD16_WORD_SWAP( "d39-04.6",	0x100000, 0x100000, 0x3800506d )	/* data rom */
		ROM_LOAD16_WORD_SWAP( "d39-05.7",	0x200000, 0x100000, 0xe2ec3b5d )	/* data rom */
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "d39-06.2",	0x000000, 0x100000, 0x52f62835 );
	
		ROM_REGION( 0x600000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD( "d39-01.29",	0x000000, 0x200000, 0xd10e9c7d );
		ROM_LOAD( "d39-02.28",	0x200000, 0x200000, 0x6c304403 );
		ROM_LOAD( "d39-03.27",	0x400000, 0x200000, 0xfc9cdab4 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );	 /* sound cpu */
		ROM_LOAD( "d39_12.5",	0x00000, 0x04000, 0x8292c7c1 );
		ROM_CONTINUE(             0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "d39-07.10",	0x000000, 0x100000, 0x28262816 );
	
		ROM_REGION( 0x080000, REGION_SOUND2, 0 );/* Delta-T samples */
		ROM_LOAD( "d39-08.4",	0x000000, 0x080000, 0x377b8b7b );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_qjinsei = new RomLoadPtr(){ public void handler(){ 	/* Quiz Jinsei Gekijoh */
		ROM_REGION( 0x200000, REGION_CPU1, 0 );    /* 512k for 68000 code */
		ROM_LOAD16_BYTE( "d48-09",  0x000000, 0x040000, 0xa573b68d );
		ROM_LOAD16_BYTE( "d48-10",  0x000001, 0x040000, 0x37143a5b );
		ROM_LOAD16_WORD_SWAP( "d48-03",  0x100000, 0x100000, 0xfb5ea8dc )	/* data rom */
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "d48-04", 0x000000, 0x100000, 0x61e4b078 );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD16_BYTE( "d48-02", 0x000000, 0x100000, 0xa7b68e63 );
		ROM_LOAD16_BYTE( "d48-01", 0x000001, 0x100000, 0x72a94b73 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "d48-11",    0x00000, 0x04000, 0x656c5b54 );
		ROM_CONTINUE(          0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x080000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "d48-05",  0x000000, 0x080000, 0x3fefd058 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_qcrayon = new RomLoadPtr(){ public void handler(){ 	/* Quiz Crayon */
		ROM_REGION( 0x80000, REGION_CPU1, 0 );    /* 512k for 68000 code */
		ROM_LOAD16_BYTE( "d55-13",  0x00000, 0x40000, 0x16afbfc7 );
		ROM_LOAD16_BYTE( "d55-14",  0x00001, 0x40000, 0x2fb3057f );
	
		ROM_REGION16_BE( 0x100000, REGION_USER1, 0 );
		/* extra ROM mapped 0x300000 */
		ROM_LOAD16_WORD_SWAP( "d55-03", 0x000000, 0x100000, 0x4d161e76 )   /* data rom */
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );/* SCR */
		ROM_LOAD( "d55-02", 0x000000, 0x100000, 0xf3db2f1c );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );/* OBJ */
		ROM_LOAD16_BYTE( "d55-05", 0x000000, 0x100000, 0xf0e59902 );
		ROM_LOAD16_BYTE( "d55-04", 0x000001, 0x100000, 0x412975ce );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 ); /* sound cpu */
		ROM_LOAD( "d55-15",  0x00000, 0x04000, 0xba782eff );
		ROM_CONTINUE(        0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "d55-01",  0x000000, 0x100000, 0xa8309af4 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_qcrayon2 = new RomLoadPtr(){ public void handler(){ 	/* Quiz Crayon 2 */
		ROM_REGION( 0x80000, REGION_CPU1, 0 );    /* 512k for 68000 code */
		ROM_LOAD16_BYTE( "d63-12",  0x00000, 0x40000, 0x0f445a38 );
		ROM_LOAD16_BYTE( "d63-13",  0x00001, 0x40000, 0x74455752 );
	
		ROM_REGION16_BE( 0x080000, REGION_USER1, 0 );
		/* extra ROM mapped at 600000 */
		ROM_LOAD16_WORD_SWAP( "d63-01", 0x00000, 0x80000, 0x872e38b4 )   /* data rom */
	
		ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );   /* SCR */
		ROM_LOAD( "d63-03", 0x000000, 0x100000, 0xd24843af );
	
		ROM_REGION( 0x200000, REGION_GFX2, ROMREGION_DISPOSE );   /* OBJ */
		ROM_LOAD( "d63-06", 0x000000, 0x200000, 0x58b1e4a8 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );     /* sound cpu */
		ROM_LOAD( "d63-11",    0x00000, 0x04000, 0x2c7ac9e5 );
		ROM_CONTINUE(          0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "d63-02",  0x000000, 0x100000, 0x162ae165 );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_driftout = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x100000, REGION_CPU1, 0 );    /* 1024k for 68000 code */
		ROM_LOAD16_BYTE( "do_46.rom",  0x00000, 0x80000, 0xf960363e );
		ROM_LOAD16_BYTE( "do_45.rom",  0x00001, 0x80000, 0xe3fe66b9 );
	
		ROM_REGION( 0x080000, REGION_GFX1, ROMREGION_DISPOSE );
		/* empty */
	
		ROM_REGION( 0x080000, REGION_GFX2, ROMREGION_DISPOSE );     /* OBJ */
		ROM_LOAD( "do_obj.rom", 0x00000, 0x80000, 0x5491f1c4 );
	
		ROM_REGION( 0x080000, REGION_GFX3, ROMREGION_DISPOSE );     /* pivot gfx */
		ROM_LOAD( "do_piv.rom", 0x00000, 0x80000, 0xc4f012f7 );
	
		ROM_REGION( 0x1c000, REGION_CPU2, 0 );	/* sound cpu */
		ROM_LOAD( "do_50.rom",  0x00000, 0x04000, 0xffe10124 );
		ROM_CONTINUE(           0x10000, 0x0c000 );/* banked stuff */
	
		ROM_REGION( 0x80000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "do_snd.rom", 0x00000, 0x80000, 0xf2deb82b );
	
		/* no Delta-T samples */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_driveout = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x100000, REGION_CPU1, 0 );    /* 1024k for 68000 code */
		ROM_LOAD16_BYTE( "driveout.003", 0x00000, 0x80000, 0xdc431e4e );
		ROM_LOAD16_BYTE( "driveout.002", 0x00001, 0x80000, 0x6f9063f4 );
	
		ROM_REGION( 0x080000, REGION_GFX1, ROMREGION_DISPOSE );
		/* empty */
	
		ROM_REGION( 0x080000, REGION_GFX2, ROMREGION_DISPOSE );     /* OBJ */
		ROM_LOAD16_BYTE( "driveout.084", 0x00000, 0x40000, 0x530ac420 );
		ROM_LOAD16_BYTE( "driveout.081", 0x00001, 0x40000, 0x0e9a3e9e );
	
		ROM_REGION( 0x080000, REGION_GFX3, ROMREGION_DISPOSE );     /* pivot gfx */
		ROM_LOAD( "do_piv.rom",    0x00000, 0x80000, 0xc4f012f7 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 ); /* sound cpu */
		ROM_LOAD( "driveout.020",  0x0000,  0x8000, 0x99aaeb2e );
	
		ROM_REGION( 0x100000, REGION_SOUND1, 0 );/* ADPCM samples */
		ROM_LOAD( "driveout.028",  0x00000, 0x20000, 0xcbde0b66 );/* banked */
		ROM_CONTINUE(              0x40000, 0x20000 );
		ROM_CONTINUE(              0x80000, 0x20000 );
		ROM_CONTINUE(              0xc0000, 0x20000 );
		ROM_LOAD( "driveout.029",  0x20000, 0x20000, 0x0aba2026 );/* sandwiched */
		ROM_RELOAD(                0x60000, 0x20000 );
		ROM_RELOAD(                0xa0000, 0x20000 );
		ROM_RELOAD(                0xe0000, 0x20000 );
	ROM_END(); }}; 
	
	
	void init_f2( void)
	{
		state_save_register_int("taitof2", 0, "sound region", &banknum);
		state_save_register_func_postload(reset_sound_region);
	}
	
	void init_finalb(void)
	{
		int i;
		unsigned char data;
		unsigned int offset;
		UINT8 *gfx = memory_region(REGION_GFX2);
	
		offset = 0x100000;
		for (i = 0x180000; i<0x200000; i++)
		{
			int d1,d2,d3,d4;
	
			/* convert from 2bits into 4bits format */
			data = gfx[i];
			d1 = (data>>0) & 3;
			d2 = (data>>2) & 3;
			d3 = (data>>4) & 3;
			d4 = (data>>6) & 3;
	
			gfx[offset] = (d3<<2) | (d4<<6);
			offset++;
	
			gfx[offset] = (d1<<2) | (d2<<6);
			offset++;
		}
	
		init_f2();
	}
	
	void init_mjnquest(void)
	{
		int i;
		UINT8 *gfx = memory_region(REGION_GFX2);
	
		/* the bytes in each longword are in reversed order, put them in the
		   order used by the other games. */
		for (i = 0;i < memory_region_length(REGION_GFX2);i += 2)
		{
			int t;
	
			t = gfx[i];
			gfx[i] = (gfx[i+1] >> 4) | (gfx[i+1] << 4);
			gfx[i+1] = (t >> 4) | (t << 4);
		}
	
		init_f2();
	}
	
	void init_yesnoj( void)
	{
		yesnoj_dsw = 0;
		state_save_register_int("yesnoj_dsw", 0, "control", &yesnoj_dsw);
		init_f2();
	}
	
	void init_driveout( void)
	{
		state_save_register_int("driveout_sound1", 0, "sound", &driveout_sound_latch);
		state_save_register_int("driveout_sound2", 0, "sound region", &oki_bank);
		state_save_register_func_postload(reset_driveout_sound_region);
		init_f2();
	}
	
	
	public static GameDriver driver_finalb	   = new GameDriver("1988"	,"finalb"	,"taito_f2.java"	,rom_finalb,null	,machine_driver_finalb	,input_ports_finalb	,init_finalb	,ROT0	,	"Taito Corporation Japan", "Final Blow (World)" )
	public static GameDriver driver_finalbj	   = new GameDriver("1988"	,"finalbj"	,"taito_f2.java"	,rom_finalbj,driver_finalb	,machine_driver_finalb	,input_ports_finalbj	,init_finalb	,ROT0	,	"Taito Corporation", "Final Blow (Japan)" )
	public static GameDriver driver_dondokod	   = new GameDriver("1989"	,"dondokod"	,"taito_f2.java"	,rom_dondokod,null	,machine_driver_dondokod	,input_ports_dondokod	,init_f2	,ROT0	,	"Taito Corporation Japan", "Don Doko Don (World)" )
	public static GameDriver driver_dondokdu	   = new GameDriver("1989"	,"dondokdu"	,"taito_f2.java"	,rom_dondokdu,driver_dondokod	,machine_driver_dondokod	,input_ports_dondokdu	,init_f2	,ROT0	,	"Taito America Corporation", "Don Doko Don (US)" )
	public static GameDriver driver_dondokdj	   = new GameDriver("1989"	,"dondokdj"	,"taito_f2.java"	,rom_dondokdj,driver_dondokod	,machine_driver_dondokod	,input_ports_dondokdj	,init_f2	,ROT0	,	"Taito Corporation", "Don Doko Don (Japan)" )
	public static GameDriver driver_megab	   = new GameDriver("1989"	,"megab"	,"taito_f2.java"	,rom_megab,null	,machine_driver_megab	,input_ports_megab	,init_f2	,ROT0	,	"Taito Corporation Japan", "Mega Blast (World)" )
	public static GameDriver driver_megabj	   = new GameDriver("1989"	,"megabj"	,"taito_f2.java"	,rom_megabj,driver_megab	,machine_driver_megab	,input_ports_megabj	,init_f2	,ROT0	,	"Taito Corporation", "Mega Blast (Japan)" )
	public static GameDriver driver_thundfox	   = new GameDriver("1990"	,"thundfox"	,"taito_f2.java"	,rom_thundfox,null	,machine_driver_thundfox	,input_ports_thundfox	,init_f2	,ROT0	,	"Taito Corporation Japan", "Thunder Fox (World)" )
	public static GameDriver driver_thndfoxu	   = new GameDriver("1990"	,"thndfoxu"	,"taito_f2.java"	,rom_thndfoxu,driver_thundfox	,machine_driver_thundfox	,input_ports_thndfoxu	,init_f2	,ROT0	,	"Taito America Corporation", "Thunder Fox (US)" )
	public static GameDriver driver_thndfoxj	   = new GameDriver("1990"	,"thndfoxj"	,"taito_f2.java"	,rom_thndfoxj,driver_thundfox	,machine_driver_thundfox	,input_ports_thndfoxj	,init_f2	,ROT0	,	"Taito Corporation", "Thunder Fox (Japan)" )
	public static GameDriver driver_cameltry	   = new GameDriver("1989"	,"cameltry"	,"taito_f2.java"	,rom_cameltry,null	,machine_driver_cameltry	,input_ports_cameltry	,init_f2	,ROT0	,	"Taito America Corporation", "Camel Try (US)" )
	public static GameDriver driver_camltrua	   = new GameDriver("1989"	,"camltrua"	,"taito_f2.java"	,rom_camltrua,driver_cameltry	,machine_driver_camltrua	,input_ports_cameltry	,init_f2	,ROT0	,	"Taito America Corporation", "Camel Try (US, alt sound)", GAME_IMPERFECT_SOUND )
	public static GameDriver driver_cameltrj	   = new GameDriver("1989"	,"cameltrj"	,"taito_f2.java"	,rom_cameltrj,driver_cameltry	,machine_driver_cameltry	,input_ports_cameltrj	,init_f2	,ROT0	,	"Taito Corporation", "Camel Try (Japan)" )
	public static GameDriver driver_qtorimon	   = new GameDriver("1990"	,"qtorimon"	,"taito_f2.java"	,rom_qtorimon,null	,machine_driver_qtorimon	,input_ports_qtorimon	,init_f2	,ROT0	,	"Taito Corporation", "Quiz Torimonochou (Japan)" )
	public static GameDriver driver_liquidk	   = new GameDriver("1990"	,"liquidk"	,"taito_f2.java"	,rom_liquidk,null	,machine_driver_liquidk	,input_ports_liquidk	,init_f2	,ROT0	,	"Taito Corporation Japan", "Liquid Kids (World)" )
	public static GameDriver driver_liquidku	   = new GameDriver("1990"	,"liquidku"	,"taito_f2.java"	,rom_liquidku,driver_liquidk	,machine_driver_liquidk	,input_ports_liquidku	,init_f2	,ROT0	,	"Taito America Corporation", "Liquid Kids (US)" )
	public static GameDriver driver_mizubaku	   = new GameDriver("1990"	,"mizubaku"	,"taito_f2.java"	,rom_mizubaku,driver_liquidk	,machine_driver_liquidk	,input_ports_mizubaku	,init_f2	,ROT0	,	"Taito Corporation", "Mizubaku Daibouken (Japan)" )
	public static GameDriver driver_quizhq	   = new GameDriver("1990"	,"quizhq"	,"taito_f2.java"	,rom_quizhq,null	,machine_driver_quizhq	,input_ports_quizhq	,init_f2	,ROT0	,	"Taito Corporation", "Quiz HQ (Japan)" )
	public static GameDriver driver_ssi	   = new GameDriver("1990"	,"ssi"	,"taito_f2.java"	,rom_ssi,null	,machine_driver_ssi	,input_ports_ssi	,init_f2	,ROT270	,	"Taito Corporation Japan", "Super Space Invaders '91 (World)" )
	public static GameDriver driver_majest12	   = new GameDriver("1990"	,"majest12"	,"taito_f2.java"	,rom_majest12,driver_ssi	,machine_driver_ssi	,input_ports_majest12	,init_f2	,ROT270	,	"Taito Corporation", "Majestic Twelve - The Space Invaders Part IV (Japan)" )
	public static GameDriver driver_gunfront	   = new GameDriver("1990"	,"gunfront"	,"taito_f2.java"	,rom_gunfront,null	,machine_driver_gunfront	,input_ports_gunfront	,init_f2	,ROT270	,	"Taito Corporation Japan", "Gun & Frontier (World)" )
	public static GameDriver driver_gunfronj	   = new GameDriver("1990"	,"gunfronj"	,"taito_f2.java"	,rom_gunfronj,driver_gunfront	,machine_driver_gunfront	,input_ports_gunfronj	,init_f2	,ROT270	,	"Taito Corporation", "Gun Frontier (Japan)" )
	public static GameDriver driver_growl	   = new GameDriver("1990"	,"growl"	,"taito_f2.java"	,rom_growl,null	,machine_driver_growl	,input_ports_growl	,init_f2	,ROT0	,	"Taito Corporation Japan", "Growl (World)" )
	public static GameDriver driver_growlu	   = new GameDriver("1990"	,"growlu"	,"taito_f2.java"	,rom_growlu,driver_growl	,machine_driver_growl	,input_ports_growlu	,init_f2	,ROT0	,	"Taito America Corporation", "Growl (US)" )
	public static GameDriver driver_runark	   = new GameDriver("1990"	,"runark"	,"taito_f2.java"	,rom_runark,driver_growl	,machine_driver_growl	,input_ports_runark	,init_f2	,ROT0	,	"Taito Corporation", "Runark (Japan)" )
	public static GameDriver driver_mjnquest	   = new GameDriver("1990"	,"mjnquest"	,"taito_f2.java"	,rom_mjnquest,null	,machine_driver_mjnquest	,input_ports_mjnquest	,init_mjnquest	,ROT0	,	"Taito Corporation", "Mahjong Quest (Japan)" )
	public static GameDriver driver_mjnquesb	   = new GameDriver("1990"	,"mjnquesb"	,"taito_f2.java"	,rom_mjnquesb,driver_mjnquest	,machine_driver_mjnquest	,input_ports_mjnquest	,init_mjnquest	,ROT0	,	"Taito Corporation", "Mahjong Quest (No Nudity)" )
	public static GameDriver driver_footchmp	   = new GameDriver("1990"	,"footchmp"	,"taito_f2.java"	,rom_footchmp,null	,machine_driver_footchmp	,input_ports_footchmp	,init_f2	,ROT0	,	"Taito Corporation Japan", "Football Champ (World)" )
	public static GameDriver driver_hthero	   = new GameDriver("1990"	,"hthero"	,"taito_f2.java"	,rom_hthero,driver_footchmp	,machine_driver_hthero	,input_ports_hthero	,init_f2	,ROT0	,	"Taito Corporation", "Hat Trick Hero (Japan)" )
	public static GameDriver driver_euroch92	   = new GameDriver("1992"	,"euroch92"	,"taito_f2.java"	,rom_euroch92,driver_footchmp	,machine_driver_footchmp	,input_ports_footchmp	,init_f2	,ROT0	,	"Taito Corporation Japan", "Euro Champ '92 (World)" )
	public static GameDriver driver_koshien	   = new GameDriver("1990"	,"koshien"	,"taito_f2.java"	,rom_koshien,null	,machine_driver_koshien	,input_ports_koshien	,init_f2	,ROT0	,	"Taito Corporation", "Ah Eikou no Koshien (Japan)" )
	public static GameDriver driver_yuyugogo	   = new GameDriver("1990"	,"yuyugogo"	,"taito_f2.java"	,rom_yuyugogo,null	,machine_driver_yuyugogo	,input_ports_yuyugogo	,init_f2	,ROT0	,	"Taito Corporation", "Yuuyu no Quiz de GO!GO! (Japan)" )
	public static GameDriver driver_ninjak	   = new GameDriver("1990"	,"ninjak"	,"taito_f2.java"	,rom_ninjak,null	,machine_driver_ninjak	,input_ports_ninjak	,init_f2	,ROT0	,	"Taito Corporation Japan", "Ninja Kids (World)" )
	public static GameDriver driver_ninjakj	   = new GameDriver("1990"	,"ninjakj"	,"taito_f2.java"	,rom_ninjakj,driver_ninjak	,machine_driver_ninjak	,input_ports_ninjakj	,init_f2	,ROT0	,	"Taito Corporation", "Ninja Kids (Japan)" )
	public static GameDriver driver_solfigtr	   = new GameDriver("1991"	,"solfigtr"	,"taito_f2.java"	,rom_solfigtr,null	,machine_driver_solfigtr	,input_ports_solfigtr	,init_f2	,ROT0	,	"Taito Corporation Japan", "Solitary Fighter (World)" )
	public static GameDriver driver_qzquest	   = new GameDriver("1991"	,"qzquest"	,"taito_f2.java"	,rom_qzquest,null	,machine_driver_qzquest	,input_ports_	,init_qzquest	,f2	,	ROT0,   "Taito Corporation", "Quiz Quest - Hime to Yuusha no Monogatari (Japan)" )
	public static GameDriver driver_pulirula	   = new GameDriver("1991"	,"pulirula"	,"taito_f2.java"	,rom_pulirula,null	,machine_driver_pulirula	,input_ports_pulirula	,init_f2	,ROT0	,	"Taito Corporation Japan", "PuLiRuLa (World)" )
	public static GameDriver driver_pulirulj	   = new GameDriver("1991"	,"pulirulj"	,"taito_f2.java"	,rom_pulirulj,driver_pulirula	,machine_driver_pulirula	,input_ports_pulirulj	,init_f2	,ROT0	,	"Taito Corporation", "PuLiRuLa (Japan)" )
	public static GameDriver driver_metalb	   = new GameDriver("1991"	,"metalb"	,"taito_f2.java"	,rom_metalb,null	,machine_driver_metalb	,input_ports_metalb	,init_f2	,ROT0	,	"Taito Corporation Japan", "Metal Black (World)" )
	public static GameDriver driver_metalbj	   = new GameDriver("1991"	,"metalbj"	,"taito_f2.java"	,rom_metalbj,driver_metalb	,machine_driver_metalb	,input_ports_metalbj	,init_f2	,ROT0	,	"Taito Corporation", "Metal Black (Japan)" )
	public static GameDriver driver_qzchikyu	   = new GameDriver("1991"	,"qzchikyu"	,"taito_f2.java"	,rom_qzchikyu,null	,machine_driver_qzchikyu	,input_ports_qzchikyu	,init_f2	,ROT0	,	"Taito Corporation", "Quiz Chikyu Bouei Gun (Japan)" )
	public static GameDriver driver_yesnoj	   = new GameDriver("1992"	,"yesnoj"	,"taito_f2.java"	,rom_yesnoj,null	,machine_driver_yesnoj	,input_ports_yesnoj	,init_yesnoj	,ROT0	,	"Taito Corporation", "Yes/No Sinri Tokimeki Chart" )
	public static GameDriver driver_deadconx	   = new GameDriver("1992"	,"deadconx"	,"taito_f2.java"	,rom_deadconx,null	,machine_driver_deadconx	,input_ports_deadconx	,init_f2	,ROT0	,	"Taito Corporation Japan", "Dead Connection (World)" )
	public static GameDriver driver_deadconj	   = new GameDriver("1992"	,"deadconj"	,"taito_f2.java"	,rom_deadconj,driver_deadconx	,machine_driver_deadconj	,input_ports_deadconj	,init_f2	,ROT0	,	"Taito Corporation", "Dead Connection (Japan)" )
	public static GameDriver driver_dinorex	   = new GameDriver("1992"	,"dinorex"	,"taito_f2.java"	,rom_dinorex,null	,machine_driver_dinorex	,input_ports_dinorex	,init_f2	,ROT0	,	"Taito Corporation Japan", "Dino Rex (World)" )
	public static GameDriver driver_dinorexj	   = new GameDriver("1992"	,"dinorexj"	,"taito_f2.java"	,rom_dinorexj,driver_dinorex	,machine_driver_dinorex	,input_ports_dinorexj	,init_f2	,ROT0	,	"Taito Corporation", "Dino Rex (Japan)" )
	public static GameDriver driver_dinorexu	   = new GameDriver("1992"	,"dinorexu"	,"taito_f2.java"	,rom_dinorexu,driver_dinorex	,machine_driver_dinorex	,input_ports_dinorex	,init_f2	,ROT0	,	"Taito America Corporation", "Dino Rex (US)" )
	public static GameDriver driver_qjinsei	   = new GameDriver("1992"	,"qjinsei"	,"taito_f2.java"	,rom_qjinsei,null	,machine_driver_qjinsei	,input_ports_qjinsei	,init_f2	,ROT0	,	"Taito Corporation", "Quiz Jinsei Gekijoh (Japan)" )
	public static GameDriver driver_qcrayon	   = new GameDriver("1993"	,"qcrayon"	,"taito_f2.java"	,rom_qcrayon,null	,machine_driver_qcrayon	,input_ports_qcrayon	,init_f2	,ROT0	,	"Taito Corporation", "Quiz Crayon Shinchan (Japan)" )
	public static GameDriver driver_qcrayon2	   = new GameDriver("1993"	,"qcrayon2"	,"taito_f2.java"	,rom_qcrayon2,null	,machine_driver_qcrayon2	,input_ports_qcrayon2	,init_f2	,ROT0	,	"Taito Corporation", "Crayon Shinchan Orato Asobo (Japan)" )
	public static GameDriver driver_driftout	   = new GameDriver("1991"	,"driftout"	,"taito_f2.java"	,rom_driftout,null	,machine_driver_driftout	,input_ports_driftout	,init_f2	,ROT270	,	"Visco", "Drift Out (Japan)" )
	public static GameDriver driver_driveout	   = new GameDriver("1991"	,"driveout"	,"taito_f2.java"	,rom_driveout,driver_driftout	,machine_driver_driveout	,input_ports_driftout	,init_driveout	,ROT270	,	"bootleg", "Drive Out" )
}
