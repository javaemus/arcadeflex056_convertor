/***************************************************************************


***************************************************************************/

/* defined in vidhrdw/ */
extern data8_t *lasso_vram;
extern data8_t *wwjgtin_scroll;


void lasso_vh_convert_color_prom  (unsigned char *palette,unsigned short *colortable,const unsigned char *color_prom);
void wwjgtin_vh_convert_color_prom(unsigned char *palette,unsigned short *colortable,const unsigned char *color_prom);


void lasso_vh_screenrefresh   ( struct mame_bitmap *bitmap, int fullrefresh );
void chameleo_vh_screenrefresh( struct mame_bitmap *bitmap, int fullrefresh );
void wwjgtin_vh_screenrefresh ( struct mame_bitmap *bitmap, int fullrefresh );
