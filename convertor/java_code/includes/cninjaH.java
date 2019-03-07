/* Video emulation definitions */
extern data16_t *cninja_pf1_rowscroll,*cninja_pf2_rowscroll;
extern data16_t *cninja_pf3_rowscroll,*cninja_pf4_rowscroll;
extern data16_t *cninja_pf1_data,*cninja_pf2_data;
extern data16_t *cninja_pf3_data,*cninja_pf4_data;


WRITE16_HANDLER( cninja_pf1_data_w );
WRITE16_HANDLER( cninja_pf2_data_w );
WRITE16_HANDLER( cninja_pf3_data_w );
WRITE16_HANDLER( cninja_pf4_data_w );
WRITE16_HANDLER( cninja_control_0_w );
WRITE16_HANDLER( cninja_control_1_w );

WRITE16_HANDLER( cninja_palette_24bit_w );
WRITE16_HANDLER( robocop2_pri_w );
