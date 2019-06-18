/***************************************************************************


***************************************************************************/

/* Variables defined in vidhrdw */


extern data16_t *cave_videoregs;

extern data16_t *cave_vram_0, *cave_vctrl_0;
extern data16_t *cave_vram_1, *cave_vctrl_1;
extern data16_t *cave_vram_2, *cave_vctrl_2;

/* Functions defined in vidhrdw */

WRITE16_HANDLER( cave_vram_0_w );
WRITE16_HANDLER( cave_vram_1_w );
WRITE16_HANDLER( cave_vram_2_w );

WRITE16_HANDLER( cave_vram_0_8x8_w );
WRITE16_HANDLER( cave_vram_1_8x8_w );
WRITE16_HANDLER( cave_vram_2_8x8_w );






void sailormn_tilebank_w( int bank );
