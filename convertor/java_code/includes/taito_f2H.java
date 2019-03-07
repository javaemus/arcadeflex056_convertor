
extern data16_t *f2_sprite_extension;
extern size_t f2_spriteext_size;

void taitof2_vh_stop (void);
void taitof2_no_buffer_eof_callback(void);
void taitof2_full_buffer_delayed_eof_callback(void);
void taitof2_partial_buffer_delayed_eof_callback(void);
void taitof2_partial_buffer_delayed_thundfox_eof_callback(void);
void taitof2_partial_buffer_delayed_qzchikyu_eof_callback(void);

void taitof2_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh);
void taitof2_pri_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh);
void taitof2_pri_roz_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh);
void ssi_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh);
void thundfox_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh);
void deadconx_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh);
void metalb_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh);
void yesnoj_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh);

WRITE16_HANDLER( taitof2_spritebank_w );
READ16_HANDLER ( koshien_spritebank_r );
WRITE16_HANDLER( koshien_spritebank_w );
WRITE16_HANDLER( taitof2_sprite_extension_w );

extern data16_t *cchip_ram;
READ16_HANDLER ( cchip2_word_r );
WRITE16_HANDLER( cchip2_word_w );

