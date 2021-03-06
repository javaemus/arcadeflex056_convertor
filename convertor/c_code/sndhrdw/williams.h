/***************************************************************************

	Midway/Williams Audio Board

****************************************************************************/

extern const struct Memory_ReadAddress williams_cvsd_readmem[];
extern const struct Memory_WriteAddress williams_cvsd_writemem[];
extern const struct Memory_ReadAddress williams_adpcm_readmem[];
extern const struct Memory_WriteAddress williams_adpcm_writemem[];
extern const struct Memory_ReadAddress williams_narc_master_readmem[];
extern const struct Memory_WriteAddress williams_narc_master_writemem[];
extern const struct Memory_ReadAddress williams_narc_slave_readmem[];
extern const struct Memory_WriteAddress williams_narc_slave_writemem[];
extern const struct Memory_ReadAddress16 williams_dcs_readmem[];
extern const struct Memory_WriteAddress16 williams_dcs_writemem[];


extern struct CustomSound_interface williams_custom_interface;
extern struct YM2151interface williams_cvsd_ym2151_interface;
extern struct YM2151interface williams_adpcm_ym2151_interface;
extern struct DACinterface williams_cvsd_dac_interface;
extern struct DACinterface williams_adpcm_dac_interface;
extern struct DACinterface williams_narc_dac_interface;
extern struct hc55516_interface williams_cvsd_interface;
extern struct OKIM6295interface williams_adpcm_6295_interface_REGION_SOUND1;
extern struct CustomSound_interface williams_dcs_custom_interface;

void williams_cvsd_init(int cpunum, int pianum);
void williams_cvsd_data_w(int data);
void williams_cvsd_reset_w(int state);

void williams_adpcm_init(int cpunum);
void williams_adpcm_data_w(int data);
void williams_adpcm_reset_w(int state);

void williams_narc_init(int cpunum);
void williams_narc_data_w(int data);
void williams_narc_reset_w(int state);

void williams_dcs_init(int cpunum);
int williams_dcs_data_r(void);
int williams_dcs_control_r(void);
void williams_dcs_data_w(int data);
void williams_dcs_reset_w(int state);


#define SOUND_CPU_WILLIAMS_CVSD								\
	{														\
		CPU_M6809 | CPU_AUDIO_CPU,							\
		8000000/4,	/* 2 MHz */								\
		williams_cvsd_readmem,williams_cvsd_writemem,0,0,	\
		ignore_interrupt,1									\
	}

#define SOUND_WILLIAMS_CVSD									\
	{														\
		SOUND_CUSTOM,										\
		&williams_custom_interface							\
	},														\
	{														\
		SOUND_YM2151,										\
		&williams_cvsd_ym2151_interface						\
	},														\
	{														\
		SOUND_DAC,											\
		&williams_cvsd_dac_interface						\
	},														\
	{														\
		SOUND_HC55516,										\
		&williams_cvsd_interface							\
	}


#define SOUND_CPU_WILLIAMS_ADPCM							\
	{														\
		CPU_M6809 | CPU_AUDIO_CPU,							\
		8000000/4,	/* 2 MHz */								\
		williams_adpcm_readmem,williams_adpcm_writemem,0,0,	\
		ignore_interrupt,1									\
	}

#define SOUND_WILLIAMS_ADPCM(rgn)							\
	{														\
		SOUND_CUSTOM,										\
		&williams_custom_interface							\
	},														\
	{														\
		SOUND_YM2151,										\
		&williams_adpcm_ym2151_interface					\
	},														\
	{														\
		SOUND_DAC,											\
		&williams_adpcm_dac_interface						\
	},														\
	{														\
		SOUND_OKIM6295,										\
		&williams_adpcm_6295_interface_##rgn				\
	}


#define SOUND_CPU_WILLIAMS_NARC								\
	{														\
		CPU_M6809 | CPU_AUDIO_CPU,							\
		8000000/4,	/* 2 MHz */								\
		williams_narc_master_readmem,williams_narc_master_writemem,0,0,\
		ignore_interrupt,1									\
	},														\
	{														\
		CPU_M6809 | CPU_AUDIO_CPU,							\
		8000000/4,	/* 2 MHz */								\
		williams_narc_slave_readmem,williams_narc_slave_writemem,0,0,\
		ignore_interrupt,1									\
	}

#define SOUND_WILLIAMS_NARC									\
	{														\
		SOUND_CUSTOM,										\
		&williams_custom_interface							\
	},														\
	{														\
		SOUND_YM2151,										\
		&williams_adpcm_ym2151_interface					\
	},														\
	{														\
		SOUND_DAC,											\
		&williams_narc_dac_interface						\
	},														\
	{														\
		SOUND_HC55516,										\
		&williams_cvsd_interface							\
	}
#define SOUND_CPU_WILLIAMS_DCS								\
	{														\
		CPU_ADSP2105 | CPU_AUDIO_CPU,						\
		10240000,	/* 10.24 MHz */							\
		williams_dcs_readmem,williams_dcs_writemem,0,0,		\
		ignore_interrupt,0									\
	}

#define SOUND_WILLIAMS_DCS									\
	{														\
		SOUND_CUSTOM,										\
		&williams_dcs_custom_interface						\
	}
