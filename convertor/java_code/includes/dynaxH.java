/***************************************************************************

						-= Dynax / Nakanihon Games =-

***************************************************************************/

/***************************************************************************


								Interrupts


***************************************************************************/

/* Variables defined in drivers: */

extern UINT8 dynax_blitter_irq;

/* Functions defined in drivers: */


/***************************************************************************


								Video Blitter(s)


***************************************************************************/

/* Variables defined in vidhrdw: */

extern UINT32 dynax_blit_reg;

extern UINT32 dynax_blit_x;
extern UINT32 dynax_blit_y;
extern UINT32 dynax_blit_scroll_x;
extern UINT32 dynax_blit_scroll_y;

extern UINT32 dynax_blit_address;
extern UINT32 dynax_blit_dest;

extern UINT32 dynax_blit_pen;
extern UINT32 dynax_blit_backpen;
extern UINT32 dynax_blit_palettes;
extern UINT32 dynax_blit_palbank;

extern UINT32 dynax_blit_enable;

extern UINT8 *dynax_bitmap[3][2];

/* Functions defined in vidhrdw: */






