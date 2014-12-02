#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <pthread.h>
#include <sys/stat.h>
#include <sys/types.h> 
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>



#ifdef __MACH__
#include <sys/time.h>
#define CLOCK_REALTIME 0 
#define CLOCK_MONOTONIC 0
//clock_gettime is not implemented on OSX
int clock_gettime(int clk_id, struct timespec* t) {
    struct timeval now;
    int rv = gettimeofday(&now, NULL);
    if (rv) return rv;
    t->tv_sec  = now.tv_sec;
    t->tv_nsec = now.tv_usec * 1000;
    return 0;
}
#endif





void readFileToMemory(const char * filename);
void networkSend(int count);

int main() {

	readFileToMemory("../fakeData.csv");
	networkSend(0);
	return 0;
}


/* Simple file read */
size_t getFileSize(const char * filename) {
	struct stat st;
	stat(filename, &st);
	return st.st_size;
}

void readFileToMemory(const char * filename) {
	
	size_t size = getFileSize(filename);
	char *mFile = (char*)malloc(size*sizeof(char));
	FILE *f = fopen(filename,"rb");
		
	struct timespec tw1, tw2;
	clock_gettime(CLOCK_MONOTONIC, &tw1);

	for (int i = 0; i < size; ++i) {
		mFile[i] = fgetc(f);
	}
	fclose(f);
	
	clock_gettime(CLOCK_MONOTONIC, &tw2);
	
	free(mFile);
	
	double posix_wall = 1000.0*tw2.tv_sec + 1e-6*tw2.tv_nsec - (1000.0*tw1.tv_sec + 1e-6*tw1.tv_nsec);
	printf("Read time passed: %.2f ms\n", posix_wall);
}


/* Send data over socket */
void* consume(void *arg) {
	int sockfd, newsockfd, portno = 10240;
	socklen_t clilen;
	char buffer[256];
	struct sockaddr_in serv_addr, cli_addr;
	int n, counter = 0;
     
	sockfd = socket(AF_INET, SOCK_STREAM, 0);
     
	bzero((char *) &serv_addr, sizeof(serv_addr));
	serv_addr.sin_family = AF_INET;
	serv_addr.sin_addr.s_addr = INADDR_ANY;
	serv_addr.sin_port = htons(portno);
	bind(sockfd, (struct sockaddr *) &serv_addr, sizeof(serv_addr));
	listen(sockfd,5);
	clilen = sizeof(cli_addr);
	newsockfd = accept(sockfd, (struct sockaddr *) &cli_addr, &clilen);
     
	while (counter < 33423360){
		n = read(newsockfd,buffer,255);
		counter += n;
		bzero(buffer,256);
	}
	close(newsockfd);
	close(sockfd);
	clock_gettime(CLOCK_MONOTONIC, (struct timespec*)arg);
}

void* produce(void *arg) {
	clock_gettime(CLOCK_MONOTONIC, (struct timespec*)arg);
	
	int sockfd, portno= 10240;
    struct sockaddr_in serv_addr;
    struct hostent *server;
    char buffer[256];

    sockfd = socket(AF_INET, SOCK_STREAM, 0);
    server = gethostbyname("localhost");
    
    if (server == NULL) {
        fprintf(stderr,"ERROR, no such host\n");
        exit(0);
    }
    bzero((char *) &serv_addr, sizeof(serv_addr));
    serv_addr.sin_family = AF_INET;
    //serv_addr.sin_addr.s_addr = *(server->h_addr_list[0]);
    bcopy((char *)(server->h_addr), (char *)(&serv_addr.sin_addr.s_addr), (size_t)(server->h_length));
    serv_addr.sin_port = htons(portno);
    connect(sockfd,(struct sockaddr *) &serv_addr,sizeof(serv_addr));
    
    for( int i = 0; i < 131072; ++i) {
		write(sockfd,buffer,255);
	}

    close(sockfd);
}

void networkSend(int count) {
	pthread_t producer, consumer;
	struct timespec tw1, tw2;
	void *status;
	
	pthread_create(&consumer, NULL, &consume, &tw2);
	pthread_create(&producer, NULL, &produce, &tw1);
	
	pthread_join(producer, &status);
	pthread_join(consumer, &status);
	
	double posix_wall = 1000.0*tw2.tv_sec + 1e-6*tw2.tv_nsec - (1000.0*tw1.tv_sec + 1e-6*tw1.tv_nsec);
	printf("Network time passed: %.2f ms\n", posix_wall);
}
