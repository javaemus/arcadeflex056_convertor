/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package vidhrdw;

public class mexico86
{
	
	unsigned char *mexico86_videoram,*mexico86_objectram;
	size_t mexico86_objectram_size;
	static int charbank;
	
	
	
	WRITE_HANDLER( mexico86_bankswitch_w )
	{
		unsigned char *RAM = memory_region(REGION_CPU1);
	
		if ((data & 7) > 5)
			usrintf_showmessage( "Switching to invalid bank!" );
	
		cpu_setbank(1, &RAM[0x10000 + 0x4000 * (data & 0x07)]);
	
		charbank = (data & 0x20) >> 5;
	}
	
	
	
	void mexico86_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh)
	{
		int offs;
		int sx,sy,xc,yc;
		int gfx_num,gfx_attr,gfx_offs;
	
	
		/* Bubble Bobble doesn't have a real video RAM. All graphics (characters */
		/* and sprites) are stored in the same memory region, and information on */
		/* the background character columns is stored inthe area dd00-dd3f */
	
		/* This clears & redraws the entire screen each pass */
		fillbitmap(bitmap,Machine->pens[255],&Machine->visible_area);
	
		sx = 0;
	/* the score display seems to be outside of the main objectram. */
		for (offs = 0;offs < mexico86_objectram_size+0x200;offs += 4)
	    {
			int height;
	
	if (offs >= mexico86_objectram_size && offs < mexico86_objectram_size+0x180) continue;
	if (offs >= mexico86_objectram_size+0x1c0) continue;
	
			/* skip empty sprites */
			/* this is dword aligned so the UINT32 * cast shouldn't give problems */
			/* on any architecture */
			if (*(UINT32 *)(&mexico86_objectram[offs]) == 0)
				continue;
	
			gfx_num = mexico86_objectram[offs + 1];
			gfx_attr = mexico86_objectram[offs + 3];
	
			if ((gfx_num & 0x80) == 0)	/* 16x16 sprites */
			{
				gfx_offs = ((gfx_num & 0x1f) * 0x80) + ((gfx_num & 0x60) >> 1) + 12;
				height = 2;
			}
			else	/* tilemaps (each sprite is a 16x256 column) */
			{
				gfx_offs = ((gfx_num & 0x3f) * 0x80);
				height = 32;
			}
	
			if ((gfx_num & 0xc0) == 0xc0)	/* next column */
				sx += 16;
			else
			{
				sx = mexico86_objectram[offs + 2];
	//			if (gfx_attr & 0x40) sx -= 256;
			}
			sy = 256 - height*8 - (mexico86_objectram[offs + 0]);
	
			for (xc = 0;xc < 2;xc++)
			{
				for (yc = 0;yc < height;yc++)
				{
					int goffs,code,color,flipx,flipy,x,y;
	
					goffs = gfx_offs + xc * 0x40 + yc * 0x02;
					code = mexico86_videoram[goffs] + ((mexico86_videoram[goffs + 1] & 0x07) << 8)
							+ ((mexico86_videoram[goffs + 1] & 0x80) << 4) + (charbank << 12);
					color = ((mexico86_videoram[goffs + 1] & 0x38) >> 3) + ((gfx_attr & 0x02) << 2);
					flipx = mexico86_videoram[goffs + 1] & 0x40;
					flipy = 0;
	//				x = sx + xc * 8;
					x = (sx + xc * 8) & 0xff;
					y = (sy + yc * 8) & 0xff;
	
					drawgfx(bitmap,Machine->gfx[0],
							code,
							color,
							flipx,flipy,
							x,y,
							&Machine->visible_area,TRANSPARENCY_PEN,15);
				}
			}
		}
	}
	
	void kikikai_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh)
	{
		int offs;
		int sx,sy,xc,yc;
		int gfx_num,gfx_attr,gfx_offs;
	
	
		/* Bubble Bobble doesn't have a real video RAM. All graphics (characters */
		/* and sprites) are stored in the same memory region, and information on */
		/* the background character columns is stored inthe area dd00-dd3f */
	
		/* This clears & redraws the entire screen each pass */
		fillbitmap(bitmap,Machine->pens[255],&Machine->visible_area);
	
		sx = 0;
	/* the score display seems to be outside of the main objectram. */
		for (offs = 0;offs < mexico86_objectram_size+0x200;offs += 4)
	    {
			int height;
	
	if (offs >= mexico86_objectram_size && offs < mexico86_objectram_size+0x180) continue;
	if (offs >= mexico86_objectram_size+0x1c0) continue;
	
			/* skip empty sprites */
			/* this is dword aligned so the UINT32 * cast shouldn't give problems */
			/* on any architecture */
			if (*(UINT32 *)(&mexico86_objectram[offs]) == 0)
				continue;
	
			gfx_num = mexico86_objectram[offs + 1];
			gfx_attr = mexico86_objectram[offs + 3];
	
			if ((gfx_num & 0x80) == 0)	/* 16x16 sprites */
			{
				gfx_offs = ((gfx_num & 0x1f) * 0x80) + ((gfx_num & 0x60) >> 1) + 12;
				height = 2;
			}
			else	/* tilemaps (each sprite is a 16x256 column) */
			{
				gfx_offs = ((gfx_num & 0x3f) * 0x80);
				height = 32;
			}
	
			if ((gfx_num & 0xc0) == 0xc0)	/* next column */
				sx += 16;
			else
			{
				sx = mexico86_objectram[offs + 2];
	//			if (gfx_attr & 0x40) sx -= 256;
			}
			sy = 256 - height*8 - (mexico86_objectram[offs + 0]);
	
			for (xc = 0;xc < 2;xc++)
			{
				for (yc = 0;yc < height;yc++)
				{
					int goffs,code,color,flipx,flipy,x,y;
	
					goffs = gfx_offs + xc * 0x40 + yc * 0x02;
					code = mexico86_videoram[goffs] + ((mexico86_videoram[goffs + 1] & 0x1f) << 8);
					color = (mexico86_videoram[goffs + 1] & 0xe0) >> 5;
					flipx = 0;
					flipy = 0;
	//				x = sx + xc * 8;
					x = (sx + xc * 8) & 0xff;
					y = (sy + yc * 8) & 0xff;
	
					drawgfx(bitmap,Machine->gfx[0],
							code,
							color,
							flipx,flipy,
							x,y,
							&Machine->visible_area,TRANSPARENCY_PEN,15);
				}
			}
		}
	}
}
