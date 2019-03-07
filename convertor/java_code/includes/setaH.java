/***************************************************************************

							-= Seta Hardware =-

***************************************************************************/

/* Variables and functions defined in drivers/seta.c */

void seta_coin_lockout_w(int data);


/* Variables and functions defined in vidhrdw/seta.c */

extern data16_t *seta_vram_0, *seta_vram_1, *seta_vctrl_0;
extern data16_t *seta_vram_2, *seta_vram_3, *seta_vctrl_2;
extern data16_t *seta_vregs;


WRITE16_HANDLER( seta_vram_0_w );
WRITE16_HANDLER( seta_vram_1_w );
WRITE16_HANDLER( seta_vram_2_w );
WRITE16_HANDLER( seta_vram_3_w );
WRITE16_HANDLER( seta_vregs_w );

void blandia_vh_init_palette (unsigned char *palette, unsigned short *colortable,const unsigned char *color_prom);
void gundhara_vh_init_palette(unsigned char *palette, unsigned short *colortable,const unsigned char *color_prom);
void jjsquawk_vh_init_palette(unsigned char *palette, unsigned short *colortable,const unsigned char *color_prom);
void usclssic_vh_init_palette(unsigned char *palette, unsigned short *colortable,const unsigned char *color_prom);
void zingzip_vh_init_palette (unsigned char *palette, unsigned short *colortable,const unsigned char *color_prom);






/* Variables and functions defined in vidhrdw/seta2.c */

extern data16_t *seta2_vregs;

WRITE16_HANDLER( seta2_vregs_w );

void seta2_vh_init_palette(unsigned char *palette, unsigned short *colortable,const unsigned char *color_prom);


/* Variables and functions defined in sndhrdw/seta.c */


READ_HANDLER ( seta_sound_r );

READ16_HANDLER ( seta_sound_word_r );
WRITE16_HANDLER( seta_sound_word_w );

void seta_sound_enable_w(int);

int seta_sh_start(const struct MachineSound *msound);

struct CustomSound_interface seta_sound_interface;
