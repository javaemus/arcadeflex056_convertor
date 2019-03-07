/* set to 1 to display tape time offset */
#define TAPE_UI_DISPLAY 0

#ifdef MAME_DEBUG
#define LOGLEVEL  0
#define LOG(n,x)  if (LOGLEVEL >= n) logerror x
#else
#define LOG(n,x)
#endif

extern extern extern extern extern extern extern extern extern extern extern extern extern extern extern int tape_dir;
extern int tape_speed;
extern double tape_time0;
extern void *tape_timer;

extern extern extern extern extern extern extern extern extern 
extern extern 
extern extern extern extern extern extern extern extern extern extern extern extern extern extern extern extern extern extern extern extern extern extern extern extern extern extern 
extern extern extern extern 
/* from drivers/decocass.c */
extern 
/* from vidhrdw/decocass.c */
extern extern extern extern extern extern extern extern extern extern extern 
extern extern extern extern extern extern extern extern extern extern extern extern 
extern extern extern 
extern unsigned char *decocass_charram;
extern unsigned char *decocass_fgvideoram;
extern unsigned char *decocass_colorram;
extern unsigned char *decocass_bgvideoram;
extern unsigned char *decocass_tileram;
extern unsigned char *decocass_objectram;
extern size_t decocass_fgvideoram_size;
extern size_t decocass_colorram_size;
extern size_t decocass_bgvideoram_size;
extern size_t decocass_tileram_size;
extern size_t decocass_objectram_size;

