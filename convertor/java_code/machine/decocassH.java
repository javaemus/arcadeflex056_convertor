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
extern void decocass_init_machine(void);
extern void ctsttape_init_machine(void);
extern void clocknch_init_machine(void);
extern void ctisland_init_machine(void);
extern void csuperas_init_machine(void);
extern void castfant_init_machine(void);
extern void cluckypo_init_machine(void);
extern void cterrani_init_machine(void);
extern void cexplore_init_machine(void);
extern void cprogolf_init_machine(void);
extern void cmissnx_init_machine(void);
extern void cdiscon1_init_machine(void);
extern void cptennis_init_machine(void);
extern void ctornado_init_machine(void);
extern void cbnj_init_machine(void);
extern void cburnrub_init_machine(void);
extern void cbtime_init_machine(void);
extern void cgraplop_init_machine(void);
extern void clapapa_init_machine(void);
extern void cfghtice_init_machine(void);
extern void cprobowl_init_machine(void);
extern void cnightst_init_machine(void);
extern void cprosocc_init_machine(void);
extern void cppicf_init_machine(void);
extern void cscrtry_init_machine(void);
extern void cbdash_init_machine(void);

extern extern extern extern 
/* from drivers/decocass.c */
extern 
/* from vidhrdw/decocass.c */
extern extern extern extern extern extern extern extern extern extern extern 
extern extern extern extern extern extern extern extern extern extern extern extern 
extern int decocass_vh_start (void);
extern void decocass_vh_stop (void);
extern void decocass_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh);

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

