#ifndef YM2413INTF_H
#define YM2413INTF_H

#define MAX_2413 	(4)

struct YM2413interface
{
	int num;
	int baseclock;
	int mixing_level[MAX_2413];
};


WRITE16_HANDLER( YM2413_register_port_0_lsb_w );
WRITE16_HANDLER( YM2413_register_port_1_lsb_w );
WRITE16_HANDLER( YM2413_register_port_2_lsb_w );
WRITE16_HANDLER( YM2413_register_port_3_lsb_w );
WRITE16_HANDLER( YM2413_data_port_0_lsb_w );
WRITE16_HANDLER( YM2413_data_port_1_lsb_w );
WRITE16_HANDLER( YM2413_data_port_2_lsb_w );
WRITE16_HANDLER( YM2413_data_port_3_lsb_w );

int YM2413_sh_start (const struct MachineSound *msound);

#endif

