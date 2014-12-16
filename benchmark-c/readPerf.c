#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <pthread.h>
#include <sys/stat.h>
#include <sys/types.h> 
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>

/* OSX time functions */
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

const char * FILENAME = "../fakeData.csv";


void readFileToMemory();
void readFileToMemoryItearting();
void readFileLineByLine();
void networkSend();
void networkSendWithFile();

int main() {

	readFileToMemory();
	readFileToMemoryItearting();
	readFileLineByLine();
	//networkSend();
	networkSendWithFile();
	return 0;
}


/* Get file size*/
size_t getFileSize(const char * filename) {
	struct stat st;
	stat(filename, &st);
	return st.st_size;
}

/* Reads file into memory with fread */
void readFileToMemory() {
	size_t size = getFileSize(FILENAME);
	char *mFile = (char*)malloc(size*sizeof(char));
	FILE *f = fopen(FILENAME,"rb");
	struct timespec tw1, tw2;
	
	clock_gettime(CLOCK_MONOTONIC, &tw1);
	
	fread( mFile, sizeof(char), size, f);
	fclose(f);
	
	clock_gettime(CLOCK_MONOTONIC, &tw2);
	
	free(mFile);
	
	double posix_wall = 1000.0*tw2.tv_sec + 1e-6*tw2.tv_nsec - (1000.0*tw1.tv_sec + 1e-6*tw1.tv_nsec);
	printf("Reads file into memory with fread ==> time passed: %.2f ms\n", posix_wall);
}

/* Read file to memory char by char */
void readFileToMemoryItearting() {
	size_t size = getFileSize(FILENAME);
	char *mFile = (char*)malloc(size*sizeof(char));
	FILE *f = fopen(FILENAME,"rb");
		
	struct timespec tw1, tw2;
	clock_gettime(CLOCK_MONOTONIC, &tw1);

	for (int i = 0; i < size; ++i) {
		mFile[i] = fgetc(f);
	}
	fclose(f);
	
	clock_gettime(CLOCK_MONOTONIC, &tw2);
	
	free(mFile);
	
	double posix_wall = 1000.0*tw2.tv_sec + 1e-6*tw2.tv_nsec - (1000.0*tw1.tv_sec + 1e-6*tw1.tv_nsec);
	printf("Read file to memory char by char ==> time passed: %.2f ms\n", posix_wall);
}

/* Read file line by line */
void readFileLineByLine() {
	char *buffer = (char*)malloc(256*sizeof(char));
	FILE *f = fopen(FILENAME,"rb");
	struct timespec tw1, tw2;
	
	clock_gettime(CLOCK_MONOTONIC, &tw1);

	while (fgets(buffer, 256, f) != NULL) {
		int i = 0;
		for (; buffer[i] != '\0' && i < 256; ++i);
		char *line = (char*)malloc(i*sizeof(char));
		free(line);
	}
	
	fclose(f);
	
	clock_gettime(CLOCK_MONOTONIC, &tw2);
	
	double posix_wall = 1000.0*tw2.tv_sec + 1e-6*tw2.tv_nsec - (1000.0*tw1.tv_sec + 1e-6*tw1.tv_nsec);
	printf("Read file line by line ==> time passed: %.2f ms\n", posix_wall);
}

/* Read file line by line */
void readFileLineBuffer() {
	char *buffer = (char*)malloc(256*sizeof(char));
	FILE *f = fopen(FILENAME,"rb");
	struct timespec tw1, tw2;
	
	clock_gettime(CLOCK_MONOTONIC, &tw1);

	while (fgets(buffer, 256, f) != NULL) {
		int i = 0;
		for (; buffer[i] != '\0' && i < 256; ++i);
		char *line = (char*)malloc(i*sizeof(char));
		free(line);
	}
	
	fclose(f);
	
	clock_gettime(CLOCK_MONOTONIC, &tw2);
	
	double posix_wall = 1000.0*tw2.tv_sec + 1e-6*tw2.tv_nsec - (1000.0*tw1.tv_sec + 1e-6*tw1.tv_nsec);
	printf("Read file line by line ==> time passed: %.2f ms\n", posix_wall);
}

/* Send data over socket */
void* consume(void *arg) {
	int sockfd, newsockfd, portno = 10240;
	socklen_t clilen;
	char buffer[256];
	struct sockaddr_in serv_addr, cli_addr;
	int n, counter = 0;
	size_t size = getFileSize(FILENAME);
     
	sockfd = socket(AF_INET, SOCK_STREAM, 0);
     
	bzero((char *) &serv_addr, sizeof(serv_addr));
	serv_addr.sin_family = AF_INET;
	serv_addr.sin_addr.s_addr = INADDR_ANY;
	serv_addr.sin_port = htons(portno);
	bind(sockfd, (struct sockaddr *) &serv_addr, sizeof(serv_addr));
	listen(sockfd,5);
	clilen = sizeof(cli_addr);
	newsockfd = accept(sockfd, (struct sockaddr *) &cli_addr, &clilen);

	do {
		n = read(newsockfd,buffer,255);
		counter += n;
		bzero(buffer,256);
	} while (counter < size);
	
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
    size_t size = getFileSize(FILENAME);

    sockfd = socket(AF_INET, SOCK_STREAM, 0);
    server = gethostbyname("localhost");
    
    if (server == NULL) {
        fprintf(stderr,"ERROR, no such host\n");
        exit(0);
    }
    bzero((char *) &serv_addr, sizeof(serv_addr));
    serv_addr.sin_family = AF_INET;
    bcopy((char *)(server->h_addr), (char *)(&serv_addr.sin_addr.s_addr), (size_t)(server->h_length));
    serv_addr.sin_port = htons(portno);
    connect(sockfd,(struct sockaddr *) &serv_addr,sizeof(serv_addr));
    
    for( int i = 0; i < (1024*1024*30)/255; ++i) {
		write(sockfd,&buffer,255);
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
	printf("networkSend ==> Network time passed: %.2f ms\n", posix_wall);
}



/* Send data over socket */
void* consumeLineByLine(void *arg) {
	int sockfd, newsockfd, portno = 10240;
	socklen_t clilen;
	const int BUFFER_SIZE = 1024000;
	char buffer[BUFFER_SIZE];
	struct sockaddr_in serv_addr, cli_addr;
	int n, counter = 0;
    size_t size = getFileSize(FILENAME);
     
	sockfd = socket(AF_INET, SOCK_STREAM, 0);
     
	bzero((char *) &serv_addr, sizeof(serv_addr));
	serv_addr.sin_family = AF_INET;
	serv_addr.sin_addr.s_addr = INADDR_ANY;
	serv_addr.sin_port = htons(portno);
	bind(sockfd, (struct sockaddr *) &serv_addr, sizeof(serv_addr));
	listen(sockfd,5);
	clilen = sizeof(cli_addr);
	newsockfd = accept(sockfd, (struct sockaddr *) &cli_addr, &clilen);
     
     do {
		n = read(newsockfd,buffer,BUFFER_SIZE);
		counter += n;
		for (int i = 0; buffer[i] != '\0' && i < n; ++i);
		char *line = (char*)malloc(n*sizeof(char));
		free(line);
		bzero(buffer,BUFFER_SIZE);
	} while (counter < size);
   
	close(newsockfd);
	close(sockfd);
	clock_gettime(CLOCK_MONOTONIC, (struct timespec*)arg);
}

void* produceFromFile(void *arg) {
	clock_gettime(CLOCK_MONOTONIC, (struct timespec*)arg);
	
	int sockfd, portno= 10240;
    struct sockaddr_in serv_addr;
    struct hostent *server;
    const int BUFFER_SIZE = 4096;
    char buffer[BUFFER_SIZE];
    
	FILE *f = fopen(FILENAME,"r");
	struct timespec tw1, tw2;
	

    sockfd = socket(AF_INET, SOCK_STREAM, 0);
    server = gethostbyname("localhost");
    
    if (server == NULL) {
        fprintf(stderr,"ERROR, no such host\n");
        exit(0);
    }
    bzero((char *) &serv_addr, sizeof(serv_addr));
    serv_addr.sin_family = AF_INET;
    bcopy((char *)(server->h_addr), (char *)(&serv_addr.sin_addr.s_addr), (size_t)(server->h_length));
    serv_addr.sin_port = htons(portno);
    connect(sockfd,(struct sockaddr *) &serv_addr,sizeof(serv_addr));
    
    size_t read = 0;
    while((read = fread( &buffer, 1, BUFFER_SIZE, f)) != 0) {
		write(sockfd,buffer,read);
	}
	
	fclose(f);
    close(sockfd);
}

void networkSendWithFile() {
	pthread_t producer, consumer;
	struct timespec tw1, tw2;
	void *status;
	
	pthread_create(&consumer, NULL, &consumeLineByLine, &tw2);
	pthread_create(&producer, NULL, &produceFromFile, &tw1);
	
	pthread_join(producer, &status);
	pthread_join(consumer, &status);
	
	double posix_wall = 1000.0*tw2.tv_sec + 1e-6*tw2.tv_nsec - (1000.0*tw1.tv_sec + 1e-6*tw1.tv_nsec);
	printf("networkSendWithFile ==> Network time passed: %.2f ms\n", posix_wall);
}
