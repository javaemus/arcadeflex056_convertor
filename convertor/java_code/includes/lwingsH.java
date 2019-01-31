extern unsigned char *lwings_fgvideoram;
extern unsigned char *lwings_bg1videoram;

int  lwings_vh_start(void);
int  trojan_vh_start(void);
int  avengers_vh_start(void);
void lwings_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh);
void trojan_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh);
void lwings_eof_callback(void);
