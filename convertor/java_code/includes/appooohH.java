/* appoooh.h */

extern unsigned char *spriteram,*spriteram_2;

/* vidhrdw */
extern unsigned char *appoooh_fg_videoram,*appoooh_fg_colorram;
extern unsigned char *appoooh_bg_videoram,*appoooh_bg_colorram;
void appoooh_vh_convert_color_prom(unsigned char *obsolete,unsigned short *colortable,const unsigned char *color_prom);

