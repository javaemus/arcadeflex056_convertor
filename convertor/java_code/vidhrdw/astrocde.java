/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package vidhrdw;

public class astrocde
{
	
	
	
	#define SCREEN_WIDTH 320
	#define MAX_LINES 204
	
	#define MAX_INT_PER_FRAME 256
	
	unsigned char *wow_videoram;
	static int magic_expand_color, magic_control, magic_expand_count, magic_shift_leftover;
	static int collision;
	
	/* This value affects the star layout, the value is correct since
	   it is mentioned in the docs and perfectly matches screen shots.
	 */
	#define CLOCKS_PER_LINE 455
	
	/* This value affects the star blinking and the sparkle patterns.
	   It is just a guess, aiming to a supposed 60Hz refresh rate, and has
	   not been verified.
	 */
	#define CLOCKS_PER_FRAME (CLOCKS_PER_LINE*262)
	
	#define RNG_PERIOD 131071	/* 2^17-1 */
	static int *rng;
	static int *star;
	
	
	static int colors[MAX_INT_PER_FRAME][8];
	static int colorsplit[MAX_INT_PER_FRAME];
	static int BackgroundData,VerticalBlank;
	
	static int sparkle[MAX_INT_PER_FRAME][4];	/* sparkle[line][0] is star enable */
	
	
	void wow_update_line(struct mame_bitmap *bitmap,int line);
	
	
	
	void astrocde_init_palette(unsigned char *palette, unsigned short *colortable,const unsigned char *color_prom)
	{
		/* This routine builds a palette using a transformation from */
		/* the YUV (Y, B-Y, R-Y) to the RGB color space */
	
		int i,j;
	
		float Y, RY, BY;	/* Y, R-Y, and B-Y signals as generated by the game */
							/* Y = Luminance -> (0 to 1) */
							/* R-Y = Color along R-Y axis -> C*(-1 to +1) */
							/* B-Y = Color along B-Y axis -> C*(-1 to +1) */
		float R, G, B;
	
		float brightest = 1.0;	/* Approx. Luminance values for the extremes -> (0 to 1) */
		float dimmest   = 0.0;
		float C = 0.75;			/* Approx. Chroma intensity */
	
		/* The astrocade has a 256 color palette                 */
		/* 32 colors, with 8 luminance levels for each color     */
		/* The 32 colors circle around the YUV color space,      */
		/* with the exception of the first 8 which are grayscale */
	
		/* Note: to simulate a B&W monitor, set C=0 and all      */
		/*       colors will appear as the first 8 grayscales    */
	
		for(i=0;i<32;i++)
		{
			RY = C*sin(i*2.0*3.14159/32.0);
			if (i == 0)
				BY = 0;
			else
				BY = C*cos(i*2.0*3.14159/32.0);
	
			for(j=0;j<8;j++)
			{
				Y = (j/7.0)*(brightest-dimmest)+dimmest;
	
				/* Transform to RGB */
	
				R = (RY+Y)*255;
				G = (Y - 0.299*(RY+Y) - 0.114*(BY+Y))/0.587*255;
				B = (BY+Y)*255;
	
				/* Clipping, in case of saturation */
	
				if (R < 0)
					R = 0;
				if (R > 255)
					R = 255;
				if (G < 0)
					G = 0;
				if (G > 255)
					G = 255;
				if (B < 0)
					B = 0;
				if (B > 255)
					B = 255;
	
				/* Round, and set the value */
	
				*palette = floor(R+.5);
				palette++;
				*palette = floor(G+.5);
				palette++;
				*palette = floor(B+.5);
				palette++;
			}
		}
	}
	
	
	
	/****************************************************************************
	 * Scanline Interrupt System
	 ****************************************************************************/
	
	static int NextScanInt=0;			/* Normal */
	static int CurrentScan=0;
	static int InterruptFlag=0;
	
	static int GorfDelay;				/* Gorf */
	static int Countdown=0;
	
	public static WriteHandlerPtr astrocde_interrupt_enable_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		InterruptFlag = data;
	
		if (data & 0x01)					/* Disable Interrupts? */
	  	    interrupt_enable_w(0,0);
		else
	  		interrupt_enable_w(0,1);
	
		/* Gorf Special interrupt */
	
		if (data & 0x10)
	 	{
	  		GorfDelay =(CurrentScan + 7) & 0xFF;
	
			/* Gorf Special *MUST* occur before next scanline interrupt */
	
			if ((NextScanInt > CurrentScan) && (NextScanInt < GorfDelay))
			{
			  	GorfDelay = NextScanInt - 1;
			}
	
	#ifdef MAME_DEBUG
			logerror("Gorf Delay set to %02x\n",GorfDelay);
	#endif
	
		}
	
	#ifdef MAME_DEBUG
		logerror("Interrupt Flag set to %02x\n",InterruptFlag);
	#endif
	} };
	
	public static WriteHandlerPtr astrocde_interrupt_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* A write to 0F triggers an interrupt at that scanline */
	
	#ifdef MAME_DEBUG
		logerror("Scanline interrupt set to %02x\n",data);
	#endif
	
		NextScanInt = data;
	} };
	
	int wow_interrupt(void)
	{
		int res,i,next;
	
		if (!osd_skip_this_frame())
			wow_update_line(Machine->scrbitmap,CurrentScan);
	
		next = (CurrentScan + 1) % MAX_INT_PER_FRAME;
		for (i = 0;i < 8;i++)
			colors[next][i] = colors[CurrentScan][i];
		for (i = 0;i < 4;i++)
			sparkle[next][i] = sparkle[CurrentScan][i];
		colorsplit[next] = colorsplit[CurrentScan];
	
		CurrentScan = next;
	
		/* Scanline interrupt enabled ? */
	
		res = ignore_interrupt();
	
		if ((InterruptFlag & 0x08) && (CurrentScan == NextScanInt))
			res = interrupt();
	
		return res;
	}
	
	/****************************************************************************
	 * Gorf - Interrupt routine and Timer hack
	 ****************************************************************************/
	
	int gorf_interrupt(void)
	{
		int res;
	
		res = wow_interrupt();
	
		/* Gorf Special Bits */
	
		if (Countdown>0) Countdown--;
	
		if ((InterruptFlag & 0x10) && (CurrentScan==GorfDelay))
			res = interrupt() & 0xF0;
	
	/*	cpu_clear_pending_interrupts(0); */
	
	//	Z80_Clear_Pending_Interrupts();					/* Temporary Fix */
		cpu_set_irq_line(0,0,CLEAR_LINE);
	
		return res;
	}
	
	/* ======================================================================= */
	
	public static ReadHandlerPtr wow_video_retrace_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return CurrentScan;
	} };
	
	public static ReadHandlerPtr wow_intercept_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int res;
	
		res = collision;
		collision = 0;
	
		return res;
	} };
	
	
	/* Switches color registers at this zone - 40 zones (NOT USED) */
	
	public static WriteHandlerPtr astrocde_colour_split_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		colorsplit[CurrentScan] = 2 * (data & 0x3f);
	
		BackgroundData = ((data&0xc0) >> 6) * 0x55;
	} };
	
	
	/* This selects commercial (high res, arcade) or
	                  consumer (low res, astrocade) mode */
	
	public static WriteHandlerPtr astrocde_mode_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	//	astrocade_mode = data & 0x01;
	} };
	
	
	public static WriteHandlerPtr astrocde_vertical_blank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (VerticalBlank != data)
		{
			VerticalBlank = data;
		}
	} };
	
	
	public static WriteHandlerPtr astrocde_colour_register_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		colors[CurrentScan][offset] = data;
	
	#ifdef MAME_DEBUG
		logerror("colors %01x set to %02x\n",offset,data);
	#endif
	} };
	
	public static WriteHandlerPtr astrocde_colour_block_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		static int color_reg_num = 7;
	
		astrocde_colour_register_w(color_reg_num,data);
	
		color_reg_num = (color_reg_num - 1) & 7;
	} };
	
	
	public static WriteHandlerPtr wow_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if ((offset < 0x4000) && (wow_videoram[offset] != data))
		{
			wow_videoram[offset] = data;
		}
	} };
	
	
	public static WriteHandlerPtr astrocde_magic_expand_color_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	#ifdef MAME_DEBUG
	//	logerror("%04x: magic_expand_color = %02x\n",cpu_get_pc(),data);
	#endif
	
		magic_expand_color = data;
	} };
	
	
	public static WriteHandlerPtr astrocde_magic_control_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	#ifdef MAME_DEBUG
	//	logerror("%04x: magic_control = %02x\n",cpu_get_pc(),data);
	#endif
	
		magic_control = data;
	
		magic_expand_count = 0;	/* reset flip-flop for expand mode on write to this register */
		magic_shift_leftover = 0;	/* reset shift buffer on write to this register */
	
		if (magic_control & 0x04)
			usrintf_showmessage("unsupported MAGIC ROTATE mode");
	} };
	
	
	static void copywithflip(int offset,int data)
	{
		int shift,data1;
	
	
		if (magic_control & 0x40)	/* copy backwards */
		{
			int bits,stib,k;
	
			bits = data;
			stib = 0;
			for (k = 0;k < 4;k++)
			{
				stib >>= 2;
				stib |= (bits & 0xc0);
				bits <<= 2;
			}
	
			data = stib;
		}
	
		shift = magic_control & 3;
		data1 = 0;
		if (magic_control & 0x40)	/* copy backwards */
		{
			while (shift > 0)
			{
				data1 <<= 2;
				data1 |= (data & 0xc0) >> 6;
				data <<= 2;
				shift--;
			}
		}
		else
		{
			while (shift > 0)
			{
				data1 >>= 2;
				data1 |= (data & 0x03) << 6;
				data >>= 2;
				shift--;
			}
		}
		data |= magic_shift_leftover;
		magic_shift_leftover = data1;
	
		if (magic_control & 0x30)
		{
			/* TODO: the collision detection should be made */
			/* independently for each of the four pixels    */
	
			if (data && wow_videoram[offset])
				collision |= 0xff;
			else collision &= 0x0f;
		}
	
		if (magic_control & 0x20) data ^= wow_videoram[offset];	/* draw in XOR mode */
		else if (magic_control & 0x10) data |= wow_videoram[offset];	/* draw in OR mode */
		wow_videoram_w(offset,data);
	}
	
	
	public static WriteHandlerPtr wow_magicram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (magic_control & 0x08)	/* expand mode */
		{
			int bits,bibits,k;
	
			bits = data;
			if (magic_expand_count) bits <<= 4;
			bibits = 0;
			for (k = 0;k < 4;k++)
			{
				bibits <<= 2;
				if (bits & 0x80) bibits |= (magic_expand_color >> 2) & 0x03;
				else bibits |= magic_expand_color & 0x03;
				bits <<= 1;
			}
	
			copywithflip(offset,bibits);
	
			magic_expand_count ^= 1;
		}
		else copywithflip(offset,data);
	} };
	
	
	public static WriteHandlerPtr astrocde_pattern_board_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		static int src;
		static int mode;	/*  bit 0 = direction
								bit 1 = expand mode
								bit 2 = constant
								bit 3 = flush
								bit 4 = flip
								bit 5 = flop */
		static int skip;	/* bytes to skip after row copy */
		static int dest;
		static int length;	/* row length */
		static int loops;	/* rows to copy - 1 */
		unsigned char *RAM = memory_region(REGION_CPU1);
	
	
		switch (offset)
		{
			case 0:
				src = data;
				break;
			case 1:
				src = src + data * 256;
				break;
			case 2:
				mode = data & 0x3f;			/* register is 6 bit wide */
				break;
			case 3:
				skip = data;
				break;
			case 4:
				dest = skip + data * 256;	/* register 3 is shared between skip and dest */
				break;
			case 5:
				length = data;
				break;
			case 6:
				loops = data;
				break;
		}
	
		if (offset == 6)	/* trigger blit */
		{
			int i,j;
	
	#ifdef MAME_DEBUG
	//		logerror("%04x: blit src %04x mode %02x skip %d dest %04x length %d loops %d\n",
	//			cpu_get_pc(),src,mode,skip,dest,length,loops);
	#endif
	
			/* Kludge: have to steal some cycles from the Z80 otherwise text
			   scrolling in Gorf is too fast. */
			z80_ICount -= 4 * (length+1) * (loops+1);
	
			for (i = 0; i <= loops;i++)
			{
				for (j = 0;j <= length;j++)
				{
					if (!(mode & 0x08) || j < length)
					{
						if (mode & 0x01)			/* Direction */
							RAM[src]=RAM[dest];
						else
							if (dest >= 0) cpu_writemem16(dest,RAM[src]);	/* ASG 971005 */
					}
					/* close out writes in case of shift... I think this is wrong */
					else if (j == length)
						if (dest >= 0) cpu_writemem16(dest,0);
	
					if ((j & 1) || !(mode & 0x02))  /* Expand Mode - don't increment source on odd loops */
						if (mode & 0x04) src++;		/* Constant mode - don't increment at all! */
	
					if (mode & 0x20) dest++;		/* copy forwards */
					else dest--;					/* backwards */
				}
	
				if ((j & 1) && (mode & 0x02))		/* always increment source at end of line */
					if (mode & 0x04) src++;			/* Constant mode - don't increment at all! */
	
				if ((mode & 0x08) && (mode & 0x04)) /* Correct src if in flush mode */
					src--;                          /* and NOT in Constant mode */
	
				if (mode & 0x20) dest--;			/* copy forwards */
				else dest++;						/* backwards */
	
				dest += (int)((signed char)skip);	/* extend the sign of the skip register */
	
				/* Note: actually the hardware doesn't handle the sign of the skip register, */
				/* when incrementing the destination address the carry bit is taken from the */
				/* mode register. To faithfully emulate the hardware I should do: */
	#if 0
				{
					int lo,hi;
	
					lo = dest & 0x00ff;
					hi = dest & 0xff00;
					lo += skip;
					if (mode & 0x10)
					{
						if (lo < 0x100) hi -= 0x100;
					}
					else
					{
						if (lo > 0xff) hi += 0x100;
					}
					dest = hi | (lo & 0xff);
				}
	#endif
			}
		}
	} };
	
	
	static void init_star_field(void)
	{
		int generator;
		int count,x,y;
	
		generator = 0;
	
		/* this 17-bit shifter with XOR feedback has a period of 2^17-1 iterations */
		for (count = 0;count < RNG_PERIOD;count++)
		{
			int bit1,bit2;
	
			generator <<= 1;
			bit1 = (~generator >> 17) & 1;
			bit2 = (generator >> 5) & 1;
	
			if (bit1 ^ bit2) generator |= 1;
	
			rng[count] = generator & 0x1ffff;
		}
	
	
		/* calculate stars positions */
		count = 0;
		for (y = 0;y < MAX_LINES;y++)
		{
			for (x = -16;x < CLOCKS_PER_LINE-16;x++)	/* perfect values determined with screen shots */
			{
				if (x >= Machine->visible_area.min_x &&
					x <= Machine->visible_area.max_x &&
					y >= Machine->visible_area.min_y &&
					y <= Machine->visible_area.max_y)
				{
					if ((rng[count] & 0x1fe00) == 0x0fe00)
						star[x+SCREEN_WIDTH*y] = 1;
					else
						star[x+SCREEN_WIDTH*y] = 0;
				}
	
				count++;
			}
		}
	
	
		/* now convert the rng values to Y adjustments that will be used at runtime */
		for (count = 0;count < RNG_PERIOD;count++)
		{
			int r;
	
			r = rng[count];
			rng[count] = (((r >> 12) & 1) << 3) +
						 (((r >>  8) & 1) << 2) +
						 (((r >>  4) & 1) << 1) +
						 (((r >>  0) & 1) << 0);
		}
	}
	
	
	/* GORF Special Registers
	 *
	 * These are data writes, done by IN commands
	 *
	 * The data is placed on the upper bits 8-11 bits of the address bus (B)
	 * and is used to drive 2 8 bit addressable latches to control :-
	 *
	 * IO 15
	 *   0 coin counter
	 *   1 coin counter
	 *   2 Star enable (never written to)
	 *   3 Sparkle 1
	 *   4 Sparkle 2
	 *   5 Sparkle 3
	 *   6 Second Amp On/Off ?
	 *   7 Drv7
	 *
	 * IO 16
	 *   0
	 *   1
	 *   2
	 *   3
	 *   4
	 *   5
	 *   6
	 *   7 Space Cadet Light ?
	 *
	 */
	
	public static ReadHandlerPtr gorf_io_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int data;
	
		data = (cpu_get_reg(Z80_BC) >> 8) & 0x0f;
	
		offset = (offset << 3) + (data >> 1);
		data = ~data & 0x01;
	
		switch (offset)
		{
			case 0: coin_counter_w(0,data); break;
			case 1: coin_counter_w(1,data); break;
			case 2: sparkle[CurrentScan][0] = data; break;
			case 3: sparkle[CurrentScan][1] = data; break;
			case 4: sparkle[CurrentScan][2] = data; break;
			case 5: sparkle[CurrentScan][3] = data; break;
		}
	
	#ifdef MAME_DEBUG
		logerror("%04x: Latch IO %02x set to %d\n",cpu_get_pc(),offset,data);
	#endif
	
		return 0;
	} };
	
	
	/* Wizard of Wor Special Registers
	 *
	 * These are data writes, done by IN commands
	 *
	 * The data is placed on the upper bits 8-11 bits of the address bus (A)
	 * and is used to drive 1 8 bit addressable latches to control :-
	 *
	 * IO 15
	 *   0 coin counter
	 *   1 coin counter
	 *   2 Star enable (never written to)
	 *   3 Sparkle 1
	 *   4 Sparkle 2
	 *   5 Sparkle 3
	 *   6 n.c.
	 *   7 coin counter
	 *
	 */
	
	public static ReadHandlerPtr wow_io_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int data;
	
		data = (cpu_get_reg(Z80_AF) >> 8) & 0x0f;
	
		offset = (offset << 3) + (data >> 1);
		data = ~data & 0x01;
	
		switch (offset)
		{
			case 0: coin_counter_w(0,data); break;
			case 1: coin_counter_w(1,data); break;
			case 2: sparkle[CurrentScan][0] = data; break;
			case 3: sparkle[CurrentScan][1] = data; break;
			case 4: sparkle[CurrentScan][2] = data; break;
			case 5: sparkle[CurrentScan][3] = data; break;
			case 7: coin_counter_w(2,data); break;
		}
	
	#ifdef MAME_DEBUG
		logerror("%04x: Latch IO %02x set to %d\n",cpu_get_pc(),offset,data);
	#endif
	
		return 0;
	} };
	
	/****************************************************************************/
	
	void astrocde_vh_stop(void)
	{
		free(rng);
		rng = 0;
		free(star);
		star = 0;
	}
	
	int astrocde_vh_start(void)
	{
		rng = malloc(RNG_PERIOD * sizeof(rng[0]));
		star = malloc(SCREEN_WIDTH * MAX_LINES * sizeof(star[0]));
	
		if (!rng || !star)
			return 1;
	
		memset(sparkle,0,sizeof(sparkle));
		CurrentScan = 0;
	
		return 0;
	}
	
	int astrocde_stars_vh_start(void)
	{
		int res;
	
		res = astrocde_vh_start();
	
		sparkle[0][0] = 1;	/* wow doesn't initialize this */
		init_star_field();
	
		return res;
	}
	
	
	
	/****************************************************************************/
	
	void wow_update_line(struct mame_bitmap *bitmap,int line)
	{
		/* Copy one line to bitmap, using current color register settings */
	
		int memloc;
		int i,x;
		int data,color;
		int rngoffs;
	
		if (line >= MAX_LINES) return;
	
		rngoffs = MOD_U32_U64_U32( MUL_U64_U32_U32(
				cpu_getcurrentframe() % RNG_PERIOD, CLOCKS_PER_FRAME), RNG_PERIOD);
	
		memloc = line * 80;
	
		for (i = 0; i < 80; i++, memloc++)
		{
			if (line < VerticalBlank)
				data = wow_videoram[memloc];
			else
				data = BackgroundData;
	
			for (x = i*4+3; x >= i*4; x--)
			{
				int pen,scol;
	
				color = data & 0x03;
				if (i < colorsplit[line]) color += 4;
	
				if ((data & 0x03) == 0)
				{
					if (sparkle[line][0])
					{
						if (star[x+SCREEN_WIDTH*line])
						{
							scol = rng[(rngoffs + x+CLOCKS_PER_LINE*line) % RNG_PERIOD];
							pen = (colors[line][color]&~7) + scol/2;
						}
						else
							pen = 0;
					}
					else
						pen = colors[line][color];
				}
				else
				{
					if (sparkle[line][data & 0x03])
					{
						scol = rng[(rngoffs + x+CLOCKS_PER_LINE*line) % RNG_PERIOD];
						pen = (colors[line][color]&~7) + scol/2;
					}
					else
						pen = colors[line][color];
				}
	
				plot_pixel(bitmap,x,line,Machine->pens[pen]);
	
				data >>= 2;
			}
		}
	}
	
	
	
	void astrocde_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh)
	{
		if (full_refresh)
		{
			int i;
	
			for (i = 0;i < MAX_LINES;i++)
				wow_update_line(bitmap,i);
		}
	}
	
	void seawolf2_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh)
	{
		int centre;
		unsigned char *RAM = memory_region(REGION_CPU1);
	
	
		astrocde_vh_screenrefresh(bitmap,full_refresh);
	
	
		/* Draw a sight */
	
		if(RAM[0xc1fb] != 0)	/* Number of Players */
		{
			/* Player 1 */
	
			centre = 317 - ((input_port_0_r(0) & 0x3f)-18) * 10;
	
			if (centre<2)   centre=2;
			if (centre>317) centre=317;
	
			draw_crosshair(bitmap,centre,35,&Machine->visible_area);
	
			/* Player 2 */
	
			if(RAM[0xc1fb] == 2)
			{
				centre = 316 - ((input_port_1_r(0) & 0x3f)-18) * 10;
	
				if (centre<1)   centre=1;
				if (centre>316) centre=316;
	
				draw_crosshair(bitmap,centre,33,&Machine->visible_area);
			}
		}
	}
}
