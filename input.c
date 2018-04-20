#include <sys/prex.h>
#include <sys/signal.h>
#include <stdio.h>
#include <time.h>
#include <unistd.h>
#include <termios.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>

#define STACKLEN	512

static char stack[2][STACKLEN];
struct termios orig_termios;
device_t trm;
thread_t thread_A;
thread_t thread_B;
u_long unix_time;
int hours;
int min;
int sec;
int HH=0,MM=0,SS=0,DD=0;

void reset_terminal_mode();
void set_conio_terminal_mode();
int kbhit();
int getch();
u_long get_time();

void reset_terminal_mode()
{
	
	int O_WRONLY;
    if (device_open("tty",O_WRONLY,&trm))
    {
    panic("open error: tty");
	}
    device_ioctl(trm, TIOCSETA, &orig_termios); 
	device_close(trm);
}

void set_conio_terminal_mode()
{
    struct termios new_termios;
    int O_RDWR;
    if (device_open("tty",O_RDWR,&trm))
    {
    panic("open error: tty");
	}
	/* take two copies - one for now, one for later*/
    device_ioctl(trm,TIOCGETA , &orig_termios);
    memcpy(&new_termios, &orig_termios, sizeof(new_termios));

    /* register cleanup handler, and set the new terminal mode*/ 
    atexit(reset_terminal_mode);
    new_termios.c_iflag &= ~(IGNBRK | BRKINT | PARMRK | ISTRIP
                           | INLCR | IGNCR | ICRNL | IXON);
    new_termios.c_oflag &= ~OPOST;
    new_termios.c_lflag &= ~(ECHO | ECHONL | ICANON | ISIG | IEXTEN);
    new_termios.c_cflag &= ~(CSIZE | PARENB);
    new_termios.c_cflag |= CS8;
    device_ioctl(trm, TIOCSETA, &new_termios);
    device_close(trm);
}

int kbhit()
{
    int number;
    int O_RDONLY;
    if (device_open("tty",O_RDONLY,&trm))
    {
    panic("open error: tty");
	}
    device_ioctl(trm,TIOCINQ,&number); /*read number of bytes in the input buffer*/
    device_close(trm);
    return number;
    
}

int getch()
{
	size_t size;
    int r;
    unsigned char c;
    int O_RDONLY;
    if(device_open("tty",O_RDONLY,&trm))
		panic("open error: tty");
    if ((r = device_read(trm, &c, &size,0)) < 0) { /*read from terminal size number of bytes*/
        device_close(trm);
        return r;
    } 
    else {
		device_close(trm);
        return c;
    }
}
void stopwatch(){
	u_long remain;
	char c;
	int loops=0;
	for( ; ; ){
			while(!kbhit()){
				loops+=1;
				if(sec>59) /*--if seconds are > 59--*/
					{
						min+=1; /*--increment minute by 1--*/
						sec=0;
					}
				if(min>59) /*--if minutes are > 59--*/
					{
						hours+=1; /*--increment hour by 1--*/
						min=0;
					}
				if(hours>23) /*--if hours are > 23--*/
					{
						hours=0; /*-Hour to 0--*/
						min=0;	/*-Min to 0--*/ 
						sec=0;	/*-Sec to 0--*/
					}   
				if(DD>99) /*--if stopwatch centiseconds are > 59--*/
					{
						SS+=1; /*--increment stopwatch seconds by 1--*/
						DD=0;
					}
				if(SS>59) /*--if stopwatch seconds are > 59--*/
					{
						MM+=1; /*--increment stopwatch minutes by 1--*/
						SS=0;
					}
				if(MM>59) /*--if stopwatch minutes are > 59--*/
					{
						HH+=1; /*--increment stopwatch hours by 1--*/
						MM=0;
					}		
				printf("\33[2J"); /*clear screen*/
				printf("\r%u:%u:%u:%u", HH, MM, SS, DD);
				timer_sleep(10,&remain); /*sleep for 10 miliseconds*/
				DD+=1; /*--increment stopwatch centiseconds by 1--*/
				if(loops==100){ /*100 loops=100*10ms=1sec*/
					sec+=1; 
					loops=0;
				}
			}   
			c=getch();
			if(c == 'r' || c == 'R'){
						DD=SS=MM=HH=0; /*reset stopwatch*/
						for( ; ;){
							printf("\33[2J"); /*clear screen*/ 
							while(!kbhit())
								printf("\r%u:%u:%u:%u", HH, MM, SS, DD); /*wait untill s is hit to restart the stopwatch*/ 
							c=getch();
							if(c == 't' || c == 'T'){
								if (thread_resume(thread_A) != 0)
									panic("error:cannot resume thread"); /* resume thread_A  */
								if (thread_suspend(thread_B) !=0); 
									panic("error:cannot suspend thread"); /* suspend thread_B  */
							}
							else if(c == 's' || c == 'S')
								break;
					    }
						continue;	
				}
			else if(c == 's' || c == 'S'){
						for( ; ;){
							while(!kbhit())
								printf("\r%u:%u:%u:%u", HH, MM, SS, DD); /*print current stopwatch time and wait untill s is hit*/	
							c=getch();
							if(c == 's' || c == 'S')
								break;
					    }
						continue;
				}
			else if(c == 'p' || c == 'P'){
						for( ; ;){
							while(!kbhit()){ /*continue counting without updating print untill p is hit again*/
									if(DD>99)
									{
										SS+=1;
										DD=0;
									}
									if(SS>59) 
									{
										MM+=1; 
										SS=0;
									}
									if(MM>59) 
									{
										HH+=1; 
										MM=0;
									}
									timer_sleep(10,&remain);
									DD+=1;	
							}
							c=getch();
							if(c == 'p'|| c == 'P')
								break;
						}	
						continue;
			}
			else if(c == 't' || c == 'T'){
						/*device_close(cpu);*/
						if (thread_resume(thread_A) != 0)
								panic("error:cannot resume thread");	
						if (thread_suspend(thread_B) !=0); 
								panic("error:cannot suspend thread");
						
			}
    }
	
}	

u_long get_time() {
    device_t rtc_dev;
	u_long sec;

	if(device_open("rtc", 0, &rtc_dev))
		panic("open error: rtc");
  	device_ioctl(rtc_dev, RTCIOC_GET_TIME, &sec); /*get from rtc unix epoch time*/
	device_close(rtc_dev);
	return sec;
}


void clock_function()
{	
	char c;
	int loops=0;
	u_long remain;
	u_long unix_time = get_time();
	hours=(unix_time%86400)/3600+3; /*convert unix epoch time to current time in hours minutes and seconds*/
	min=((unix_time%86400)%3600)/60;
	sec=((unix_time%86400)%3600)%60;
	for( ; ; ){
			while(!kbhit()){
				loops+=1;
				if(sec>59) 
					{
						min+=1; 
						sec=0;
					}
				if(min>59) 
					{
						hours+=1; 
						min=0;
					}
				if(hours>23) 
					{
						hours=0; 
						min=0;
						sec=0;
					}   
				if(DD>99)
					{
						SS+=1;
						DD=0;
					}
				if(SS>59) 
					{
						MM+=1; 
						SS=0;
					}
				if(MM>59) 
					{
						HH+=1; 
						MM=0;
					}		
				printf("\33[2J"); /*clear screen*/
				printf("\r%u:%u:%u", hours, min, sec); /*print time*/
				timer_sleep(10,&remain); /*sleep for 10 miliseconds*/
				DD+=1;
				if(loops==100){ /*100 loops=100*10ms=1sec*/
					sec+=1;
					loops=0;
				}
			}   
			c=getch();
			if(c == 'h' || c == 'H'){
						hours+=1; /*--increase hours by 1--*/
						continue;	
				}
				else if(c == 'm' || c == 'M'){
						min+=1;	/*--increase minutes by 1--*/
						continue;
				}
				else if(c == 'z' || c == 'Z'){
						sec=0; /*--increase seconds by 1--*/
						continue;
				}
				else if(c == 't' || c == 'T'){
						if (thread_resume(thread_B) != 0)	/* resume thread_B  */
							panic("error:cannot resume thread"); 
						if (thread_suspend(thread_A) !=0); /* suspend thread_A  */
							panic("error:cannot suspend thread");
						
				}
    }
}

int main(void) {
	
	set_conio_terminal_mode(); /*set terminal to raw mode*/
	if (thread_create(task_self(), &thread_A) != 0) /*create thread A  */
		return 0;
	if (thread_create(task_self(), &thread_B) != 0) /*create thread B  */
		return 0;
	if (thread_load(thread_A, clock_function, stack[0]+STACKLEN) != 0) /*load state of thread A  */
		return 0;
	if (thread_load(thread_B, stopwatch, stack[1]+STACKLEN) != 0) /*load state of thread b  */
		return 0;
	if (thread_resume(thread_A) != 0) /* thread is initially set to the suspended state so thread_resume() must be called to start it */
		return 0;
	for (;;)
		thread_yield(); /*never return*/	
    return 0; /* NOTREACHED */
}

