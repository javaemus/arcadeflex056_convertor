/* sys16spr.c
**
**	This module maps spriteram for various Sega System16/System18 games to
**	a shared abstraction represented by the sys16_sprite_attributes
**	structure.
**
**	Each function takes a pointer to a sprite attribute structure to fill
**	out.  This function is initialized to zero automatically prior to the
**	function call.
**
**	The next parameter, source points to the spriteram
**	associated with a particular sprite.
**
**	The final parameter bJustGetColor is set when this function is called while
**	marking colors.  During this pass, only the palette field needs to be
**	populated.
**
**	This function must return 1 if the sprite was flagged as the last sprite,
**	or 0 if the sprite was not flagged as the last sprite.
**
**	We should probably unpack sprites into an array once, rather than processing
**	sprites one at a time, once when marking colors and once when rendering them.
**	Note that we need to draw sprites from front to back to achieve proper sprite-tilemap
**	orthogonality, so would make it easier to know how many sprites there are (and
**	thus which is the 'last sprite' with which we need to start.
*/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package vidhrdw;

public class sys16spr
{
	
	int sys16_sprite_shinobi( struct sys16_sprite_attributes *sprite, const UINT16 *source, int bJustGetColor ){
	/* standard sprite hardware (Shinobi, Altered Beast, Golden Axe
		0	YYYYYYYY	YYYYYYYY	top, bottom (screen coordinates)
		1	-------X	XXXXXXXX	left (screen coordinate)
		2	-------F	FWWWWWWW	flipx, signed pitch
		3	TTTTTTTT	TTTTTTTT	word offset for start of sprite data; each word is 16 bits (4 pixels)
		4	----BBBB	PPCCCCCC	attributes: bank, priority, color
		5	------ZZ	ZZZZZZZZ	zoomx: 0 for 100%; 0x3ff for 50% size
		6	------ZZ	ZZZZZZZZ	zoomy: (defaults to zoomx)
		7	--------	--------
	*/
		UINT16 ypos = source[0];
		UINT16 width = source[2];
		int top = ypos&0xff;
		int bottom = ypos>>8;
		if( bottom == 0xff || width ==sys16_spritelist_end ) return 1; /* ? */
		/* end of spritelist */
		if( bottom !=0 && bottom > top ){
			UINT16 attributes = source[4];
			UINT16 zoomx = source[5]&0x3ff;
			UINT16 zoomy = (source[6]&0x3ff);
			if( zoomy==0 || source[6]==0xffff ) zoomy = zoomx; /* if zoomy is 0, use zoomx instead */
			sprite->x = source[1] + sys16_sprxoffset;
			sprite->y = top;
			sprite->priority = (attributes>>6)&0x3;
			sprite->color = 1024/16 + (attributes&0x3f);
			sprite->screen_height = bottom-top;
			sprite->flags = SYS16_SPR_VISIBLE;
			if( width&0x100 ) sprite->flags |= SYS16_SPR_FLIPX;
	#ifdef TRANSPARENT_SHADOWS
			if ((attributes&0x3f)==0x3f) sprite->flags|= SYS16_SPR_SHADOW;
	#endif
			sprite->zoomx = zoomx;
			sprite->zoomy = zoomy;
			sprite->pitch = source[2]&0xff;
			sprite->gfx = 2*(source[3] + sys16_obj_bank[(attributes>>8)&0xf]*0x10000);
		}
		return 0;
	}
	
	int sys16_sprite_passshot( struct sys16_sprite_attributes *sprite, const UINT16 *source, int bJustGetColor ){
	/* Passing shot 4p needs:	passshot_y=-0x23; passshot_width=1;
		0	-------X	XXXXXXXX	left (screen coordinate)
		1	YYYYYYYY	YYYYYYYY	bottom, top (screen coordinates)
		2	XTTTTTTT	TTTTTTTT	(pen data, flipx, flipy)
		3	--------	FWWWWWWW	pitch: flipy, logical width
		4	------ZZ	ZZZZZZZZ	zoom
		5	PPCCCCCC	BBBB----	attributes: priority, color, bank
		6	--------	--------
		7	--------	--------
	*/
		int passshot_y=0;
		int passshot_width=0;
		UINT16 attributes = source[5];
		UINT16 ypos = source[1];
		int bottom = (ypos>>8)+passshot_y;
		int top = (ypos&0xff)+passshot_y;
		if( bottom>top && ypos!=0xffff ){
			int bank = (attributes>>4)&0xf;
			UINT16 number = source[2];
			UINT16 width = source[3];
			int zoom = source[4]&0x3ff;
			int xpos = source[0] + sys16_sprxoffset;
			sprite->screen_height = bottom - top;
			sprite->priority = attributes>>14;
			sprite->color = 1024/16+ ((attributes>>8)&0x3f);
			/* hack */
			if( passshot_width) { /* 4 player bootleg version */
				width = -width;
				number -= width*(bottom-top-1)-1;
			}
			sprite->flags = SYS16_SPR_VISIBLE;
			if( number & 0x8000 ) sprite->flags |= SYS16_SPR_FLIPX;
	#ifdef TRANSPARENT_SHADOWS
			if (((attributes>>8)&0x3f)==0x3f)	// shadow sprite
				sprite->flags|= SYS16_SPR_SHADOW;
	#endif
			sprite->pitch = width&0xff;
			if( sprite->flags&SYS16_SPR_FLIPX ){
				bank = (bank-1) & 0xf; /* ? */
			}
			sprite->gfx = (number*4 + (sys16_obj_bank[bank] << 17))/2;
			sprite->x = xpos;
			sprite->y = top+2;
			sprite->zoomx = sprite->zoomy = zoom;
		}
		return 0;
	}
	
	int sys16_sprite_aurail( struct sys16_sprite_attributes *sprite, const UINT16 *source, int bJustGetColor ){
	/* Aurail, Bay Route, riotcity
		0	YYYYYYYY	YYYYYYYY	(screen coordinates)
		1	-------X	XXXXXXXX	(screen coordinate)
		2	-------F	FWWWWWWW	flipx, pitch
		3	TTTTTTTT	TTTTTTTT	(pen data)
		4	----BBBB	PPCCCCCC	(attributes: bank, priority, color)
		5	------ZZ	ZZZZZZZZ	zoomx
		6	------ZZ	ZZZZZZZZ	zoomy (defaults to zoomx)
		7	--------	--------
	*/
		UINT16 ypos = source[0];
		UINT16 width = source[2];
		UINT16 attributes = source[4];
		int top = ypos&0xff;
		int bottom = ypos>>8;
		if( width == sys16_spritelist_end) return 1;
	#ifdef TRANSPARENT_SHADOWS
		if(bottom !=0 && bottom > top)
	#else
		if(bottom !=0 && bottom > top && (attributes&0x3f) !=0x3f)
	#endif
		{
			UINT16 zoomx = source[5]&0x3ff;
			UINT16 zoomy = (source[6]&0x3ff);
			if( zoomy==0 ) zoomy = zoomx; /* if zoomy is 0, use zoomx instead */
			sprite->color = 1024/16 + (attributes&0x3f);
			sprite->x = source[1] + sys16_sprxoffset;;
			sprite->y = top;
			sprite->priority = (attributes>>6)&0x3;
			sprite->screen_height = bottom-top;
			sprite->pitch = width&0xff;
			sprite->flags = SYS16_SPR_VISIBLE;
			if( width&0x100 ) sprite->flags |= SYS16_SPR_FLIPX;
	#ifdef TRANSPARENT_SHADOWS
			if ((attributes&0x3f)==0x3f)	// shadow sprite
				sprite->flags|= SYS16_SPR_SHADOW;
	#endif
				sprite->zoomx = zoomx;
				sprite->zoomy = zoomy;
				sprite->gfx = (source[3] + sys16_obj_bank[(attributes>>8)&0xf]*0x10000)*2;
	//			sprite->gfx = ((gfx &0x3ffff) + (sys16_obj_bank[(attributes>>8)&0xf] << 17))/2;
		}
		return 0;
	}
	
	int sys16_sprite_fantzone( struct sys16_sprite_attributes *sprite, const UINT16 *source, int bJustGetColor ){
	/*	0	YYYYYYYY	YYYYYYYY	bottom,top (screen coordinates)
		1	-------X	XXXXXXXX	left (screen coordinate)
		2	--------	FWWWWWWW	pitch
		3	FTTTTTTT	TTTTTTTT
		4	--CCCCCC	--BB--PP	attributes
		5	--------	--------
		6	--------	--------
		7	--------	--------
	*/
		UINT16 ypos = source[0];
		UINT16 pal = (source[4]>>8)&0x3f;
		int top = ypos&0xff;
		int bottom = ypos>>8;
		if( bottom == 0xff ) return 1; /* end of spritelist marker */
	#ifdef TRANSPARENT_SHADOWS
		if(bottom !=0 && bottom > top)
	#else
		if(bottom !=0 && bottom > top && pal !=0x3f)
	#endif
		{
			UINT16 bank=(source[4]>>4)&0x3;
			int gfx = 4*(source[3]&0x7fff);
			sprite->priority = source[4]&0x3;
			sprite->flags = SYS16_SPR_VISIBLE;
			if( source[3]&0x8000 ) sprite->flags |= SYS16_SPR_FLIPX;
			if( (source[3] & 0x7f80) == 0x7f80 ){ /* ? */
				bank=(bank-1)&0x3;
				sprite->flags ^= SYS16_SPR_FLIPX;
			}
			sprite->screen_height = bottom-top;
			top++; bottom++;
			sprite->x = source[1] + sys16_sprxoffset;
			if( sprite->x > 0x140 ) sprite->x -= 0x200;
			sprite->y = top;
			sprite->color = 1024/16 + pal;
			sprite->pitch = source[2]&0xff;
	#ifdef TRANSPARENT_SHADOWS
			if( pal==0x3f ) sprite->flags|= SYS16_SPR_SHADOW;
	#endif
			sprite->gfx = ( (gfx &0x3ffff) + (bank<<17) )/2;
		}
		return 0;
	}
	
	int sys16_sprite_quartet2( struct sys16_sprite_attributes *sprite, const UINT16 *source, int bJustGetColor ){
	/* Quartet2, Alexkidd, Bodyslam, mjleague, shinobibl
		0	YYYYYYYY YYYYYYYY	bottom, top
		1	-------X XXXXXXXX	xpos
		2	-------- WWWWWWWW	pitch
		3	FTTTTTTT TTTTTTTT	flipx, gfx
		4	--CCCCCC BBBBPPPP	color, bank, priority
		5	-------- --------
		6	-------- --------
		7	-------- --------
	*/
		UINT16 ypos = source[0];
		int top = ypos&0xff;
		int bottom = ypos>>8;
		if( bottom == 0xff ) return 1;
		if(bottom !=0 && bottom > top){
			UINT16 spr_pri=(source[4])&0xf; /* ?? */
			UINT16 bank=(source[4]>>4) &0xf;
			UINT16 pal=(source[4]>>8)&0x3f;
			UINT16 tsource[4];
			UINT16 width;
			int gfx;
			tsource[2]=source[2];
			tsource[3]=source[3];
	#ifndef TRANSPARENT_SHADOWS
			if( pal==0x3f ) pal = (bank<<1); // shadow sprite
	#endif
			if((tsource[3] & 0x7f80) == 0x7f80){
				bank=(bank-1)&0xf;
				tsource[3]^=0x8000;
			}
			tsource[2] &= 0x00ff;
			if (tsource[3]&0x8000){ // reverse
				tsource[2] |= 0x0100;
				tsource[3] &= 0x7fff;
			}
			gfx = tsource[3]*4;
			width = tsource[2];
			top++;
			bottom++;
			sprite->x = source[1] + sys16_sprxoffset;
			if(sprite->x > 0x140) sprite->x-=0x200;
			sprite->y = top;
			sprite->priority = spr_pri;
			sprite->color = 1024/16 + pal;
			sprite->screen_height = bottom-top;
			sprite->pitch = width&0xff;
			sprite->flags = SYS16_SPR_VISIBLE;
			if( width&0x100 ) sprite->flags |= SYS16_SPR_FLIPX;
	#ifdef TRANSPARENT_SHADOWS
			if( pal==0x3f ) sprite->flags|= SYS16_SPR_SHADOW; // shadow sprite
	#endif
			sprite->gfx = ((gfx &0x3ffff) + (sys16_obj_bank[bank] << 17))/2;
		}
		return 0;
	}
	
	int sys16_sprite_hangon( struct sys16_sprite_attributes *sprite, const UINT16 *source, int bJustGetColor ){
	/*
		0	YYYYYYYY YYYYYYYY	bottom, top
		1	-------X XXXXXXXX	xpos
		2	-------- -WWWWWWW	pitch
		3	FTTTTTTT TTTTTTTT	gfx
		4	--CCCCCC ZZZZZZ--	zoomx
		5	-------- --------
		6	-------- --------
		7	-------- --------
	*/
		UINT16 ypos = source[0];
		int top = ypos&0xff;
		int bottom = ypos>>8;
		if( bottom == 0xff ) return 1; /* end of spritelist marker */
		if(bottom !=0 && bottom > top){
			UINT16 bank=(source[1]>>12);
			UINT16 pal=(source[4]>>8)&0x3f;
			UINT16 tsource[4];
			UINT16 width;
			int gfx;
			int zoomx,zoomy;
			tsource[2]=source[2];
			tsource[3]=source[3];
			zoomx=((source[4]>>2) & 0x3f) *(1024/64);
			zoomy = (1060*zoomx)/(2048-zoomx);
			if((tsource[3] & 0x7f80) == 0x7f80){
				bank=(bank-1)&0xf;
				tsource[3]^=0x8000;
			}
			if (tsource[3]&0x8000){ // reverse
				tsource[2] |= 0x0100;
				tsource[3] &= 0x7fff;
			}
			gfx = tsource[3]*4;
			width = tsource[2];
			sprite->x = ((source[1] & 0x3ff) + sys16_sprxoffset);
			if(sprite->x >= 0x200) sprite->x-=0x200;
			sprite->y = top;
			sprite->priority = 0;
			sprite->color = 1024/16 + pal;
			sprite->screen_height = bottom-top;
			sprite->pitch = width&0xff;
			sprite->flags = SYS16_SPR_VISIBLE;
			if( width&0x100 ) sprite->flags |= SYS16_SPR_FLIPX;
	//			sprite->flags|= SYS16_SPR_PARTIAL_SHADOW;
	//			sprite->shadow_pen=10;
			sprite->zoomx = zoomx;
			sprite->zoomy = zoomy;
			sprite->gfx = ((gfx &0x3ffff) + (sys16_obj_bank[bank] << 17))/2;
		}
		return 0;
	}
	
	int sys16_sprite_sharrier( struct sys16_sprite_attributes *sprite, const UINT16 *source, int bJustGetColor ){
	/*
		0	YYYYYYYY	YYYYYYYY	bottom, top
		1	BBBB---X	XXXXXXXX	bank, xpos
		2	--CCCCCC	WWWWWWWW	color, width
		3	FTTTTTTT	TTTTTTTT	gfx
		4	--------	--ZZZZZZ	zoom
		5	--------	--------
		6	--------	--------
		7	--------	--------
	*/
		UINT16 ypos = source[0];
		int top = ypos&0xff;
		int bottom = ypos>>8;
		if( bottom == 0xff ) return 1; /* end of spritelist marker */
		if( bottom !=0 && bottom > top ){
			int bank=(source[1]>>12);
			sprite->color = 1024/16 + ((source[2]>>8)&0x3f);
			sprite->zoomx = (source[4]&0x3f)*(1024/64);
			sprite->zoomy = (1024*sprite->zoomx)/(2048-sprite->zoomx);
	#ifndef TRANSPARENT_SHADOWS
	//		if (pal==0x3f) pal=(bank<<1); // shadow sprite
	#endif
			sprite->x = ((source[1] & 0x3ff) + sys16_sprxoffset);
			if(sprite->x >= 0x200) sprite->x-=0x200;
			sprite->y = top+1;
			sprite->priority = 0;
			sprite->screen_height = bottom-top;
			sprite->pitch = (source[2]&0x7f)*2;
			sprite->flags = SYS16_SPR_VISIBLE;
			if( source[3]&0x8000 ) sprite->flags |= SYS16_SPR_FLIPX;
	#ifdef TRANSPARENT_SHADOWS
			if (sys16_sh_shadowpal == 0){ // space harrier
				if( pal==sys16_sh_shadowpal ) sprite->flags|= SYS16_SPR_SHADOW;
			}
			else { // enduro
				sprite->flags|= SYS16_SPR_PARTIAL_SHADOW;
				sprite->shadow_pen=10;
			}
	#endif
			sprite->gfx = ((bank<<15)|(source[3]&0x7fff))*4;// + (sys16_obj_bank[bank] << 17);
		}
		return 0;
	}
	
	int sys16_sprite_outrun( struct sys16_sprite_attributes *sprite, const UINT16 *source, int bJustGetColor ){
	/*		case 7: // Outrun
		0	??---BBB	YYYYYYYY
		1	TTTTTTTT	TTTTTTTT
		2	WWWWWWWX	XXXXXXXX
		3	-1----ZZ	ZZZZZZZZ
		4	-F?---ZZ	ZZZZZZZZ
		5	HHHHHHHH	-CCCCCCC
		6	--------	--------
		7	--------	--------
	*/
		if( source[0]&0x8000 ){ /* end of spritelist */
			return 1;
		}
		else if( source[0]&0x4000 ){ /* hidden sprite */
			return 0;
		}
		else {//if (!(source[0]&0x4000)){
			int zoomx = source[3];
			int zoomy = source[4];
			int x = (source[2]&0x1ff);
			int bank = (source[0]>>9)&7;
			int gfx = (source[1]+(bank<<16))*4;
			sprite->flags = SYS16_SPR_VISIBLE;
			zoomx&=0x3ff;
			zoomy&=0x3ff;
			if(zoomx==0) zoomx=1;
			if(zoomy==0) zoomy=1;
			sprite->y = source[0]&0xff;
			sprite->priority = 3;
			sprite->color = 0x80 + (source[5]&0x7f);
			sprite->screen_height = (source[5]>>8)+1;
			sprite->pitch = (source[2]>>8)&0xfe; /* 32 bit sprites */
			if( (source[4]&0x4000)==0 ) sprite->flags |= SYS16_SPR_FLIPX;
			if( (source[4]&0x2000)==0 ) sprite->flags |= SYS16_SPR_DRAW_TO_LEFT;
			if( (source[4]&0x8000)==0 ) sprite->flags |= SYS16_SPR_DRAW_TO_TOP;
			sprite->x = x + sys16_sprxoffset;
			sprite->zoomx = zoomx;
			sprite->zoomy = zoomy;
			sprite->gfx = gfx;
	#ifdef TRANSPARENT_SHADOWS
			if( pal==0 ) sprite->flags|= SYS16_SPR_SHADOW;
			else if( source[3]&0x4000 ){
				sprite->flags|= SYS16_SPR_PARTIAL_SHADOW;
				sprite->shadow_pen=10;
			}
	#endif
		}
		return 0;
	}
	
	int sys16_sprite_aburner( struct sys16_sprite_attributes *sprite, const UINT16 *source, int bJustGetColor ){
	/*		case 9: aburner
		0	EH--BBB1	YYYYYYYY	end-of-list, hide, bank, screen ypos
		1	TTTTTTTT	TTTTTTTT	gfx dword offset
		2	WWWWWWWX	XXXXXXXX	pitch, screen xpos
		3	------ZZ	ZZZZZZZZ	zoomx
		4	UFL---ZZ	ZZZZZZZZ	draw-to-top, flipx, draw-to-left, zoomy
		5	--------	HHHHHHHH	screen height
		6	--------	CCCCCCCC
		7	--------	--------
	*/
		if( source[0]&0x8000 ){ /* end of spritelist */
			return 1;
		}
		else if( source[0]&0x4000 ){ /* hidden sprite */
			return 0;
		}
		else {
			int zoomx = source[3];
			int zoomy = source[4];
			int x = (source[2]&0x1ff);
			int bank = (source[0]>>9)&7;
			int gfx = (source[1]+(bank<<16))*4;
			sprite->flags = SYS16_SPR_VISIBLE;
			zoomx&=0x3ff;
			zoomy&=0x3ff;
			if(zoomx==0) zoomx=1;
			if(zoomy==0) zoomy=1;
			sprite->y = source[0]&0xff;
			sprite->priority = 0;
			sprite->color = source[6]&0xff;
			sprite->screen_height = (source[5]&0xff)+1;
			sprite->pitch = (source[2]>>8)&0xfe; /* 32 bit sprites */
			if( (source[4]&0x4000)==0 ) sprite->flags |= SYS16_SPR_FLIPX;
			if( (source[4]&0x2000)==0 ) sprite->flags |= SYS16_SPR_DRAW_TO_LEFT;
			if( (source[4]&0x8000)==0 ) sprite->flags |= SYS16_SPR_DRAW_TO_TOP;
			sprite->x = x + sys16_sprxoffset;
			sprite->zoomx = zoomx;
			sprite->zoomy = zoomy;
			sprite->gfx = gfx;
		}
		return 0;
	}
}
