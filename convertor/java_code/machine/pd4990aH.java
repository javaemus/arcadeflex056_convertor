/*
 *	Header file for the PD4990A Serial I/O calendar & clock.
 */
struct pd4990a_s
{
	int seconds;
	int minutes;
	int hours;
	int days;
	int month;
	int year;
	int weekday;
};

extern struct pd4990a_s pd4990a;

void pd4990a_addretrace (void);
WRITE_HANDLER( pd4990a_control_w );
WRITE16_HANDLER( pd4990a_control_16_w );
void pd4990a_increment_day(void);
void pd4990a_increment_month(void);

