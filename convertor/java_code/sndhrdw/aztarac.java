/*
 * Aztarac soundboard interface emulation
 *
 * Jul 25 1999 by Mathis Rosenhauer
 *
 */

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package sndhrdw;

public class aztarac
{
	
	static int sound_status;
	
	READ16_HANDLER( aztarac_sound_r )
	{
	    if (Machine->sample_rate)
	        return sound_status & 0x01;
	    else
	        return 1;
	}
	
	WRITE16_HANDLER( aztarac_sound_w )
	{
		if (ACCESSING_LSB)
		{
			data &= 0xff;
			soundlatch_w(offset, data);
			sound_status ^= 0x21;
			if (sound_status & 0x20)
				cpu_cause_interrupt( 1, Z80_IRQ_INT );
		}
	}
	
	public static ReadHandlerPtr aztarac_snd_command_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	    sound_status |= 0x01;
	    sound_status &= ~0x20;
	    return soundlatch_r(offset);
	} };
	
	public static ReadHandlerPtr aztarac_snd_status_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	    return sound_status & ~0x01;
	} };
	
	public static WriteHandlerPtr aztarac_snd_status_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	    sound_status &= ~0x10;
	} };
	
	int aztarac_snd_timed_irq (void)
	{
	    sound_status ^= 0x10;
	
	    if (sound_status & 0x10)
	        return interrupt();
	    else
	        return ignore_interrupt();
	}
	
	
}
